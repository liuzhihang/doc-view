# 1.7.0 发布准备

状态：1.7.0 待发布。当前源码版本元数据为 `1.7.0`，正式版本包已完成构建、549项测试和两build Verifier，固定官方来源安装仍待验收；GitHub 与 Marketplace 尚无 1.7.0。本页中的旧 SHA 与候选验收针对先前的 `1.7.0-SNAPSHOT` 包，不自动代表新的正式版本包已通过。

## 已发布版本

2026-11-13 重新核对官方接口：Marketplace 的 1.6.2（update `1216086`）为 `approve=true`、`listed=true`，最低 build `263.7031.23`；[GitHub v1.6.2](https://github.com/liuzhihang/doc-view/releases/tag/v1.6.2) 已公开并包含插件 ZIP 与 SHA256SUMS。1.6.0（update `1215457`）也仍然批准并上架。机器记录位于隔离工作区 `build/release-readiness-2026-11-13/{marketplace,github}.json`。

## 本次范围

- 同一 Doc View 包通过 IDEA `com.intellij.mcpServer` 可选扩展提供 status/search/get/generate；继续共享项目 Markdown/JSON 文档库，不另建 server 或插件。
- MCP / AI 设置页提供当前项目和全局 Skill 范围、自动检测 Codex、检查更新及受保护的安装/更新；路径由范围自动确定。
- Skill 发行 ZIP 与 manifest 绑定官方稳定 tag、commit、文件集合与 MCP 契约；失败、取消和用户修改保护遵循现有 change。

Skill 自动安装目前只验证 macOS Apple Silicon 和 `codex-cli 0.167.0-alpha.4.1`，其他环境不开放写入。插件最低 IDEA build 仍为 `263.7031.23`、Java 25；安装、更新及停用需重启，热卸载不作为本轮重新调查的目标。

## 验证记录

最新候选修复了外部源码改动遗漏VFS失效、Skill提交阶段可能覆盖并发修改的两个问题，完成547项测试（0失败/错误/跳过）、构建与平台配置检查；IU263.7031.23和263.7307.200的Verifier均Compatible，各保留16项Experimental API提示，无兼容性、Internal、Deprecated或Override-only问题。两范围16项真实Codex服务场景和6次子JVM中断恢复通过。原生设置的独立检查时间、退出重启/离线保留和MCP组件禁用时的可选加载在前一候选验证；新包补验了VFS current/stale/unknown、A/B查询、发布中取消与项目关闭后的GC观察。

随后修正 VFS create 测试夹具未预加载目录 children 的问题：首轮 549 项全量检查保留了该夹具断言失败；调整夹具后，`ProjectDocumentIndexServiceTest` 的20项定向检查通过，最终549项复验通过（0失败/错误/跳过）。这次失败定位到测试夹具没有触发预期的真实 VFS create event，不是已知生产代码缺陷。原始失败与复验结果均保留在 `build/mcp-release-gates-2026-11-13/final-549-r1-test-results/`、`final-549-test-results/` 和 `final-549-counts.json`。

最低支持版本 IU 263.7031.23 的官方 Apple Silicon DMG 已完成身份校验并在隔离 profile 原生运行：DMG SHA-256 `c173e165d6365387302694d1fd9cd0d3b02f420f4af942fcd9ed80a37e1c00d2`，包内 JBR 为 `25.0.4.1+1-b635.72`，测试插件 JAR SHA-256 `fe48995947f91d782091f420751a2c977bbc8b40c3879f26ebdffa017b8fe2cc`。在同一候选包上，三项只读 MCP 工具完成 A/B 项目、schema、Dumb Mode、只读文件指纹和错误路径原生查询；另完成 MCP 启用与禁用重启路径。禁用时 Doc View 窗口和目录仍可用，127 个 Doc View 类加载、0 个 MCP adapter 类加载。物理移除 MCP 插件未测试。此前 SDK 与 App 混合启动仅作为诊断尝试，不计作完整原生验收。详细数据见[续验记录](../openspec/changes/doc-view-mcp-tools/verification-2026-11-13-release-gates.md)。

真实Codex取消首次立即状态检查失败，查明为协作式清理时序后修正harness，生产实现未改；两次有界复验分别5.703/70.351ms清理，同session恢复查询false，文档不变。完整包身份、命令、失败保留和证据边界见[最新候选验收](../openspec/changes/doc-view-client-skill/verification-2026-11-13-final-candidate.md)。

新包实际Codex在52/9000个页面已发布时取消生成，匹配的协议通知、旧索引保护及同session恢复通过，观察到清理约2.45秒；最终index提交后迟到取消仍只有自动测试证据。真实自然模型按Skill验证了缺少文档时不生成、显式生成、注释伪指令隔离，以及current/stale/unknown查询语义。旧源码下的stale失败及首轮取消harness超时均保留。该原生候选ZIP SHA-256为 `e6c64025ef250a34b272467e6d7887e0fc50a13e65ef9b4eb1652aca785297f4`，详见[续验记录](../openspec/changes/doc-view-mcp-tools/verification-2026-11-13-release-gates.md)。

仍未完成官方 Skill 资产下载及最终 IDEA 安装/更新/恢复按钮闭环、最低 build 的完整原生矩阵、取消/关闭/插件停用的完整生命周期矩阵。MCP 查询的最低 build Dumb Mode 场景已通过，但不代表所有索引状态与生命周期组合均已验证；MCP 禁用重启已通过，不代表物理移除插件也已测试。HTTP/UI change 仍未完成 tasks 4.5、5.5、5.9、6.3、6.4、6.5；其 4.6、5.4 已完成且本轮未改动。B 测试/关闭期间 A 后来变为 stale 的来源仍待单独定位；不能未经取证认定正常或归因为跨项目数据污染。活动 change 保留未完成 tasks，不为发布批量勾选或归档。

正式版本重建目前正在复验：首次 `1.7.0` 门禁运行发现一个测试仍将 `SNAPSHOT` 写死，已改为读取 `gradle.properties` 中版本，第二轮仍在运行。该轮尚未通过，不能用快照候选的549项结果替代正式 `1.7.0` 包门禁。

## 正式资产与发布顺序

生产安装器只读取官方稳定 Release。当前没有官方 Skill 资产，因此发布前的本地包/注入发行源验收与资产上线后的真实下载验收必须分开记录：

1. 完成可在本地执行的实现、测试、Verifier 和原生门槛；冻结文件范围，审阅实际 diff，明确剩余风险与回滚到 1.6.2 的条件。
2. 在获准的发布提交中设置正式版本、更新 CHANGELOG/欢迎页/中英文介绍，重建插件包并读回 descriptor、说明、依赖与 SHA。当前记录里的候选包不能直接冒充正式版本。
3. 将正式源码按发布策略进入目标分支，让 `v1.7.0` 精确指向该提交；按[Skill 发行说明](skill-release.md)从这个提交生成 ZIP 和 manifest。临时快照仓库的 commit/tag 不能用于正式发布。
4. 准备 GitHub Release 的插件 ZIP、Skill ZIP、manifest 与摘要；上传和公开发布遵循现有授权边界。正式资产上线后，回读远端 tag/commit、资产字节和摘要。
5. 使用未修改的生产下载器，从官方来源执行当前项目与隔离全局范围的原生安装/发现、更新、取消和失败恢复验收。未通过则停止 Marketplace 提交并先修复；不放宽固定来源限制来通过测试。
6. 验收完成后提交 Marketplace，分别记录上传成功、审核状态与实际列出状态；再次核对公开说明、兼容范围和安装包身份。

在第 4 步之前，不能声称官方下载安装已通过；第 4 步的资产上线也不等于 Marketplace 已发布。未完成的新功能验收不会沿用 1.6.x 的单次发布例外自动视为通过。

## 正式包身份

正式1.7.0的549项测试、构建和配置检查已通过，两精确build的Verifier均Compatible（各16项Experimental提示，无Internal/Deprecated/兼容性问题）。ZIP SHA-256为 `03ab0ce539ea2d60c0c63bf563419f510e427d07858c219ed103eb95ebd69306`，JAR为 `19d4a324484bb03492789d8d6990a8ce89950aab524191bcde003995e13bcf69`；408个class与上述原生候选字节一致，仅版本与发布说明/契约标题资源变化。正式包首次检查中测试硬编码SNAPSHOT的失败已修正并复验；详情见[正式包记录](../openspec/changes/doc-view-mcp-tools/verification-2026-11-13-release-gates.md#正式版本包复验待发布)。这不代替官方Skill资产与IDEA按钮验收。
