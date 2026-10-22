# Design

## Context

动机与用户可见范围见 [proposal.md](proposal.md)，行为契约见 [integration-credentials spec](specs/integration-credentials/spec.md)。当前 `ShowDocSettings`、`YApiSettings`、`YuQueSettings` 是项目级持久化状态，分别含 ShowDoc key/token、YApi token 和 YuQue token；三个设置表单使用普通文本控件并直接读写这些字段。三类上传服务从设置状态取得凭据。`DocViewUploadService.upload()` 目前在进入后台任务前检查设置，`PreviewForm` 另有直接调用 `doUpload()` 的入口。

项目使用 Java-only 生产代码和 IntelliJ IDEA IU 2026.1 基线。目标 SDK 本地 API 检查确认 `PasswordSafe`、`CredentialAttributes`、`Credentials`、`generateServiceName()`、`Project.getLocationHash()`、`Task.Modal` 与 `ProgressManager.run(Task)` 在 IU 261 可用。IntelliJ 官方文档指出 `PasswordSafe` 的读取和写入会阻塞且不得在 EDT 调用；`getAsync()`/`Ephemeral` 是实验性 Kotlin-only API。IU 261 中 `runProcessWithProgressSynchronously()` 和 `Task.WithResult` 被标记为 obsolete，本设计不依赖它们。

当前活动的 `fix-integration-settings-state` 负责空值、URL、YApi ID、Reset 和设置导航；凭据变更需要在其完成后协同落到相同表单与服务。`PreviewForm` 的上传调用由 `fix-preview-lifecycle` 当前文件所有者维护，本 change 先记录接入依赖，实施时避免并行编辑该文件。

实现依据： [Persisting Sensitive Data](https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html)、[Background Processes](https://plugins.jetbrains.com/docs/intellij/background-processes.html)、[Threading Model](https://plugins.jetbrains.com/docs/intellij/threading-model.html)。

## Goals / Non-Goals

**Goals:**

- 将当前版本继续保存的集成凭据移到 IntelliJ PasswordSafe，并以项目和平台隔离。
- 让用户能从密码控件编辑凭据、明确选择清除，并在 Apply 失败时保留草稿。
- 保证安全存储访问在后台进行，上传入口不从 EDT 取凭据。
- 让新版持久化的项目设置只包含非敏感字段，同时保留现有文件名和普通设置值。

**Non-Goals:**

- 不读取、迁移或自动清理升级前 XML 中的凭据；不增加恢复旧版凭据的兼容路径。
- 不改变 ShowDoc、YApi、YuQue 请求 URL、payload、headers、上传结果或凭据校验含义。
- 不调整集成设置布局、普通字段校验或路由；不增加连接测试、第三方依赖或新的平台。
- 不建设跨上传任务的明文凭据缓存或通用凭据管理框架。

## Decisions

### 1. 用项目服务封装 PasswordSafe，密钥由项目与平台组成

新增一个 project-level `IntegrationCredentialService`，集中生成 `CredentialAttributes` 并读写 PasswordSafe。`serviceName` 必须按 `Project.getLocationHash()` 与固定平台标识共同隔离，`userName` 保持项目 hash；身份中不含项目原始路径、项目名称、账号或凭据。这样不依赖所有 PasswordSafe backend 都把用户名当成独立条目键。目标 IU 261 KeePass 后端对非 `IntelliJ Platform` service 的写入会先按 serviceName 与空用户名查找现有条目，复用条目时再更新其用户名；若不同项目共用 serviceName，后写项目可能让前一项目按原用户名无法读取。服务名按项目与平台区分后，每个项目凭据使用独立的后端条目。项目身份或 PasswordSafe profile 不再可用时，凭据显示为未配置，用户可重新录入；插件不尝试搜索其他项目凭据。

服务保留一个窄的 package-private 存储访问 seam，测试以内存 fake 代替系统钥匙串。它只负责调用 PasswordSafe，不负责长期缓存、重试队列或凭据跨线程共享。凭据服务和所有调用方 SHALL 在调用 `get`、`set` 或移除项之前断言当前执行线程不是 EDT。

ShowDoc 的 key 与 token 使用 PasswordSafe 的一个 password 值保存，值内容用项目已有的 Gson 编码；不把 key 放入 `Credentials.username` 或其他可见 identity metadata。这样替换或清除整组凭据只涉及一个安全存储项。需要只改其中一个字段时，在后台读取现有 bundle、按各字段操作意图合并，再一次写回；读取失败则整个 Apply 失败，不能丢失另一个字段。YApi token 和 YuQue token 各自占用一个安全存储项。移除凭据使用 PasswordSafe 的公开空值删除方式。

选择单项 ShowDoc bundle 而非两个独立 item，是为了避免 key 与 token 写入一半；不使用用户名字段存放 key，因为 PasswordSafe 文档把用户名作为可见身份信息。选择 project service 包装而非直接在表单和上传服务散落调用，可将项目键、EDT 限制和测试 seam 保持在一个边界内；该 service 不成为可配置 extension point。

### 2. 保持现有设置文件，只从新状态移除 secret 字段

保留三个现有项目设置的 `@State` 名称和 XML 文件名，只从新版本状态对象中移除凭据字段；URL、YApi project ID、YuQue login/repos 等普通字段继续按现有存储读取与写入。插件代码不增加旧 XML 解析、字段映射、迁移、备份或删除逻辑。新版正常重写设置状态时序列化结果不再包含凭据字段；在文件被新版重写前，旧 XML 可能继续留有原始明文值。升级用户需重新输入凭据，普通设置仍保留。

不换文件名之外的双状态方案，避免非敏感设置也全部要求重录；不实现“迁移时只取非敏感 XML 字段”的自定义解析，因为它会引入第二套状态读取路径并触及本 change 明确排除的旧配置处理。旧 XML 残留是已接受且需写入升级说明的风险，不由插件自动清理。

### 3. 设置表单只缓存操作意图与凭据状态

把 secret 输入换为 `JBPasswordField`。未输入字符（空字符串）且未勾选清除代表 `KEEP`；输入任何字符（包括空白字符）代表 `REPLACE`，按输入原样保存；用户显式勾选清除代表 `CLEAR`。清除操作仅在 Apply 成功后发生。`isModified()` 将非空草稿或清除意图视为修改，零字符输入不会覆盖或删除已有凭据。

打开或 Reset 设置页时，不在生命周期回调同步访问 PasswordSafe。一个后台读取仅用于得出 `configured`、`not configured` 或 `unavailable` 状态，完成后将状态投回 EDT；只缓存这些布尔/状态值，不保存明文。异步结果使用表单生命周期代次检查，丢弃重置、关闭或较新读取之后到达的旧结果。读取失败只显示不可用状态，不破坏普通字段或用户草稿。不同于新增全局缓存，凭据明文只在单次后台操作或单次上传所需的最窄范围内使用；JVM `String` 无法保证物理擦除，不对内存零化作承诺。

Reset 从最近已应用的普通设置回填字段，清空本次密码草稿并复位清除意图，再异步刷新状态；Cancel 只丢弃未 Apply 的草稿。Apply 成功后清空密码输入并刷新状态，避免继续在表单中保留输入副本。

### 4. 以不可取消的 modal task 完成 Apply 存储，再提交普通状态

`Configurable.apply()` 的 UI 与非敏感表单快照在 EDT 准备；密码字段值只临时复制到一次 Apply 操作数据中，绝不写入 settings state、日志或错误文本。若本次仅改普通设置且没有 secret 替换/清除，可直接应用普通字段；若有凭据操作，则使用目标 IU 261 的公开 `ProgressManager.run(Task)` 与 `Task.Modal` 执行后台安全存储，并在调用返回后才提交该平台普通设置。

modal task 不提供取消按钮。PasswordSafe 调用本身是阻塞的，取消可能发生在安全存储已完成、普通设置尚未提交的间隙且无法原子回滚；不可取消的短期 Apply 把这段不确定窗口限定在存储返回与普通字段赋值之间。Task 失败或 PasswordSafe 报错时，Apply 转换成不含凭据的设置错误，保持普通 state 不变并留住用户草稿，用户可重试。Apply 成功后再次关闭设置窗口的 Cancel 不回滚已 Apply 内容，符合 IntelliJ 设置生命周期。

凭据操作的普通 `RuntimeException` 转换为上述脱敏设置错误；JVM `Error` 不属于可恢复的存储失败。非 EDT 调用直接向上传播 `Error`；EDT 调用在 modal task 中暂存该错误，等待平台完成任务清理后再向调用方传播，不将其误报为普通设置错误。

不用 `getAsync()`/`Ephemeral`，因为目标 Java 代码不能调用其 Kotlin-only 实验 API；不用已标记 obsolete 的 `runProcessWithProgressSynchronously()` 或 `Task.WithResult`。`ProgressManager.run(Task.Modal)` 在目标 IU 261 公共 API 中可用，平台以 modal progress 运行任务并在返回前等待完成；原生验收仍需确认 PasswordSafe/OS 凭据授权提示能在该执行方式下正常完成。若无法完成 OS 授权，不将其记为通过，也不暗中退回 EDT 调用。

PasswordSafe 单项写入成功与 IDEA 项目 XML 落盘不是跨存储事务。正常 Apply 顺序确保 PasswordSafe 报告失败时不提交普通设置；若进程在安全存储成功后、IDE 持久化普通状态前崩溃，不能承诺两处数据同时回滚。此边界会在设计与验收记录中说明。

### 5. 让所有上传入口在后台解析凭据

当前 `DocViewUploadService.upload()` 在 `DocViewBackgroundTasks.runTask()` 之前运行 `checkSettings()`；此结构要调整为在后台任务中执行配置完整性检查、PasswordSafe 解析和上传。三个平台 service 从新凭据服务取得 secret，并仅在原请求 DTO/参数上替换读取来源；既有请求字段与协议保持原样。上传完成或失败后不保存凭据缓存。

`PreviewForm` 和 `PreviewRightUploadAction` 曾直接调用 `doUpload()`，未走 `DocViewUploadService.upload()` 的后台包装。`PreviewForm` 由 `fix-preview-lifecycle` 文件所有者负责；toolbar action 也必须改调服务 wrapper，再验证所有入口无 EDT PasswordSafe 调用。本 change 不并行改写 `PreviewForm`。

缺失凭据继续走现有平台配置提示并停止请求。日志与 UI 错误不加入 key/token；`YApiFacadeServiceImpl.getCatMenu()` 当前请求将 token 放在 query 参数里，本 change 为保持 API 协议不改这一行为，也不新增完整 HTTP 日志；query-string 凭据暴露作为独立后续议题。本 change 不访问真实服务验证账号有效性。

### 6. 用 fake seam 做确定性测试，原生 IDE 验证 OS-backed 行为

自动测试通过包内 fake 存储 seam 断言 key、secret 状态、每种操作调用次数、非 EDT 线程和失败原子性；另用目标 IU 261 `InMemoryCredentialStore`（与该基线 KeePass 后端具有相同条目写入语义）验证同平台不同项目的保存、读取和单项目清除，以及跨平台条目互不影响，避免 fake 的复合 map key 掩盖 backend 条目覆盖。不写系统钥匙串，不使用真实 token/服务。设置状态序列化测试填入虚构凭据后断言输出 XML 不含 secret，并确认普通 URL/项目标识仍可保留。表单测试验证密码控件、零字符保留、非空输入原样替换、显式清除、Reset/Cancel 与 Apply 后清空。上传测试通过 fake service 检查凭据查找发生在后台，并断言没有密钥进入日志或错误消息。

隔离 IDEA profile 的原生验收使用虚构 key/token，不调用 ShowDoc、YApi 或 YuQue。分别验证设置状态、PasswordSafe 提示/拒绝路径、密码遮蔽、Apply/Reset/Cancel、失败后重试、IDE 重启后同项目读取、不同项目隔离，以及首次/之后状态保存时 XML 变化。环境没有可交互 OS 密码提示时，单独记录跳过原因，不以 fake 自动测试代替原生结论。

## Risks / Trade-offs

- [用户升级后需要重新填写三个平台凭据] → 普通设置仍保留；用户文档和 changelog 说明凭据必须重录。
- [旧 XML 中原有凭据可能继续以明文存在] → 插件不扫描/清理；明确记录旧文件在新版正常状态重写前可能残留，由用户自行决定备份或清理。
- [PasswordSafe 被配置为仅内存存储、系统钥匙串锁定或权限提示拒绝] → 状态显示为未配置/不可用，Apply 失败时保留草稿；在隔离原生 profile 记录实际行为，不写普通 XML 作为回退。
- [项目身份变化使原凭据不可达] → 不扫描或复用别的项目凭据；提示当前项目缺少凭据，用户重新输入。
- [Apply 成功后 IDE 状态文件仍未落盘时进程崩溃] → 顺序提交能覆盖常规失败，但不承诺跨系统存储与 IDEA XML 的崩溃事务。
- [YApi 目录查询把 token 放在 URL query 参数] → 保持平台协议边界，本 change 不改传输格式；禁止插件新增完整 URL 日志，并将协议层改造留作单独评估。
- [PreviewForm 直接上传入口绕过现有后台 wrapper] → 与 `fix-preview-lifecycle` 文件所有者协调，不合并前遗漏该路径。

## Migration Plan

没有自动凭据迁移或删除。新版移除 secret 设置状态字段，保留普通设置和原 XML 文件名；用户需要在各平台设置页重新输入凭据并 Apply。新版本第一次正常保存后，设置状态序列化不再写入凭据字段；旧版 XML 在此之前可能仍留有明文凭据。升级指南说明凭据重填和旧文件残留风险。没有自动回滚适配或旧格式兼容分支。
