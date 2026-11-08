# Design

## Context

动机与范围见 [proposal](proposal.md)。本文件记录实施设计与选路决定；实际完成度和未覆盖项见 [验证记录](verification.md)，规范性输出以 [HTTP contract](specs/http-request-generation/spec.md) 和 [UI contract](specs/doc-view-action-layout/spec.md) 为准。

2026-10-23 核对基线：分支 `develop`，HEAD `7b2e0a8`，插件 1.4.0，Java 21，Gradle 9.5.0，IntelliJ Platform Gradle Plugin 2.16.0，目标 IU 2026.1；本机目标 SDK build 为 `261.22158.277`。这些是本次设计输入，不自动升级支持矩阵。初始工作区仅有既有未跟踪 `.agents/`，本次规划不触碰。

实施前链路：`CatalogHttpClientAction` → `CustomFileUtils.openHttp` → 同步 read action 构建 `DocView` → 拼接文本 → 创建 Scratch。根因包括参数模型无 HTTP location、`@Parameter.name` 可作为文档名但不是实际绑定名、统一 `reqFormExample` 被写入 query、header 丢失、表单 Content-Type 错误，以及缺失值拼成 `null`。`MethodNode.httpPath` 使用业务标题，不能独立表达重载身份。参数编辑器只编辑必填/描述并确认后写回源码，没有 HTTP 示例草稿需要迁移。

已存在的 `DocViewBackgroundTasks`、单方法 Markdown 后台路径、`ProjectDocumentIdentity` 和类型示例解析可按用途复用。现有 `DocView`/`Param` 含 PSI 或展示语义，不适合作为跨线程 HTTP 传输模型；全项目导出服务也不应在单方法 HTTP 动作中调用。

关联 change 保持各自职责：`api-tree-navigation` 管目录和图标，`localize-plugin-ui` 管语言，`async-method-doc-preview` 管 Markdown 打开，`clear-cache-safety` 管缓存删除边界。它们尚未同步的 delta 不因本计划提前归档；新增 HTTP 草稿身份是本次显式行为变化，不修改 Markdown 身份或旧 spec。

## Goals / Non-Goals

**Goals:**

- 先取得原生生成实际可用性证据，再交付满足同一最小 HTTP contract 的单一生产路线。
- HTTP 传输语义和文档显示语义分开，沿用公共 PSI 能力，不复制 Markdown/上传解析体系。
- 生成一次只处理一个确定方法；有明确取消、错误和不支持终态，不覆盖已有请求。
- 通过菜单分组、命名和启用状态精简 UI，保持平台外观与既有业务操作。

**Non-Goals:**

- 不实现完整 Spring/OpenAPI 端点模型，不建设通用 provider 注册体系、可切换后端或版本兼容桥。
- 不解析真实运行环境、Feign 服务发现、远端 OpenAPI、HTTP Client 私有环境或平台上传凭据。
- 不在本 change 提供 MCP、专用索引、DTO 模型目录或 Air 集成；它们不是 HTTP 的前置依赖。
- 规划阶段仅创建文档；2026-10-23 用户已授权执行下列 PoC、代码、fixture、本机服务、UI 操作和构建验证，发布与归档不在本轮授权范围。

## Decisions

### 实施选路记录（2026-10-23）

采用唯一 **Java 自生成 → 新 HTTP Scratch → IDEA HTTP Client** 路线。目标 IU `261.22158.277` 的公共 `generateRequestsInHttpEditor(List)` 返回 `void`，字节码在 offset 64 启动 coroutine、67 丢弃 `Job`、68 返回；Spring gutter 与 Endpoints 最终共用该入口。Doc View 无法获得受控完成/取消/发布边界，触发下述门槛 4 的硬拒绝；`generateRequestsInGivenFile` 的 Internal 入口不作为替代。独立探针在目标 SDK 下编译、打包并实际运行了 Verifier；探针自身的 descriptor/deprecated 问题修复与复验单独记录，不能当成原生不可行依据。完整报告见 verification 及 PoC evidence。

生产侧不添加 Spring/HTTP Client API 编译依赖，不保留原生候选或运行时 fallback；只检测当前 HTTP Client 组件可用性，缺失时禁用请求入口。参数快照用 Java 实现，复用既有有界 body 解析；发布使用平台 Scratch/VFS/Document 公共 API。HTTP 动作统一“生成 HTTP 请求”/“Generate HTTP Request”。候选请求语义与原生取消/编辑矩阵未运行，按上游硬拒绝出口记录不适用；选定生产路线的这些验收仍必需。

实现细节：项目级 `HttpRequestService` 使用可取消 `Task.Backgroundable` 和写优先的 non-blocking read action；action update 只检查直接注解语法，后台再严格解析身份和支持范围。提交选中 Java 文档到 PSI，不强制保存文件；smart pointer 跨提交保留方法身份，源文件文档戳保护生成期间的修改，完成后的 PSI 代际复核保护解析依赖到发布之间的变化。无关文件写入允许 read action 重试。每次成功用 `create_new_always` 新建 Scratch；不覆盖旧 Document。Feign 的静态 path 前缀参与组合，动态前缀拒绝；空可选表单保留为空 body。JSON 集合/参数化 DTO 复用既有有界类型 helper，顶层数组使用可编辑的空数组示例。

### 1. 原生复用是有明确出口的前置实验

规划时的候选调查如下；实施后的硬拒绝结论见上节，实际执行/未运行项见 verification：

| 候选 | 已知证据 | 仍需验证 |
| --- | --- | --- |
| Spring `UrlPathContextUtilsKt.findUrlPathContext(Project, PsiMethod)` | 目标 SDK 中可见 JVM 方法；所检方法未见 Internal 注解 | 类/包/模块的完整 API 状态、Java 调用、支持的映射范围、声明依赖 |
| `HttpRequestGenerationManager.generateRequestsInHttpEditor(...)` | 目标 SDK 有可见方法；另一 `generateRequestsInGivenFile(...)` 方法有 Internal 标记 | 完整参数对象构建链是否公开；返回/完成/取消/写入行为能否满足 contract |
| Endpoints 的 `HttpGenerateRequestFromEndpointsAction` | Action 已注册；读取端点 `URL_TARGET_INFO`，不是直接读取任意 PsiMethod | 所选方法的上下文获取、禁用原因、焦点及项目隔离 |
| Structure view 生成 Action | 依赖特定 place 与结构树元素 | 不伪造内部结构节点；不作为首选捷径 |

JVM `public` 或单个方法未标 Internal 只能作为线索。先查公开 API/扩展文档、目标 jar descriptor 和包状态，再验证调用链；不以反射、复制内部实现或手动构造内部 UI 对象绕过边界。

PoC 已放入独立 `tools/http-client-poc/`（规划阶段不存在），生产 source set 不包含它。探针使用 Java 21、目标 SDK、隔离 config/system/plugins/log profile 及虚构端点；普通 IDEA profile、真实业务工程和现有 MCP 配置不变。若需操作现有 `doc-demo`，先冻结文件/设置基线并恢复临时内容。

**准入条件全部成立才采用原生路线：**

1. 完整所需 API 链在目标版本允许第三方使用；编译/descriptor/Plugin Verifier 无 internal、override-only、deprecated 或 compatibility 问题，无法确认公开边界也不能判通过。
2. 从 Doc View 方法选择可达，无需用户先打开 Endpoints 或伪造焦点，Spring 与 Feign 的首版必需场景分别通过。
3. 下文 HTTP 矩阵 A01–A12 的请求语义成立，包含 wire name、示例/缺省值、编码、form 和 header；原生格式差异可以接受，语义缺失不能接受。
4. 能提供受控后台构建/完成边界，取消、源码变化、项目关闭和旧结果写入均满足 contract。仅能 fire-and-forget 而无法保证终态的入口不准入。
5. 重复生成保护已保存与未保存内容，重载/项目隔离可验证，生成动作不自动执行请求。

**决策出口：** 全部通过 → 采用原生集成，删除旧错误 HTTP 拼接调用；任一硬条件不通过 → 记录具体失败证据，采用一个 Java 自生成实现。环境不可用属于“未验证”，不能伪造“原生不可行”来跳过实验。决策写入本 change 的 `verification.md` 并更新 design 中的路线/依赖结论，然后才进入生产实现。无需为计划内这两个互斥结果重复请求批准；若要降低必需场景、升级 IDE、采用 internal API 或引入新语言，则超出本计划，必须先修订范围。

实验按 API/依赖、请求语义、生命周期逐层推进；出现有证据的硬拒绝后不继续建设该候选。后续候选评估项以“不适用：引用上游拒绝证据”的记录完成评估，清楚保留实际未运行的子检查，不能报告其通过；仅因环境受阻时相关项继续未完成。这样自生成分支可以形成完整任务闭环，而无需为了勾选继续调用已禁止的 API。

探针的构建与验证入口为 `python3 tools/http-client-poc/run.py --ide-home "$DOC_VIEW_HTTP_POC_IDE_HOME" --output-dir "$DOC_VIEW_HTTP_POC_OUTPUT" --verify`。两个变量由实施者设置为核实的目标 SDK 和隔离临时输出目录；脚本需在运行时打印不含敏感信息的实际命令/包 hash，`--verify` 明确检查探针 ZIP，不启动 IDE、不发送请求。不得执行主工程的 `verifyPlugin` 后声称探针已通过；原生安装/交互另行进行。

两种候选不是两组都要实现的任务；tasks 的公共实现步骤消费最终决策，只保留一个生产实现和一组产品文案。不提供设置开关，不在运行失败或 Feign 不支持时暗中切换生成器。

### 2. 最小支持集合先固定，按它评估原生能力

| 输入 | 首版约定 | 非目标/拒绝边界 |
| --- | --- | --- |
| 框架 | Spring Controller；直接声明标准 Spring mapping 的 Feign 方法 | Dubbo/普通接口；Feign 专有 contract；继承或自定义组合映射不扩大承诺 |
| mapping | 标准 `RequestMapping` 和 Get/Post/Put/Delete/Patch 快捷注解；单有效 method；单类/方法路径组合；literal/可解析字符串常量；path/value 别名 | 多路径、多 method、method 未限定、动态表达式、歧义别名、继承映射、通配/正则路径、非空 params/headers 条件 |
| 参数位置 | 显式 PathVariable、RequestParam、RequestHeader 的 String/boolean/数值基本及包装类型 | Map/集合/数组/Optional 形式的绑定、Cookie、文件、流、未绑定业务对象 |
| JSON | 一个受既有类型解析支持的 RequestBody；无 consumes 或单一 application/json；复用有界类型示例 | 多 body、无法解析 body、multipart、多种或其它媒体类型、非空 consumes 但无对应 body |
| Form | POST + 明确 form-urlencoded consumes + 显式标量 RequestParam；没有 RequestBody/RequestPart | ModelAttribute/隐式对象展开、混合 JSON/form、文件 body |
| Query | 非表单场景的显式 RequestParam，包括与 JSON body 共存 | 不根据 POST 自动推断 form |
| 基础地址 | `{{host}}`，用户在 HTTP Client 配置环境或编辑草稿 | 不解析 Feign URL、server.port、服务发现和生产地址 |

这是 Doc View 本轮生成支持范围，不是对 Spring 所有绑定能力的描述。Spring `@RequestParam` 可接收 query 或 form；本方案用有效 consumes 选择明确的生成位置。类与方法的有效映射按 Spring 语义组合，method 级 consumes 覆盖类级；校验失败就不给出“成功”的残缺请求。

### 3. HTTP 请求数据不借用文档展示字段

无论选哪条路线，都围绕选中方法验证传输语义，不依赖旧 `reqFormExample`。自行生成时优先使用一个最小不可变 Java 请求快照，字段为来源身份、method、path、按位置分组的 wire name/value、媒体类型和 body 文本；对象不持有 PSI。名称仅是设计描述，不强制增加通用策略接口。

- 位置由 Spring 绑定决定；wire name 为 Spring `name/value` 或 Java 参数名。文档名继续遵循已有 OpenAPI/Javadoc 规则，`@Parameter.in` 不新增位置控制。
- 标量 example 优先于绑定 defaultValue；required/defaultValue 采用 Spring 的传输规则，不能用文档的 Swagger/Validation 必填优先级替代。defaultValue 的框架“未设置”哨兵不能当成实际值。
- 具体 query/path/form 值在输出阶段按组件编码；HTTP Client 变量占位符不进行百分号编码。必需未知值使用按位置和名称生成的安全变量名，变量名冲突需稳定消歧；可选无示例 Query/Form/Header 省略，但现有路径段始终用值或变量替换，不因 required=false 省略。有效 defaultValue 包含显式空字符串。
- Header 不用 query 编码，检查 CR/LF、重复名称和 Content-Type 冲突。有 body 时，显式匹配 Content-Type 合并；未提供值则取有效媒体类型；具体冲突或不可确认的变量拒绝。按 spec 固定名单识别的认证 header 使用变量，不能从配置或真实示例复制敏感值。
- JSON 示例直接复用现有类型/递归限额，HTTP 生成无需构建响应 DTO 或渲染整份 Markdown。共享工具只做必要的最小抽取，现有输出 golden 不应因修 HTTP 被重录为新结果。
- 不直接修改已有 `reqParamKV` 文档输出 contract；新增 HTTP RED/GREEN 证明缺失 example 不再产生 `null`。已有 `OpenApi3ParameterMetadataTest` 中的文档模型断言和新 HTTP 断言分开，避免为了 HTTP 修复改变所有文档消费者。

### 4. 生命周期和草稿发布边界

```text
目录方法选择
  → 轻量可用性检查（不构建文档）
  → 捕获所属项目/方法智能指针及请求身份
  → 提交当前编辑内容到 PSI，后台读取并构建
  → 校验取消/源码版本/项目状态/请求代次
  → 一次性发布完整新草稿，聚焦 HTTP Client
```

自生成路径沿用 `DocViewBackgroundTasks` 的管理方式，核实其文档提交、read action 和取消传播，不假设异步 Markdown 已覆盖 HTTP。原生路径使用平台支持的调度，但要用相同可观察标准验收。两个方案都禁止在 action `update` 做项目扫描或生成全量 DocView。

同一项目/完整方法身份的在途请求合并；任务完成后再次点击产生独立新草稿。通过 project 作用域、模块/相对源码路径/FQN/完整签名标识方法，可以复用 `ProjectDocumentIdentity` 的局部身份编码，再增加项目作用域，不将业务标题、行号或绝对私有路径作为可见文件唯一名称。

自生成优先每次成功新建唯一 `.http` Scratch；原生路线可使用原生新增请求块，但必须保持原有 Document（含未保存编辑）全部内容、来源可辨别和新块定位。不得直接覆盖磁盘绕过 Document，也不得因 Clear Cache 清理这些草稿。新草稿写入失败应清理本次未完成产物，不能删除既有用户文件；追加无法证明原子失败保护时，原生路线不通过准入。

取消的保证边界是“发布开始前取消不写入”；已完整生成后用户再取消不回滚已交付文件。源变化使旧结果失效，提示重新生成，不无限自动重试。构建/发布错误只给脱敏原因，不在通知或日志输出源码、完整请求体或凭据。

原生压力验收发现 25,488,492 bytes 请求会进入平台只读大文件编辑器。发布前须在后台按包含来源注释的完整 UTF-8 字节数检查平台完整内容加载上限，过大时明确拒绝，不写入 Scratch、不截断 body。此检查沿用目标 IDE 的大小判断，不新增设置或固定另一套阈值；代码洞察阈值不等于可编辑阈值，不能据此拒绝仍可编辑的请求。它约束草稿交付，不宣称限制整个 PSI 展开的峰值内存。

### 5. 平台依赖与能力缺失

当前生产 descriptor 未声明 Spring Web/HTTP Client API 依赖。PoC 必须从目标 SDK descriptor 核对精确 plugin/module ID、加载器和 API 状态，不能从磁盘文件夹名推断依赖。

原生路线如需新增依赖，仅允许本计划内必需的目标平台 bundled Spring/HTTP Client/microservices 组件；在 Gradle 与 descriptor 同步声明。原生集成代码放在可选功能加载边界内，相关组件缺失时核心文档功能仍可用，HTTP 明确不可用；这属于当前版本功能依赖管理，不提供旧 IDE 适配或备用生成器。PoC 无法证明此加载边界时选择自生成路线。自生成路线使用平台公开 Scratch/文件打开能力，HTTP Client 不可用时不能把普通文本打开描述为完整 HTTP Client 能力。

不提升 Java、Gradle 或 IDE 基线，不改变 Verifier 失败级别；一次额外 IDE 验证只记录前向结果。移除生产源中的 PoC、失败候选和未使用依赖后再构建最终包。

### 6. UI 收纳与既有 action 身份

目标工具栏：`刷新 | 导出▼ 上传 | 展开全部 折叠全部`；使用工具窗口原生 `…` 的附加动作机制承载设置/清缓存，保留平台自身菜单。目录菜单去掉项目级清缓存，减少其“仅清当前方法”的误导。

保留现有 action ID、快捷键和业务服务；用 reference/同一 action 实例挂接新位置，不复制另一个设置/清缓存实现。移动后确保动作读取所属 tool window 的项目上下文；Find Action 若保留既有 action ID，其显示名称同步更新。工具栏 `setTargetComponent` 和现有 data snapshot identity 保持。

`CatalogOpenAction` 和 HTTP action 的非方法分支都改为无操作，不再借机展开整棵树；展开仍由专门动作承担。预览、参数编辑、欢迎页只核对同名动作一致性，不重排这些界面，也不改品牌 SVG。中英文名称和不可用说明复用消息 bundle。

### 2026-11-08 目录方法搜索补全说明

原生搜索探针输入 `Nativepageitem23` 未命中。源码核对后确认，现有业务标题由 `DocViewUtils.getName()` 按兼容规则截到 15 个字符（该例显示为 `Nativepageitem2`）；完整说明由 `DocViewUtils.getMethodDesc()` 提供，并已放入方法 tooltip。搜索遗漏的根因是 `MethodNode.getSearchText()` 只拼了签名和截短后的业务标题，没有完整说明，和树 renderer 的截断无关。`TreeSpeedSearch.NODE_PRESENTATION_FUNCTION` 通过 `SimpleTreeStructure` 的 `NodeDescriptor.toString()` 读取 `DocViewNode.getSearchText()`。

修复仅在 `MethodNode` 创建时保存既有完整说明快照，并在 tooltip 与搜索文本中复用。搜索继续包含完整签名与业务标题；业务标题、树上呈现及 Markdown/HTTP 文件路径仍沿用 15 字符规则，不改生成文档内容或文件命名。自动回归需使用公开 `StructureTreeModel` 构造真实模型包装路径，验证 15 字符以后的描述尾部关键词进入平台 formatter；新包原生仍需核对真实 SpeedSearch 输入和选中节点。

### 2026-11-07 UI 追加修复

用户在原生验收中要求检查所有 icon，并指出设置说明像等宽正文、Doc View 弹窗切换其他应用仍遮挡。此前 UI 不重排的范围在本节按新授权扩展为以下最小修复，不改变 HTTP、文档解析和业务写回规则。

- 设置根页的 `JBTextArea` 未设置 UI 小字和说明前景色；沿用生成规则页已经采用的 `JBUI.Fonts.smallFont()` / `UIUtil.getContextHelpForeground()`，检查其他说明控件是否同类遗漏，不建立新的字体配置。
- 原生 full-14 发现设置搜索使可见 viewport 变窄后，普通 JPanel 根保留 preferred width，形成水平滚动和说明裁切。根面板使用标准 Swing Scrollable 宽度契约随 viewport 排版，高度继续自然滚动。full-15 原生发现仅此仍不足：263 ConfigurableCardPanel 会给 JPanel 子类再包普通 JPanel，导致内层 Scrollable 失效。因此 SettingsConfigurable 沿用仓库 TemplateConfigurable 的公开 Configurable.NoScroll 模式，返回自身 JBScrollPane，使 viewport 直接管理该根面板。不使用 resize 定时器或尺寸反馈覆盖。回归必须经过真实 ConfigurableCardPanel 包装后的宽→窄布局，不能只测直接 JBScrollPane(root) 或根组件 setSize。
- 参数底部按 2026-11-08 用户实机反馈改为三个纯图标按钮：`AllIcons.FileTypes.Json` 表示复制 JSON，`AllIcons.FileTypes.Markdown` 表示复制 Markdown，`AllIcons.Actions.Checked` 表示写回源码。移除常驻文字标记；保留 action 的完整本地化名称、description、键盘/无障碍名称及原有处理函数，三个图标外形分别可辨。三个动作实现公开 `TooltipDescriptionProvider`，让平台 `ActionButton` 将 description 放入浮动提示；仅设置 `AnAction.description` 在 263 默认只提供状态栏说明，不能满足该要求。以 tooltip 明确“复制”或“写回”语义，不增加新依赖、自绘图标或额外菜单。这取代 11 月 7 日的短文字设计。
- 2026-11-08 原生键盘续验发现：263 在关闭屏幕阅读器选项时，平台 ActionButton 默认不可聚焦，参数弹窗的 Tab 停在滚动区，三个纯图标动作无法到达。仅对本参数编辑器工具栏的直接 ActionButtonComponent 子组件启用 Swing focusable，并通过同一容器的 ContainerListener 覆盖平台重建；不引用 ActionToolbarImpl/ActionButton 实现类，不改变全局屏幕阅读器选项。保留默认 Space 激活和现有 AnAction 业务处理，以实际 Tab/Shift+Tab、名称与复制/写回复验确认。监听器由工具栏组件持有，不注册到全局服务。 随后原生复验显示仅启用按钮 focusable 仍不足：精确 263 的 AbstractPopup 窗口级 FocusTraversalPolicy 无论前后遍历都返回首选滚动区。参数根面板通过公开 Swing setFocusCycleRoot(true) 与显式 LayoutFocusTraversalPolicy 建立内部焦点循环，避免继承窗口的固定策略，保留初始首选焦点，不修改平台窗口策略或添加全局按键拦截；以新包重新验证实际遍历。 表格本身继续使用 Tab/Shift+Tab 编辑单元格；原生发现 Ctrl+Tab 被 IDE Switcher 接管后，在参数弹窗 rootPanel 注册由 popup Disposable 解绑的 Ctrl+Tab / Ctrl+Shift+Tab 局部动作，仅当上下文组件是表格或其编辑器后代时启用；先尝试结束有效 cell edit，再调用标准 transferFocus/transferFocusBackward，返回同一弹窗的焦点循环，不改变全局快捷键或 modal context。
- 预览和参数编辑均使用公开 `ComponentPopupBuilder.setNormalWindowLevel(true)` 解决跨应用遮挡，保留未确认的参数草稿及 Pin，不用失活自动 cancel 来掩盖置顶问题。精确 `263.7031.23` SDK 与源码确认该 API 默认 false，macOS 的 AbstractPopup 据此选普通窗口类型；Wayland 的平台实现有不同分支，本轮不承诺已验。原生覆盖 Pin 开/关、切换应用、返回继续编辑、设置/导出子窗口与 Escape。
- 全量 icon 审查列入 verification；只替换语义有问题或同动作不一致的资源，不重绘全套图标，不复制平台 SVG。

先记录截图/源码或旧组件失败证据，修改后运行受影响的 UI/本地化/lifecycle 自动回归、完整构建与两个精确 build 的 Verifier；每一份 native 结果绑定自己的 ZIP/JAR。基线 change 的 full-13 既有通过证据保留，不能自动套到新 UI 包。新增原生场景待验证时不关闭旧 U01–U04 或基线 R2。

平台依据：[ActionUtil](https://github.com/JetBrains/intellij-community/blob/idea/263.7031.23/platform/platform-api/src/com/intellij/openapi/actionSystem/ex/ActionUtil.kt)、[ComponentPopupBuilder](https://github.com/JetBrains/intellij-community/blob/idea/263.7031.23/platform/platform-api/src/com/intellij/openapi/ui/popup/ComponentPopupBuilder.java)、[AbstractPopup](https://github.com/JetBrains/intellij-community/blob/idea/263.7031.23/platform/platform-impl/src/com/intellij/ui/popup/AbstractPopup.java)。公开 API 状态和 Verifier 是兼容性证据，不替代窗口遮挡、文本裁切或键盘操作的原生结果。

### 7. 阶段路线与完成门槛

| 阶段 | 前置 | 产物与退出条件 |
| --- | --- | --- |
| P0 基线与失败证据 | 用户明确启动 apply | 冻结工作区/验证工程基线，建立 HTTP/动作 RED 案例；未提交改动受保护 |
| P1 原生 PoC 与选路 | P0 | 独立探针、目标 SDK API/依赖证据、Spring/Feign/生命周期矩阵；在 verification 记录选定唯一方案 |
| P2 HTTP 正确性与生命周期 | P1 选路完成 | 选定实现满足 HTTP contract，针对性 GREEN、实际接收语义和隔离/取消成立 |
| P3 UI 精简 | P0 后可并行准备；HTTP 名称等 P1 | 工具栏/目录/窗口菜单、语言/图标/禁用状态通过自动和原生验证 |
| P4 整体验证与交付记录 | P2/P3 | 完整测试、配置/打包/Verifier、最终 ZIP 原生矩阵和文档一致；tasks 按证据完成 |

UI 的组布局和本地化准备可与 P1 并行；API 接入和 HTTP 最终名称不抢跑路线决策。既有稳定性欠账继续在原 change 验收，不复制到本 change 并自动勾选。MCP PoC/正式查询路线见 [roadmap](../../../docs/roadmap.md)，不阻塞本 change 的规划和 HTTP 开发。

### 8. 验收矩阵与证据层次

下表每行都必须记录预期、实际、证据位置、精确 build/包身份和结论。自动与原生两栏独立填写；本次 proposal 不填运行时“通过”。

| ID | 场景 | 自动/静态证明 | 原生或接收端证明 |
| --- | --- | --- | --- |
| A01 | Spring GET + path `42` + query `a&b 空格` + header | PSI/HTTP golden，编码按组件解码断言 | HTTP Client 打开；本机接收值一致 |
| A02 | JSON POST 同时有 query/header | JSON 可解析、位置与单一 Content-Type | 本机接收 URL/header/body 相符 |
| A03 | 显式 form-urlencoded POST | body 编码、URL 不重复 form | 本机 form 解码值相符 |
| A04 | path/value 与字符串常量 | 等价输出；类方法拼接 | 代表样例从目录可生成 |
| A05 | 文档名与 wire name 冲突 | Spring 名/Java 名优先；Parameter.in 不改位置 | 请求实际 key 正确 |
| A06 | example/default/required/optional | Spring required 与文档标记分离、哨兵/空 default、可选路径变量、无 null | 变量可编辑/配置，不自动执行 |
| A07 | 数值 header/重复/CRLF/Content-Type | 匹配合并、缺值推导、冲突拒绝，认证 header 占位 | 不支持原因可见，无额外请求 |
| A08 | 单方法 Feign | 与 Spring 分开的 fixture | 从 Feign 方法生成，host 为变量 |
| A09 | 多路径/method、动态/继承/歧义映射、params/headers 条件、通配/正则路径 | 全部拒绝，不挑第一项或丢条件 | 提示明确，不创建草稿 |
| A10 | multipart、隐式对象、多 body、多种/未知媒体类型、consumes 无对应 body | 全部拒绝，无部分输出 | 不出现伪成功 |
| A11 | 无 body GET、POST query、递归 JSON | 无自动 Content-Type、有界 JSON | 界面无冻结；有限输出 |
| A12 | 新编辑但未保存源码 | PSI 文档提交边界 | 使用新路径，磁盘不被强制保存 |
| L01 | 类/模块/空/Dubbo、无 editor | action 状态/执行无副作用 | 隐藏规则、树展开状态不变 |
| L02 | 两项目、同标题重载、来源身份 | ID 与目标隔离 | 新草稿互不覆盖 |
| L03 | 同方法连续点击、已有未保存草稿 | 在途合并、完成后新草稿 | 旧内容逐字保持，新结果聚焦 |
| L04 | 取消、源删除/变更、关闭项目 | 延迟任务及写入探针 | 可见取消终态、无迟到文件 |
| L05 | 写入失败/依赖不可用/Dumb Mode | 错误与加载边界 fixture | 明确原因，旧文件无损 |
| L06 | 大模型和资源释放 | 无 EDT 重解析、递归/取消断言 | 固定样例冷/热各 7 次时间和 UI 响应观察；关闭后无在途任务/回调 |
| U01 | 主工具栏和 `…` | action 分组、无重复注册 | 5 个常用入口、维护项可达、平台菜单保留 |
| U02 | 中英/深浅/Compact/窄窗/键盘 | 本地化键和 action 状态 | 实际菜单/tooltip/焦点可达，无缺字裁切 |
| U03 | 两项目设置/清缓存 | 上下文与文件保护回归 | 只作用所属项目，HTTP/Markdown/Scratch 保留 |
| U04 | 预览/复制/导出/上传菜单/Enter | 既有文档/图标/本地化回归 | 代表交互不回退；打开关闭上传菜单不上传 |

本机接收端只绑定 loopback，使用虚构数据；主动执行代表 HTTP 请求是验收操作，不是生成器自动发网。不得连接真实平台或生产端点。记录编码后的请求和解码后的值，不能只凭文本“看起来正确”。

L06 的时间用于记录固定环境 p50/p95 和定位阻塞，不能无基线宣称性能提升。取消/关闭后的资源证据记录受管任务/监听器状态；不以字段置空声称 GC 或全量内存稳定。最终包的 UI/生命周期结果按 ZIP hash 区分，不用旧候选包截图替代。

### 9. 验证命令和文档交付

已经建立的新测试为：`HttpRequestGenerationContractTest`、`HttpRequestLifecycleTest`、`DocViewActionLayoutTest`，HTTP 语义测试位于 `com.liuzhihang.doc.view.ui.window` 包，真实 Scratch 生命周期集成测试位于 `com.liuzhihang.doc.view.service.http` 以注入受控 builder，布局测试位于 `com.liuzhihang.doc.view.contract`。实施时若名称调整，先同步 tasks/矩阵再执行，不能让不存在的筛选项产生“0 tests 通过”。

针对性命令见 tasks；完整门禁统一使用：

```bash
./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks
openspec validate http-client-generation-and-ui --strict
git diff --check
```

原生验证使用匹配目标 SDK 的隔离实例；`./gradlew runIde` 仅作为启动手段，不代表任何场景已通过。执行前确认实际 config/system/plugins/log 隔离且使用计划中的包；最终验收必须读回 ZIP SHA-256、实际加载版本与对应日志，避免测试到旧包。界面不能操作时记录阻塞，保留相关 checkbox。

实现后更新 `CHANGELOG.md` 的 Unreleased、相关用户指南（HTTP 支持范围、菜单位置、生成后编辑/执行方式）、roadmap；不把 proposal 的候选路线写成已交付功能。本轮不修改已发布的 1.4.0 发布证据或线上状态。只有另行授权发布时才选择未使用的新版本并完成发布清单。

## Risks / Trade-offs

2026-11-07 Keymap 补充：full-16 原生日志确认七个已注册组缺少 `text`，平台构建动作树时回退为 ID 并产生警告。仅在现有 descriptor 补英文 fallback、bundle 补同 ID 的中英文组名；覆盖所有注册组的测试先验证旧资源失败，再验证修复。保持 action/group ID、层级、顺序及 popup/compact 不变，不启用 dormant 的 editor floating toolbar。最终包原生检查 Keymap 标题及本次会话日志，并与菜单和跨应用验收分别记录。

- [原生能力可调用但不满足协议或生命周期] → P1 使用同一语义矩阵和可见终态准入，失败有自生成单一路线，不为迁就原生降低 contract。
- [首版对复杂端点限制较多] → 界面给出具体原因，指南列支持表；新支持需求通过独立 delta 扩展，不静默猜测。
- [HTTP 与文档命名/必填逻辑串用] → 传输快照隔离 wire name/location/required，保留文档 golden；共享 PSI 工具的修改同时回归 Spring/Feign/Dubbo。
- [平台依赖影响插件加载] → 原生功能使用明确可选加载边界并实测禁用组件；无法满足则选自生成，不改 IDE 基线。
- [原生写入覆盖或追加失败损坏用户内容] → 必验未保存 Document、错误注入和并发，不能证明安全则不采用该入口。
- [移动 Clear Cache 使旧验收步骤过时] → 保留旧证据为历史事实，按最终包重做当前入口验收；原 clear-cache change 未完成项不会因按钮移动自动通过。
- [自动检查被误认为完整验收] → checkbox 按实际证据更新，verification 分开记录自动和原生结果；实施授权不扩展为发布或归档。

## Migration Plan

1. 规划 artifacts 已完成严格校验；2026-10-23 已获准进入 P0–P4 实施，插件版本保持 1.4.0，包仅用于本轮验证。
2. 已依据 P1 选路记录移除旧 HTTP 拼接路径，保留现有 action ID；实施和验收进度以 tasks/verification 为准。
3. 不迁移旧 HTTP 草稿，不删除历史 Scratch，不重命名 Markdown，不增加旧格式/旧 IDE 的兼容分支。
4. 新版仅交付一个生成实现；若原生方案在合入前失败，回到 P1 出口并同步 design/tasks/验证，不能追加隐式备用实现。
5. 若已发布版本出现回归，按发布清单回退到此前正式插件版本或另发修复版本；本计划不自动执行撤回、tag、推送或发布。

## References

- [HTTP Client 与源代码端点入口](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)：官方描述 Spring gutter/Endpoints 可生成请求，不证明第三方插件已能无损调用。
- [Action System](https://plugins.jetbrains.com/docs/intellij/action-system.html#executing-actions-programmatically)：支持程序化调用，并建议避免不必要的 Action 耦合。
- [Plugin Dependencies](https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html)：依赖 ID、构建 classpath 和 descriptor 需同步声明。
- [Spring RequestParam](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html)、[Mapping Requests](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)：query/form 绑定与映射输入依据；本 change 的支持集合小于框架能力。
- [Contract 设计](../../../docs/contract-design.md)、[性能指南](../../../docs/performance-guide.md)、[兼容性](../../../docs/intellij-compatibility.md)、[发布检查清单](../../../docs/release-checklist.md)。外部文档核对日：2026-10-23；最终接入仍以目标 SDK 与实际验证为准。
