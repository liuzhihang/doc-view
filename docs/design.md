# Doc View 产品与 UI 设计研究

调研日期：2026-10-18。代码基线：`develop` / `00f1f94`，插件 1.3.14，Java 21，目标 IU 2026.1（261）。

**状态：基线设计研究；实施状态另见路线图。** 本文保留 `00f1f94` 的源码、用户截图和官方资料研究，不替代 [路线图](roadmap.md) 或 OpenSpec requirements。模板生命周期的独立修复及原生验收见[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)。后续预览、平台设置、凭据、目录、配置和首页已分别建立 change 并进入实现/验证；下文的“当前”是评估基线，不表示尚未修复。最新操作以[用户指南](user-guide/index.md)为准。

首项 [`fix-template-settings-lifecycle`](../openspec/changes/fix-template-settings-lifecycle/proposal.md) 已获授权并完成当前工作树实现；状态细节以[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)为准。该修复未发布，当前已发布的 1.3.14 仍有旧行为风险，见下方设置生命周期说明。后续工作顺序以[路线图](roadmap.md)为准。

## 1. 产品方向与推荐顺序

建议把 Doc View 定位为 **IDE 内由源码驱动的 API 文档工作台**：开发者能找到接口、理解生成依据、调整模板、输出可追溯文档；AI 通过相同数据查询接口。

近期优先降低导航和模板配置成本，随后补齐文档身份、更新与检索能力，再接入 MCP。全仓库 AI Wiki 不纳入第一轮。

| 阶段 | 候选 change | 用户可观察结果 | 前置条件 |
| --- | --- | --- | --- |
| A0：模板生命周期 | [`fix-template-settings-lifecycle`](../openspec/changes/fix-template-settings-lifecycle/proposal.md) | 当前工作树中，Reset 回读最近一次 Apply 的模板；Cancel 丢弃未应用草稿；Apply 后再 Cancel 保留已保存结果 | 自动回归、Verifier 和原生操作见[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)；最终包重启核验已完成，修复尚未发布 |
| A0 后续：配置可靠性 | `fix-integration-settings-state`、`integration-credential-storage`（候选） | 空配置/ID 校验/取消行为可解释，平台跳转正确，凭据不与普通设置混存 | 根据设置评估分别复现与提案；凭据新存储单独验收，用户按新版重新配置 |
| A1：导航改进 | `api-tree-navigation` | 通过类名、方法名、业务名找到接口，区分同名方法，打开文档并返回源码 | UI contract、真实 IDE 验证 |
| A2：紧随其后 | `doc-view-configuration` | 找得到设置，看得懂规则，修改模板后能预览和撤销 | 新版模板 contract、设置范围与 Apply/Cancel 语义 |
| A3：轻量引导 | `doc-view-welcome` | 编辑器内完成入门、查看更新、进入模板帮助 | 首次使用/升级展示策略，避免打断 |
| B：可检索文档 | `doc-export-workspace`，必要时拆出文档元数据 change | 稳定目录、稳定身份、源码引用、可重复导出及过期提示 | 源数据正确性与新版输出 contract |
| C：AI 工具 | 独立 MCP 可行性验证，再考虑 `doc-view-mcp-tools` | AI 可按接口信息查询有来源的文档 | 目标 build、扩展 API、Java-only 与序列化验证 |
| D：知识组织 | 轻量 API Wiki；按需要评估 Air 协作 | 模块/服务/API/模型间相互导航 | 复用 B/C，不另建解析和存储体系 |

原路线图把 AI/MCP 排除在近期主线；本次根据用户要求，已将其作为后续候选与独立可行性研究记入路线图，未授权实施。原有 `requiredMode`、`@Parameter` 元数据和性能工作仍然保留：AI 访问不能放大错误的文档语义。

## 2. 当前证据与问题

| 证据 | 当前事实 | 对设计的影响 |
| --- | --- | --- |
| 用户图 1 | Translation 在编辑器页展示入门与更新 | 可借鉴承载位置和操作入口；不能从截图推断其实现 API |
| 用户图 2；[ClassNode](../src/main/java/com/liuzhihang/doc/view/ui/window/ClassNode.java)、[MethodNode](../src/main/java/com/liuzhihang/doc/view/ui/window/MethodNode.java) | 类节点使用文档标题，方法节点使用业务名称；图中有相同的“方法2”标签 | 业务标题不能替代代码身份，重载需要可区分的签名 |
| [plugin.xml](../src/main/resources/META-INF/plugin.xml) | 工具窗口声明 `anchor="right"`；设置主节点未指定 `parentId`，子节点用官方已标为 deprecated 的 `groupId` | 新安装默认位置与设置归属应单独设计；设置采用当前推荐层级，不保留旧 Configurable ID 别名或旧 XML 迁移逻辑 |
| 用户图 3 | 设置页混合名称来源、必填规则、文档显示和导出选项；存在禁用但勾选的控件 | 应按用户任务分组，解释来源优先级与不可修改状态 |
| [TemplateSettingForm](../src/main/java/com/liuzhihang/doc/view/ui/TemplateSettingForm.java)、[TemplateConfigurable](../src/main/java/com/liuzhihang/doc/view/config/TemplateConfigurable.java) | 已有模板变量清单和 Markdown 编辑器，但无渲染预览；当前工作树已修复模板设置生命周期 | 保留变量帮助；模板 Apply/Reset/Cancel 的原生验收见[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)，预览仍是后续建议 |
| [DocViewWindowPanel](../src/main/java/com/liuzhihang/doc/view/ui/window/DocViewWindowPanel.java) | 已有原生 Tree Speed Search 和后台刷新；树根可见，空状态文字被清空；刷新路径会在窗口隐藏时调用 `show()` | 改进搜索和状态反馈；刷新可能重新显示隐藏窗口，实际激活/焦点效果待 IDE 验证 |
| [CustomFileUtils](../src/main/java/com/liuzhihang/doc/view/utils/CustomFileUtils.java) | 打开 Markdown 使用 `ExtensionsRootType`，写入 Scratch 扩展目录 | 编辑器里能打开，不代表已成为项目内可移植的文档资产 |
| [MethodNode](../src/main/java/com/liuzhihang/doc/view/ui/window/MethodNode.java)、[ExportUtils](../src/main/java/com/liuzhihang/doc/view/utils/ExportUtils.java) | 文档路径包含展示名称；批量导出调用追加写入 | 引入索引前需验证重名、非法文件名、重复导出和覆盖语义 |
| [DocView](../src/main/java/com/liuzhihang/doc/view/dto/DocView.java) | DTO 持有 `PsiClass`、`PsiMethod`；已有路径、方法、参数、响应、标签等数据 | 对外查询应投影为无 PSI 的文档快照，不直接序列化当前对象 |
| [Markdown golden sample](../src/test/testData/contracts/markdown/spring-basic.md) | 默认输出以加粗文字分段，已有精确输出回归契约 | 输出结构变化时可直接采用新版 contract，并更新现有 golden 与指南；不保留双轨输出或旧模板变量适配 |
| 用户图 4、5 | Agent Sessions 和 Agents 列表展示另一套 agent 工作入口 | 只能作为交互参考，不能证明 Doc View 已可与之集成 |

以上 UI 观察来自用户截图与静态代码。原设计调研没有启动 IDE，也未测量真实响应时间、键盘路径、屏幕阅读器或主题对比度；模板生命周期由后续独立 change 做了原生验收，见其[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)。批量导出的追加写入是需要补复现的风险，不能写成已完成运行时验证的缺陷。

## 3. 工作台与导航

### 3.1 侧栏位置

推荐新安装默认左侧，因为计划中的主要任务是从项目结构定位文档；文档内容在中央编辑器阅读。右侧适合与 Project 同时展开、作为辅助检查窗，因此仍是合理的用户选择。

这属于 Doc View 的产品选择，并非 JetBrains 强制要求。官方 [UI Overview](https://plugins.jetbrains.com/docs/intellij/ui-overview.html#stripes) 描述了左右 stripes 和编辑区分工，没有规定文档插件必须放在哪一边。

新安装建议默认放左侧；用户可用 IDE 自带 Move To 等能力调整位置，不新增与平台重复的“左侧/右侧”设置。升级时沿用 IDE 自身的布局保存行为，不增加插件级布局迁移、回退逻辑或跨版本验收，也不主动重置用户布局。支持用户隐藏窗口；启动或后台刷新不应为了更新数据而主动夺取焦点。

### 3.2 树节点与搜索

建议视觉示例，括号说明颜色角色，不是实际节点文案：

```text
doc-demo
  UserController                       用户相关接口（次要文字）
    getUser                              查询用户（次要文字）
    createUser                           创建用户（次要文字）
  UserFacade                           用户服务（次要文字）
    queryUserAccount                     查询账户（次要文字）
```

- 主文字为原始类名/方法名；方法行不附加参数括号和类型。完整签名放在 tooltip 和速度搜索中，重载保留独立节点，避免长参数列表挤占目录宽度。
- 同行次要文字为注释/注解解析的业务名；为空或与主文字重复时省略。
- 使用 IDE 主题中的 secondary text 样式，兼顾选中态；不写死灰色，不用颜色作为唯一的信息区分。
- 方法保留原生方法图标；HTTP method/path 可以出现在详情或 tooltip，首轮不强塞进每一行。
- 隐藏重复的“Doc View”树根，先保留模块 → 类 → 方法这一条清晰层级。path/tag 分组后续按需求增加。
- 搜索覆盖代码名、完整方法签名和业务名；路径检索基于已有轻量元数据实现，避免在 renderer 或每次按键时生成完整文档。
- 保留现有双击/Enter 打开文档语义；增加清晰的“转到源码”动作，键盘可达。
- 工具栏保留高频的刷新、搜索入口和必要的折叠操作；两个导出入口收纳到一个 Export 下拉菜单，其它低频清缓存、批量上传等放到 More/context menu，并明确操作范围。

应区分“正在索引”“正在查找接口”“没有支持的接口”“筛选无结果”“加载失败”状态，每种状态给对应下一步，而不是统一空白。

### 3.3 图标一致性

插件的通用动作统一使用目标 IDEA 提供的 `AllIcons`，覆盖工具窗口、目录右键菜单、预览与参数编辑器及程序化 popup。节点图标继续使用 `AllIcons.Nodes`。Doc View 品牌图标作为例外保留为单色主题资源；通知复用 16×16 品牌资源，传统工具窗口使用 13×13，New UI 工具窗口使用 20×20 与 Compact Mode 16×16，gutter 使用 12×12 与 New UI 14×14。官方图标指南明确列出的尺寸分别适用于 tool window、New UI tool window 和 gutter；通知的 16×16 是本项目的资源选择。Plugin Manager 的 `META-INF/pluginIcon.svg` 单独维护为 40×40 plugin logo；其它动作不使用自绘或复制来的通用操作 SVG。工具窗口两个导出操作在一个 Export 下拉菜单中按名称区分。

这一选择符合 JetBrains 图标指南中优先复用平台图标的建议；工具栏动作仍按原有操作组织，只有重复的导出入口收拢。参考 [Icons style](https://plugins.jetbrains.com/docs/intellij/icons-style.html)、[Working with Icons](https://plugins.jetbrains.com/docs/intellij/icons.html) 与 [Toolbars](https://plugins.jetbrains.com/docs/intellij/toolbar.html)。具体实现及验收范围见 [api-tree-navigation contract](../openspec/changes/api-tree-navigation/specs/api-directory-navigation/spec.md)。

### 3.4 原生 UI 取舍

遵循 [JetBrains 平台组件](https://plugins.jetbrains.com/docs/intellij/user-interface-components.html)：原生 Action System、树、工具栏、边框、字号和间距，优先复用当前 Java/Swing 结构。

官方推荐设置表单使用 Kotlin UI DSL，但本仓库明确 Java-only；本轮建议不为布局更换生产语言。可用 Java 的平台组件实现相同信息层级，API 选择仍需目标版本与 Verifier 检查。

## 4. 编辑器首页、帮助与设置

### 4.1 首页

当前实现提供可重新打开的 `Doc View Welcome` 编辑器标签页，承载项目介绍、三步快速开始、模板入口和当前版本变更摘要。

优先内容顺序：

1. 打开接口目录，按类和方法找到需要查看的入口。
2. 使用默认模板预览，再按需要进入模板编辑。
3. 复制或导出；上传平台属于可选步骤。
4. 本版本变化、完整更新记录、帮助和反馈。

无需配置平台凭据即可完成首份本地文档。首次使用自动打开欢迎页；升级时按应用级版本状态显示一次通知，由用户选择打开更新页；更新提醒可以关闭。首页不触发全项目扫描，也不依赖在线页面显示。

本轮已按用户要求以随包 HTML/CSS 和 IDE JCEF 实现欢迎/更新页；使用指南只维护在 GitHub，设置帮助在默认浏览器打开对应页面。实际通过与待补范围见[本轮验证记录](validation-2026-10-20.md)及各 change verification，不以设计稿作为验收证据。Translation/IDE 更新页仅作为信息层级和留白参考，不复制内部 API。

### 4.2 设置结构

补充评估见[设置与用户文档评估](settings-audit.md)：逐项核对设置消费路径、三平台状态问题和现有语雀指南。当前设置页已按常规行为、生成规则、模板与输出、平台集成四类组织；模板与输出合并，避免为单个导出开关单建页面。

建议放在 `Settings → Tools → Doc View`，保持项目设置的现有作用域。官方 [Settings Guide](https://plugins.jetbrains.com/docs/intellij/settings-guide.html) 明确不推荐 `other`，并使用 `parentId` 建立层级。

建议子页：

| 页面 | 用户任务 |
| --- | --- |
| Doc View / 常规 | 选择接口扫描范围、编辑器行标记，了解项目作用域并进入用户指南 |
| 生成规则 | 理解名称来源、字段命名、必填规则及其实际生效顺序 |
| 模板与输出 | 修改 Spring/Dubbo 模板、查变量、预览输出、恢复默认；管理分表、层级前缀、合并导出 |
| 平台集成 | 按需进入 YApi、ShowDoc、YuQue 配置，说明所需字段与获取途径 |

命名、项目内目标目录、覆盖策略等新增导出能力随 B 阶段独立设计，功能足够多时再拆 Export 页面。

2026-10-20 原生反馈要求生成规则页减少常驻说明和明显方框：分组使用 IDEA 原生浅分隔线及留白，组级说明图标提供完整来源顺序与固定规则，页面仅保留短灰字并按可用宽度换行。不为每个复选框重复放置说明图标；详细规则继续由 GitHub 指南维护。[上下文帮助规范](https://plugins.jetbrains.com/docs/intellij/context-help.html)

首轮只分组和解释已有行为，不顺便增加可拖拽优先级或改变默认解析规则。对勾选但不可改的规则，逐项核实并说明具体原因；“始终启用/由框架决定”仅是候选文案，需符合实际机制，不能从禁用外观直接推断。

`build.gradle` 当前关闭 `buildSearchableOptions`。后续要验证实际的中文/英文设置关键字搜索，不把实现 `SearchableConfigurable` 等同于所有字段都可搜到。设置层级可使用当前推荐的 `parentId` 和新版 ID；不为旧 Configurable ID 增加别名，也不迁移旧 XML 字段。本 change 已验证模板生命周期的目标 IDE API、项目隔离和 Apply/Reset/Cancel 持久化；设置分类和当前60个中英文关键词的原生页面归属核对已完成；其他设置页的完整保存、隔离与重启矩阵仍按 tasks 保留。版本演进边界见[兼容性策略](intellij-compatibility.md#版本演进策略)。

**模板设置生命周期状态。** 当前工作树已修复原先 `reset()` 覆盖已保存模板的问题。现在 Reset 将编辑器内容回读到最近一次 Apply/保存的值，不代表恢复出厂默认；Apply/OK 保存当前编辑内容，Cancel 丢弃未应用草稿；如果先 Apply、再继续编辑并 Cancel，已 Apply 的值仍保留。修复尚未发布，当前已发布的 1.3.14 仍有旧行为风险；本 change 的详细验证记录见[验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)，最终包重启核验已完成。后续配置 change 才考虑单独的“恢复默认”动作；不新增 Cancel 快照回滚。

### 4.3 模板学习闭环

2026-10-18 根据用户对设置密度的反馈，当前实施决定为：**模板编辑、预览结果、输出选项使用三个独立页签**，默认只展示编辑区；变量目录按需展开，变量属性名和中文名称分行，完整表达式与用途在独立可换行详情中显示。保持 IDE 原有字号，给长表达式和说明预留阅读空间，不用缩小字体容纳更多控件。预览只使用固定虚构样例，不读取当前接口。实现契约和验证分别见 [`doc-view-configuration` spec](../openspec/changes/doc-view-configuration/specs/configuration-experience/spec.md) 与 [verification](../openspec/changes/doc-view-configuration/verification.md)。

下列为最初研究方向；其中“编辑与预览并排”和“选择当前接口作为输入”未被本轮采用，以前述实施决定为准。

模板页的核心是同一处完成“编辑 → 看示例 → 确认结果”：

- 模板契约覆盖当前支持的 Spring/Dubbo；新增 profile 需独立评估。若新版需要改变模板格式或变量，直接定义单一新版 contract，并同步更新 golden 与用户指南，不维护旧变量适配或并行模板路径。
- 默认提供可离线运行的虚构样例，用户也可主动选择当前接口作为输入。
- 模板编辑与渲染预览并排，窄窗口可切换；预览用编辑中的值，不提前保存设置。
- 变量说明能搜索，展示名称、类型、用途、空值语义和小例子，并支持插入。
- 提供一个“调整标题”和一个“删减参数列”的完整教程，说明预期结果。
- 错误指向模板位置和原因，保留输入；预览失败不覆盖最后保存的模板。
- 若后续增加“恢复默认”显式动作，应只修改编辑副本，Apply/OK 才持久化；当前 IDE 的 Reset 是回读最近一次 Apply，不是恢复默认，Cancel 丢弃未应用草稿。

帮助按任务组织：首次生成、理解名称/必填来源、修改模板、导出/上传、常见失败。不把贡献者调试说明当用户指南。默认样例、帮助中的变量和实际渲染模型要共用可核对的来源。

### 4.4 文档来源与阅读入口

**2026-10-19 用户澄清后的决定：GitHub 维护完整文档，IDE 编辑区只提供本地 HTML 欢迎与更新说明。** 使用指南、模板教程、平台配置和故障排查不打包为离线手册，也不维护另一套语雀正文。本轮已将运行时切换到该结构；原七章阅读器不再作为备用实现，当前轮验证仍在进行。

| 内容/方式 | 取舍与决定 |
| --- | --- |
| GitHub `docs/user-guide/` | 唯一完整文档源，按任务组织短步骤和示例；设置帮助直接链接相应页面。 |
| 随包 HTML 欢迎/更新页 | 已采用。打开不依赖网络，显示简短引导和本次更新摘要；内容随插件发版。 |
| 远端 HTML 首页 | 可独立更新，但引入网络、内容漂移及远端与 IDE 动作边界；当前没有必要，不采用。 |
| 本地外壳 + 自动远端正文 | 增加加载、缓存和失败路径，不采用。 |
| 内嵌语雀/GitHub 手册 | 不作为首页；用户点击后用外部浏览器阅读。 |

页面以品牌与安装版本、主操作、三步摘要、少量更新亮点和页尾链接组成。内容控制宽度、保留区块留白，三步说明纵向排列；不堆多年更新记录或完整教程。开发构建的未发布变更明确标注为预览，不能当作正式版本介绍。

使用目标平台公开 `JBCefBrowser` API 作为单一 HTML 渲染路径，并由项目内 `FileEditor` 持有 browser；Java 控制主题、动作与生命周期，静态 HTML/CSS 不引入新生产语言或前端框架。JCEF 不可用时显示简短提示，不维护旧 Swing reader fallback。依据：[JetBrains JCEF](https://plugins.jetbrains.com/docs/intellij/embedded-browser-jcef.html)。详细 contract 和验证范围见 [design](../openspec/changes/doc-view-welcome/design.md)。

GitHub 指南已在本地补全，尚未推送前对应公开链接可能暂时返回 404；获准推送后需单独验证远端正文。欢迎 UI 原生验收、GitHub 文档发布和外链可达性分别记录，不相互替代。

## 5. 文档格式、项目目录与索引

### 5.1 文档格式按新版 contract 演进

未来可直接以新版 Markdown contract 替换当前输出格式，例如使用语义明确的标题、请求/响应章节、源码引用和稳定链接。每次格式或模板变量变化，都在同一 change 中更新 contract、现有 golden 与用户指南；不保留旧/新双轨输出、旧模板变量适配或选择性兼容开关。用户自定义模板可能需要按新版说明重新配置；插件不得借此自动清理用户文件或设置。该原则不要求本轮立即改动运行时输出。

项目内文档目录可选用 `docs/api/`，包含目录页与按模块组织的接口文档；具体命名在导出 contract 中确认。一次一个接口的块更便于引用和局部读取，合并导出仍可保留。

供机器读取的数据应从文档模型投影，而不是反向解析用户可任意改写的 Markdown。可先在查询服务中定义快照，确有文件集成需求时再输出版本化 `catalog.json` 或 sidecar，避免一开始维护多份内容。

建议快照包含：

- `schemaVersion`、framework、module、类全名、方法签名。
- 可重复计算的接口 ID；业务标题不作为 ID，重载/多模块/映射变体需要区分。
- HTTP method/path，或 Dubbo service/method；不将所有框架强行解释为 HTTP。
- 参数、响应和已解析的说明；保留未知、截断或未支持的信息，不补写猜测。
- 项目相对源码路径和定位信息；行号是辅助定位，不是稳定身份。
- 来源版本/内容指纹、设置与模板版本、生成状态，以判断文档是否过期。

“稳定 ID”首先保证相同输入重复生成一致，不承诺类/方法重命名后自动保持不变。导出 contract 需定义重命名、重复导出和人工编辑文件的处理；不得自动清理用户文件。不要用每次变化的时间戳制造无意义正文 diff。

现有 DTO 含 PSI 引用，对外快照应去掉 PSI 和设置凭据。内部 PSI 生命周期、schema 缓存和快照失效须保持一致；不能只看 Controller 修改而漏掉关联 DTO 或模板变化。

### 5.2 三种检索不是同一件事

| 层次 | 用途 | 建议 |
| --- | --- | --- |
| IDEA 文件/符号索引 | 查文件、词和代码声明 | 先复用 Java 现有索引与普通项目 Markdown 文件 |
| Doc View API 查询 | 查业务名、方法、路径并生成准确文档 | 轻量 API 目录 + 按需解析 + 项目级失效策略 |
| AI 上下文检索 | 由具体 agent 选择文件或调用工具 | 项目文档、显式上下文、MCP；单独验证各客户端 |

[Indexing and PSI Stubs](https://plugins.jetbrains.com/docs/intellij/indexing-and-psi-stubs.html#performance-metrics) 介绍 IDE 索引机制与性能指标，不是“把内容送进 AI 知识库”的注册入口。Stub 主要用于声明结构；Doc View 不需要为 Java/Markdown 另造一种语言 Stub。

先将需要分享的生成文档输出到用户选择的项目内容目录，刷新 VFS 并检查 IDE exclusion；验证能被文本搜索定位。Scratch 文件的实际搜索范围另测，不能断言它们完全不会被索引，也不能假定 CLI agent 自动能看到。

自定义 FileBasedIndex 仅在普通搜索/现有索引不足且有实测需求后考虑。官方 [File-Based Indexes](https://plugins.jetbrains.com/docs/intellij/file-based-indexes.html) 要求索引结果只依赖输入文件内容，因此不能在 indexer 中跨文件递归解析 DTO、读取项目模板或生成整份 Markdown。可索引已落盘文档自身的轻量元数据，完整文档按需生成。

需定义 Dumb Mode 降级：明确返回暂不可用，或显示带过期标记的已有快照，不阻塞 EDT，不无限等待。新增索引前后记录官方 indexing diagnostics、查询延迟和项目规模。

[AI Assistant 文档](https://www.jetbrains.com/help/ai-assistant/chat-mode.html) 支持显式文件/文件夹上下文，且 Codebase Mode 受 `.gitignore` / `.aiignore` 影响。IDE 能搜索文件不等于每个 agent 会自动使用它；Doc View 不应擅自改写用户忽略规则或 Agent 指令文件。

## 6. MCP、Wiki 与 Air 的共同边界

建议依赖方向：源码解析 → 无 PSI 的接口快照 → 原生 UI、Markdown/目录、只读 MCP。各入口共用解析语义，避免维护第二套“AI 专用解析器”。

### 6.1 MCP 可行性：有入口，先验证 Java 与版本边界

已核对用户指定的 [262.10968.63 descriptor](https://github.com/JetBrains/intellij-community/blob/idea/262.10968.63/plugins/mcp-server/resources/META-INF/plugin.xml)：插件 ID 为 `com.intellij.mcpServer`，包含 `mcpToolset`、`mcpToolsProvider` 等动态扩展点。官方 [Community 插件扩展点清单](https://plugins.jetbrains.com/docs/intellij/intellij-community-plugins-extension-point-list.html) 的 2026.2.3 列表未给这两个入口标记 Internal/Experimental；这不是跨版本稳定性承诺。用户链接的 `#comintellijtasks` 是 Tasks/issue tracker 区域，不是 MCP 入口。

[同 tag 的 MCP 开发说明](https://github.com/JetBrains/intellij-community/blob/idea/262.10968.63/plugins/mcp-server/README.md) 以 Kotlin 工具函数和 `kotlinx.serialization` 为中心；[反射实现](https://github.com/JetBrains/intellij-community/blob/idea/262.10968.63/plugins/mcp-server/src/com/intellij/mcpserver/impl/util/toolsetReflection.util.kt#L105-L108) 同时支持 suspend/non-suspend 调用。因此 **Java-only 并非已证实不可行**，优先用 Java 普通方法、简单参数、Markdown String 结果做最小验证。任意 Java DTO 的 schema/序列化不能直接套用 Kotlin 示例；如改走底层 `McpTool.call`，还涉及 suspend 的 Continuation 桥接。在编译与真实调用通过前，不宣称 Java 集成已支持，也不默认引入 Kotlin。

当前项目支持 261，所给源码属于 262。补充核对 [261.22158 descriptor](https://github.com/JetBrains/intellij-community/blob/261.22158/plugins/mcp-server/resources/META-INF/plugin.xml) 已有核心 `mcpToolsProvider` / `mcpToolset`，不是只能在 262 研究；但 262 新增接口不能回填为 261 能力。可行性验证需要固定一个实际 261 build 和一个候选 262 build，检查依赖可见性、工具注册、取消、线程和项目生命周期。若实际 261 无法满足，应选择限制可选集成的支持范围或推迟，不借 MCP 功能默默提高核心插件平台基线。

建议把 MCP 依赖隔离为可选集成：只有对应 bundled plugin 可用时才加载适配器；禁用 MCP 后，原有导航、生成和导出仍可运行。具体 descriptor 与打包方式由目标版本验证决定，不复制一段未经验证的配置到正式插件。

MCP 首轮候选能力仅为 `search_api_docs` 与 `get_api_doc`：搜索返回有上限的摘要和接口 ID，详情按 ID 返回参数、响应、来源与过期状态。工具名是设计占位名称，并非已注册 API。

搜索输入应有项目/模块范围、关键词或方法/路径过滤、分页/结果上限；返回错误应区分索引中、源码无效、未支持和已过期。查询不触发上传、源码写回、HTTP 请求或批量导出。Markdown、Javadoc 等内容作为数据返回，不作为 agent 指令。

项目内 Markdown 能先被现有文件工具读取；专用工具的价值在于稳定的接口检索与结构化结果，而不是再次实现文件读取。内置 MCP Server 的存在不证明第三方扩展 API 稳定，需针对项目支持的 build 单独验证。

### 6.2 Wiki：先做 API Reference，暂不拆独立插件

[Qoder Repo Wiki](https://docs.qoder.com/user-guide/repo-wiki) 面向整个仓库的结构与实现知识，持续跟踪变化，供 agent 理解架构与开发任务；这比接口参考文档范围更大。其 Wiki 资料位于 IDE/CLI 产品文档，[JetBrains 插件介绍](https://docs.qoder.com/plugins/introduction) 未列出 Repo Wiki，不能据此宣称该插件已有完全相同能力。

Wiki 优先做同一插件中的 API 文档组织：模块概览、接口目录、模型引用和源码链接。跨语言架构总结、业务流程推断、LLM 调用与自动维护属于另一层产品能力，不能从现有 DTO 导出直接推导为已具备。

完整 Wiki 还需要模型配置/费用、事实来源与引用、分支/增量更新、人工修改保护和生成内容质量验证。近期不新增 LLM 依赖、向量库或独立 Wiki 存储；先验证 API Reference 是否解决实际浏览与检索问题。

### 6.3 Air：作为文档消费者，而非核心依赖

官方 [Air in IDEs 产品页](https://www.jetbrains.com/air/ides/) 将其定位为 agent 工作空间。[Air 概览](https://www.jetbrains.com/help/air-ides/air-overview.html) 当前说明 2026.2 通过 Marketplace 安装、2026.3 EAP bundled，且仍属于探索中的产品方向；这不能证明项目最低基线 2026.1 已支持它。

ACP 连接 IDE 和 coding agent；MCP 让 agent 调用工具。官方 [自定义 agent 配置](https://www.jetbrains.com/help/air-ides/air-manage-agents.html) 提供 `use_idea_mcp` 和 `use_custom_mcp` 选项。因此 Doc View 的合适角色是提供文档数据与查询能力，不是实现一个 ACP agent。Air 是否能发现并调用第三方 Doc View 工具，仍需真实会话验证。

Air 协作优先通过可读的项目文档与 MCP；不在 Doc View 内复制 Agent Sessions、模型选择或 agent 执行器。是否需要单独 Wiki 插件，应由全仓库能力出现独立用户、依赖与发布节奏后决定。

## 7. 后续验收设计

以下候选场景尚未全部执行；模板生命周期专项验收见其[change 验证记录](../openspec/changes/fix-template-settings-lifecycle/verification.md)：

| 范围 | 可观察的完成信号 |
| --- | --- |
| 导航 | 新安装默认位置正确；既有左右位置不被覆盖；代码名/业务名可搜索；重载可区分；文档与源码定位准确 |
| 窗口状态 | 索引中、空项目、取消、失败、项目关闭均有正确反馈，无后台任务强制重新显示窗口 |
| 主题/可访问性 | 深浅主题、缩放、窄侧栏、长中文标签、键盘导航、选中态次要文字和屏幕阅读器标签逐项核对 |
| 模板 | Apply/Reset/Cancel、重开与持久化已按专项 change 验收；默认模板首份文档、实时预览和错误定位仍属候选验证 |
| 输出 contract | 新版格式更新现有 golden 与用户指南；按变更范围覆盖 Spring/Feign/Dubbo；不维护旧/新双轨输出或旧模板变量适配 |
| 导出 | 重复导出不追加重复正文；重名和路径字符处理确定；人工编辑文件覆盖可控且不自动清理用户文件；源码或关联 DTO 变化可发现过期 |
| 检索 | IDEA 普通搜索、Doc View 查询与所选 AI 客户端分别记录结果；缓存命中不冒充索引或 AI 验收 |
| MCP | 目标精确 build 能发现/调用 Java 实现工具；插件禁用、Dumb Mode、多项目、取消和动态卸载行为正确 |

实施时按 [Contract 设计](contract-design.md)、[性能指南](performance-guide.md)、[兼容性策略](intellij-compatibility.md) 和 [发布检查清单](release-checklist.md) 选择实际检查。先定义 contract，再用最小可证明范围实施；不能把本设计稿或链接检查视为 UI/MCP 已通过。

## 本轮设置密度修订

用户在实际模板页反馈编辑、变量、预览与输出选项同时常驻导致拥挤。第6项改为“模板编辑 / 预览结果 / 输出选项”三页签，默认聚焦编辑；变量目录按需展开，预览和输出拥有独立空间。表单使用清晰行距、左对齐标签与可换行说明，验收应覆盖常用设置窗口尺寸，不依靠最大化窗口隐藏布局问题。具体行为与验证以 [配置 change](../openspec/changes/doc-view-configuration/design.md) 为准。
