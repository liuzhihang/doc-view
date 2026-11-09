# Design

## Context

见 [proposal.md](proposal.md) 的 Why。当前项目设置由一个包含多类规则的 `SettingsForm`、独立模板 Configurable 和三个平台 Configurable 组成；`plugin.xml` 使用 `groupId` 组织子页。模板仍由原生 IntelliJ Editor 编辑，经 `VelocityUtils` 和 `DocViewData` 输出 Markdown。项目级设置和模板状态已分别持久化，集成设置另有独立 change 处理凭据。

本设计沿用 Java、Swing、现有项目服务和 IntelliJ Platform API，不更改模板渲染规则、生成文档格式或平台请求。目标 IDE 中 `SearchableOptionContributor` 为公开 Java 可继承类，`processOptions(SearchableOptionProcessor)` 可由 Java 实现；官方扩展点表将 `com.intellij.search.optionContributor` 列为 `SearchableOptionContributor`，未标 Internal 或 Experimental。证据来自本地 IU 261 `intellij.platform.ide.jar` 的 `javap -v` 和 [官方扩展点列表](https://plugins.jetbrains.com/docs/intellij/intellij-platform-extension-point-list.html)。该实现已编译并完成当前60个中英文搜索词的原生页面归属核对；证据与包身份见本 change verification。

## Goals / Non-Goals

**Goals:**

- 让用户在 Tools 下找到有清晰分组、项目作用域明确的 Doc View 配置。
- 保留现有可配置规则与模板持久化，修复行标记和普通接口识别/扫描选项之间的 UI 联动。
- 在模板保存前提供变量说明、默认模板草稿恢复和不依赖用户项目/网络的示例 Markdown 预览。
- 以三个清晰工作区呈现模板、预览和输出选项；默认只显示模板编辑主区，变量目录按需展开。
- 让 Settings 搜索能通过中英文关键词定位页面或控件。

**Non-Goals:**

- 不改变 PSI 解析、字段过滤、Markdown 内容、模板变量语义、导出或平台上传请求。
- 不改变当前项目级配置范围，不迁移凭据；凭据存储由独立 change 定义。
- 不引入新语言、依赖、WebView/JCEF、在线 Wiki 或外部预览服务。
- 不保留无效设置字段或为移除的 Configurable ID 增加兼容别名。

## Decisions

### 生成规则的低密度说明（2026-10-20）

用户实际气泡截图显示箭头长句虽然自动折行但缺少结构；来源优先级改为编号逐行，回退另段，必填规则按字段/直接参数分段。资源存储纯文本换行，原生气泡转换成受支持的 HTML 换行/段落，可访问描述保留纯文本；中英界面的等价文案在 `localize-plugin-ui` 中维护。

移除五组规则的四边 TitledBorder，使用平台 TitledSeparator 的主题分隔线和组间留白。每组只放一个平台 ContextHelpLabel 说明图标，详细来源顺序/固定规则按短段落置于悬浮帮助；不为每个复选框重复添加图标。页面只保留与操作结果有关的简短灰字，使用平台小字号与 context-help 颜色，左对齐并按可用宽度换行；长注解名称、优先级和固定回退的完整说明继续由 GitHub 设置指南承载。说明图标提供无障碍名称/完整描述，不使 tooltip 成为生成逻辑的一部分；所有原复选框、值和 Apply/Reset/Cancel 保持。依据 [Context Help](https://plugins.jetbrains.com/docs/intellij/context-help.html)、[Inline Help Text](https://plugins.jetbrains.com/docs/intellij/inline-help-text.html) 与 [Tooltip](https://plugins.jetbrains.com/docs/intellij/tooltip.html)。

验证：现有 GenerationRulesLayoutTest 更新为验证简短说明能换行、详细优先级不常驻、说明入口有可访问描述、分组不再使用 TitledBorder；先取得行为 RED，再最小实现并跑配置/布局回归及完整门禁。原生 doc-demo 核对深浅主题、紧凑/普通窗口、说明图标及窄窗口换行，未实际执行的悬停/键盘场景单独保留。


### 使用平台原生设置层级和现有项目状态

保留现有 Doc View 根 Configurable ID 作为 Tools 下入口，把根页用于常规行为；增加生成规则、模板与输出、平台集成页面，平台集成页下放置现有三个平台页。现有各 Configurable ID 与项目级 `nonDefaultProject` 范围保持不变，只把父子关系迁移到 `parentId`，不再使用已弃用的 `groupId`。根页保留现有“Doc View”名称，常规行为作为根页分组标题，避免创建空的导航父页。

平台集成父页使用 `IntegrationSettingsConfigurable`，提供简短说明和到子页的入口，不复制密钥字段或提交逻辑；按后续 `doc-view-welcome` change 的渠道决策，从父页和集成子页在默认浏览器打开 GitHub `integrations.md`，模板页打开 `templates.md`，常规行为/生成规则页打开 `settings.md`；不再内嵌完整手册。设置页面默认由平台提供外层滚动；若某页自行提供滚动容器，则实现 `Configurable.NoScroll` 移除平台外层滚动，避免双层滚动。参照 [Settings Guide](https://plugins.jetbrains.com/docs/intellij/settings-guide.html) 中关于 `parentId`、`groupId` 弃用以及 `Configurable.NoScroll` 的说明。

### 以现有持久化状态拆分设置，不改变生成规则

将 `SettingsForm` 的标题、接口名称、描述、必填注释和字段名来源选择放到“生成规则”，常规行为页只保留行标记与普通接口；模板与输出页承接参数分表、两个参数表嵌套层级前缀和合并导出。`prefixSymbol1` / `prefixSymbol2` 用于嵌套参数行的前缀，不能作为标题前缀说明。配置值仍读写当前项目的 `Settings` 与 `TemplateSettings`，除移除无效字段外不改字段默认值和生成来源优先级。

行标记与普通接口始终独立启用、保存和重置。`lineMarker` 控制 gutter 标记；`includeNormalInterface` 不仅影响目录，它还决定 `DocViewUtils.isDocViewClass` 是否把普通接口识别为 Doc View 类、`DocViewService.getInstance` 是否对普通接口选择 Dubbo 服务，以及 `ModuleNode` 是否扫描当前 module 的普通接口。只修复 UI 对另一个选项的禁用，不改该识别/扫描范围。移除 `hideLeft`（界面中未生效）及 `excludeParamTypes`（无解析/输出消费者，只进入 `DtoSchemaCacheService.settingsHash`）；保留 `excludeParameterType`（单数，当前用于排除特定参数类型）。移除死字段时一并删除它对缓存键的影响，不增加旧字段适配或双轨逻辑。

帮助文本把可配置来源与固定规则分开说明。可配置项为标题 4 项、接口名称 4 项、接口描述 2 项、必填注释 1 项和字段名来源 1 项；Validation、Lombok、Swagger 等固定注解及方法名回退不能伪装成可切换设置。规则顺序从当前生成代码/测试确认后写入帮助，并由回归测试保护；不借 UI 改变优先级。

### 复用原生模板编辑器，预览使用固定 DTO 样例

保留 Spring/Dubbo 的 IntelliJ Markdown 编辑器和现有 Velocity 渲染入口。变量目录以 Velocity context 中实际可用的 `DocViewData` 属性为准，提供中英文名称/说明和插入语法；测试应校验变量目录与运行时可访问属性保持一致。恢复默认只更新编辑器草稿，Apply 才写入项目设置，Cancel/Reset 遵循 IntelliJ Configurable 的草稿生命周期。

模板与输出页用三个平台原生页签分开显示“模板编辑”“预览结果”“输出选项”，首次打开默认选中模板编辑。模板编辑区内部继续保留 Spring 与 Dubbo 子页签。变量目录默认收起，由“显示模板变量”按钮显式展开/收起；展开时使用平台 splitter，编辑器保留可读的最小宽度，窗口空间不足时用户可收起侧栏继续编辑。依据2026-10-21原生反馈，设置指南统一为原生问号，悬浮包含简短用途和GitHub链接，不再用大按钮或常驻长文字链接。模板问号靠近简短说明；恢复默认模板移入模板编辑工作区，与变量入口同组，不占页头。输出选项独占页签，参数分表、两个嵌套层级前缀和合并导出纵向分组、标签与输入框对齐，并保留足够行间距，不占据编辑或预览工作区的常驻高度。

预览使用固定、虚构的 `DocView`/参数数据构造 `DocViewData`，在后台线程调用现有渲染语义，结果和错误通过 EDT 更新 UI。用户点击 Spring 或 Dubbo 的预览按钮时，立即切换到“预览结果”页签以显示生成中状态；成功、空结果或错误均在该页签展示，用户可以通过页签或“返回模板编辑”入口回到编辑。请求完成后不强制切换用户当前页签。每次模板变更递增请求序号或取消旧任务；只有最新请求可写入预览。页面展示只读 Markdown 源码和渲染错误，Velocity 能提供行列位置时才显示位置。预览不调用 PSI 扫描、不读取项目业务代码，也不调用网络服务；不修改 `VelocityUtils` 的生产模板语义。Apply、Reset、Cancel、预览状态和编辑器资源释放继续由现有 `TemplateSettingForm` / `TemplateConfigurable` 生命周期负责，不引入新的设置服务或改变保存边界。

原生 IDEA 视觉复核反馈要求模板页使用平台分隔条并让变量说明与状态可读。变量区采用平台 `OnePixelSplitter`；变量搜索采用 `JBTextField` 的可见 empty text，并设置无障碍名称。为避免长表达式和中文标签挤在同一行，变量列表主行显示简洁属性名（如 `responseParamDataList`），第二行显示中文标签；筛选仍搜索原始插入表达式、属性名、标签和说明。选中变量的详情区显示完整 `${DocView...}` 插入表达式、中文标签及完整用途/适用性说明，并可换行阅读。浅色主题原生复核显示，原 64 logical 的详情高度仅容纳约三行正文，长表达式换行后会挤掉说明；因此将详情滚动区首选高度提高到 120 logical、文本区设为六行，在常规窗口高度下为表达式、标签和用途留出可读空间，短窗口高度不足时由详情区纵向滚动。列表继续占据剩余空间并保留独立滚动。列表插入仍使用原始完整表达式，当前字号和搜索语义不变。底部使用提示显示短文案“插入到当前模板光标处”，tooltip 保留完整操作说明，避免占用详情区宽度或单行裁切。预览状态分别表达“尚未预览”“正在生成”“预览完成”“模板已修改”“结果为空”和错误；Configurable `reset()` 清空旧预览并回到“尚未预览”，修改当前草稿只标记预览过期。以上只调整设置页呈现，不改变 Velocity 输入、Markdown 输出或 Apply/Cancel 持久化语义。生成规则页较长说明使用可换行、左对齐的文本组件，仍保留原规则与顺序。

### 使用平台 searchable options，而不是只依赖 Configurable 标记

为页面和关键同义词提供中英文 searchable options；通过公开平台 `SearchableOptionContributor` 补充搜索词。维持 `build.gradle` 中 `buildSearchableOptions = false`，不额外运行后台 IDE 生成搜索索引；`SearchableConfigurable` 本身也不作为所有字段均可搜索的验收证据。目标 IU 261 类的公开签名允许 Java 实现，官方扩展点表将 `com.intellij.search.optionContributor` 列为 `SearchableOptionContributor` 且未标 Internal/Experimental。最终要在目标 IDEA 的 Settings 中用中英文词搜索并打开正确页面/控件，以实测结果判断是否需要调整。

### 集成凭据清除选项在窄设置页中完整显示

2026-10-22 的 IU 原生截图显示，900×700 logical Settings 窗口内 ShowDoc 密码框右侧的 “Clear saved credentials” 文案被内容区右边缘裁切。三个集成表单统一把清除凭据复选框放在对应密码框的下一行，并从密码框起始列横跨其余可用列；ShowDoc 的 API key 与 API token 各有独立清除行。完整本地化文案始终可见，不缩短、不改写或隐藏；密码框仍保持可输入宽度，状态与指南内容继续位于其后。

本项只改变表单几何布局。空密码仍表示保留已保存凭据，勾选清除仍表示明确删除，非空密码仍表示替换；布局回归仅检查控件边界、完整首选尺寸和对应关系，不填写、勾选、应用或读取任何凭据。任何凭据生命周期或存储变化都属于独立的 credential change，不能由本布局项代替验证。

## Risks / Trade-offs

- [把页面重组误做成全局配置] → 保留所有项目级 Configurable 的范围标记，并在原生 IDE 中分别打开两个项目，确认设置互不串用。
- [说明文字与真实生成优先级漂移] → 从生成代码核实来源顺序，为说明文本建立测试断言；任何顺序变化另走生成 contract。
- [预览任务阻塞 EDT 或旧结果覆盖新结果] → 渲染放到后台、使用单调请求序号并只在 EDT 应用最新结果；测试延迟完成的旧任务场景。
- [模板变量列表与 Velocity 实际暴露属性不一致] → 从 `DocViewData` API 建立可检验的变量目录，测试每个插入项均能在样例中解析。
- [平台 Settings 搜索不采用 contributor 提供的全部词项] → 先在目标 IDEA 中验证中英文搜索及结果定位；如实际结果不足，再基于该验证提出生成索引或其他平台能力调整。
- [移除死配置造成持久化 XML 留下无效旧字段] → 不读取该字段作为运行时行为；确认其不再进入设置 UI、状态哈希和当前状态类。其余设置 XML 字段和现有平台凭据保持原样。

## Migration Plan

1. 重组项目级设置页与父子入口，保存字段继续写入原项目状态。
2. 删除无效设置状态并更新缓存哈希；增加页面/状态/规则回归测试。
3. 在模板设置加入变量帮助、草稿恢复和虚构样例预览；验证默认模板 Markdown 与已有 golden 行为一致。
4. 注册平台 searchable option contributor，在目标 IDEA 中验证导航、搜索、Apply/Reset/Cancel、项目隔离及预览响应；保持 searchable options 生成任务关闭，除非手工验证给出调整依据。

此变更不迁移用户状态文件，不修改远程平台数据。若回滚代码，保留用户项目设置文件；移除的无效属性不会重新启用。

## Open Questions

无。
