# 1.7.0 GitHub 预发布与验收记录

状态：**GitHub 1.7.0 作为预发布保留，Marketplace 未提交。** `v1.7.0` 指向 `1612cdc3ac62323c04fca0301843a0a0be5e8f0c`，GitHub 已将其设为 prerelease 且非 Latest。正式包已完成549项测试、构建/配置和两个精确 build 的 Verifier；官方 Skill 两范围首次安装通过，重启事务恢复发现一个已复现的功能缺陷，1.7.1 修复与复验见[补丁发布记录](release-1.7.1.md)。本版本不提交 Marketplace；后续稳定版 1.7.1 已正式发布并提交 Marketplace，审核状态见补丁记录。旧 snapshot SHA 与历史候选记录继续保留。

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

首次项目和全局安装使用官方 GitHub v1.7.0 release 资产；IDEA 均显示 Skill 1.7.0 已安装并由 Codex 发现。随后官方更新检查受 GitHub 匿名 API 限流（403、remaining=0）阻断，显示离线状态，先前 Skill 与 metadata 字节保持未变。为验证重启恢复，在隔离项目和隔离用户 HOME 写入旧目录备份与有效 journal 后重启 IDEA。设置页检测到项目范围的恢复待处理状态，但主按钮被禁用，未能从原生 UI 触发恢复，验收失败。全局范围在此轮重启后的 UI 状态未单独打开检查。该 journal 是测试夹具注入，不是 IDEA 在真实进程崩溃中自然留下；独立子 JVM 故障恢复测试仍通过，不能替代本次原生按钮失败。

根因是只读状态检查在查看 journal 后提前返回，未先确认 Codex build；因此状态缺少已验证客户端身份，UI 的写操作门禁按预期保持关闭。1.7.1 候选改为先读取并验证客户端 build，再将该身份带入恢复状态；未知/不支持客户端仍不得恢复写入。后续共四个恢复路径回归先失败再通过；1.7.1 的最终553项测试、构建、两build Verifier、两范围原生恢复及官方更新/取消后重试已通过，并已正式发布。完整补丁证据见[1.7.1 发布记录](release-1.7.1.md)，此处保留 1.7.0 的失败历史。

## 正式资产与发布顺序

生产安装器只读取官方稳定 Release。v1.7.0 的资产仍可从 GitHub 读取，但该 Release 已标记为 prerelease，不作为正式安装源；以下保留本次实际采用的发布顺序；1.7.1 的执行结果与审核状态见[补丁发布记录](release-1.7.1.md)：

1. 完成可在本地执行的实现、测试、Verifier 和原生门槛；冻结文件范围，审阅实际 diff，明确剩余风险与回滚到 1.6.2 的条件。
2. 在获准的发布提交中设置正式版本、更新 CHANGELOG/欢迎页/中英文介绍，重建插件包并读回 descriptor、说明、依赖与 SHA。当前记录里的候选包不能直接冒充正式版本。
3. 将正式源码按发布策略进入目标分支，让 `v1.7.1` 精确指向该提交；按[Skill 发行说明](skill-release.md)从这个提交生成 ZIP 和 manifest。临时快照仓库的 commit/tag 不能用于正式发布，不能移动或覆盖既有 v1.7.0 tag。
4. 准备 GitHub Release 的插件 ZIP、Skill ZIP、manifest 与摘要；上传和公开发布遵循现有授权边界。正式资产上线后，回读远端 tag/commit、资产字节和摘要。
5. 使用未修改的生产下载器，从官方稳定来源执行当前项目与隔离全局范围的原生安装/发现、更新、取消和失败恢复验收。1.7.0 注入 journal 后重启的项目恢复入口已失败，必须在补丁包上修复并复验；不得放宽固定来源限制来通过测试。
6. 验收完成后提交 Marketplace，分别记录上传成功、审核状态与实际列出状态；再次核对公开说明、兼容范围和安装包身份。

在第 4 步之前，不能声称官方下载安装已通过；第 4 步的资产上线也不等于 Marketplace 已发布。未完成的新功能验收不会沿用 1.6.x 的单次发布例外自动视为通过。

## 正式包身份

正式1.7.0的549项测试、构建和配置检查已通过，两精确build的Verifier均Compatible（各16项Experimental提示，无Internal/Deprecated/兼容性问题）。ZIP SHA-256为 `03ab0ce539ea2d60c0c63bf563419f510e427d07858c219ed103eb95ebd69306`，JAR为 `19d4a324484bb03492789d8d6990a8ce89950aab524191bcde003995e13bcf69`；408个class与上述原生候选字节一致，仅版本与发布说明/契约标题资源变化。正式包首次检查中测试硬编码SNAPSHOT的失败已修正并复验；详情见[正式包记录](../openspec/changes/doc-view-mcp-tools/verification-2026-11-13-release-gates.md#正式版本包复验待发布)。这不代替官方Skill资产与IDEA按钮验收。

## GitHub 发布结果

源码经 [PR #128](https://github.com/liuzhihang/doc-view/pull/128) 进入develop，再经 [PR #129](https://github.com/liuzhihang/doc-view/pull/129) 进入master；两次合并后发布tree与已验收源码tree一致，原工作区未切分支或修改文件。

[GitHub v1.7.0](https://github.com/liuzhihang/doc-view/releases/tag/v1.7.0) 已在2026-11-13 10:47:16 UTC公开，release ID `444110461`。插件ZIP、Skill ZIP、manifest和SHA256SUMS均已重新下载并与本地字节核对：

| 资产 | SHA-256 |
| --- | --- |
| `Doc.View-1.7.0.zip` | `03ab0ce539ea2d60c0c63bf563419f510e427d07858c219ed103eb95ebd69306` |
| `doc-view-skill-1.7.0.zip` | `f0f50211f7492167f99f391d0e740c4880bf69146d2989d6342b6267333955f9` |
| `doc-view-skill-1.7.0.manifest.json` | `d1d1865697720f25c0069adb080fa6b889ed986dedbe0d4133ae0a63a070d17b` |

manifest绑定该正式tag commit与指纹 `sha256:400aacb3bc373117e01288d995be7969d3e962c2ace58880a4a1775752ec2a17`。初次按tag读取draft的REST接口返回404；公开后元数据及二进制回读通过，未据draft的404声明上传失败。

正式包在263.7307.200隔离实例启动，界面显示1.7.0。额度恢复后，项目和全局两范围各自通过官方安装及 Codex 发现，文件 SHA 与官方 ZIP 中的 SKILL.md 一致。后续更新检查再遇 GitHub 匿名 API 限流（403、remaining=0，reset=2026-11-13 11:26:53 UTC），旧 Skill 与 metadata 未变化。注入 journal 并重启后，项目恢复状态虽被识别，更新按钮仍禁用。三项事实分别记录为首次安装通过、更新未验证（限流）、原生重启恢复失败。完整证据在隔离工作区 `build/mcp-release-gates-2026-11-13/formal-native/`；修复门禁见[1.7.1记录](release-1.7.1.md)。
