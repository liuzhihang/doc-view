# Verification

验证日期：2026-10-18。测试由主线程串行运行；本 change 未自行启动 Gradle。

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 旧实现 RED | `OpenApi3SchemaRequiredModeTest` 8 项，7 失败、0 error、0 skipped；失败对应 requiredMode、写回和缺少成员等新增行为。 | `/tmp/doc-view-reliability-20261018/schema-required-mode-red.xml` |
| OpenAPI 3 字段契约 GREEN | 8/8 通过，覆盖字段模式、AUTO 来源、真实 Spring request/response DTO、扁平参数、完整 Markdown golden、Schema 写回与缺少成员提示。 | `/tmp/doc-view-reliability-20261018/round10-TEST-com.liuzhihang.doc.view.contract.OpenApi3SchemaRequiredModeTest.xml` |
| 既有 Spring Markdown contract | 1/1 通过。 | `/tmp/doc-view-reliability-20261018/round10-TEST-com.liuzhihang.doc.view.contract.SpringMarkdownContractTest.xml` |
| 串行联合 targeted 回归 | 总计 32/32 通过，包含 OpenAPI 3（8）、Spring Markdown（1）、Preview（12）、Configuration（5）和 Template Configuration（6）。 | `/tmp/doc-view-reliability-20261018/round10-TEST-*.xml`；主线程记录 session 33498 |
| OpenSpec strict validation | `openspec validate openapi3-schema-required-mode --strict` 通过。 | 命令输出：Change is valid |
| `git diff --check` | 通过，无输出。 | 本地检查 |

## 全量门禁

- 主线执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 成功：35 个测试套件、180 tests、0 failures、0 errors、0 skipped；Gradle 执行了 18/18 项任务，构建耗时约 1 分 3 秒。
- Plugin Verifier 对 IU 261.22158.277 的实际 verdict 为 `Compatible`。JUnit 报告备份：`/tmp/doc-view-reliability-20261018/round21-fullgate-TEST-*.xml`。
- `openspec validate openapi3-schema-required-mode --strict` 通过（`Change is valid`）；task 3.2 已完成。

未做原生 IDE 手工确认。自动化 PSI fixture、全量测试与 Plugin Verifier 不替代原生界面验收。

2026-10-20 发布就绪复核：为上述既有缺口补列 task 3.4。没有撤销已通过的自动验证，也没有新增运行行为；在原生写回验收完成前，不把本 change 作为稳妥归档候选。

## 2026-10-22 Schema description / 批次预检自动回归与原生只读查看

- 自动回归先以 RED 重现 Java 字符串 description 的读取缺陷：9 项中 1 项失败，反斜杠、引号和 LF 被当作源码转义文本；修复后该轮 9/9 通过。后续 RED 又覆盖显式空串/空白与 Javadoc 回退，以及 `LinkedHashMap` 中普通字段先于缺少 `requiredMode` 的 Schema 字段时的整批写入问题；11 项中对应 2 项失败。最终工作区测试 XML 列出 `OpenApi3SchemaRequiredModeTest` 12 项，0 failures、0 errors、0 skipped；其用例覆盖转义字符串和 PSI 可求值拼接、显式空/空白、清空后不回退、以及缺少 requiredMode 时预检在任何字段写入前拒绝整批。RED/GREEN 日志位于 `/tmp/doc-view-native-20261022-all/schema-plan/`，最终类级 XML 为 `build/test-results/test/TEST-com.liuzhihang.doc.view.contract.OpenApi3SchemaRequiredModeTest.xml`。
- candidate235 全量门禁摘要 `/tmp/doc-view-native-20261022-all/candidate235-gate-summary.json`：49 suites、235 tests，0 failures、0 errors、0 skipped；构建成功，IU 261.22158.277 Plugin Verifier Compatible。ZIP SHA-256：`2221a77261d1de30f09c202446cd4565080b1aeff733585a9c44587e496f448d`；插件 JAR SHA-256：`62b06ff49d5f18c89f104983bd7dce5e028282134a30fe23c6689bbd481a9f78`。
- 同一候选包在 IU `261.22158.277` 的 `doc-demo` 中打开真实 Doc Editor，但只读查看后按 Esc 退出。表格中的 `memo` description 显示为已解码的 Java 字符串，保留实际引号、反斜杠和换行；源上 `deprecated required=true` 搭配 `requiredMode=AUTO` 的字段显示 Required=false，符合当前 contract。证据：`/tmp/doc-view-native-20261022-all/iu261-2221-schema-read.txt` 与 `.jpg`。Esc 后源码保持原样；`checkpoint-restoration-0959.json` 也记录 `doc-demo` 的文件、index 与 HEAD 未变化。

本次原生操作没有切换 true/false、点击 Confirm、触发缺少成员通知或执行写回，故不能作为原生写回通过。任务 3.4 仍未勾选；该原生只读场景不替代其指定的写回、成员保留与错误通知验收。


## 2026-11-09：原生 LF 输入失败

ZIP `03f483361591f7bbd2a7f139ef457f27fdc76c0eb15419e1d2427dcbd504205b` / JAR `20fdd64ad3ac775655edb633f198c04d57ae001abf8d8112005b93e9ed62a74b` 已通过 414 全量测试和两个精确 build Verifier。PID21400 在保留用户激活信息的隔离 `profile` 中运行正式 IU263.7307.200；未读取或复制凭据，普通 IDEA 与 Junie 不动。新增纯虚构 Schema 项目使用现有 swagger-annotations 2.2.38 测试 JAR，两个字段含 name/title/example 哨兵。

真实描述输入包含 LF，Enter 提交后单元格中 LF 成为空格；Apply to Source + Save 后读取 Java description 字符串值再次确认该丢失。首轮 focused AX 值为空不可读，不能作为丢失证据；最终以已提交 cell 和实际源码值判定。原生 Undo/Save 后完整编辑器与磁盘恢复准备态 SHA，未修改现有 fixture-b。证据：`remaining-native/tree-ui-candidate/schema-multiline-input-result.json`、`schema-multiline-written.json`、`schema-multiline-undo.json`。3.4 保持未完成，先修复输入链路再复验，不将既有 PSI 转义用例当作原生输入通过。

## 2026-11-09：真实描述编辑器 RED/GREEN

旧 Description 列继承 JTable String 编辑器，其 JTextField 会把插入 LF 替换为空格。真实 `editCellAt` 路径的 RED 为 5 tests / 1 failure：`first line\n\n` 变成 `first line  `。修改仅在描述列使用私有 TableCellEditor、JBTextArea 和 JBScrollPane，不改变行高，不新增弹窗或依赖，沿用 setValueAt/modifiedMap/Apply；Required 编辑器不变。

首次修复运行 8 tests 全在构造期失败，根因为 JViewport 不支持 setBorder；移除该调用后定向 8 tests 全通过、0 skipped，14 tasks 实际执行。覆盖连续/尾部 LF、引号/反斜杠、Enter 换行、Ctrl/Meta+Enter 提交、Esc 不发布草稿、Tab/Shift+Tab 交给表格以及既有双击/Required/renderer 路径。失败与 GREEN 均保留在 `remaining-native/description-editor-{red,initial-failure,green}/`。源码 SHA `b82b1b6ff77bf8b2aeb9f28737149a3d6c0d0585c62d78760a65bb27e02578a9`；此处只关闭实现与定向测试 4.1，完整门禁及原生可输入性/Schema 矩阵 4.2、3.4 仍待验。

全量第一轮实际执行 79 suites / 418 tests，1 failure、0 errors/skips；唯一失败是旧 `ParamDocMarkdownCopyTest` 强制 JTextField 类型。证据 `remaining-native/multiline-initial-full-failure/`。更新到真实多行组件的复制检查又确认，既有 shared renderer 直接拼接 LF 会拆散 Markdown 表格行；此问题单独补充任务 4.3，按表格输出边界修复，不把原样跨行结果记为正确。后续全量须在输入与输出两处修复冻结后重跑。

Markdown 输出修复取得独立 RED/GREEN：RED 19 tests / 2 failures，分别是当前未提交单元格的复制路径和 DTO 描述换行矩阵；修复仅新增一个私有换行投影方法用于普通/分表描述列。最终定向 23 tests（编辑器 8、Markdown 复制 4、表格层级 7、展示 4），0 failures/errors/skips，14 tasks 实际执行，15s。CRLF 计一次换行，LF/独立 CR 各计一次，连续/尾部换行保留；单行输出、DTO 原始值、Required 和复制不写源码断言通过。证据 `multiline-markdown-red/` 与 `multiline-markdown-green/`。源码已冻结并重启完整门禁；4.3 的原生复制与 Markdown 显示尚未执行，仍不勾选。

## 多行输入和表格输出修复包的完整门禁

ZIP SHA `2f067769a174b8b2b6bda04f1b903bd04c206ded619c9abe133fe6382ff8b6cf`，JAR SHA `946feb0351a56f0f05ef61be4c29dfdb383f2f34c3be4fe850c11a36c6ca69a3`。实际执行 79 suites / 419 tests，0 failures/errors/skips，18 tasks，1m51s；342 个 class 全部 major 69。最低 `263.7031.23` 与正式 `263.7307.200` Verifier 均 Compatible，16 Experimental，兼容性/Internal/deprecated/override-only 问题为 0。18 条测试编译提示和 Gradle 10 弃用警告仍保留。

证据冻结在 `remaining-native/multiline-candidate/`；构建前后源码、测试与打包输入哈希核对一致。与 tree-ui 包逐 entry 比较仅 DocViewData、多行参数编辑器及其匿名/内部类变化，HTTP/目录/文档库实现 class 未变。此结果不替代新包原生输入、复制、Schema 写回与资源释放。已核对安装 JAR 并以显式 `unload-clean/idea.properties` 启动隔离 PID 32358，原生结果随后追加。第一次辅助启动 PID 32296 因父命令结束后进程退出，尚未形成 IDE 窗口；没有执行原生断言，改用持续托管的启动命令后确认测试项目 Trust 窗口，再仅信任该虚构项目。

## 419 项包原生结果：输入与写回正确，撤销及 Escape 尚未通过

PID 32358 同包真实双击聚焦 JBTextArea，粘贴含引号/反斜杠、连续 LF 的文本，再用 Enter 插入行和尾部 LF；Command+Enter 提交后单元格值逐字一致，Apply 前源码未变。双字段 Apply 后 native editor 与磁盘一致：displayName 从 NOT_REQUIRED 变 REQUIRED、age 从 REQUIRED 变 NOT_REQUIRED，description 重解析为原始多行值，name/title/example 六个哨兵全部保留，注解之外源码未变。证据 `multiline-input-result.json`、`schema-writeback-before-undo-result.json`。

原生验收同时发现两个未通过点：一次 Undo 只恢复 age，第二次 Undo 才恢复全部源码（已回读全文与原 SHA）；真实 Escape 在描述编辑中关闭整个参数 popup，而不是只取消当前 cell。另 mac Ctrl+Enter 被 IDEA 全局 New 动作截走，Command+Enter 正常；已关闭 New 菜单且未创建文件。分别保留 `schema-undo-red.json`、`schema-escape-result.json`、`schema-control-enter-conflict.json`。批量 Undo 已补任务 4.4；快捷键由公开平台 API 核对后修复/复验，不用 fixture 直接调用 Action 当成原生键分发通过。

同 PID 的原生 Tab/Shift+Tab 提交后聚焦表格，Ctrl+Tab/Ctrl+Shift+Tab 提交后聚焦图标按钮，四项均通过。未提交多行描述直接点击 Copy as Markdown，复制内容为完整四行表格，Required=N 和所有描述字符保留，实际换行仅在 Markdown 中投影为 `<br>`，源码未变。随后在 IDEA Markdown Preview 实际显示该复制文件，确认一个表头、两个字段行，displayName 描述包含引号/反斜杠及空行分隔，age 保持独立行。证据 `schema-navigation-result.json`、`schema-copy-result.json`、`markdown-rendered-result.json` 和 `markdown-rendered.png`。据此关闭 4.3；Escape/批量撤销等独立门槛保持未通过，未发布。

独立 `schema-old-schema-project` 原生 Cmd+B 已跳转到项目内 Schema 桩，确认没有 requiredMode。普通字段和该 Schema 字段同时修改草稿后 Apply，显示缺少 requiredMode/建议 2.2.5+ 的通知，弹窗及全部草稿保留，编辑器和磁盘逐字不变，普通字段没有部分写入；未编辑状态 Esc 正常关闭。证据 `old-schema-declaration.json` 与 `old-schema-rejection-result.json`。开启新项目的 CLI 客户端在等待 New Window/Trust UI 时触发 45 秒超时，但请求已送达隔离实例；在原生窗口继续完成仅该虚构项目的信任后才执行测试，不把启动辅助超时记为产品故障。

## 批次 Undo 与 popup Escape 的自动修复证据

真实 Undo/Redo 定向 RED 调用生产 `DocViewUtils.writeComment` 写入两个 Schema 字段，首次 Undo 仅恢复第二字段。修复在全字段能力预检之后为非空批次建立一个命名的 WriteCommandAction，保留原同步字段 writer；空 map 不建立命令。定向 `OpenApi3SchemaRequiredModeTest` 13 项通过，包含一次 Undo/Redo 整批恢复以及空批次不替换撤销项；证据 `build/native-validation/2026-11-09/undo-batch/automated-red-green.md`。不扩展成任意异常事务保证。

Escape 使用公开 ComponentPopupBuilder key handler，仅在无修饰的按下 Escape 且当前表格正在编辑时取消 cell 并消费事件，其余保留平台默认行为。审查发现表格由后台解析异步建立，不能在 popup 创建时捕获初始 null；实现改为每次按键读取实例当前 tableView。真实 JBPopup.dispatchKeyEvent 回归覆盖未加载默认关闭、同一已注册 handler 看到稍后建立的表格、首 Esc 取消当前 edit 并保留此前已提交草稿、再次 Esc 关闭。提交键限定 Mac Meta、非 Mac Ctrl。

两个键盘/展示测试类共 13 项通过（presentation 5、table 8），0 failures/errors/skips；日志与哈希相同的 JUnit 已保存在 `build/native-validation/2026-11-09/keyboard-popup-targeted/`。此处仍是自动证据，非 mac 原生未执行，后续全量与新包原生结果另列。没有依据这些定向结果提前勾选 4.4/4.5。

## 421 项修复包与原生单次 Undo/Redo

新候选 `remaining-native/batch-escape-candidate/` ZIP SHA `cbc2c7142094ec1905fc8443a435b23026eb34052a1b4a1418228f5fc39a9a4d`、JAR SHA `4b587f4884f45653eca10e6817aec3ae063c3dd7de684cc134aab788790dbe3f`。79 suites / 421 tests，0 failures/errors/skips；18 tasks 全部实际执行，1m55s。342 个生产 class 均 major 69。最低 `263.7031.23` 与正式 `263.7307.200` 的 Verifier 均 Compatible、16 Experimental，compatibility/Internal/deprecated/override-only findings 为 0。测试编译及 Gradle 警告保留；324 个构建/测试输入前后哈希一致。生产 JAR entry 差异只涉及参数 popup/表格及 DocViewUtils，无新增/删除 entry，未包含 observer。

安装哈希回读后，显式 `unload-clean/idea.properties` 启动 PID 46071 / IU `263.7307.200`。实际键盘 Esc：第一次取消未提交多行草稿、恢复原单元格，popup 保留，源码不变；第二次关闭 popup。再次打开后，原生粘贴/Enter 保留连续和结尾换行、引号与反斜杠，Command+Enter 提交正确。证据 `schema-escape-result.json` 和 `multiline-input-result.json`。

双字段写回时第二个多行 cell 仍处于编辑状态，直接点击 Apply 正确提交。displayName/age 的 requiredMode 双向变化、description 精确字符串、六个 name/title/example 哨兵、注解外源码不变均通过；随后一次原生 Undo 恢复完整原文，一次 Redo 恢复完整批量写回，再次 Undo 恢复测试源码。每一步均同时核对原生编辑器全文及实际磁盘文本，证据 `schema-writeback-result.json`、`schema-undo-redo-progress.json`。据此关闭 4.4；旧 Schema、复制/键盘和 GC 等后续复验单独记录，不据本段宣称整个发布矩阵通过。

同一 421 包随后完成 Tab/Shift+Tab/Ctrl+Tab/Ctrl+Shift+Tab 与真实未提交 cell 的 Copy Markdown，四行表格、描述 `<br>` 和源文件不变均通过。独立旧 Schema 项目再次 Apply 时显示 requiredMode/2.2.5+ 通知，两个字段草稿和 popup 保留，编辑器/磁盘逐字不变，无普通字段部分写入；随后 Esc 关闭。分别见 `batch-escape-candidate/schema-navigation-result.json`、`schema-copy-result.json`、`old-schema-rejection-result.json`。本轮使用已授权的纯虚构隔离项目，原 doc-demo/工作区不作测试写入；Schema 必填/转义/成员/拒绝/Undo 矩阵完成，据此关闭 3.4、4.2。4.5 的资源/GC 仍需本包后续证据，未提前关闭。
