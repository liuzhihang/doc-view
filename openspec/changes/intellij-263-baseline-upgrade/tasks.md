# Tasks

## 2026-11-05 正式版补验

- [x] R1 补齐 bundled XML 的构建及 descriptor 依赖，运行完整测试、构建、配置检查和原 EAP / 正式 `IU-263.7307.200` 的严格 Plugin Verifier；full-11 为 67 suites / 352 tests / 0 failures/errors/skips，18 tasks 实际执行，两 build 均 Compatible、16 Experimental usages、无 compatibility/internal/deprecated/override-only 问题。最终 ZIP SHA `95c47deb64df9de9c40fab39e5ad07521e8ae61348e9184a08006e5e6d61e6dc`，329 个生产 class 均 major 69，单一 JAR，无测试依赖。详见[正式版补验记录](verification-2026-11-05.md)。
- [ ] R2 将重建最终包安装到正式版隔离 profile，补验 Copy/Export、参数写回、菜单、资源/GC 和受影响 XML/Dubbo 路径；Junie 保留，未通过场景保持明确，不推进正式 MCP。

截至 2026-11-07，full-17 已修复七个 Keymap 组的中英文标题，70 suites / 362 tests、构建及 EAP `263.7031.23` / 正式 `263.7307.200` 两个 Verifier 通过，仍有 16 Experimental usages。同包原生已验中英 Keymap、参数 Copy JSON、预览 Copy/Export、中文 Schema 写回与一次 Undo、上传菜单取消、目录 Enter，以及 Pin 开/关跨应用草稿保留和前台交互。三轮关闭后静置约 150 秒，表单、Markdown JCEF panel、RemoteBrowser 和客户端 Java 对象全部归零，FD 无逐轮增长；正常重启/退出后对应 IDEA 与 CEF 进程已消失。完整桌面视觉遮挡、`…`/目录右键菜单可见结果、CEF native peer/listener/性能及其余矩阵仍未通过，R2/4.2 保持未勾选，正式 MCP 不推进。Junie 保留，临时语言与源码已恢复；最低 build 仍为 `263.7031.23`。详情见[full-17 续验记录](verification-2026-11-07-full17.md)；full-13/14/16 和下方旧日期均保留为历史阶段。

## 1. Codex 客户端取消互操作准入（阻塞生产升级与回归）

- [x] 1.1 使用随 Codex app 的 CLI `0.163.0-alpha.11.2`，通过临时 Codex app-server MCP 配置连接隔离 IU `263.7031.23` 的 MCP Server，启动一项有界且仍在运行的探针调用；保存 Codex turn、session、JSON-RPC request ID、IDE build、服务端 run ID 与时间戳。不得修改普通 Codex 客户端配置，也不得用自制 HTTP client 或官方 TypeScript SDK 代替该客户端门槛。
- [x] 1.2 在任务1.4固定候选包后，通过 Codex turn interrupt 在原调用仍在途时主动取消，连续复验三轮；保存 initialize protocolVersion，按协商协议关联真实取消通知或原请求 SSE 响应流关闭、服务端对应 Job 取消并只 cleanup 一次、无迟到成功结果，随后同一 session 的正常 echo 调用成功。结果：IU `263.7031.23` 隔离服务与 `codex-app-server 0.165.0-alpha.1` 协商 `2025-06-18`；原 delay request ID `5`、`7`、`9` 均收到匹配取消通知，服务端 run `1`–`3` 各取消并 cleanup 一次，31.005秒观察内无迟到完成，echo ID `6`、`8`、`10` 均成功。证据与延迟口径见 [verification](verification.md)。只收到 HTTP 202、源码有补丁或客户端 turn=interrupted 不通过；保留0.163失败记录。官方 TypeScript SDK 结果仅作为补充。
- [x] 1.3 对 `0.163.0-alpha.11.2` 实测失败时，记录 blocked/failed 并保持当时的 261 基线与任务 2–4 未完成；对比当时冻结基线，确认旧尝试未改动 `gradle.properties`、`build.gradle` 或 `src/main/resources/META-INF/plugin.xml`，保留既有 HTTP/UI descriptor diff。此项记录的是旧客户端失败时完成的保护措施；后续候选准入由 1.2 三轮通过，现可进入任务 2–4。
- [x] 1.4 在仓库外下载并核验官方 `rust-v0.165.0-alpha.1` Apple Silicon app-server 资产，读回真实版本/路径/SHA并对应[官方元数据摘要](../../../docs/mcp-cancellation.md)；通过临时配置复验，不安装替换日常 Codex、不改全局配置、不以滚动 latest 代替。包 SHA-256 `f99678f8b60657ee1e466ebcd4e2d6202c63bc9d12b9872ff4b8a8ac4b56f4af` 与官方元数据一致；可执行文件版本为 `codex-app-server 0.165.0-alpha.1`，SHA-256 `a2fea747e927713c65a2de22a1e7c1be84009e24d53f85948649ec41bb949c18`。隔离运行证据见 [verification](verification.md)。

## 2. 精确平台目标与最低 build

- [x] 2.1 任务 1.2 通过后，将 `platformType=IU`、平台解析及 `pluginVerifierIdeVersions=263.7031.23-EAP-SNAPSHOT`、`pluginSinceBuild=263.7031.23` 固定。`verifyPluginProjectConfiguration`、平台元数据和 Verifier 报告读回实际目标 `IU-263.7031.23`；中间 ZIP descriptor 一致。裸版本 POM 不存在，固定 snapshot 只作为可解析的工件输入。
- [x] 2.2 将构建期 IntelliJ Platform Gradle Plugin 升级为正式版 `2.17.0`（release `61cf376`，2026-06-26）；它更新 build 262 起的 Java 映射，不增加 production/runtime dependency。用户于 2026-10-24 批准 Java 25 源码/API/classfile 基线。`javaVersion=25` 驱动 JBR25 toolchain、source/target 与 `options.release=25`；`verifyPluginProjectConfiguration` 无 source/target 警告，实际 javac 参数为 `--release 25`，编译 classfile 为 major 69。`properties` 命令只读回允许公开的八项配置：Java 25、IU、固定 snapshot 输入、since-build 263.7031.23、until-build 为空、Gradle 9.5.0 与插件 1.4.0。证据见 `baseline-263-ipgp217-config.log`、`baseline-263-javac-flags.log`、`baseline-263-properties.txt`；最终包内 329 个生产 class 均为 major 69。
- [x] 2.3 已更新 `docs/intellij-compatibility.md`、`CHANGELOG.md`、`README.md`、`parts/description.html`、`docs/user-guide/index.md` 与 `AGENTS.md`：区分已发布 261 基线和未发布 263/Java 25 候选，说明更早 build 不受新版本支持、未来 build 尚未验证，并指向此 change 的验证记录。保留 HTTP/UI 未验状态；未勾选其他 change 的任务。

## 3. 完整自动验证与产物读回

- [x] 3.1 运行 `./gradlew test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --console=plain`；2026-10-25 full-10 历史记录为真实执行 67 suites / 352 tests / 0 failures / 0 errors / 0 skipped，18 个 Gradle tasks executed，`BUILD SUCCESSFUL`，用时 1 分 55 秒。`IntegrationCredentialServiceTest` 使用 test-only `testBundledModule("intellij.platform.credentialStore.impl")` 保留真实 IU 263 `InMemoryCredentialStore` 行为；测试源码编译仍有 15 条 test-only deprecated/removal warnings，不能写成全项目零警告。日志及冻结结果见 `/tmp/doc-view-store-impl-20261024/baseline-263-full-10.log` 和 `/tmp/doc-view-store-impl-20261024/263-full-10-results/`。full-9 历史执行 67 suites / 350 tests 全绿；full-7 为 344 tests 全绿；full-5/targeted-6 历史失败仍保留，targeted-7 另有 51 tests 全绿。
- [x] 3.2 固定输入 `263.7031.23-EAP-SNAPSHOT` 的 Plugin Verifier 报告目标为 IU `263.7031.23`，verdict 为 Compatible；production 报告无 compatibility、internal、override-only 或 deprecated API 问题。另报告 16 处 Experimental API 使用，作为已记录的平台风险保留。该 Verifier 结论不等于 test source 编译没有 deprecated/removal warnings。证据见 `/tmp/doc-view-store-impl-20261024/baseline-263-full-10.log` 与 `/tmp/doc-view-store-impl-20261024/263-full-10-results/pluginVerifier/IU-263.7031.23/report.html`。
- [x] 3.3 检查 full-10 ZIP 并读回嵌套插件 JAR、descriptor 与构建元数据：ZIP SHA-256 `b238f5412d134ff4311ba5422fb78d50562bfecf80826964858984d5d69bcc0b`、722,118 bytes；JAR SHA-256 `b3615cd1f5a44de8dd472c3f2c8715ecd4126fe6892374fc307a5a1d7ca6f267`。插件 ID/version 为 `com.liuzhihang.doc-view`/1.4.0，`since-build=263.7031.23`，build metadata 一致，329 个生产 class 均为 major 69；ZIP 仅含一个插件 JAR，无 test-only 依赖或额外条目。full-10 相对 full-7 的冻结源码差异仅含 `ProjectDocumentIndexService.java`；222 个源码文件及包身份见 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/source-manifest.json` 和 `/tmp/doc-view-mcp-candidate-20261024/final/263-baseline-full-10/identity.json`，完整 package audit 见 `/tmp/doc-view-store-impl-20261024/263-full-10-results/package-audit.json`。

## 4. IU 263.7031.23 隔离原生功能回归

- [x] 4.1 把任务 3 生成的最终 ZIP 安装到独立 IU `263.7031.23` profile；核对 Help/About build、插件加载日志和隔离路径，使用虚构或可公开样例项目，不操作普通 profile、不真实上传、不启用许可试用。full-10 原生包/JAR 身份已核对，Help/About 与插件加载通过；隔离运行 PID `44922`，普通 IDEA PID `93930`。之后一次读取意外启动的默认 263 实例 PID `46026` 不计为隔离功能证据；最终进程与监听器读回见[验证记录](verification.md)。
- [ ] 4.2 在该最终包上回归 Spring、Feign、Dubbo 发现及 Markdown 生成；验证 tool window 打开/刷新/选择、预览、参数编辑、复制和导出代表路径。记录实际观察、包 hash 和日志；自动测试或旧包观察不能替代此项。full-10 的 Spring/Feign/Dubbo 与 Welcome 观察保留为历史；full-17 已补中英 Keymap、代表性 Copy/Export、Schema 写回/Undo、目录 Enter 和有限资源回归。独立菜单可见结果、完整参数/取消矩阵、跨应用视觉遮挡及 CEF native peer/listener/性能仍未完成；不能把代表场景归为全矩阵通过。
- [x] 4.3 回归 GET/表单等代表性 HTTP 草稿生成及打开流程；full-10 上从正确的 Doc View tree 方法焦点用 Cmd+Shift+A 执行 Generate HTTP，GET 与 form 各新建 `_2.http` 并在 HTTP Client tab 打开。磁盘审计确认新文件内容/时间及六个旧草稿 hash/mtime 未变。本轮未执行 HTTP Client Run（unrun），原因未记录，因此不能计为请求发送通过。HTTP change 中的双项目、快速重复点击/未保存编辑、完整取消/关闭/性能和 UI 矩阵继续保持原状态，证据见 `full10/native/http-full10-generation-disk-audit.json`。
- [x] 4.4 复核 [HTTP/UI 未验任务](../http-client-generation-and-ui/tasks.md) 与其 verification 中的所有未完成范围没有被本次基线 smoke 误标为通过；已确认双项目并行隔离、Clear Cache 原生文件/A-B 保护、L04 取消/源变化/项目关闭、L06 完整性能与资源测量、U01 菜单可达性、U02 语言/主题/布局/键盘、U03 原生 Clear Cache 矩阵及 U04 预览/复制/导出等仍有未完成项，未借本次基线 smoke 勾选或宣称通过。

## 5. 验收结论与范围检查

- [x] 5.1 汇总实际 Codex 取消门槛、配置、自动测试、Verifier、产物读回及原生回归证据，分别标记 passed、partial、blocked、skipped 或 unrun；任何关键依赖未通过时不得写整体通过。
- [x] 5.2 `openspec validate intellij-263-baseline-upgrade --strict` 及其他三个本轮 changes 的 strict validation 均通过；`git diff --check`、文档尾随空白与链接检查通过。最终范围审计确认 HEAD 与冻结基线一致，98 个本轮 scope delta 全部归入已授权的 baseline263、HTTP/UI、MCP 文档和 store 四类，无未分类项；222/222 冻结源码/构建文件 hash 一致，`.agents`/`.codex` 既有改动保留，未 stage，未执行发布、push、tag、sync 或 archive。完整清单见 `/tmp/doc-view-store-impl-20261024/scope-audit-final.json`，四个 change 的 strict 结果见 `/tmp/doc-view-store-impl-20261024/final-openspec-validation.json`。

## 历史结果（2026-10-24）

实际 Codex 客户端准入 1.2 已由官方 `0.165.0-alpha.1` app-server 候选三轮通过，候选资产核验 1.4 完成；`0.163.0-alpha.11.2` 的历史失败仍保留。平台精确目标、Java 25/IPGP 2.17.0 配置、full-10 的 352 项全量测试、Verifier 与新 ZIP 已分别验证。IU 263 原生功能回归已完成部分代表场景，但验收仍不完整；Copy/Export、菜单、完整参数编辑、取消/partial/retry 与资源基线等边界尚未通过验证，详见[验证记录](verification.md)。当时共 15/16 项完成，4.2 未勾选；新增 R1/R2 后当前为 16/18，最新边界见首段。

## 6. 卸载引用补验（2026-11-08）

- [x] 6.1 通过公开 beforePluginUnload 在自身卸载时清理平台图标缓存，补齐注册 RED / 3 项 GREEN 自动回归；新包完整构建、最终串行 77 suites / 398 tests 及两个精确 build Verifier 通过；无诊断 overlay 的正式包未打开 UI 卸载后类加载器释放。平台共享缓存的影响边界、晚返回测试夹具修正及中途失败均见[卸载 API 续验](verification-2026-11-08-unload-api.md)。
- [x] 6.2 独立复验使用目录后的 JNI 引用和剩余弹窗场景：公开 API 清理树模型/parent/鼠标监听器后，最新包同进程两轮原生卸载均实例归零、目录面板类消失、无保留告警；参数与预览 Pin 开/关四组 Finder 合成遮挡通过。402 tests、两精确 build Verifier 通过，边界见[中文/键盘/JNI 续验](../http-client-generation-and-ui/verification-2026-11-08-keyboard-resources.md)。这只完成已执行的代表场景，R2、4.2 与完整发布矩阵仍未完成。

- [x] 6.3 按 XML PSI 候选原生失败 heap 复现并清理树 UI/展开监听器保留的旧路径；先 RED 后公开 API 最小修复及 GREEN，再执行全量构建、两个精确 build Verifier 和目录/XML/文档库路径后的原生热卸载。不得仅凭参数/面板实例归零勾选。

6.3 的 RED/GREEN、新包 414 tests/两个精确 build Verifier，以及 PID19963 XML/文档库使用后卸载通过，见[续验记录](../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md#树-ui-修复包原生热卸载通过)。其余 R2/4.2 门槛仍开放。

- [x] 6.4 针对已复现的原生项目 UI/EDT 关闭路径，使用公开 `ProjectCloseListener.projectClosing` 移除已关闭项目的 Doc View tool window entry；RED/GREEN 验证未展开窗口关闭、`projectClosingBeforeSave`/其他项目不误删、关闭后 listener connection 释放。428 候选在正式 IU 263.7307.200 完成项目关闭/保留另一项目、重开 freshness、资源循环及热卸载验收，详见[当前验证记录](verification.md)。可取消的应用退出发生在 `projectClosing` 前，不据此声称项目关闭 veto；也不承诺任意后台同步 close API 的线程行为。

## 7. 重启生效的插件生命周期（2026-11-09）

- [x] 7.1 用户批准以公开 `require-restart=true` 作为安装、更新和卸载的生命周期策略；429 候选及无 JDWP 对照的热卸载失败保留为历史诊断，不宣称修复了平台引用根。
- [x] 7.2 在 `plugin.xml` 根声明 `require-restart="true"`，并对真实 source descriptor 增加契约测试：RED 为 3 tests / 1 expected failure，GREEN 为 3 tests / 0 failures，JBR 25 定向执行。该项只证明 source descriptor contract，不证明原生安装/更新提示或最终 ZIP 内容；日志和 JUnit 位于 ignored `build/native-validation/2026-11-09/restart-required-option/descriptor-{red,green}/`。
- [x] 7.3 1.6.0 最终 ZIP `0f58a436…c4cc4` 读回 `META-INF/plugin.xml` 根属性精确为 `require-restart="true"`；同包 80 suites / 430 tests、构建及两个精确 build Verifier 通过。证据：`build/native-validation/2026-11-08/remaining-native/release-1.6.0-candidate/package-audit.json` 与 `verifier-summary.json`；不据此标原生重启流程通过。
- [ ] 7.4 在隔离 IDEA 核对安装、更新、禁用/卸载的重启提示及重启后的插件状态。当前尚未完成原生验证；依用户明确决定，若功能验收完成后此项仍无法执行，可在发布说明列为未验证并发布后收集反馈，不得标为通过或谎称热卸载已修复。
