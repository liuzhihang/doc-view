# Design

## Context

动机和可观察要求见 [proposal.md](proposal.md) 与 [integration-settings-lifecycle spec](specs/integration-settings-lifecycle/spec.md)。当前三个平台均用项目级 `PersistentStateComponent` 保存设置；表单自己实现 `reset`、`isModified` 和 `apply`。ShowDoc/YApi 字段分别是 URL、key/token 与 URL、`Long` projectId/token；YuQue 字段为页面 URL、API URL、login、repos、token。现有持久化类和 XML 文件名不需要调整。

当前源码的缺陷集中在表单状态转移：ShowDoc/YApi 的空输入与 `null` 用 `String.equals` 比较；YuQue 虽先 trim，却仍把空串与 `null` 判为不同；三个表单在 Apply 时剥除的 URL 尾斜线没有和修改状态检测共享规范化。YApi 的 `isModified()` 会在空/非数字 ID 时提前返回 false，Apply 对无效非空 ID 只通知而继续写其他字段；溢出会在 URL 已写入后才转成配置错误。YApi Reset 遇到已保存 `null` ID 不清草稿。YuQue 配置缺失时导航到 ShowDoc；YApi descriptor ID 与 configurable 的 `getId()` 不同。

现有 `TemplateSettingsLifecycleTest` 已展示 `LightJavaCodeInsightFixtureTestCase`、项目 service、EDT 操作和表单字段测试的可复用模式；当前没有集成设置专用回归测试。实现保持 Java-only，不增加依赖或真实服务访问。

## Goals / Non-Goals

**Goals:**

- 让空白配置、修改状态、Apply 和 Reset 按同一状态规则工作。
- 对 URL 文本做本地且幂等的基址规范化，不添加地址格式或远程可达性约束。
- 让 YApi 项目 ID 可清空，并在写入任何字段前完成非空 ID 校验。
- 修正 YuQue 设置导航和 YApi configurable 注册 ID。
- 为用户可见状态和本地设置写入增加可重复的 RED/GREEN 回归验证。

**Non-Goals:**

- 不改变 ShowDoc/YApi/YuQue 的上传请求、payload、检查完整配置的规则或错误通知；不执行真实上传。
- 不改变项目设置类、状态字段类型、XML storage、token/key 的存储方式或各平台现有的非 URL 字段 trim 语义。
- 不调整表单布局、字段文案、密码显示、安全存储或连接测试。
- 不引入旧配置迁移、兼容分支或上传失败重试。

## Decisions

### 1. 用规范化后的当前值比较，不把验证塞进 `isModified`

`isModified()` 只比较表单草稿与保存状态，不负责拒绝输入，也不解析 YApi ID。字符串比较把持久化 `null` 映射为空输入；YApi ID 比较把 `null` 映射为 `""`，其他 ID 与表单文本比较其十进制表示。这样输入空值或无效文本仍会正确显示为未应用修改。

URL 字段使用同一规范化函数比较、Reset 回读后的判断和 Apply 写入：去除外层空白，剥除末尾连续 `/`，但保留 `://` scheme 分隔符；空值保持为空。该字段是服务基址，不尝试校验 scheme、host、path 或网络连通性。Apply 一次后，界面与保存值在规范化语义下相等；反复打开和 Reset 不产生虚假修改。key/token/login/repos 仍沿用各表单当前 trim 行为，避免随本 change 改变认证输入。

备选方案：只在 Apply 中继续剥除一个斜线会使比较状态不一致；在 `isModified()` 中解析 YApi ID 会让无效草稿无法正确启用 Apply；统一 trim 所有凭据会扩大认证行为变化。因此不采用这些方案。

### 2. 将设置是否完整与设置能否保存分开

Apply 允许空字段和部分配置，便于首次打开、暂存参数、切换平台或清除旧配置；不在设置页调用服务或执行网络检查。实际上传继续使用现有 `checkSettings` 必需字段门禁，不把“可保存”解释为“可上传”。YApi 空 ID 应用为 `null`，因此现有上传前置检查仍会将其视为未配置。

YApi Apply 按顺序先读入并规范化所有表单值，再把非空 ID 校验为仅十进制数字、`long` 范围内且大于零。空白 ID 转为 `null`。前导零有效，例如 `001` 解析为数值 `1`；setter 全部成功后，将 ID 输入框回填为保存值的十进制文本，避免表单仍显示 `001` 而被判定为未应用。校验失败抛出 `ConfigurationException` 并保留草稿；只有所有值都准备成功后才调用设置 setter。这样无效字符、零、负数及溢出不会造成 URL/ID/token 部分变更。成功后再由 IntelliJ 按既有 project persistent-state 机制保存。

### 3. Reset 每次覆盖全部草稿字段

三个表单的 Reset 都从对应项目设置完整回填所有输入。YApi 以空串回填 null project ID，而不是跳过赋值；Reset 只改界面，不写 persistent state。ShowDoc 和 YuQue 的 null 字符串交给 Swing 文本字段显示为空，并在修改比较时按空值处理。

### 4. 让平台注册身份和设置导航指向实际配置

将 plugin descriptor 中 YApi configurable 的 ID 改为 `YApiSettingsConfigurable.getId()` 当前返回的 `liuzhihang.api.doc.YApiSettingsConfigurable`。服务导航继续使用具体 configurable class；YuQue 缺配置分支改为 `YuQueSettingsConfigurable.class`。不改设置层级、display name 或平台 extension 类型。

### 5. 以真实项目设置 service 编写本地测试

新增集成设置契约测试，沿用现有 IntelliJ Java fixture 与项目设置 service；在 EDT 操作表单组件，测试通过窄测试辅助读取/修改 `.form` 绑定的私有字段，不新增生产公共 API。每项测试恢复原 project settings，ID 值仅用虚构数字，不写密钥或 URL 之外的真实连接数据。

自动回归覆盖：全空/null 页面初始未修改；空配置和部分配置可 Apply；URL 多尾斜线 Apply 后规范化且未修改；Reset 清空 null ID；YApi 空 ID 清除、前导零 ID `001` 应用并回填为 `1`、合法上界可保存、非数字/零/负数/溢出报错且全部保存字段保持原值；descriptor ID 与 Java accessor 相同。YuQue 缺配置时打开正确页面通过隔离 IDE 设置导航烟测验证，禁止触发远程请求。优先使用可由 Computer Use 控制的官方 IDEA 262 隔离 profile；若 `runIde` 窗口可控，也可用该入口。项目使用临时 fixture 或已授权的专用 `doc-demo`，记录原状态并避免改写既有用户内容。若平台测试夹具可用公开方式截获 `ShowSettingsUtil`，可同时加入自动断言；不为测试增加生产注入层。

RED 阶段先运行新增测试并确认它们在对应旧行为断言处失败；不能把测试发现失败或 fixture 启动失败当作目标 RED。生产实现完成后重跑相同用例，再执行全量测试、插件项目配置和目标平台 Plugin Verifier；最终 IDE 导航只在隔离临时项目中手工验证。没有联网服务依赖。

## Risks / Trade-offs

- [已有项目可能保存带空格或多个尾斜线的 URL] → 只在用户 Apply 时规范化 URL，不在加载或打开设置页时改写持久化状态；非 URL 认证字段保留既有 trim 规则。
- [YApi 用户过去可能使用零或非标准 ID] → 本 change 明确只接受正 long；错误会在 Apply 阶段提示且不改变已保存值，用户可修正或清空字段。
- [设置页缺配置导航依赖 IntelliJ UI] → 自动测试覆盖纯设置状态，导航以隔离 IDE 页面结果作为独立证据，不声称本地 fixture 取代原生 UI。
- [descriptor ID 调整影响设置搜索项身份] → 与 configurable accessor 统一并用 descriptor 回归检查；项目持久化 XML 与设置字段不迁移。

## Migration Plan

无数据格式迁移。应用配置时规范化 URL；非 URL 字段和持久化 state 格式保持现状。YApi descriptor 改用与 Java configurable 一致的 ID。实施后按 tasks 先取得目标 RED，再最小实现并取得 GREEN、全量回归、配置/Verifier 与隔离 IDE 导航证据。更新 changelog 时仅记录实际完成的代码行为。本批不提交、发布、sync 或 archive。
