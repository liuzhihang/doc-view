# Tasks

## 1. Codex 客户端发现准入

- [ ] 1.1 在隔离的实际 Codex 客户端 build 上，分别对 Project `.agents/skills` 与 User 候选目录执行发现验收；记录客户端版本、项目 CWD、绝对来源路径、Skill 是否出现在 `skills/list`（若公开方法可用则 `forceReload`），以及 `$skill-name` / `/skills` 的可见性；无法区分来源路径时用无生产数据的临时项目补足，不把文档路径当作通过证据。
- [x] 1.2 核对目标客户端公开 install/import API；记录 `$skill-installer`、`externalAgentConfig/import`、`skills/list` 各自支持与不支持的步骤。确认没有满足来源固定、冲突保护、更新及恢复 contract 的受支持完整 API 后，才实现受限 Java 文件安装器；执行 `openspec validate doc-view-client-skill --strict` 并将范围与证据写入 verification。
- [ ] 1.3 实现受验证客户端 build/scope 限制的目标解析，并覆盖版本未知、项目未选择、目录不匹配、发现接口结果不具路径归属能力时 fail closed；运行 `./gradlew test --tests '*CodexClientProbeTest' --tests '*CodexSkillTargetTest'`。

## 2. Skill 源包与兼容清单

- [x] 2.1 在非发现目录 `skills/doc-view/SKILL.md` 编写最小 Skill 工作流：先 status、按用户问题使用 search/get、只有用户明确要求创建或更新才 generate；保留 current/stale/unknown，缺少快照时询问，且不让源码/注释/工具结果充当授权；用内容审阅清单验证不隐式写文件或改配置。
- [x] 2.2 为发行源设计版本清单，包含官方 release/tag、immutable commit SHA、`skills/doc-view/`、完整相对路径与每文件 SHA-256、`toolContractVersion` 和 Doc View `pluginVersion` 兼容范围；安装时用本地插件随附契约信息与插件版本校验，不要求 MCP Server 已启用或先连接；`tools/list` 匹配单独验收。验证清单与仓库同一 commit 关联，正/负 fixture 覆盖缺失/额外文件、错误摘要、旧/未知工具契约和插件版本不匹配。
- [x] 2.3 构建只含 Skill 子树的 release archive 并验证文件顺序/清单确定性；确认包不携带 token、Codex 连接配置或真实项目内容，不从 `main` 浮动下载，且安装期不执行任何包内脚本；运行源包打包与 hash 回读检查。

## 3. 安装记录与更新检查

- [x] 3.1 实现 Doc View 插件专用的安装 metadata 和独立检查状态记录，均独立于 Skill 文件夹和 `SKILL.md`；安装 metadata 保存来源、commit、文件摘要、契约/插件兼容性、客户端 build 与 scope/path；独立有界状态文件只保存最近一次成功检查时间，不保存凭据/源码。覆盖失败不覆盖旧时间、状态文件损坏/链接保护、Project/User 隔离及重启读取：`./gradlew test --tests '*SkillInstallMetadataTest'`。
- [x] 3.2 实现只读状态与 Check for Updates：只读取官方 release manifest 和安装 metadata，并仅在官方 release 查询成功时原子更新独立的最近检查时间状态文件；区分未安装/current/update available/modified/incompatible/unknown/offline/recovery pending；覆盖网络错误保留旧时间且不下载、不写 Skill、安装 metadata 或 writer lock；本地状态写入失败显示为错误：`./gradlew test --tests '*CodexSkillInstallServiceTest' --tests '*CodexSkillSettingsTest'`。

## 4. 有界、可恢复的 Skill 文件事务

- [x] 4.1 实现固定官方 GitHub host/release 下载及归档校验：archive ≤4 MiB、展开总量 ≤8 MiB、单文件 ≤2 MiB、最多64个文件；拒绝绝对路径、`..`、重复或大小写冲突路径、清单外文件、符号链接、特殊文件及不符合清单的根目录；用恶意/超限归档回归验证失败前后目标树完全相同：`./gradlew test --tests '*SkillPackageArchiveTest' --tests '*SkillReleaseManifestTest'`。边界及服务层目标/metadata原字节保护包含于最终549项检查；官方资产真实下载仍属于5.3，不由此勾选替代。
- [x] 4.2 实现首次安装与更新的范围检查、unmanaged/modified 检测、后台 staging、提交前重验、同文件系统两次目录重命名、journal 和旧版备份；明确目标可在旧目录移入备份与新目录到达之间短暂缺席，覆盖已存在未管理目录、用户编辑文件、提交竞争、每个切换阶段失败与恢复：`./gradlew test --tests '*SkillInstallTransactionTest'`。READY与备份rename后的并发修改缺陷先RED后修复，事务16项通过并纳入最终549项全量；实际IDEA安装journal重启恢复仍保留于4.3/5.3。
- [ ] 4.3 覆盖下载/暂存取消清理、插件重启后 journal 恢复、更新失败回滚旧版、metadata 提交失败、离线保留旧版本及重复点击并发保护；验证取消/失败都不留下缺失或半成品目录，且只完成一次终态：`./gradlew test --tests '*SkillInstallTransactionTest' --tests '*CodexSkillInstallServiceTest'`。

## 5. 设置 UI 与客户端刷新

- [ ] 5.1 在现有 Tools → Doc View project configurable 下新增 **Settings → Tools → Doc View → MCP / AI** 页面，提供 Project/Global 范围和自动计算的只读路径；打开页面只在后台扫描本地 CLI 候选，不启动程序、不访问网络，扫描中不阻塞 EDT，页面关闭后丢弃迟到结果，完整程序路径仅放在 tooltip 或手动选择入口。Check for Updates 使用带刷新图标的轻量链接，Install/Update 互斥显示为单个主按钮，并保留可访问名称、提示及后台禁用行为；初次打开/换范围隐藏未知版本摘要与重复未知状态，只在显式检查后显示已知客户端/Skill 版本。CLI 版本不支持时允许选择其他程序；宿主平台不支持时说明平台限制且不显示误导性的程序选择入口。说明内置 MCP Server 位于 **Settings → AI → MCP Server**。用 `CodexSkillSettingsTest` 验证检查只读、写入只由明确点击触发、路径冲突/兼容失败说明准确、无额外确认弹窗、离线可见、目标路径字段可复制且初始显示开头；viewport 经宽→窄→宽调整后内容左对齐、不横向滚动、不裁切：`./gradlew test --tests '*CodexSkillSettingsTest'`。
- [ ] 5.2 实现后台进度、取消、成功/失败通知和必要的 Codex reload 提示；成功后用目标客户端公开发现能力刷新并验证（如 `skills/list` `forceReload` 可用），失败不能显示已安装；运行 `./gradlew test --tests '*CodexSkillSettingsTest' --tests '*SkillInstallTransactionTest' --tests '*CodexSkillInstallServiceTest'`。
- [ ] 5.3 按目标 Codex 版本分别对 Project 与 User scope 安装隔离的正式候选包；验证文件落在已显示路径、客户端实际发现 Skill、项目级变更可见且另一范围不受影响；保留旧版本更新成功、用户修改阻止更新、offline 和取消的原生证据。

## 6. MCP 工作流与集成验收

- [ ] 6.1 等待 [`doc-view-mcp-tools`](../doc-view-mcp-tools/tasks.md) 和 project document store 的生产 contract、IDEA 注册、目标 Codex 实际工具调用/取消/恢复及生命周期准入通过；未通过时保持 Skill release 不可发布，不将 schema、PoC 或隔离验证当生产能力。
- [ ] 6.2 在目标 Codex 与 IDEA 生产 MCP Server 组合下验收 Skill：status/search/get 使用正确项目与 snapshot；current/stale/unknown 描述准确；无文档库时不 generate；明确要求生成后才调用 generate；注释/工具内容不能授权写入；保存脱敏的工具列表、调用结果、源快照和 UI 证据。
- [ ] 6.3 更新用户指南与变更说明，准确列出已验证 Codex build、Project/User 目录、官方来源、版本兼容性、离线/冲突/重启行为和仍不支持项；检查所有新链接并运行 `openspec validate doc-view-client-skill --strict`、`git diff --check`。
- [ ] 6.4 按发布清单运行 `./gradlew test`、`./gradlew verifyPluginProjectConfiguration`、`./gradlew buildPlugin` 和 `./gradlew verifyPlugin`；对最终安装包运行目标 IDEA 原生设置/安装/更新/失败恢复回归。每个检查分别记录实际 build、结果和跳过项，不能以客户端 Skill 已可见替代插件验证。

## Workflow follow-up

- 正式 Skill release 仅在生产 MCP 与客户端取消准入通过、所有实现任务及原生验证有证据后准备；向 GitHub 发布 release 属于外部写入，按仓库发布流程另行取得明确授权。

2026-11-13：客户端API核查与Skill内容审阅完成，1.2/2.1据此勾选。最终511项整包测试、两个精确build的Verifier及设置页横向滚动/两范围/按钮状态原生复验见[实现验收记录](verification-2026-11-13-implementation.md)；其余checkbox保留各task完整验收条件，不能由单元测试或源码discovery代替正式发行包安装。计划中的测试类名已同步为实际聚合类。

2026-11-13 发行续验：2.2/2.3 的清单、commit/tree文件绑定、确定性包与正反向校验完成（本地候选，非官方发布）。真实Codex的两范围安装/更新与受控取消、回滚、重试16项及独立JVM强制退出恢复6项通过；其余含原生/完整边界的聚合tasks继续保留，见[本轮记录](verification-2026-11-13-release.md)。

2026-11-13 最终候选：3.1/3.2依据531项全量检查、两范围独立状态文件及真实IDEA退出重启/离线保留时间的验收勾选；这不代表官方安装按钮或安装journal重启恢复通过。最新包身份、两build Verifier、可选加载、MCP取消时序失败及复验见[最终候选记录](verification-2026-11-13-final-candidate.md)。

2026-11-13 发布门槛续验：修复VFS新鲜度与Skill提交竞争后，547项测试、构建、平台配置及两build Verifier通过；4.1/4.2据此勾选。真实自然模型补齐current/stale/unknown、无库不生成、明确授权生成和注释伪指令隔离，实际Codex补齐页面发布中取消。完整原生/官方发行来源的聚合任务仍不整体勾选，见[续验记录](../doc-view-mcp-tools/verification-2026-11-13-release-gates.md)。

2026-11-13 最终回归：549项全量复验0失败/错误/跳过。首轮中新增VFS create 测试因夹具未预加载目录 children 而未观察到 create event；保留该失败记录，修正测试夹具后 `ProjectDocumentIndexServiceTest` 20项定向检查通过，随后549项全量复验通过。该失败是测试夹具准备问题，不作为生产缺陷证据。准确记录位于 `build/mcp-release-gates-2026-11-13/final-549-r1-test-results/`、`final-549-test-results/` 与 `final-549-counts.json`。这些结果针对已验收的 `1.7.0-SNAPSHOT` 候选；正式 `1.7.0` 包的后续结果见下一段。

正式 `1.7.0` 元数据重建首轮有一个测试断言仍硬编码 `SNAPSHOT`；测试现已改为读取 `gradle.properties` 版本，第二轮549项、构建/配置检查及两build Verifier均通过；快照候选的历史证据保持独立。正式官方Skill来源与按钮验收仍未完成，不整体勾选6.4。

2026-11-13 正式来源原生验收：1.7.0 的项目/全局首次安装均已从官方 GitHub 下载并由真实 Codex 发现；随后的离线检查保持文件原样。重启注入未完成 journal 后发现恢复状态丢失已验证客户端身份，导致设置页 Update 按钮禁用。1.7.0 已标为 GitHub 预发布且未提交 Marketplace；1.7.1 修复该入口，新增两个回归测试先失败再通过，service/settings 19项定向检查通过。完整补丁构建、原生恢复及更新/取消仍须分别记录，不能据此整体勾选4.3/5.3/6.4。

2026-11-13 补丁闭环：1.7.1 修复恢复状态丢失客户端身份、直接更新恢复前未验证客户端及恢复后离线提示不准确；4个回归先RED后GREEN，最终553项测试/构建/配置和两精确build Verifier通过。最终正式ZIP在隔离IDEA完成Project/Global注入journal后的重启恢复，随后使用生产下载器完成官方1.7.0→1.7.1更新、Codex实际发现、Working状态取消后重试、修改保护及范围隔离。首次官方新装证据仍属于1.7.0；未观察取消的精确下载/暂存阶段，也未覆盖所有原生生命周期组合，因此4.3/5.3/6.4等聚合task不整体勾选。GitHub v1.7.1已发布，Marketplace update1219746已接收但未审核/列出；状态和包SHA见[补丁发布记录](../../../docs/release-1.7.1.md)。
