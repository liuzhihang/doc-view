# 验证记录

2026-11-08 后续新包：已修复自身卸载前的平台禁用图标缓存清理，无 Doc View UI 的正式 build 卸载对照通过；目录使用后的 JNI 类加载器保留仍未通过。新增修复不自动继承旧包的完整原生验收结论，剩余弹窗和资源矩阵继续开放，详见[卸载 API 续验](../intellij-263-baseline-upgrade/verification-2026-11-08-unload-api.md)。下段为此前 final-v2 阶段结果。

2026-11-08 整合包续验：首次生成/幂等更新、搜索并打开文档、55 条结果的三页加载、手工修改冲突保护与恢复、项目关闭重开后的 unknown 已原生通过；磁盘索引为 57 个唯一 API ID。相同生产输入此前 395 tests 与两个 Verifier 通过，本轮未重跑自动测试。热卸载在关闭 UI、只使用目录及不打开 Doc View 界面的对照中仍失败；官方本机分析取得无障碍 JNI 与平台图标缓存保留 ClassLoader 的不同引用链，尚未验证修复。仍缺新包的在途取消、partial/retry 及完整生命周期/资源矩阵，5.4 保留未勾选，正式 MCP 未推进。详见[新整合包原生记录](../intellij-263-baseline-upgrade/verification-2026-11-08-integration-native.md)；下文 full-10/11 是旧包历史结果。

2026-11-05 在正式 `IU-263.7307.200` 补验时修复了生产 XML 插件依赖缺失，full-11 已通过 352 项全量测试与 EAP/正式版两个精确 build 的 Verifier。此次未重新执行 full-10 的 8,030 接口取消、partial/retry 等文档库完整原生矩阵，5.4 仍未完成；当前包身份和 UI/资源边界见[正式版补验记录](../intellij-263-baseline-upgrade/verification-2026-11-05.md)。下文保留旧包历史结果。

## 2026-11-08：Dubbo 项目目录遗漏修复

这是已有目录发现缺口，不是本轮引入的回归。原生候选 ZIP `50a956be5ee2534be9a2f1cf14fdc8766dc5b4b66c208f3027f2d0ef5928d94e` / JAR `d094f2b7c1d7bf7a706ea1844036595db6a64e933a9b373fbbafc1e7692273f0` 在 IU `263.7307.200` 中，从 `remaining-native/fixture-b` 的 `FictionalXmlService#readXmlUser` 直接打开 Doc View 预览，成功输出 Dubbo 文档和 `id` 参数；同一项目刷新并展开目录只列出 60 个方法，不包含该接口。原生证据为整合工作区 `build/native-validation/2026-11-08/remaining-native/xml-preview-native.json`、`new-catalog-expanded.json` 和 [当轮原生记录](../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。这证明直接预览的 XML 识别与目录枚举的候选发现范围不一致；没有修改 Markdown 字段规则。

实现收窄在 `DubboPsiUtils.findDocViewFromModule`：以 project-scoped Java annotation index 取短名候选后严格比较 Dubbo 注解 FQN；注解在实现类时遍历其父类/接口闭包；XML DOM 在项目范围发现后，使用声明 XML 的模块依赖 scope 解析 FQN，并只归档到接口源码所属模块。同来源经集合去重；在注解、继承和 XML 循环边界检查取消。没有使用 `AllClassesSearch`，也没有改 `isDubboClass`、Dubbo 方法/字段解析、Markdown 模板或 identity。

按实际修复前后的 targeted RED/GREEN 取证：

- RED 用原有 `DubboPsiUtils` 执行 2 suites / 5 tests，其中 2 tests 失败：单模块 fixture 缺少 XML-only/实现类关联接口；API/provider fixture 缺少由 provider 注册的 API。Dubbo 旧短名 alias 与 FQN 噪音断言、取消传播仍通过。原命令日志和 XML 归档在 `build/native-validation/2026-11-08/dubbo-catalog-fix/red-old-implementation.log`、`red-results/`。
- GREEN 对修复实现执行 2 suites / 5 tests / 0 failures / 0 errors / 0 skipped。覆盖 XML-only、实现类注解的传递接口、XML 与注解重复登记、缺失 FQN、错误注解 FQN、取消，以及 API/provider 跨模块和不可见同 FQN 模块隔离。provider fixture 已断言无 `PsiErrorElement`。命令日志与 JUnit XML 保存在 `build/native-validation/2026-11-08/dubbo-catalog-fix/green-targeted.log`、`green-results/`。

这是当前 Java 25 / IDEA IU 263 light fixture 定向结果；本子任务没有运行完整测试、构建、Plugin Verifier 或原生 IDEA。修复后新包的原生目录需要重新核对，目录 5.4 总体验收、项目生命周期/取消矩阵和发布门禁仍按主线程结果记录；本段不能将其标为通过。当前未覆盖 provider 依赖范围中存在多个可见同 FQN 类型的歧义场景；本轮证明的是非依赖的重复 FQN 不会串入目录。

## 当前结论（2026-10-25）

当前源码已通过 IU 263 自动全量、构建配置、构建包和严格 Plugin Verifier；新包已通过完整性错误恢复专项原生闭环。Task 2.4、4.2、5.2、5.3、5.5 已完成；5.4 的其他原生场景仍在验收。下文按阶段保留历史记录，早期段落中的未实现或待验结论只描述其对应时点。

2026-10-25 继续使用同一个 full-10 冻结包，新增原生通过：20+5 分页与末尾文档打开、用户改写冲突保护与恢复重试、真实部分发布失败与修复重试、8,030 接口生成任务在途取消与取消后重试，以及关闭/重开后的 `unknown`。没有修改生产源码，也没有把前五次未激活取消的尝试算作通过。完整资源基线与其余 UI/生命周期矩阵仍未完成，5.4 保持未勾选，正式 MCP 仍未进入生产实现。

## 2026-10-25：full-10 原生补验

本轮固定官方 `IU-263.7031.23`、JBR `25.0.4.1`，隔离 PID `41280`；启动后用实际 JVM system properties 核对 config/system/log/plugins 均位于 `/tmp/doc-view-mcp-263-20261023/profile/`。安装 JAR SHA-256 为 `b3615cd1f5a44de8dd472c3f2c8715ecd4126fe6892374fc307a5a1d7ca6f267`，ZIP 仍为 `b238f5412d134ff4311ba5422fb78d50562bfecf80826964858984d5d69bcc0b`；222/222 冻结源码和构建输入未变，因此本轮复用此前 352 项测试、构建与 Verifier 证据，没有声称重新运行 Gradle。原始 fixture、partial 副本与 cancel 副本均为虚构源码。

| 场景 | 原生观察与磁盘证据 | 状态 |
| --- | --- | --- |
| 生成、分页、打开 | 原始 30 接口项目生成返回 `No content changes`、状态为 `Current`；搜索 `fixture/items` 首次 20/25，More results 后 25/25 且按钮禁用；选择 item25 后打开正确 Markdown 路径与正文 | passed |
| 用户改写保护 | 仅在 item25 页面末尾加入虚构注释，Generate 明确列出冲突目标；除人为编辑的文件外，其余原始文件 hash 均不变，编辑未被覆盖。还原精确字节后原生重试返回 30 接口、`No content changes` | passed |
| 真实部分发布 | 在独立 partial 副本修改两个方法说明；同 JBR/同卷 probe 先证明 `uchg` 会拒绝原子替换，再对 createUser 既有页面施加限制。原生生成实际发布 findUser Markdown/JSON，createUser Markdown 替换失败；通知列出 published/failed 路径及无残留 temp。旧 `index.json` hash 不变，受影响页面打开被拒绝 | passed |
| 部分发布修复重试 | 清除唯一测试文件的 immutable flag 后，从同一 IDEA 动作重试，返回 `Updated 4 file(s)` 与 `Current`；旧搜索结果提示 snapshot changed，重新搜索后成功打开新 findUser 页面。30 条 index/detail schema、自完整性和 Markdown/detail 原始 hash 校验通过，无 tmp/immutable flag 遗留 | passed |
| 在途取消与重试 | 新建 80 个 Controller、8,000 个端点，加原有 30 条，共 8,030 接口；首次生成成功。前五轮鼠标尝试未实际激活取消或已正常完成，均不计通过。第六轮从在途进度条打开任务列表，Tab 明确聚焦 `Cancel Generate or update project documentation` 后按 Space，观察到 `Stopping`，随后面板显示 `Project documentation generation was cancelled.`；16,063 个既有文档库文件 hash 全部不变。之后原生重试返回 8,030 接口、`No content changes`、`Current` | passed，限有既存快照的生成取消 |
| 关闭、重开 | 关闭 cancel 项目后，采样中不再出现 Doc View 业务执行栈或项目 scope 描述，文档 hash 不变；重开原始 30 接口项目并搜索，状态为 `Freshness is unknown` | passed，限已观察路径 |

上述是文档库生成任务的原生取消，不替代 MCP 协议取消或 HTTP 草稿生成的取消矩阵。没有测量有效取消请求至终态的精确延迟，也没有验证全新文档根在首次发布前取消、发布中取消及大项目关闭时仍在途的全部组合。部分发布是实际文件系统 I/O 故障，不是用测试 observer 注入或断点暂停冒充原生结果。

资源观察保存了取消后与项目关闭前后的 `Thread.print`、`GC.heap_info` 和 RSS。取消后只剩一个 DocViewWindowPanel 的 invoker scope 描述，没有 exporter/service/action 业务执行栈；项目关闭后该描述也消失。RSS 在两个关闭前后采样点为 6,350,240 / 6,490,320 KiB，不能描述为下降或性能改善。没有强制 GC、监听器计数或冷/热各 7 次完整样本，因此资源/GC 门禁仍未完成。预览 Tab/Shift-Tab 未得到可观察的按钮焦点；参数编辑器只核对到 Schema 的描述与 required 初值，不能算完整写回通过。Copy/Export、完整菜单/参数编辑、HTTP Client Run 等继续由相应 change 保留待验。

证据根目录为 `/tmp/doc-view-native-20261025/`：`paging-*-ax.txt`、`conflict-*-ax.txt`、`conflict-disk-result.json`、`partial-*-ax.txt`、`partial-disk-result.json`、`partial-retry-disk-audit.json`、`cancel-keyboard-*-ax.txt`、`cancel-keyboard-disk-result.json`、`cancel-retry-success-ax.txt`、`original-reopen-freshness-ax.txt` 与 `*-project-close-resources.json`。成功取消的证据是 keyboard 轮次；`cancel-second` 至 `cancel-fifth` 等文件不代表取消通过。

收尾已退出隔离 PID `41280`；日志 1,994 行、0 ERROR/FATAL，含明确 `IDE SHUTDOWN`。普通 IDEA PID `93930` 保持运行，64343/64344 无监听，64342 仍仅为普通 IDEA；Junie 二进制 SHA 与起点一致。默认 2026.2/2026.3 配置冻结的 392/393 个 xml/properties/txt/vmoptions 文件全部未变，原始 fixture 的 84 个源/文档/清单文件全部还原且 hash 一致。`cleanup.json` 记录检查范围；不据此扩展为所有磁盘文件或所有资源均已释放。没有提交、推送、发布、sync 或 archive。

## 2026-10-24：规划与格式验证

本轮只写 proposal、design、spec、tasks、格式/schema/虚构样例与使用说明。**没有实现 JSON 文档库、搜索 UI 或生成器修改**，全部实施任务保持未勾选。当前 Markdown 导出行为的历史证据仍由原 change 管理。

已完成：

- `openspec validate project-document-store --strict` 通过。
- 仓库外临时 venv 使用 `jsonschema==4.25.1` 校验 JSON Schema 2020-12；两份磁盘样例（index/detail）符合 schema。与 MCP 契约合计21个正例及18个拒绝例通过。
- 虚构样例的现有 identity 编码、canonicalForm、apiId、snapshotId、JSON self-hash 与 Markdown/详情原始字节 hash 可复算；Node 与 Python 对这组 ASCII 键/安全整数样例的规范化 hash 一致。这不是完整 RFC 8785 实现合规测试。
- `tool-contracts.json` 中复制的磁盘 `$defs` 与源 schema 逐项相同，未知字段/版本、路径越界、篡改、缺页和原始字节变化拒绝样例符合预期。缺页/字节变化在内存模拟，未改动样例原文件。
- 本轮变更文档的相对链接、标题锚点及空白检查通过；核对冻结基线，修改范围限文档/OpenSpec，未改变生产源码、Gradle、descriptor 或既有 HTTP/UI 改动。

检查命令与详细输出保留在本机临时证据 `/tmp/doc-view-mcp-plan-20261024/`：`validate-contracts.py`、`schema-validation.json`、`check-links.py`、`link-validation.json`、`baseline.json` 与 `changed-files.json`。这些是本轮辅助证据，不能作为发布包的一部分；未来生产测试需用实际 Java exporter 生成的内容重跑。

未执行：Java 编译、Gradle tests/build、Plugin Verifier、IDEA 原生读写/搜索/UI、10,000条性能、未保存 Document 与来源变化、锁和跨进程并发、取消/partial 恢复、MCP/Skill 实际调用。所有耗时/容量指标是契约目标，未宣称已测性能。没有提交、推送、发布、sync 或 archive。

## 2026-10-24：文档库实现与定向复验

本节更新上方规划阶段状态：Java 实现已加入工作树，尚未发布。文档库固定写入项目 `docs/doc-view/`，一次 PSI snapshot 同时投影 Markdown 与结构化 JSON；UI 的生成/更新、搜索、打开使用同一组项目服务。详情与目录采用 schema v1、自完整性 hash、稳定 identity/snapshotId；只读查询不扫描 PSI。写入按逐文件原子替换，`index.json` 最后提交，跨进程 writer lock 和同进程读写锁负责协调，不承诺整批目录事务或跨进程 reader 锁。

### 定向验证与修复

`targeted-tests-6.log` 对应 `./gradlew test --tests '*ProjectDocument*' --tests '*DocumentStoreLocksTest' --tests '*ClearCacheActionTest' --tests '*PluginActionLocalizationTest' --tests '*DocViewActionLayoutTest' --tests '*ApiTreeRefreshTest' --console=plain`：实际 **93 tests / 0 failures / 0 errors / 0 skipped**，测试任务真实执行。环境 IU `261.22158.277`、macOS `27.0.1` ARM64、JBR `25.0.2`、10 processors；生产源码当时仍按 Java 21 编译。XML 与性能 JSON 留存 `/tmp/doc-view-store-impl-20261024/targeted-6-results/`，原始日志同目录上级。此结果不是正在升级的 263 的验收结果。

先前失败保留在 `targeted-tests-3.log` 至 `targeted-tests-5.log`。本轮已修复/复验的真实问题包括：Backgroundable 重复启动同一 ProgressIndicator；基础值/集合根匿名字段被 JSON 格式错误拒绝；外部 content root 的 Java/DTO 编辑未标记 stale；部分发布后取消错误地将已成功发布的路径重复列为失败路径。测试层还修正了本地化期望、light project 服务复用假设和已改名的 UI 字段引用。最终 commit callback 中取消实际 indicator 并抛出 PCE 的动作测试已通过，成功提交不会被报成取消。

随后复查 targeted-5 的原始栈，定位到提交完成后 `LocalFileSystem.refreshAndFindFileByNioFile` 仍观察到已取消 indicator，并从 `Task.run` 的 finally 抛出 PCE，导致该轮完成通知未到达。targeted-6 恰好未再触发该路径，不能因此忽略已有失败证据。动作现已隔离这次 VFS 刷新的 PCE，保留已取得的成功/partial 结果；此最后修复待263定向及全量复验。

跨进程 writer 排他由独立 JVM 测试证明；查询的 path/hash/freshness/cursor、两个 Project 隔离和 UI 状态由 light fixture/contract tests 证明，尚不替代原生操作。

### 性能与取消范围

10,000-entry index（12,060,587 bytes）本次 fixture 的 warm search 30 samples p95 **27.357 ms**、cold load 7 samples p95 **440.422 ms**、query cancellation 30 samples p95 **0.578 ms**。该取消样本在查询启动后发出，不能单独证明解析阶段中断。

另用合法近上限 index（31 entries，31,655,679 bytes）观察工作线程确实进入 `ProjectDocumentFormat.readIndex` 后再取消，5 次均观察到解析栈、无提前完成/漏取消；p95 **438.219 ms**，低于 500 ms 契约目标。该实现是在解析结束后的活动检查处响应，不宣称解析每个 token 均可中断；数据仅代表当前测试机器及 fixture。报告分别为 `performance.json` 与 `parse-cancellation.json`，均明确 `nativeIdeValidated=false`。

### 未完成门禁

用户随后批准 `intellij-263-baseline-upgrade` 将 Java 一并提高到25；该独立 change 的基线变化与文档库功能分开取证。下节记录后续263复验，尚未加入正式 MCP provider 或 Skill 安装器。未提交、推送、发布、sync 或 archive。

## 2026-10-24：IU 263 自动回归阶段记录

263 的测试依赖闭包补齐后，`baseline-263-full-5.log` 实际执行339项测试，16项失败。失败包括新平台测试类加载器无法提供插件版本、旧菜单名称断言，以及异步打开后的测试等待与沙箱 Scratch 清理。该轮构建/配置/严格 Verifier 分别通过，但组合命令失败，不能记为完整通过；原XML和日志保存在 `/tmp/doc-view-store-impl-20261024/263-full-5-results/`。

版本现从与 descriptor 同一 `pluginVersion` 构建属性生成的 `META-INF/doc-view-build.properties` 读取，不依赖 Internal API 或测试类加载器。`baseline-263-targeted-6.log` 的52项复验中49项通过、3项失败：实际 descriptor 与构建元数据一致性、6项 snapshot、Markdown、2项导出动作（含提交后取消）、25项 HTTP、6项欢迎页已通过；仍失败的旧菜单名称及 MethodDoc Scratch 残留隔离继续修复。后者实际创建 `status_1.md`，说明此前超时留下未打开的 `status.md`，使精确路径断言和只读注入失效；不能仅归咎于等待时间。证据保存在 `/tmp/doc-view-store-impl-20261024/263-targeted-6-results/`。

静态复查还发现 `requestOpenFile` 异步异常会绕过外层同步 catch，项目文档可能在真正打开前显示成功。随后按基线 change contract 加入打开成功/失败/取消反馈，并完成后续全量矩阵；结果见下节。IU 263 原生生成/搜索/分页、外部改写、取消/partial/retry、项目关闭/重开仍未验收，不能以自动测试关闭正式 MCP 的前置门禁。

## 2026-10-24：IU 263 full-7 自动回归与包检查

`/tmp/doc-view-store-impl-20261024/baseline-263-full-7.log` 的组合构建实际执行18项 Gradle tasks，包含 `verifyPluginProjectConfiguration`、`buildPlugin` 与 `verifyPlugin`；构建成功。对应 XML 汇总 `/tmp/doc-view-store-impl-20261024/263-full-7-results/summary.json` 为 **344 tests / 0 failures / 0 errors / 0 skipped**，包含文档 snapshot、format、查询、发布锁与取消、UI actions、异步打开生命周期和全量项目回归。此前 full-5、targeted-6 等失败日志保留在本机证据目录；full-7 是修复后的新结果，不删除历史失败。

本次构建的 `Doc View-1.4.0.zip` 读回后，内层插件 JAR 的全部 classfile major 为 **69**；打包 `plugin.xml` 中 plugin ID/version 与 `META-INF/doc-view-build.properties` 中同一组值一致。IU-263.7031.23 Plugin Verifier 报告为 **Compatible**，列出16处 experimental API 使用（`FileEditorOpenRequest` / `requestOpenFile`），因此仍需按基线 change 做原生 IDE 验收。文档库实现未新增 production/runtime dependency；Gradle 中为 263 Java/XML/VCS/Maven resolver 环境添加的 bundled modules 仅用于测试运行时。

性能报告 `performance.json` 与 `parse-cancellation.json` 位于 `/tmp/doc-view-store-impl-20261024/263-full-7-results/`，均声明 `nativeIdeValidated=false`。在 IU-263.7031.23、JBR 25.0.4.1、macOS 27.0.1 ARM64 的 light fixture 中，10,000-entry、12,060,587-byte index 的 warm search p95 **17.309 ms**（30 samples），cold load p95 **426.271 ms**（7 samples），query cancel p95 **0.103 ms**（30 samples）。合法近上限 index 为 31,655,679 bytes；5/5 次均观察到 worker 进入 `ProjectDocumentFormat.readIndex` 后再取消，parse-stage cancel p95 **318.439 ms**。这些指标只代表本机测试 fixture，不替代原生 IDEA 性能。

### 工具窗口状态测试迭代

`ProjectDocumentLibraryUiStateTest` 在 IU 263 light fixture 中驱动真实 `DocViewWindowPanel` 和可见控件，并使用真实项目索引服务、文件与 store lock。迭代证据按时间保留：

- `263-ui-state-1.log`：编译失败，**0 tests**。原因是 EDT 测试回调类型不匹配；没有行为结论。
- `263-ui-state-2.log`：**3 tests / 1 failure**。失败仅因 tamper 测试的文本查找器未匹配无固定前缀的校验错误标签，未证明生产行为失败。
- `263-ui-state-3.log`：**3 / 0 / 0 / 0**。无索引搜索保持只读且能显示无结果；`unknown → current → stale` 状态通过提交事件自动刷新；外部改写 Markdown 后，面板拒绝打开并显示校验错误。
- `263-ui-state-4.log`：增加 busy 与 pending-search 场景后为 **5 tests / 1 failure**。四个真实读取占满 read slots 时，面板 busy 反馈通过；pending 测试因 headless `Task.Backgroundable` 同步执行，等到 query timeout 后才返回，未能观察待取消状态，不构成生产取消失败。
- `263-ui-state-5-results/TEST-com.liuzhihang.doc.view.ui.window.ProjectDocumentLibraryUiStateTest.xml`：最终 **5 / 0 / 0 / 0**，约9秒。测试启用异步 headless 调度后，实际 panel search worker 进入 `DocumentStoreLocks.tryAcquire` 等待；返回目录视图取消请求、丢弃迟到结果，释放锁后新搜索成功。相关用例耗时 **0.814 s**。

该轮测试覆盖无索引、无结果、unknown/current/stale、外部 Markdown 改写 fail-closed、busy、pending 搜索取消与恢复，但当时没有覆盖部分发布在面板中的反馈，因此 task 4.2 当时仍未完成。后续补充的 partial publication UI 证据见下节；这组 light fixture 测试本身仍不等于 IU 原生安装包验收。

随后 full-8 使用包含这组 UI 状态测试的当前源码完成自动回归：`/tmp/doc-view-store-impl-20261024/baseline-263-full-8.log` 显示 **18 Gradle tasks executed**、构建成功、耗时 **1m51s**；冻结 XML 汇总 `/tmp/doc-view-store-impl-20261024/263-full-8-results/summary.json` 为 **66 suites / 349 tests / 0 failures / 0 errors / 0 skipped**。full-8 插件 ZIP 与 full-7 ZIP 字节一致，SHA-256 为 `361bfe2033c0e6f394ccb8c5edbfcb31418a648e7ba2ca1c165416ddc6398872`，包含 **329 个 major 69 class**；IU-263.7031.23 严格 Plugin Verifier 为 **Compatible**，仍报告16处 experimental API。故 task **5.2 自动回归已完成**；这些结果不替代 task **5.4** 的原生安装包烟测。

full-8 的 `performance.json` 与 `parse-cancellation.json` 也保存在该冻结目录：10,000-entry light fixture 的 warm search p95 **20.773 ms**（30 samples）、cold load p95 **571.919 ms**（7 samples）、query cancel p95 **0.417 ms**（30 samples）；合法近上限 index 的 parse-stage cancel p95 **401.108 ms**（5 samples）。报告的 `targetMet=true`；样本来自 light fixture，不代表原生 IDEA 性能。full-7 的数值保留为上一轮历史记录，不覆盖 full-8 结果。

此前的 **261 targeted-6（93项）**与 **263 targeted/full-5 失败**均保留为历史记录，不作为当前通过结果。5.4 原生项目读写、搜索、取消与生命周期烟测正在进行；5.5 的最终严格状态/范围核对尚未完成。UI-state 单测的失败轮次仅以各轮测试 XML 和命令日志为证；曾复制到该目录的旧 `idea.log` 已标注为非本轮日志，不用于本节结论。

### IU 263 partial publication UI 定向复验

`./gradlew test --tests '*ProjectDocumentPartialLibraryUiTest'` 最终执行 **1 test / 0 failures / 0 errors / 0 skipped**，测试耗时 **4.832 s**，命令耗时约 **9 s**。冻结证据为 `/tmp/doc-view-store-impl-20261024/263-partial-ui-4-results/TEST-com.liuzhihang.doc.view.service.export.ProjectDocumentPartialLibraryUiTest.xml`、同目录 `summary.json` 和本轮 `idea.log`。

用例以真实 `ProjectDocumentExporter` 和确定性 `ExportObserver.beforeMove` 在第二次文件替换前注入 I/O 失败，确认首个 Markdown 已发布、`index.json` 仍是旧提交；随后通过反射调用现有 `ProjectDocumentExportAction.reportFailure`，只隔离验证真实 partial exception 到面板内联错误及 ERROR 通知的映射。它没有模拟整条后台 action 的失败注入，也没有验证原生取消。用例接着通过真实后台生成 action 重试，验证新 snapshot/current 状态、更新后的 Markdown 内容、搜索与打开成功；旧 snapshot 下的已发布页面则搜索后 fail-closed。

此前迭代作为测试修复证据保留：partial-ui-1 编译失败、0 tests（`Notifications` 不是函数式接口）；partial-ui-2 有1项失败（测试把首个发布路径错误固定为 alpha）；partial-ui-3 有1项失败（期望保留空格的 Javadoc 文案，与既有解析会移除空格的语义不符）。partial-ui-4 修正断言后通过。上述失败均未发现生产缺陷，不覆盖或删除对应冻结证据。

partial-ui-4 完成后，tool-window 状态测试与本用例共同覆盖 task 4.2 的自动化 UI 状态和操作。随后 full-9 冻结为 **67 suites / 350 tests / 0 failures / 0 errors / 0 skipped**，但该运行早于下方完整性错误恢复修复，不能作为当前源码的最终全量证据。IU 263 原生最终包烟测仍由 task 5.4 单独验收。

### 完整性错误恢复修复（自动复验通过，专项原生复验通过）

原生验证发现：外部改写 API Markdown 后，服务按 hash 拒绝打开并记录 `INDEX_INCONSISTENT`；将文件恢复为原始字节后再次成功打开，状态仍停留在 `INCONSISTENT`。`263-integrity-recovery-red-results/` 中的 `ProjectDocumentIndexServiceTest.testRestoredDocumentResourcesClearOnlyTheirOwnIntegrityErrors` 在旧实现下复现该问题，失败为预期状态仍是 INCONSISTENT 而非 READY。

当前实现把错误按 snapshot 与文档根内的资源相对路径分别记录；Markdown、详情 JSON 和 `index.md` 只有在路径、hash、适用的 schema/identity、UTF-8 及活动状态检查全部成功后才清除各自错误。每次失败递增单调 token，成功读取仅能清除启动时捕获的相同 token；cache 清理和新提交不重置计数，避免 ABA。成功恢复只发布状态变化，不改变 freshness epoch；其它资源错误仍保留。新增测试覆盖 Markdown、详情 JSON、`index.md` 的逐资源恢复，以及同资源新失败和 cache 清理/提交竞态。

定向命令 `./gradlew test --tests '*ProjectDocumentIndexServiceTest' --tests '*ProjectDocumentLibraryUiStateTest' --tests '*ProjectDocumentPartialLibraryUiTest'` 在 IU 263 light fixture 上完成 **3 suites / 20 tests / 0 failures / 0 errors / 0 skipped**；冻结 XML、正确 `idea.log` 与汇总位于 `/tmp/doc-view-store-impl-20261024/263-integrity-recovery-green-2-results/`。恢复用例验证 Markdown、详情 JSON、`index.md` 分别恢复后只清除自身错误；token 竞态用例通过。UI 用例在真实工具窗口中验证外部改写后拒绝打开、恢复原字节后再次打开并显示当前状态；partial publication UI 回归也通过。因此 task 2.4 与 4.2 重新完成。

该轮首次 green-1 有一项既有 freshness 用例失败：同一 light Project 复用 service，新的测试和先前提交的 `Greeter` 快照 identity 相同，初始 disk-only 查询因此继承先前 generation 的 `STALE`，而用例期望 `UNKNOWN`。测试改用唯一的 `FreshnessProbe.captureFreshness` fixture，保留 UNKNOWN 期望和生产 freshness 语义；green-2 已复验通过。

### IU 263 full-10 当前源码自动回归与包审计

完整性恢复修复后的 full-10 完成 **67 suites / 352 tests / 0 failures / 0 errors / 0 skipped**；18 项 Gradle tasks 均实际执行，耗时约 **1m55s**。证据位于 `/tmp/doc-view-store-impl-20261024/263-full-10-results/`，包括 XML、正确的 `idea.log`、`summary.json`、`performance.json`、`parse-cancellation.json`、`package-audit.json` 与 Plugin Verifier 报告。故 task 5.2 和 5.3 已完成。

目标平台为官方 IU `263.7031.23`、JBR `25.0.4.1`。构建 ZIP 为 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/Doc View-1.4.0.zip`，SHA-256 `b238f5412d134ff4311ba5422fb78d50562bfecf80826964858984d5d69bcc0b`，大小 **722,118 bytes**；内层 JAR SHA-256 `b3615cd1f5a44de8dd472c3f2c8715ecd4126fe6892374fc307a5a1d7ca6f267`。包含329个生产 class，class major 全为69；descriptor/build metadata 的插件 ID、版本一致，`sinceBuild=263.7031.23`。严格 Plugin Verifier verdict 为 **Compatible**，0 deprecated/Internal API、16 experimental API。该记录只证明当前候选构建和自动门禁；task 5.4 仍须对这个新 ZIP 做隔离 IU 263 原生复验。

full-10 light fixture 的 10,000-entry、12,060,587-byte 索引测得 warm search p95 **22.310 ms**（30样本）、cold load p95 **472.359 ms**（7样本）、query cancellation p95 **0.332 ms**（30样本）。合法近上限索引为31,655,679 bytes；5/5 次采样观察到 worker 已进入 `ProjectDocumentFormat.readIndex` 后再取消，解析阶段取消 p95 **479.984 ms**。目标均在该 fixture 中达成；报告明确 `nativeIdeValidated=false`，不能当作原生 IDEA 性能数据。该自动测量阶段尚未完成 task 5.4 与 5.5；后续原生专项和最终范围核对见下文，5.4 总体验收仍不完整。

### IU 263 完整性恢复专项原生复验

使用 full-10 冻结 ZIP 在隔离 IU `263.7031.23` 实例中复验。GUI 操作链为：外部篡改 API Markdown 后尝试打开并被拒绝；将文件精确恢复为原始字节；通过 Find Action 执行 **Reload All from Disk**，再搜索 `findUser` 并打开落盘 Markdown。恢复后的原文成功打开，面板自动显示本 session 的 `Current`、30 个 API 及对应的 `Opened` 路径；此流程没有重新生成文档或清理缓存。

磁盘审计确认文档根内 **63 个文件**的 hash 与字节数全部恢复到篡改前值；重读索引后，**30 个条目**的 schema 与 hash 校验通过，snapshotId 为 `6fb9df1f6c8c29d2eaca307f10f8bb68ac2d9e01710fbf7538a00d484c0b927a`。隔离安装读回的 JAR SHA-256 与 full-10 包记录一致。证据位于 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/native/`，包括 `integrity-tamper-disk.json`、`integrity-restore-disk.json`、`integrity-before-files.json`、`integrity-after-files.json`、`store-after-integrity-restoration.json` 和 `installation-readback.json`；磁盘脚本自身不声称验证 GUI，GUI结果以本轮隔离实例操作记录为依据。

该专项只关闭原生完整性恢复场景，不代表 task 5.4 整体完成。新包的原生取消、partial publication/retry 与其余生命周期场景仍待验证。

### 最终范围与状态核对

Task 5.5 的收尾检查通过：`openspec status --change project-document-store --json`、`openspec validate project-document-store --strict` 与 `git diff --check` 均完成；四个并行 change 的 strict validation 结果都为 valid。最终只读 scope audit 保留于 `/tmp/doc-view-store-impl-20261024/scope-audit-final-close.json`：HEAD `7b2e0a8` 未变、无 staged 路径、98 个工作差异路径全部归入获准的 baseline、HTTP/UI、MCP 文档与 store 范围，未分类项为0；222/222 个冻结源码/构建输入 hash 与 full-10 候选包匹配，`.agents`/`.codex` 基线内容保留，未发现正式 MCP 注册或生产依赖。

full-10 清理证据 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/native/cleanup.json` 显示测试进程均已退出、普通 IDEA 与 Junie 保留、20 个 fixture 文件均保留且 hash 与清单一致，临时负载源码已移除；store 的63个文件 hash/bytes 均恢复，30条索引 schema/hash 校验通过。意外的默认263启动记录单独保留，不计入功能验证。未执行 commit、push、tag、release、sync 或 archive 操作。

因此 task 5.5 已完成；task 5.4 仍未完成，Copy/Export 完整 UI、原生取消、partial/retry 与其余资源/项目生命周期场景未全部验收，不能将自动化与完整性恢复专项结果扩大为整体验收通过。

## 2026-11-08：Dubbo XML freshness 与未保存输入状态

整合包原生复现了一个既有 freshness 跟踪缺口：隔离 fixture-b 中将 `fictional-dubbo.xml` 的服务接口改为不存在的 `poc.MissingXmlService`，IDE 编辑器确认该修改尚未保存、磁盘字节保持原样；项目文档搜索仍显示 `Current`、62 个 API 且匹配 1 项。随后原生 Undo 和 Save 恢复了编辑器及磁盘内容。记录见 [`xml-freshness-result.json`](../../../build/native-validation/2026-11-08/remaining-native/dubbo-candidate/xml-freshness-result.json) 与 [`xml-freshness-observed.json`](../../../build/native-validation/2026-11-08/remaining-native/dubbo-candidate/xml-freshness-observed.json)。此前候选的原生 `Generate or Update` 62 条目录及无变化重复生成通过；这条旧候选结果不替代 XML freshness 修复后的新包验收。

根因是索引服务虽把 PSI 与 Document 事件交给同一 freshness handler，但 handler 只接收 `.java`，未跟踪 Dubbo XML 声明。当前实现仅将项目内容根内 Java/XML 文件变化纳入 epoch，继续排除 `docs/doc-view/`，并保留外部内容根。快照的 `hasUnsavedDocuments` 同步覆盖内容根内未保存 Java/XML Document、排除输出目录；既有未提交 Java 源拒绝用例继续通过。对 XML 使用保守范围，因此不含 Dubbo 声明的未保存 XML 也可能令标记为 true。JSON schema v1 与既有 Markdown 输出保持不变，`source.capturedContentSha256` 仍只对应 API Java 来源 Document。

首批两项定向回归在旧实现下均失败：XML 编辑后 freshness 预期 `STALE`、实际为 `CURRENT`；快照已解析出未保存 XML 注册的 Dubbo-only API，但 `hasUnsavedDocuments` 错误为 false。RED 为 **2 tests / 2 failures**，日志及 XML 报告保存在 `build/native-validation/2026-11-08/xml-freshness/red-targeted.log` 和 `red-results/`。

复核发现未保存输入 helper 在看到第一项已提交的未保存 Document 后提前返回，无法检查后续输入。新增多输入测试按 `FileDocumentManager.getUnsavedDocuments()` 的实际顺序构造一个已提交但未保存 XML 与其后的未提交 XML；旧实现错误地返回完整快照，因此再现该边界 **1 test / 1 failure**。有两次测试夹具尝试未构成产品 RED：一次平台 `commitDocument` 把同批编辑的两份 XML 都提交，另一次在 PSI indexes 未就绪时提前拒绝；记录保留在 `red-multiple-input-ordering-fixture-failure.log` 与 `red-multiple-input-psi.log`，最终用例在设置 dirty 状态前等待索引就绪。

修复恢复了对所有未保存输入的完整遍历：任一项目内容根内 Java/XML Document 尚未提交即拒绝快照；扫描完毕后再返回保守 `hasUnsavedDocuments` 值。JSON schema v1、Markdown 输出与 `source.capturedContentSha256` 语义不变。IU 263.7031.23 / JBR 25.0.4.1 下两完整相关测试类最终 **2 suites / 23 tests / 0 failures / 0 errors / 0 skipped**（索引 15、快照 8）；GREEN 日志与冻结 XML 在 `green-focused-suites-after-accumulator.log`、`green-final-results/`。它们覆盖外部 content root 的 XML freshness、未保存 Dubbo XML 的解析与磁盘保护、已提交项之后的未提交输入拒绝，以及原有 Java freshness/DTO 测试。

`openspec validate project-document-store --strict` 和 `git diff --check` 通过，task 2.6 完成。最终 accumulator 修复后的全量 `./gradlew test`、Plugin Verifier 和新包原生复验尚待主线执行，tasks 5.2、5.3 留待门禁，task 5.4 仍未完成；此前整合包的 `Current` 不能证明 XML freshness 修复已在最终新包原生通过。

## 2026-11-07：1.5.0 基线整合中的取消修复

本节仅对应独立 `codex/release-263-integration` 工作树，原开发工作区保持不变。冷索引解析在首轮全量中真实失败：31,655,679-byte 合法 index，5/5 进入 readIndex 后取消，p95 512.483 ms，超过既定 500 ms；不是只收到取消命令或提前取消。原始 RED 位于 `build/integration-verification/red/`，未降低阈值。

修复为严格 UTF-8/JSON 分块读取、条目校验、canonical JSON 和 SHA-256 轮询同一 ReadContext；取消/超时/项目关闭保留原错误分类，缓存安装前检查状态。Gson 的内存树到 DTO 调用只有边界检查，不宣称其内部逐字段可中断。格式 v1 字节、schema 和严格性保持不变。新增条目阶段测试也先 RED 后 GREEN，定点读取/完整性/语义校验取消均通过。

最终针对性 3 suites / 28 tests 通过；全量 76 suites / 395 tests，0 failures/errors/skipped，构建、配置检查及两个精确 build 的 Verifier 通过。最终全量取消 p95 为 0.147 ms，5/5 observed、0 missed、0 completedBeforeCancel。此为 light fixture，不代表原生或 MCP 协议取消。中间工具链/编译失败与修复均保留，详细包 hash、日志、版本风险和发布前条件见[发布前核对](../../../docs/release-preflight-2026-11-07.md)。

Task 5.4 仍未完成；当前整合包没有进行新的原生回归，不将 full-10/full-17 的证据迁移到新包，不归档此 change。

## 2026-11-08：XML freshness 全量与原生复验

补齐完整遍历防御后，78 suites / 409 tests、构建和最低/正式两个精确 build 的 Verifier 通过。冻结 ZIP `a08ae7e2…d6319` 在隔离 PID 3786 中完成 XML 编辑 → Stale → 未保存生成 61 条及标记 → Undo/Save → 恢复 62 条 → 幂等更新；未自动保存 XML。完整 SHA、输入与原生证据见 [续验记录](../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md#xml-freshness-最终修复包原生闭环)。新发现的 CGLIB 类加载器保留由 task 1.6 单独修复，不能由本次 freshness 通过代替；5.4 总体原生验收仍待补齐。

## 2026-11-09：XML PSI 新候选自动门禁

自有 DOM model/注册已移除，XML 识别、模块目录与定义导航共用公开 PSI 查询。真实 RED 的空属性 NPE、consumer 停止失效已修复；4 suites / 18 定向 tests 全通过。随后 `test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-parallel` 实际执行 18 tasks，79 suites / 414 tests，0 failures/errors/skips，构建耗时 1m57s。最低 IU 263.7031.23 和正式 IU 263.7307.200 的 Verifier 均 Compatible、16 Experimental，未报告 compatibility/Internal/deprecated/override-only 问题。测试编译仍有 18 个 test-only warnings 与 Gradle 10 兼容提示。5.2/5.3 依此完成；1.6 的原生卸载及 5.4 总体仍未勾选。

冻结 ZIP SHA `67cdbe75cfed62279075bfd980efa39194e9e67c98deaeca10bbc4d62706b9c7`，JAR SHA `db1e4e32e8466bd64965bd57a35830dcf7d123fb35bedd667c2709ebb5288f32`，338 个生产 class 均 major69、无探针及被移除 DOM classes。包内与409候选相比，仅 descriptor、DubboPsiUtils、DubboDefinitionSearch 改变，新增 DubboXmlPsi、删除三个 DOM model；其余生产 entries 字节一致。证据在 `build/native-validation/2026-11-08/remaining-native/xml-psi-candidate/`。新包安装到正式 IU263 的隔离 PID13788 后，原生结果由主线单独记录，不能继承旧包完整通过结论。


## 2026-11-09：414 项候选原生 XML 与文档库复验

同包 PID 13788 已通过 XML-only/实现类注解目录及 Markdown、实际 XML 定义导航、XML 未保存编辑 freshness 和 Undo/Save 幂等更新。真实 index.json 发布失败触发 partial，旧索引拒绝已改写页面，解除故障后重试成功并核对 62 条/124 个引用 hash，源码恢复原快照。精确数据见 [本轮原生记录](../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md#2026-11-09xml-psi-最终候选的构建与原生回归)。

三轮资源和四组 Finder 遮挡通过，但完整卸载仍失败：新 heap 最短链为 Local JNI → 平台树展开监听器 → BasicTreeUI Handler.pressedPath → 插件 ClassNode。这是基线 change 新 task 6.3 的待修复边，不能将 XML PSI 自动/功能通过写成卸载通过；本 change 1.6 保持未勾选，5.4 的完整取消/项目生命周期矩阵仍保留。进程退出后 21 个原 fixture 源码/模块文件和原工作区 584 文件 hash 不变；没有发布。


树 UI 修复后的 ZIP `03f48336…4205b` / JAR `20fdd64a…2a74b` 再次通过 414 tests 和两精确 build Verifier，唯一生产 entry 变化为 `DocViewWindowPanel.class`。PID19963 同包 XML/Dubbo/导航/freshness 复验通过，实际禁用后插件对象记录归零、目标类消失且无 still-referenced 警告；1.6 因此完成，5.4 的完整矩阵仍保留。详见[热卸载结果](../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md#树-ui-修复包原生热卸载通过)。

## 2026-11-09：428 项目关闭与文档库 freshness 原生复验

使用 IU `263.7307.200` 隔离 PID 66650 的 428 候选：ZIP SHA-256 `d377299230eb6ac6ff773f25283664839cd54b4b17c4622ddcee62cb9230efc8`，JAR SHA-256 `a7bcdf905c920a48900fba0680d1b46699e8d8513963b05ddda94e5ef4892b53`。相应自动门禁为 80 suites / 428 tests，0 failures/errors/skips，两个精确 build Verifier 均 Compatible。候选身份见[`artifact.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/artifact.json)。

原生项目关闭验证中，未创建过 tool window 内容的 `fixture-close-unopened` 可关闭；关闭 `fixture-b` 后 `schema-current-project` 窗口仍打开且目录刷新成功。应用退出确认框取消后项目和目录仍保留；这是应用退出被取消、尚未进入 `ProjectCloseListener.projectClosing` 的结果，不把它写成已测试项目关闭 veto。结果见[`unopened-close.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/unopened-close.json)、[`a-close-b-kept-result.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/a-close-b-kept-result.json) 与[`exit-cancel-result.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/exit-cancel-result.json)。项目关闭清理覆盖已复现的原生 UI/EDT 路径，不声明任意后台同步 close API 的线程契约。

文档库显式生成后为 Current，共 65 entries 且 `hasUnsavedDocuments=false`。关闭并重开项目后显示 `Freshness is unknown`，65 entries 保留；133 个生成文件 hash 全部不变，且未触发 source parse。新会话需显式 Generate 后才确认 Current。证据见[`freshness-generated-result.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/freshness-generated-result.json) 与[`freshness-reopened-result.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/freshness-reopened-result.json)。

同一候选的三轮资源观测中 `ParamDocEditorForm` / `ParamTreeTableView` 均为 0，`DocViewWindowPanel` 为 2、`PreviewForm` 为 1，renderer children 为 0；三轮可见 preview 分别为 HTML、源码、HTML，FD 均为 515。三轮 RSS 为 4,363,056 / 4,362,416 / 4,371,664 KiB。此处记录有限实例与模式观察，不称为长期资源基线或所有 preview 对象归零。禁用插件后 histogram 中 Doc View 对象为 0、目标类缺席且无 still-referenced 告警；卸载日志仍包含平台 `FileManagerImpl` possible-invalidation warning。证据见[`resource-cycle-summary.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/resource-cycle-summary.json)、[`resource-visible-modes.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/resource-visible-modes.json) 与[`unload-result.json`](../../../build/native-validation/2026-11-08/remaining-native/project-close-candidate/unload-result.json)。

因此，本轮补齐了项目关闭、另一项目隔离、重开 freshness 和对应热卸载证据；task 5.4 仍未完成，文档生成取消后的可见终态/无部分索引验证及其余原生矩阵仍待补齐。429 guard 修正版的 429 项测试和两个精确 build Verifier 已通过；429 搜索取消的原生功能也已通过，但当时显示文案误称“状态检查”已取消，通用文案改动尚待新候选原生复验。其 `ToolWindowFactory` 与文档库生产 entries 和428一致，但本节不声称428是429的整包原生回归。PID 66650 已退出，原工作区冻结的584个文件未变，Junie 保留。

## 2026-11-09：429 搜索取消功能与共享提示文案

正式 IU `263.7307.200` 的 429 候选在真实搜索迭代中暂停 `ProjectDocumentIndexService.lambda$search$0`（line 266，BCI 147），随后通过 IDEA 原生 Stop 取消。暂停只控制调用次序，没有调用产品取消 API；取消由原生 Stop 发起。`query-cancel-result.json` 确认取消终态没有应用部分搜索结果、文档库 133 个文件 hash 不变；`query-retry-result.json` 显示重试搜索返回预期 3/3 条，且仍未改动文件。对应的 `query-stop-clicked.json` 和 `query-cancel-breakpoint.txt` 位于 `build/native-validation/2026-11-08/remaining-native/status-cancel-candidate/`。这是受控原生功能验证，不测量取消延迟，也不表示所有搜索阶段都已验收。

取消功能通过时，UI 显示 `Project document status check was cancelled.`。源码中 `project.library.status.cancelled` 同时供 status refresh 的 onCancel 与查询/打开的 `Code.CANCELLED` 错误映射使用，因此该状态专属文案不准确。已将同一个 key 调整为通用 `Project document operation was cancelled.` / `项目文档操作已取消。`，不增加 key 或改变错误分类、取消流程。429 包仍是旧文案；新文案要由下一冻结候选在搜索取消和状态检查取消场景分别原生复验，不能将 429 的功能通过描述为本次文本也已通过。

同目录的生成任务取消是另一条功能路径：原生暂停在发布前观察到 3 个临时文件，Stop 后临时文件清理、133 个旧文件 hash 不变，源码由原生 Undo/Save 还原；随后重试回到 Current 且旧文件字节不变。证据为 `generation-cancel-result.json`、`generation-retry-result.json`、`generation-staged-before-cancel.json` 和 `generation-cancel-source-state.json`。该结果不与只读搜索取消混为一项，也不替代 task 5.4 其余生命周期验收。

429 候选为搜索取消和文档生成取消分别补充了原生功能证据，但 task 5.4 仍未完成：通用取消文案须在新候选上复验，资源/GC 与其余原生场景也仍待主线验收。
