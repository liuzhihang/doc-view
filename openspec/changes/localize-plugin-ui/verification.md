# 中英界面验证记录

本轮从 2026-10-20 延续到 10-21，按用户明确授权实现插件主要界面中英化和生成规则气泡结构修正；不提交、不推送、不发布、不 sync/archive。

## 自动验证

- 旧资源专项先取得3项预期 RED：英文 base 仍含中文 UI、简中资源缺失、双 locale 默认模板无法核对。日志为 `/tmp/doc-view-validation-20261020-rules-locale/localization-red.log`。
- 集成中发现并修复设置搜索常量漏引用；分段测试发现字段名 fallback 缺少独立段落，英文摘要超过原短文案限制，均修复后复验。
- 依据本地 IU261 SDK `ActionManagerImpl` / `ActionManagerImplKt` 与 `ConfigurableEP` 的实际行为，修复错误的 `%key` 通用 XML 插值：action/group 采用 convention keys 和可读英文 fallback，设置 EP 使用 bundle/key。通过真实 ActionManager presentation 回归验证欢迎、Upload group 和三个平台子项。
- 专项命令执行12 suites、46 tests，0 failures/errors/skips，14 tasks 全部实际执行；覆盖资源 key/占位符、默认模板 SHA、五组帮助纯文本/HTML fragment、布局/状态、欢迎 locale/链接/CSP、上传枚举路由及相关取消场景。日志为 `localization-green3.log`。
- 英语与简中各396个 key；默认 Spring/Dubbo 模板语义值逐字保持且不随 locale 改变。来源按编号逐行，fallback 独立段，固定必填规则按字段/直接参数分段；可访问描述保留同等纯文本换行。

- 首次全量实际执行219项，发现平台表单沿用 JVM 默认 locale 的真实缺陷，以及目录测试2处旧语言硬编码。保留布局，五个 `.form` 的文案改为构造器经 DynamicBundle 赋值；根分类保留 Doc View，行为分区标题单独翻译。新增回归故意令 JVM default 与 IDEA UI locale 相反，并恢复 default / DISPLAY / FORMAT；补齐语雀 token label 绑定后，扩大专项15 suites、58 tests，0 failures/errors/skips，14 tasks实际执行（`localization-green5.log`）。

全量门禁、候选包与原生语言矩阵在实际执行后补记。自动渲染/fixture 不替代真实 IDEA 气泡、主题或菜单验收。

## 原生边界

使用专用 `/Users/liuzhihang/workspace/doc-demo` 和隔离 profile；旧长句气泡的用户截图只作为缺陷复现证据。新包五组问号、英语/简中主要页面与深浅主题需要独立核对。完整纯键盘专项沿用用户延期；不选择真实上传平台、不录入凭据。

## 首轮双语包的原生发现

`85ed7b18871524c65a1ac1e0631a5c9945c3dc7516a80258566fbe699b24ba4a` 包执行完整222项（48 suites）测试、18项任务、IU261 Verifier Compatible 后安装到隔离 IU262。

- 英语 UI + 系统简中实际核对了设置根页、生成规则五组、ShowDoc/YApi/YuQue 标签/状态、目录扫描提示/动作、工具栏 Upload 三平台列表与 Esc、参数编辑表头/动作，以及欢迎 HTML 正文；业务源码、方法描述和模板中文按设计保留。
- Settings 预设780×650，被 IDEA 最小尺寸约束为900×700 logical（1800×1400截图）。规则页窄窗口可读，滚动条从19变到103后，底部字段名摘要和指南按钮完整可达。
- 问号的原生无障碍描述为编号换行/独立段落；工具点击未取得实际悬浮气泡，不把该状态当成气泡视觉通过。
- 目录右键和主题下拉仍在工具调用后收起，键盘尝试未改变状态；工具栏 Upload 可读取三选项。未选择平台、未写凭据、未确认参数写回。
- 英文模板页出现摘要省略及固定示例提示被 FlowLayout 换行裁切，原生截图 `en-template.png` 是缺陷证据。已改成可换行独立摘要/动作行、预览按钮与示例说明分行，布局专项通过。分类名收敛为 Template and Output，不增加助记符转换逻辑；后续新包复验单列。

原始截图及 AX 位于 `/tmp/doc-view-validation-20261020-rules-locale/`；本节证明的是上述包和实际场景，不外推未运行的语言/主题/气泡矩阵。

## 编号气泡与初轮本地化候选包（2026-10-21）

- 英文模板页原生裁切修复后执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：48 suites、223 tests，0 failures/errors/skipped；18 tasks 实际执行，IU-261.22158.277 Plugin Verifier Compatible。日志 `layout-gates.log`；JUnit 与汇总在 `candidate-xml/`、`candidate-results.json`。
- ZIP `Doc View-1.3.14.zip` SHA-256：`66a6c82ea3b64d31e0b5f290e4f89b3ec332e2b28775160a71fb9e34546d5657`；包内唯一生产 JAR SHA-256：`7366dd5c80ac07b9092a036a2b13cead656a79ff83c25fd1baf01a04e337ea85`。安装到隔离 profile 后逐字核对哈希；包含中英资源与四份欢迎/更新 HTML，不含 Test.class/testsupport。
- 新增布局回归在900×700和640×700根容器内检查摘要、操作按钮、预览按钮与固定示例提示的实际位置/边界。原生900×700英语浅色模板页已无此前两处裁切；Preview complete 状态及源码结果可达，Output options 独立页面可达。
- 最终同一包的生成规则页完成英语/简中 × Islands Light/Dark 四组900×700原生截图：浅分隔线、五组帮助图标和短灰字可见，12个规则控件保留；Compact Mode 本轮保持开启，不能外推为开/关矩阵通过。
- `openspec validate --all --strict`：23 passed、0 failed。原生真实悬停、目录右键和更多语言页面以实际后续记录为准，不能将无障碍文本、测试HTML片段或先前包截图当作最终气泡视觉通过。

## 设置指南问号与模板操作行（最新包）

用户进一步指出页头指南/恢复两枚大按钮不合理，并明确指南只需IDEA原生问号。已先更新本change及configuration的design/spec/tasks，再移除模板和六处其它设置的指南按钮，使用平台公开 `ContextHelpLabel.createWithLink`，保留本地化可访问名称/说明，气泡内链接仍打开相同GitHub文章。模板恢复默认按钮移至模板编辑操作行，与变量入口同组；预览/输出页不含恢复按钮，草稿/保存语义不变。平台总览摘要同时缩短，减少英语窄窗长句。

- 模板组件/层级新断言先取得旧页缺少原生问号的RED（`auxiliary-red.log`）。其它设置取有效UI断言RED（`guides-red-confirm.log`）；先前一次因共享工厂尚未创建导致编译失败，不把它作为UI复现证据。临时旧态已恢复。
- 合并专项首轮38项有1项失败：旧“所有HelpIcon共5个”过滤误包含新增页面指南；改为只计generation命名的五组细节帮助，另外独立核对页面指南。复验6 suites、38 tests，0 failures/errors/skipped，14 tasks实际执行（`guides-green2.log`）。保留模板/凭据状态、动态UI locale和相反JVM locale断言。
- `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`：48 suites、226 tests，0 failures/errors/skipped，18 tasks实际执行，IU261.22158.277 Verifier Compatible（`guide-full-gates.log`）。中英各399个消息键对应。
- 最新ZIP SHA-256 `e0b695126fc6cc089b9db4bfcc076aa76cead5080802a105c03080309ac5edca`；JAR `d86007e940efc7753aad6435c9532e73fc2dad9745d6be6085ca87a2b17d15fe`。汇总 `guide-candidate-results.json`，JUnit `guide-candidate-xml/`；隔离安装哈希另存。此前66a6包仅保留其历史范围。
- 最新包中文浅色900×700：模板页无指南大按钮，原生问号可见、恢复/变量同属编辑操作行；写入临时Spring草稿再点恢复，回到默认文本；预览完成可达且恢复按钮不在预览页；Cancel后重开Apply禁用，所有DocView项目XML字节未变、未新增模板XML。一般设置、规则页、集成总览及三平台页面的指南问号均已原生核对；规则页滚动到底可读字段名摘要。

其余最终语言/主题复验和环境恢复在完成后补记；真实悬停/气泡链接操作仍不能由AX文本证明。

### 原生收尾和恢复

最新e0b6包的英语/简中 × Islands Light/Dark 均完成900×700模板页与规则页截图；Compact保持开启。其它常规/集成总览/三平台页面本轮为中英浅色核对，不外推全部页面的四组矩阵。模板及规则的问号、细线、灰字在两种主题可见；真实气泡和气泡内链接点击仍未由工具触发，保留未验项。

全部测试实例均正常退出。隔离profile的语言registry、LocalizationStateService、LAF、Compact/编辑器颜色文件与备份逐字一致，SettingsEditor几何属性恢复；doc-demo两份既有DocView XML全程字节未变、无新增模板XML，35个tracked/non-ignored文件无新增/修改/删除。普通IDEA已恢复并实际读取到 `doc-view – ai-context-assessment.md [doc-view]` 窗口。

以本轮baseline逐文件核对，doc-view本轮91条路径（75修改、16新增、0删除）均属于本地化、问号/模板布局、相应测试/规格/指南及验收记录；没有改动AGENTS、构建依赖、平台基线、默认模板语义或图标资源等本轮外区域。doc-view和doc-demo HEAD/index均未变化。生产源码与最终打包后的manifest一致，后续仅整理文档/任务。范围快照 `guide-final-scope.json`、恢复结果 `restoration-results.json`，原始资料仍在本轮/tmp目录。

开发实现可以按独立scope审阅后另行提交；localize/configuration仍有真实气泡、全部原生场景或完整项目隔离矩阵待验，不能据此整批归档或正式发布。延续用户边界：本轮未stage/commit/push/sync/archive/publish，未使用Credits或额度重置。

最终文档检查：OpenSpec strict全量23 passed/0 failed，git diff --check通过；本轮Markdown的84条本地文件链接均存在。普通额度仍允许使用（本轮末8%已用），Credits余额与开始检查相同，没有调用重置额度或付费入口。

## 问号和指南点击续验：等待悬停操作方式（2026-10-21）

本次按用户指示仅继续气泡/指南点击。安装包仍为上方e0b6，安装JAR逐字核对；生产源码与上轮打包manifest一致，未修改运行时代码，也未重复把226项旧门禁当作本次新执行。

- 正常退出普通IDEA后，使用专用doc-demo和隔离profile打开中文浅色900×700生成规则。Computer Use没有悬停API；拖拽及最小滚动等现有指针动作未触发真实气泡，截图只看到问号/指针，不计气泡排版通过。
- 已询问是否明确指定使用macOS系统鼠标移动补足该工具能力，或由用户手动悬停；尚未收到选择，因此没有执行准备好的系统鼠标程序，也没有更改系统权限。指南气泡尚未显示，未完成气泡内链接点击，不能推定导航或GitHub正文成功。
- 暂停前正常退出测试实例，按本轮新备份恢复隔离语言、主题、Compact/编辑器颜色及窗口几何；doc-demo的DocView XML字节未变。已实际读到恢复后的普通 `doc-view – ai-context-assessment.md [doc-view]` 窗口。

证据目录：`/tmp/doc-view-tooltip-20261021/`；相关4.2c及configuration真实气泡任务继续保持未完成。不提交、不发布、不归档。

## 系统鼠标移动授权后的续验（2026-10-21）

用户明确允许使用 macOS 系统鼠标移动。本轮仅将该例外用于鼠标移动事件，点击、窗口操作和截图仍经 Computer Use；没有调整系统权限。

- 使用新的证据/备份目录 `/tmp/doc-view-tooltip-20261021-approved/`，正常退出普通 IDEA 后启动隔离 doc-demo，中文、Islands Light、900×700 logical。安装 ZIP/JAR 哈希与上方 e0b6/d860 候选逐字一致。
- 已实际执行 CoreGraphics 鼠标移动程序，并读回系统指针为 `(876, 101)`；另执行窗口 Raise 和定向到测试进程的 mouseMoved。Computer Use 的真实窗口截图始终没有出现气泡。系统指针位置变化不能证明 IDEA 已收到悬浮；当前证据不足以确定是前台/坐标/工具输入限制还是插件缺陷，因此没有据此修改运行时代码。
- 已请求用户将测试设置窗口保持在桌面前台，本轮结束前尚未收到确认。未完成五组气泡视觉检查、双语复验或气泡内指南链接点击；不将此前 AX Help、自动 HTML 结构断言或链接目标常量当作本轮原生通过。GitHub 正文可用性也未在本轮验证。
- 测试实例已正常退出；隔离 profile 的语言、主题、Compact/编辑器颜色和窗口几何已恢复。doc-demo 两份既有 DocView XML 在测试中均字节未变。普通 IDEA 已重新启动，实际窗口核对另见本轮总记录。

本轮仅补充验收记录，4.2c 与 configuration 4.2c/4.3b 继续未完成；没有重新执行或重复计入此前226项自动门禁，没有提交、推送、归档或发布。

收尾实际读取到普通 `doc-view – ai-context-assessment.md [doc-view]` 窗口。新 baseline 比对仅上述本文件和 `docs/validation-2026-10-20.md` 两份验收记录有变化；doc-demo tracked/non-ignored 文件没有变化，两仓库 HEAD/index 不变，未新增模板 XML。隔离语言/主题/Compact/颜色文件均与新备份逐字一致。OpenSpec strict 本次执行23 passed/0 failed，`git diff --check` 通过；普通额度仍允许使用，Credits 余额与开始一致，未调用重置入口。

## 维护者确认气泡人工验收完成（2026-10-21）

用户明确反馈“人工验收气泡完毕”。按此将实际气泡阅读/分行登记为维护者人工验收完成，并拆分原4.2c中的复合事项：气泡验收为已完成，指南气泡内点击及双语覆盖范围记录保留在4.2d。

这条反馈没有逐项列明语言、主题、窗口尺寸或包哈希，也没有确认指南链接点击与草稿保持，因此不据此宣称完整双语/主题矩阵、GitHub正文可用或所有链接均通过。此前工具未捕获气泡是历史过程记录，不能覆盖本次人工结论；本次也没有新的工具截图。

仅更新OpenSpec任务/验证记录和总验收记录，不操作IDEA，不修改运行时代码，不提交、发布或归档。

## 最终包主要界面双语续验

继续使用e0b6 ZIP、d860生产JAR，在IU262.10968.63的隔离doc-demo核对；安装哈希见 `/tmp/doc-view-native-20261021-followup/en-light-package.json`。本轮没有修改运行时代码，也没有把此前226项门禁计作本轮新执行。

| 场景 | 英语 | 简体中文 |
| --- | --- | --- |
| 目录扫描与就绪 | Searching for interfaces后显示5个类型，工具栏英文 | 正在查找接口后显示相同5个类型，工具栏简中 |
| 工具栏Upload列表 | Upload to YApi/ShowDoc/YuQue，Esc取消 | 上传到YApi/ShowDoc/语雀，Esc取消 |
| DTO参数编辑 | Parameter/Type/Required/Description，Copy as JSON/Confirm/Settings/Pin | 参数名/类型/必选/描述，复制为JSON/确认/设置/固定 |
| 方法预览 | Preview/Upload/Export/Copy/Settings/Pin | 预览/上传/导出/复制/设置/固定 |
| 欢迎/更新正文 | 英文HTML正常渲染，开发预览状态和当前版本可读 | 简中HTML正常渲染，开发预览状态和当前版本可读 |
| 欢迎页目录/模板入口 | 实际点击进入目录、Template and Output | 实际点击进入目录、模板与输出 |
| 查看更新及更新页模板/平台入口 | 实际打开更新正文及对应设置页 | 实际打开更新正文及对应设置页 |
| 更新页返回首页 | 实际点击并核对欢迎页截图 | 实际点击并核对欢迎页截图 |

两种语言均保留源码业务名称和原默认Markdown正文；DTO弹窗只阅读并Esc关闭，未确认写回；设置页未Apply，平台列表未选择服务。上述矩阵为Islands Light，Compact沿用开启，不扩展为本轮全部深浅/Compact矩阵。

目录右键的工具点击仍立即收起；英语方法预览内Upload点击也使预览返回源码，未取得列表。此前IDE自带右键也有同类工具限制，不能仅据此认定插件缺陷或声称菜单通过。设置指南经已授权系统鼠标移动仍未显示真实气泡，未将AX说明当作点击证据。

已独立使用公开网页读取核对settings.md、templates.md、integrations.md三个GitHub目标，均返回404；该读取不是插件点击。指南正文尚未在公开分支可用，本轮不推送，不能通过更改目标到旧文档掩盖该发布前缺口。

维护者随后确认三类指南均已打开并返回设置，且气泡已覆盖简中和英语。工具读到 settings.md 与指南目录；维护者明确补充模板、集成两个实际地址分别以 templates.md / integrations.md 结尾，均仍为 develop 并显示404。模板 `QA_GUIDE_CLICK_DRAFT` 在指南往返后由AX核对保留；Cancel后重开标记消失，Apply禁用。中文返回首页已补齐实际点击与截图。旧e0b6实例正常退出；上述结果只覆盖旧包，分支缺陷修复及恢复结果在下节续记。证据目录 `/tmp/doc-view-native-20261021-followup/`。

## 指南分支修正（2026-10-22）

原生点击暴露设置指南误用 develop；维护者明确指定 master。按该反馈更新当前change的design/spec/tasks，再修改共用设置指南基址、WelcomeDocuments精确外链白名单，以及中英欢迎/更新HTML的指南目录和CHANGELOG链接。仓库主页/issues、开发预览文案、内部action、安全加载、文章相对路径和草稿语义保持。没有新增备用分支或旧版本兼容逻辑。

- RED：新增设置指南基址与双语HTML/白名单分支断言，旧包2项预期失败；14 tasks实际执行，日志 `master-links-red.log`，JUnit留存于 `master-links-red-xml/`。
- GREEN：同一专项3 suites、20 tests，0 failures/errors/skipped，14 tasks实际执行（`master-links-green.log`）。该结果不替代气泡原生点击。
- 独立公开网页读取 master 的模板和集成指南仍返回404，不能仅靠改地址解决尚未发布正文的问题；本轮不提交或推送。

- 完整命令 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 实际执行18 tasks，48 suites、228 tests，0 failures/errors/skipped，IU261.22158.277 Verifier Compatible。日志 `master-links-full-gates.log`、JUnit `master-full-xml/`；原有Gradle弃用/测试API编译警告保留，不属于本次新增链接缺陷。
- ZIP SHA-256 `af61112b30c0b15cae24fdfdb7bdc5c933d1112a2991ad197c2bcfd3248f796b`；唯一生产JAR `a637d0633f8382d37b0e8e0bcddf467f83cff045393ab7bdf6b4d4e1a1f14978`。隔离安装逐字核对（`master-zh-light-package.json`），本节新包取代此前e0b6作为当前构建，不把旧包完整矩阵转移到新包。
- 新包原生IU262/doc-demo，中文、Islands Light、Compact开：从首页实际点击完整指南，新浏览器标签读到 `tree/master/docs/user-guide`；首页完整历史实际打开 `blob/master/CHANGELOG.md`；查看更新后从更新页指南再次打开master目录。三个实际新标签及时间保存于 `master-welcome-guide-confirmed.json`、`master-changelog-confirmed.json`、`master-updates-guide-confirmed.json`。更新页模板入口打开“模板与输出”，Apply禁用且没有临时标记，随后Cancel。
- 三处设置指南气泡本轮人工点击证据属于旧e0b6包；修复包未再请求人工点同三处，master基址由RED/GREEN覆盖，其原生点击不宣称已重验。目录右键/预览Upload的最终双语完整矩阵仍待补，4.2b不勾选；全键盘验收仍按维护者之前要求跳过，未做真实上传。
- 两个测试实例均正常退出。隔离registry、locale、LAF、Compact/颜色按本轮备份恢复，SettingsEditor几何恢复；doc-demo两份既有DocView XML测试中均未变化，无新增模板XML。普通IDEA已实际读到 `doc-view – ai-context-assessment.md [doc-view]`。本轮14条修改路径均属于链接/相关回归、OpenSpec/CHANGELOG与验收记录，doc-demo tracked/non-ignored文件无新增/修改/删除，两仓库HEAD/index不变；范围与恢复证据为 `final-scope-results.json`、`restoration-results.json`。

截至该次收尾为13/14项，4.2b未完成，不能以旧包或部分菜单证据替代。master指南正文404与其它活动change原生缺口仍是发布待办；本轮不stage/commit/push/sync/archive/publish，未使用Credits或额度重置。

## af61 设置指南与菜单续验（2026-10-22）

本轮使用同一af61候选包和独立备份 `/tmp/doc-view-native-20261022-menus/`，没有修改运行时代码或重复计算旧自动门禁。普通IDEA正常退出后，安装哈希逐字核对，在专用doc-demo的IU262开启简中、Islands Light、Compact开。

- 键盘Shift+F10与工具右键仍未留下菜单。维护者手动核对类/方法右键上传三平台及预览上传/取消，确认“两项均正常”；登记本组人工通过，不把工具的无菜单截图当作通过。
- 维护者在新包手动打开模板、常规、集成总览三个问号内链接，确认“三处均为 master，已返回模板页”。本轮浏览器工具未返回相应标签；目标地址以该人工反馈为证据，不能声称工具已读回三个URL或正文可用。
- 工具读回 `QA_MASTER_GUIDE_DRAFT` 在往返后仍保留且Apply可用；Cancel后重开，标记消失且Apply禁用。未点击Apply或选择上传平台。证据为 `master-guide-draft-before.txt`、`master-guide-draft-after.txt`、`master-guide-draft-reopened.txt`。

维护者确认简中其余三组均正常；切英语重启后，工具确认目录五类型就绪，维护者随后确认英语四组均正常。两次人工验收结束均恢复Light＋Compact开。八组均覆盖类/方法右键Upload三平台、Preview Upload三平台及Esc返回保留预览，不执行真实上传。

| 主题/密度 | 简体中文 | 英语 |
| --- | --- | --- |
| Islands Light / Compact开 | 人工通过 | 人工通过 |
| Islands Light / Compact关 | 人工通过 | 人工通过 |
| Islands Dark / Compact开 | 人工通过 | 人工通过 |
| Islands Dark / Compact关 | 人工通过 | 人工通过 |

本轮14/14任务完成。本轮新增证据仅覆盖上述指南、菜单与草稿，不宣称把旧包全部页面/状态重新执行一遍；其余界面的既有证据仍按原包保留。完整键盘继续按维护者明确要求跳过，真实平台上传未验，公开正文可达性未重新联网核对。没有修改runtime、重打包或将上轮228项自动门禁重复计为本轮执行。

测试实例均正常退出。普通接口扫描的额外对照验证及恢复见configuration最新记录；退出后doc-demo两份既有DocView XML已与开始备份逐字一致，无新增模板XML。隔离语言、LAF、Compact/颜色与SettingsEditor几何已恢复，普通 `doc-view – ai-context-assessment.md [doc-view]` 窗口已实际读取。没有提交、推送、sync、archive或发布；本change的任务完成不代表其它活动change或整批发布就绪。
