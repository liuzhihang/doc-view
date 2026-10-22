# 验证记录

## 2026-10-20 续验与 UI 反馈

调整前基线为43 suites / 207 tests，生成规则 UI 修订后重新完整执行为43 suites / 208 tests；两者均为0 failures/errors/skipped、18 tasks实际执行，IU261 Verifier Compatible。包身份分开记录在[本轮验证](../../../docs/validation-2026-10-20.md)。

IU262隔离doc-demo逐词核对 `DocViewSearchableOptionContributor` 的全部60个中英文词，对应页面breadcrumb全部正确，task3.1b完成。原始逐项记录 `/tmp/doc-view-validation-20261020/search-{0..59}.txt`、`search-results.json`；分类树离屏点击限制不是搜索失败。常规页另验证Revert/Cancel/Apply，但不据此完成3.4b的全部设置/隔离/重启矩阵。

用户追加要求将来源顺序/固定规则收进原生说明图标，页面只保留可换行短灰字，并去除明显四边框；已先修订proposal/design/spec/tasks，新实现单独取得RED/GREEN和新包验收。生成规则布局专项3项、配置专项5项通过；原生浅/深主题2268×1694下核对浅分隔线与短灰字，Revert/Cancel/Apply后重开正常。生成规则单项已完成退出落盘及独立重启持久化检查，最终恢复原值；两份doc-demo设置XML与备份字节一致，普通doc-view窗口已恢复。帮助名称和完整说明通过AX读回；原生窄窗口及悬浮气泡仍待验，未勾选4.2b。


## 实现与描述符核对

插件描述符把 `Doc View` 注册到 `Tools` 下，`Generation Rules`、`Template & Output` 和 `Platform Integrations` 是它的子页；YApi、ShowDoc、YuQue 三页是 `Platform Integrations` 的子页。七个页面均保留项目作用域（`nonDefaultProject="true"`）。公开的 `search.optionContributor` 已注册，Gradle 的 `buildSearchableOptions` 保持关闭。

生成规则页包含 12 个可选规则控件，并用说明文字列出固定规则和来源优先级。普通接口识别与 gutter 行标记分别保存。模板与输出页提供“模板编辑 / 预览结果 / 输出选项”三个工作区，Spring/Dubbo 原生编辑器仍保留各自子页签；变量侧栏默认收起，输出选项单独纵向排列。预览采用固定虚构 DTO，在后台生成 Markdown 源码；模板编辑会使已排队或已完成但尚未交付的旧预览失效。输出项的 Apply 只保存这四项及当前两份模板，不覆盖常规行为或生成规则。

## 自动验证

- `./gradlew test --rerun-tasks`：14 个 Gradle 任务执行，96 tests、0 failures、0 errors、0 skipped。结果 XML 已备份到 `/tmp/doc-view-reliability-20261018/round7-TEST-*`，摘要为 `/tmp/doc-view-reliability-20261018/round7-summary.json`。
- 本轮包含配置描述符与搜索契约 4 项、模板/预览/输出设置 6 项、模板保存与编辑器生命周期 10 项；全套测试均通过。
- `git diff --check` 针对本 change、用户指南和 CHANGELOG 检查通过。
- `openspec validate doc-view-configuration --strict`：Change valid。
- `verifyPlugin` 首次检查发现 3 处 deprecated API，未通过。`TemplateSettingForm` 的文档监听已切换到 IntelliJ 的 Disposable-owned 重载，并在表单释放时销毁 owner；其余两处由主 Agent处理。最终 round8 已修正并通过，见下方整批门禁。

验证过程中发现并修正了 IntelliJ `Document` 监听器误用 Swing `DocumentListener`、导航谓词对基础 `Configurable` 直接调用 `getId()`，以及测试移除辅助方法时误删仍被使用的 `java.lang.reflect.Method` 导入。一次 round 6 全量运行有 96 tests、93 passed、3 failed：`TemplateConfigurationExperienceTest.testOnlyLatestPreviewResultIsDeliveredAndFailuresAreVisible` 假定已失效的排队任务仍会进入 UI 队列；`WelcomeLifecycleTest.testReopeningHelpReusesProjectVirtualFile` 和 `IntegrationCredentialApplyTest.testFailedApplyKeepsDraftAndStateThenRetryKeepCancelAndClearAreExplicit` 的 fixture 分别由对应实现负责人修正。模板预览用例现先启动旧渲染再触发新请求，确认旧结果晚到时会被丢弃；最终 round 7 全量运行为 96/0/0/0。

## 尚未验证

- 最新 IU 262 原生验收包含 2268×1694 浅色主题下模板变量详情可读，以及切换到深色主题后取消并重开时变量目录收起、Apply 禁用；这不证明窄窗口、其他缩放比例或所有主题状态均通过。项目隔离、关闭 IDE 后重启恢复仍待验；设置搜索在2026-10-20已完成当前贡献的全部60个中英文关键词，见本页顶部记录。
- 此处早期只确认行标记和普通接口开关的持久化草稿状态；当时 `includeNormalInterface=false` 后的实际扫描未验。该缺口已由本页2026-10-22的af61五类→三类→五类原生对照补齐，仍不代表项目隔离或其它设置矩阵完成。
- 本轮没有在真实业务项目中验证完整文档生成、项目间设置隔离或远程平台上传；固定虚构样例和自动测试不替代这些验收。

## 用户指南示例执行

2026-10-18 新增 `TemplateGuideExamplesTest`，直接提取 `docs/user-guide/templates.md` 的可复制代码段，避免测试维护另一份模板：内置 Spring/Dubbo 与资源逐字一致；标题/Param 示例的虚构输入对应预期输出；递归宏覆盖三层、true/false 和空 childList。初轮 3 项中 2 项失败来自测试样例 Param 未设置 required 导致现有 DTO unbox NPE，补齐真实生产参数应有的值后 `./gradlew test --tests '*TemplateGuideExamplesTest'` 为 3/3 GREEN，0 error/skip。没有因此修改运行时代码。原始 XML：`/tmp/doc-view-reliability-20261018/guide-examples-green.xml`。用户指南已去除“宏未在当前环境执行”的过时说明，保留原生设置页手工验收边界。

## 整批门禁与帮助草稿保护

2026-10-18 round8 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：18/18 Gradle tasks 实际执行，96 tests、0 failure/error/skip，IU 261.22158.277 Compatible，未报告 deprecated/experimental API。证据 `/tmp/doc-view-reliability-20261018/round8-summary.json`、`verifier-1-7-green/` 和 `package-1-7-green.json`。

随后 `ConfigurationExperienceTest` 增加真实帮助按钮 ActionListener 测试：同时保留常规设置、生成规则及 Spring/Dubbo 模板的未保存草稿，依次点击四个页面的指南入口；核对目标章节、草稿、modified 状态和完整保存状态不变。round9 该类 5/5 GREEN；原始 XML `/tmp/doc-view-reliability-20261018/ConfigurationExperienceTest-round9.xml`。此处验证实际按钮处理器与项目虚拟文件，light fixture 文件管理器不证明原生首页渲染和焦点体验。

## IU 262 第一轮原生布局反馈（历史记录）

主代理报告：在 IU 262.10968.63 隔离 profile 打开 round21 构建包时，模板页的 `JSplitPane` 显示 Metal 点状分隔条和折叠箭头；变量说明挤在列表单行而被截断，搜索提示不可见；初次打开、尚未预览时状态误显示“模板已修改”，预览成功后仍显示提示用户开始预览的文案。生成规则页的长说明也被单行裁切；截图记录为 `/tmp/doc-view-reliability-20261018/native-generation-rules-before.png`。这是主代理的原生观察，本子任务未独立操作 IDE 复现。

按反馈已在实现草稿中改用平台 `OnePixelSplitter`、给搜索框设置平台 empty text 和无障碍名称、把所选变量说明放入可换行详情区；模板预览状态现在区分初始、成功和已过期，设置页 `reset()` 会使旧任务失效、清空旧预览并回到初始状态。生成规则页的长说明布局由另一实现负责人改为左对齐可换行文本，没有改动规则内容或优先级。`TemplateConfigurationExperienceTest` 已补布局、搜索、说明及状态用例，`GenerationRulesLayoutTest` 由对应负责人新增；本轮尚未运行 Gradle，不能将这些改动记为自动验证通过。

当时尚待的自动和原生复核已由后续 Round 28/29 记录部分覆盖；未覆盖的范围以本文件“尚未验证”和 tasks 当前状态为准。

## Round 29 整批门禁与模板低密度布局原生验收

`./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 成功：37 个测试套件、185 tests、0 failure、0 error、0 skipped；Verifier 结果为 IU 261 Compatible。Round 29 ZIP SHA-256：`92879354d4be39148439627e573b76579bcecfec680c98a432624a7bdee2c029`；在隔离 profile 安装的插件 JAR SHA-256：`a41b3d1a4606d8a95911ac10e0b75b4c898394d274955a56e631a6c2665870d3`。

在 IU 262 隔离 profile 加载该包后，于 2268×1694 Settings 窗口观察到：首次打开默认是模板编辑页且变量侧栏收起；展开侧栏后搜索 `responseExample`、将变量插入草稿并收起侧栏均可操作；点击 Spring 预览自动进入结果页并显示完成状态。Dubbo 模板页的预览也自动进入结果页，输出包含 `DemoUserService.findUser`。输出选项页以纵向布局显示控件。Revert 后模板编辑页回到默认首行，Apply 禁用；Cancel 退出后重新打开仍默认选中模板编辑页。

截图与辅助功能证据：`/tmp/doc-view-reliability-20261018/native-template-low-density.png`、`native-template-preview-workspace.png`、`native-template-output-workspace.png`、`native-template-three-workspaces-revert-ax.txt`、`native-template-dubbo-preview-ax.txt`。尝试将窗口缩至 1800×1400 未成功；`native-template-low-density-narrow.png` 的实际图像仍是 2268×1694，本记录不将它作为窄窗口证据。

同一 Round 29 原生会话还核对了 `doc-demo` 项目常规页两个开关的保存生命周期：该项目会话初始 `lineMarker` 与 `includeNormalInterface` 均开启；把行标记关闭、普通接口保持开启并 Apply 后，Apply 按钮禁用；Cancel 后重开仍观察到 false/true。随后将普通接口草稿关闭并点 Revert，恢复普通接口开启、行标记仍关闭；最后只将行标记开启并 Apply，恢复两者均开启。最终状态的 XML 安全只读读回为 `includeNormalInterface=true`；`lineMarker` 字段没有显式序列化，按 [Settings.java](../../../src/main/java/com/liuzhihang/doc/view/config/Settings.java) 的字段默认值 `true` 解释，不能把字段缺省当作 false。`Settings.java` 的代码默认值是 `lineMarker=true`、`includeNormalInterface=false`，因此上述初始两项开启是该项目的会话状态，不是新项目的通用默认值。证据：`/tmp/doc-view-reliability-20261018/native-settings-independent-applied.png`、`native-settings-independent-reopen-ax.txt`、`native-settings-independent-revert.png`。这些证据只确认设置控件和持久化/草稿状态，不证明普通接口关闭后实际扫描行为。未采用 debounce 前的 `independent-apply-ax` 或 `independent-switches.png` 中间态。

上述原生验收不包含项目隔离、主题切换、重启后状态恢复或完整 Settings 搜索词矩阵；这些仍留在 tasks 3.1b/3.4b。也未原生触发模板空结果和错误文案，详见 task 2.3b。Round 28 针对体验/生命周期的 33 项测试中，`TemplateConfigurationExperienceTest` 7/7、`TemplateSettingsLifecycleTest` 10/10 通过；最终全量门禁结果见本节开头。

## Round 30 模板空结果/渲染失败表单回归与搜索抽查

Round 30 执行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateConfigurationExperienceTest' --rerun-tasks`：8 tests、0 failure、0 error、0 skipped。新增的 `testEmptyAndFailedPreviewsShowFormStateAndClearPreviousMarkdown` 使用真实 `TemplateSettingForm`、固定示例渲染器和结果页 Editor：先确认 Spring Markdown 成功显示，再将模板置空并确认结果页显示空状态、旧 Markdown 已清除；随后使用未闭合 Velocity `#if` 指令触发渲染失败，确认结果页显示带原因的本地化错误状态且不保留旧 Markdown。日志与 XML：`/tmp/doc-view-reliability-20261018/round30-template-states.log`、`/tmp/doc-view-reliability-20261018/round30-template-states-TEST-com.liuzhihang.doc.view.contract.TemplateConfigurationExperienceTest.xml`。

该轮只新增测试，没有运行时变化；不需要重打 Round 29 已做原生验收的插件包。Round 29 原生记录仍仅证明其列出的正常模板编辑/预览操作，不代表在 IDEA 中实际触发过空结果或错误状态。

设置搜索另做了关键词抽查：`nested prefix` 与模板变量关键词均定位到 `Template & Output`；模板变量结果的截图目视确认显示正确 breadcrumb 且变量按钮高亮。该抽查不覆盖完整中英文关键词矩阵，任务 3.1b 和隔离项目、主题、重启验收仍未完成。

## Round 33 浅色主题变量详情复核

主代理在 IU 262 浅色主题下报告：变量列表的属性名/中文标签分行已清楚，但详情区固定 64 logical 高度只显示约三行；插入表达式折成两行后，用途说明被截断，仅能小幅滚动，列表区域则留有较多空白。该观察促使 2.3c contract/design 增加约六行常规可见详情空间、短窗口纵向滚动和精简提示文案的要求。Round 33 当时完成了 120 logical、六行详情区、纵向按需滚动和简短提示改动；该轮未验证改动。后续 Round 34 的自动门禁与原生复核见下文。

## Round 34 整批门禁与模板变量详情复核

`./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 完成：38 个测试套件、187 tests、0 failure、0 error、0 skipped，build 成功；Plugin Verifier 对 IU 261.22158.277 的结果为 Compatible。Round 34 ZIP SHA-256：`8c85ee9f2b7f990e24d67dca013c8dac2981c1745bafc2f8ef4d8c79e67f2ec6`；隔离 profile 中安装的插件 JAR SHA-256：`b98e320e72308e73b8b0f3f7b04f7ce5ccb0b7c1390ba859d2a364a177ce0b49`。记录引用的门禁日志与包审计：`/tmp/doc-view-reliability-20261018/round34-fullgate.log`、`round34-package-audit.json`。

IU 262 隔离 profile 的 2268×1694 浅色主题原生复核中，搜索 `responseParamDataList` 后列表以属性名和中文标签分行显示；详情区的完整插入表达式换为两行，随后显示中文标签和两行用途说明，均能在无需滚动时读完，底部精简提示完整可见。截图与辅助功能树：`/tmp/doc-view-reliability-20261018/native-round34-template-variable-light.png`、`native-round34-template-variable-light-ax.txt`。随后切回 Islands Dark，Cancel 后重开设置；截图确认默认变量目录已收起且 Apply 禁用：`/tmp/doc-view-reliability-20261018/native-round34-template-editor-dark.png`。

Round 33 的辅助功能记录还确认将完整 `${DocView.responseParamDataList}` 表达式插入模板后，点击 Revert 恢复默认模板且 Apply 禁用：`/tmp/doc-view-reliability-20261018/native-round33-variable-insert-ax.txt`、`native-round33-variable-revert-ax.txt`。这条插入/撤销路径本轮未改变。上述原生证据只覆盖列出的窗口尺寸、主题和操作，不替代窄窗口、项目隔离或 IDE 重启验收。

## Round 34 项目 B 保存读回与较小窗口观察

IU 262 原生检查在 `DocViewNativeProjectB` 项目中 Apply 了包含 `QA_PROJECT_B` 标记的 Spring 模板；重新打开设置页后，编辑器仍显示 `QA_PROJECT_B` 和 `${DocView.name}`，Apply 禁用。证据为 `/tmp/doc-view-reliability-20261018/native-project-b-applied-ax.txt` 与 `native-project-b-reopen-ax.txt`。这只证明项目 B 的已应用模板在该项目设置页重开后保留；A/B 跨项目隔离比较因原生会话被锁屏中断而未完成，仍不能标记为通过。

在 1800×1400 的 IU 262 设置窗口截图中，模板页头部说明被单行 `JBLabel` 省略显示；预览按钮旁“使用虚构示例数据；不会读取项目源码或访问网络。”说明仍可见，变量列表及所选变量详情也在该截图中可读。截图：`/tmp/doc-view-reliability-20261018/native-round34-template-smaller-window.png`。因此目前确认的是页头说明在该尺寸被截断，不是预览说明消失；其他小窗口尺寸和缩放比例仍未验。随后将页头说明压短为“编辑 Velocity 模板并预览示例。”，帮助入口及预览样例行为不变。新文案尚未安装到原生实例复验，不能将旧截图作为修复后的小窗口证据。

## Round42 整批复验（2026-10-18）

主代理完整执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：40 个 suite、196 项测试，0 失败、0 错误、0 跳过；构建成功，IU261.22158.277 Plugin Verifier verdict 为 `Compatible`。包括本 change 的最新修改及 Round41 的取消/释放守卫和真实 Scratch 文件失败用例。日志与精确 XML：`/tmp/doc-view-reliability-20261018/round42-fullgate.log`、`round42-fullgate-TEST-*.xml`；包审计为 `round42-package-audit.json`。

Round42 ZIP SHA-256：`e214cd4525968356c936f798191006391415019afe48ee4e5235c02f342372d7`。7 个离线章节与仓库正文逐字节一致，生产包未包含测试框架、夹具或 MCP PoC。测试运行使用 JBR25，生产目标仍为 Java21；构建配置中跳过的 searchable-options 任务不计为测试跳过。完整包与原生已安装版本的区别见 [路线图](../../../docs/roadmap.md#本轮验证与续作入口)。

macOS 锁屏阻挡后续原生操作。IU262 测试实例仍安装 Round34，尚未安装或操作 Round42；本节自动验证不能替代新包原生双击/Enter、项目关闭、多项目隔离、主题及窗口矩阵验收，相关任务保持未完成。

## Round 46 IU 262 原生局部复核

Round 46 先退出旧包，再在隔离 profile 安装 Round 42 插件 JAR（SHA-256：`993fdc650c5b2f0a4a4aa0c45d292c8130051c1938df4df36f63b1263019a2fc`）。以下只记录本轮实际交互；此前 Round 42 的锁屏状态是历史记录，不代表本轮仍在运行 Round 34。

在 `doc-demo`（项目 A）查看 Spring 模板，编辑器仍显示默认 `${DocView.name}` 内容，没有项目 B 的 `QA_PROJECT_B` 标记。结合此前项目 B 保存并重开仍有该标记的记录，这次只核对了 A 的读回状态，不等同于完整的项目隔离、重启和多项目矩阵。低密度模板设置页截图为 `/tmp/doc-view-reliability-20261018/native-round46-template-editor.png`。

在模板编辑器留下 `QA_HELP_DRAFT` 后点击“打开模板使用指南”，Doc View 帮助编辑器显示模板教程；返回 Settings 后草稿仍可见且 Apply 可用。点击 Revert 后草稿被丢弃，Apply 与 Revert changes 均禁用。证据：`native-round46-help-from-draft-ax.txt`、`native-round46-help-return-draft-ax.txt`、`native-round46-help-revert-ax.txt`。这确认了 Template & Output 的帮助往返与当前模板草稿状态，不证明其他设置子页、所有帮助入口或平台上传行为。

将普通接口识别关闭后刷新 `doc-demo` 的 Doc View 树，可见 `User2Controller`、`UserController`、`XmlDubboService` 三项；恢复开启并刷新后，可见五项，并增加 `AnnotationDubboService`、`UserFacade`。证据：`native-round46-normal-interface-off-tree-ax.txt`、`native-round46-normal-interface-restored-ax.txt`。这只证明该示例项目在两种设置状态下的树结果；不外推到所有项目或框架。另一次 Refresh、Hide、Reopen 操作只读取到最终树状态，不能据此声称 Hide 命中了仍在运行的刷新任务。

设置搜索的逐帧快照保留为 `native-round46-search-0.txt` 至 `native-round46-search-8.txt`。其中“模板”“恢复默认模板”“生成规则”及 `nested prefix` 的快照分别显示到对应设置路径；`ordinary interface` 搜索中虽然出现 Doc View 结果，当前选中路径仍是 Inspections，不能记为成功定位到 Doc View。平台搜索随后超时在 Platform Integrations；`native-round46-search-showdoc.txt`、`native-round46-search-yapi.txt`、`native-round46-search-yapi-final.txt`、`native-round46-search-yuque-zh.txt` 是当时的后续局部快照，不构成 YApi/YuQue 搜索矩阵验收。完整中英文查询、焦点及返回行为仍未覆盖。

此前旧包中 Markdown source viewer 可打开；点击 Pin 或 Preview 时界面被工具交互关闭，无法据此确认按钮后续行为。JCEF 原生渲染仍未验；这是当前工具交互的证据边界，不据此判定插件存在缺陷，也不声称 JCEF 通过。

## 2026-10-21 生成规则与指南续验

生成规则来源已按编号逐行、回退另段，必填规则按字段/直接参数分组；HTML片段和相同纯文本可访问描述由双语回归保护。用户随后要求指南只用原生问号，已统一模板/常规/规则/集成与三平台指南入口，模板恢复默认操作移入编辑区；原有草稿/保存行为不变。

最新e0b6候选包全量48 suites、226 tests，0 failures/errors/skipped，18 tasks实际执行，IU261 Verifier Compatible。doc-demo中英浅色逐页核对设置问号，模板/规则另完成中英深浅四组900×700截图；中文规则滚动到底、模板临时草稿恢复/预览/Cancel后重开和项目XML字节保持均实际核对。真实悬停和气泡内链接点击仍待人工，不能用AX描述或旧长句截图代替。完整各页生命周期/多项目隔离仍按3.4b保留。

环境和普通IDEA已恢复，包身份、原始证据和范围见[本地化联合验证](../localize-plugin-ui/verification.md)。未提交、发布或归档。

## 维护者人工气泡验收（2026-10-21）

用户在本轮对话明确反馈“人工验收气泡完毕”，据此将气泡阅读与分行复验任务4.2c、4.3b登记完成。这是维护者人工验收结果，不是工具新取得的截图；此前系统鼠标移动未触发气泡的记录仍是当时的真实结果。

本次反馈未逐项列明语言、主题、窗口尺寸或包哈希，因此不新增这些矩阵通过声明；也没有确认指南气泡内链接点击、GitHub正文可读或草稿往返。剩余指南/双语覆盖记录见[本地化任务](../localize-plugin-ui/tasks.md)4.2d，完整项目隔离和各页生命周期仍见本change的3.4b。

仅更新任务和验证记录，没有操作IDEA、修改运行时代码或执行提交/发布/归档。

## af61 普通接口扫描原生对照（2026-10-22）

使用IU262、英语、Islands Light、Compact开、隔离doc-demo及af61候选包；安装身份及本轮备份见 `/tmp/doc-view-native-20261022-menus/`。没有修改运行时代码。

1. 初始 `includeNormalInterface=true`，目录扫描完成后为User2Controller、UserController、AnnotationDubboService、UserFacade、XmlDubboService五类。
2. 在常规页只取消Recognize and scan ordinary interfaces并Apply，Apply恢复禁用。回目录手动Refresh后，终态仅有两个Controller和XmlDubboService；另两接口消失。没有用Searching/Indexing/失败状态当作空结果。
3. 重开常规设置，重新勾选并Apply，再Refresh；AnnotationDubboService与UserFacade返回，恢复五类。没有修改行标记、参数分表、模板或凭据，没有执行Clear Cache。
4. 正常退出后，两份既有DocView XML与开始备份逐字一致，includeNormalInterface和separateParam恢复true；其间只做可逆设置对照，没有写入测试源码。隔离profile恢复，普通doc-view窗口实际确认恢复。

原始AX/截图：`en-directory-ready.txt`、`ordinary-scan-off-draft.txt`、`ordinary-scan-off-ready.txt/.jpg`、`ordinary-scan-on-restored.txt/.jpg`；最终磁盘比对 `settings-before-restoration.json`，恢复结果 `restoration-results.json`。

该结果只补齐3.4b中的实际扫描效果；项目隔离、所有设置页生命周期、主题变化和重启状态仍未完整补齐，因此3.4b继续不勾选。本轮问号新包master链接及中英菜单八组人工结果见[本地化续验](../localize-plugin-ui/verification.md)。不提交、发布或归档。

## 2026-10-22 项目隔离、设置生命周期及窄窗口修复

### 环境与包边界

使用安装版 IU 262.10968.63、隔离 profile、900×700 logical Settings、Compact 开。项目 A 为专用验证工程 `/Users/liuzhihang/workspace/doc-demo`，项目 B 为本轮创建的 `/tmp/doc-view-native-20261022-settings/doc-demo-settings-b`，只含 README，不依赖账号或服务。普通 doc-view IDEA 先正常退出。原始 AX、截图、配置备份和启动记录位于 `/tmp/doc-view-native-20261022-settings/`。

首轮各页生命周期使用 af61 包（ZIP `af61112b30c0b15cae24fdfdb7bdc5c933d1112a2991ad197c2bcfd3248f796b`）。发现窄窗口布局问题后，后续重启、A/B 保存值读回及中英布局使用修复后的 df21 包：ZIP SHA-256 `df21ba775bd79032a3667e7ddef3984efee271a0511140977ceb33415cf50adb`，唯一插件 JAR SHA-256 `6eec14f6752b1a31aa9f16e1bffde276bacf7d92b6f18b2a24fd85534b0e4c2e`；安装时逐 JAR 比对，见 `a-dark-fixed-package.json`。三份 `.form` 布局是本轮唯一生产变更，不改 Configurable、凭据或生成逻辑。

### 各页面生命周期

本轮使用代表字段验证每个有持久化数据的 Configurable，不表示逐个点击所有控件或遍历全部非法输入组合。通用步骤为：编辑草稿 → Revert changes 恢复最近保存值且 Apply 禁用 → 编辑并 Apply → 新草稿后 Cancel → 重开仍为最近 Apply 值。Revert 是设置页的 reset 入口，不把“恢复默认模板”当作 reset。平台页只填写虚构普通字段；密码始终留空、清除复选框未选，不触发上传，不验证 OS PasswordSafe 的新增/替换/清除。

| 页面 | 实际验证字段 | Apply 后及 Cancel 重开结果 |
| --- | --- | --- |
| 常规 | 行标记、普通接口识别 | A 保存 false/false；取消后仍 false/false |
| 生成规则 | `@DocView.Title` 来源开关 | A 保存 false；取消后仍 false |
| 模板与输出 | Spring 模板、第一层前缀 | 保存 `QA_A_TEMPLATE` 注释及 `QA-A>`；Revert/Cancel 标记均未保留 |
| ShowDoc | 服务地址 | 保存 `https://showdoc-a.example.test`；末尾斜杠规范化，取消草稿未保存 |
| YApi | 服务地址、项目 ID | 保存 `https://yapi-a.example.test`、7319；取消地址草稿未保存 |
| 语雀 | 站点、API 地址、用户/团队、知识库路径 | 保存 A 专属虚构值；取消路径草稿未保存，URL 规范化 |
| 平台集成总览 | 无独立字段 | 只导航时 Apply 禁用，不产生单独脏状态 |

证据按 `a-general-*`、`a-rules-*`、`a-template-*`、`a-output-*`、`a-showdoc-*`、`a-yapi-*`、`a-yuque-*` 保存。ShowDoc 初次编辑时 AX 曾短暂报告 Apply/Revert 禁用，而新截图显示二者已启用，实际 Revert 成功；此处作为工具快照不同步记录，不判定为保存功能缺陷。

### A/B 隔离及重启读回

在同一隔离 IDEA 进程中通过 File → Open 打开 B 的独立窗口，确认其初始平台字段未带入 A 值、模板没有 A 标记。B 保存 `QA_B_TEMPLATE`、`QA-B>`、三个 B 专属域名、YApi ID 7320、语雀 B 路径；B 行标记与普通接口为 true/true，规则 Title 保持默认 true。切回 A 后逐页确认 A 值未改变，A 模板有 A 标记而无 B/Cancel 标记。规则开关用可逆切换读回 Value 后立即切回，未 Apply 探针草稿。

正常退出后，五份 `.idea/DocView*.xml` 各自保留 A/B 值；A 的 false 开关与 B 的 true 开关按实际 Java 默认值解读，字段缺省不一律视为 false。磁盘快照 `a-b-persisted-before-restart.json` 与原始 XML/AX共同取证。

随后安装 df21 并以英语 Islands Dark 重启，重新打开 B；逐页读回 A/B 的模板标记、前缀、规则、常规开关、三平台普通字段，Apply 均禁用。证据为 `a-dark-*-persisted*`、`b-dark-*-persisted*`。再以简中 Islands Light 启动 A，平台值仍为 A。这里证明的是深浅主题启动/重启后的保存状态；外观页即时切换主题的工具操作未生效，最终读回仍是原主题，任务3.4c继续未完成。也不将此轮固定 Compact 开外推为开/关全矩阵。

### 发现与修复：清除凭据标签裁切

`a-showdoc-current.jpg` 显示英语窄窗口中清除标签右侧被裁切。根因是清除复选框与密码框占同一行；新测试先得到 RED（1 test / 1 failure），失败为 `ShowDoc API key clear action must be on a separate row below its password field`。随后只改三平台 `.form`，将每个清除项放到对应密码框下一行并使用剩余列宽，状态/指南顺延。

专项布局测试 GREEN 1/1，检查650/1000 logical内容宽度、完整本地化文本、密码框可用宽度和清除项/状态/指南顺序。完整执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：49 suites、229 tests，0 failures/errors/skipped，18 tasks实际执行，IU261.22158.277 Plugin Verifier Compatible。原始日志为 `layout-red.log`、`layout-green.log`、`full-validation.log`，精确XML副本在 `full-test-results/`。该门禁包含凭据生命周期自动回归，但不等同于 PasswordSafe 原生验收。

df21 包的 `en-dark-{showdoc,yapi,yuque}-fixed.jpg` 与 `zh-light-{showdoc,yapi,yuque}-fixed.jpg` 六张原生截图均确认清除标签完整可读、紧随对应密码框、状态与问号在后；独立只读视觉复核一致。本轮仅覆盖这两组语言/主题与900×700窗口。

### 恢复和剩余范围

三次隔离实例均正常退出。A 的两份既有 DocView XML按本轮备份恢复字节一致；本轮创建的模板/ShowDoc/YApi三份XML先保留在 `created-settings-after/` 再从A移除。隔离语言、主题、Compact、编辑器颜色与原有设置窗口几何恢复，详见 `restoration-results.json`。B留在临时证据目录，不进入仓库。

即时主题切换、OS凭据存储/拒绝/替换/删除、真实上传、完整键盘与其它change原生缺口未由本轮代替。项目隔离和各页代表字段生命周期完成，3.4c保留；本轮未提交、推送、sync/archive或发布。

普通 `doc-view – ai-context-assessment.md [doc-view]` 窗口已通过Computer Use实际读回确认，证据 `ordinary-restored.txt/.jpg`。doc-demo tracked/non-ignored文件无新增/修改/删除；两仓库HEAD和index保持本轮基线。OpenSpec strict校验23 passed/0 failed；未使用Credits或额度重置。

## 2026-10-22 即时主题切换人工验收完成

维护者在上述工具验收后明确反馈“切主题验收完毕”，据此将任务3.4c登记为人工验收通过。此前工具无法完成主题选择的记录仍为历史事实；本次通过证据来自维护者确认，不是新增工具截图或自动测试。

反馈未逐项列出语言、Compact、窗口尺寸或插件包身份，因此不新增这些组合的通过声明。`doc-view-configuration` 当前29/29任务完成；OS凭据原生场景、真实上传、完整键盘和其它change的未验项不由此确认覆盖，不能据单个change完成宣称整批可发布。

本轮仅更新验收任务与文档，不操作IDEA、不修改运行时代码、不重跑Gradle，不提交、推送、sync/archive或发布。
