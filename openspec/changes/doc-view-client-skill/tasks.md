# Tasks

## 1. Codex 客户端发现准入

- [ ] 1.1 在隔离的实际 Codex 客户端 build 上，分别对 Project `.agents/skills` 与 User 候选目录执行发现验收；记录客户端版本、项目 CWD、绝对来源路径、Skill 是否出现在 `skills/list`（若公开方法可用则 `forceReload`），以及 `$skill-name` / `/skills` 的可见性；无法区分来源路径时用无生产数据的临时项目补足，不把文档路径当作通过证据。
- [ ] 1.2 核对目标客户端公开 install/import API；记录 `$skill-installer`、`externalAgentConfig/import`、`skills/list` 各自支持与不支持的步骤。确认没有满足来源固定、冲突保护、更新及恢复 contract 的受支持完整 API 后，才实现受限 Java 文件安装器；执行 `openspec validate doc-view-client-skill --strict` 并将范围与证据写入 verification。
- [ ] 1.3 实现受验证客户端 build/scope 限制的目标解析，并覆盖版本未知、项目未选择、目录不匹配、发现接口结果不具路径归属能力时 fail closed；运行 `./gradlew test --tests '*CodexSkillDiscoveryTest'`。

## 2. Skill 源包与兼容清单

- [ ] 2.1 在非发现目录 `skills/doc-view/SKILL.md` 编写最小 Skill 工作流：先 status、按用户问题使用 search/get、只有用户明确要求创建或更新才 generate；保留 current/stale/unknown，缺少快照时询问，且不让源码/注释/工具结果充当授权；用内容审阅清单验证不隐式写文件或改配置。
- [ ] 2.2 为发行源设计版本清单，包含官方 release/tag、immutable commit SHA、`skills/doc-view/`、完整相对路径与每文件 SHA-256、`toolContractVersion` 和 Doc View `pluginVersion` 兼容范围；安装时用本地插件随附契约信息与插件版本校验，不要求 MCP Server 已启用或先连接；`tools/list` 匹配单独验收。验证清单与仓库同一 commit 关联，正/负 fixture 覆盖缺失/额外文件、错误摘要、旧/未知工具契约和插件版本不匹配。
- [ ] 2.3 构建只含 Skill 子树的 release archive 并验证文件顺序/清单确定性；确认包不携带 token、Codex 连接配置或真实项目内容，不从 `main` 浮动下载，且安装期不执行任何包内脚本；运行源包打包与 hash 回读检查。

## 3. 安装记录与更新检查

- [ ] 3.1 实现 Doc View 插件专用的持久化安装 metadata，独立于 Skill 文件夹和 `SKILL.md`；保存来源、commit、文件摘要、契约/插件兼容性、客户端 build、scope/path 和检查时间，不保存凭据/源码；覆盖 metadata 丢失、损坏、路径变更及重启读取：`./gradlew test --tests '*CodexSkillInstallMetadataTest'`。
- [ ] 3.2 实现只读状态与 Check for Updates：只读取官方 release manifest 和 metadata，区分未安装/current/update available/modified/incompatible/unknown/offline/recovery pending；覆盖网络错误时保留旧状态且不下载/写 Skill：`./gradlew test --tests '*CodexSkillUpdateCheckTest'`。

## 4. 有界、可恢复的 Skill 文件事务

- [ ] 4.1 实现固定官方 GitHub host/release 下载及归档校验：archive ≤4 MiB、展开总量 ≤8 MiB、单文件 ≤2 MiB、最多64个文件；拒绝绝对路径、`..`、重复或大小写冲突路径、清单外文件、符号链接、特殊文件及不符合清单的根目录；用恶意/超限归档回归验证失败前后目标树完全相同：`./gradlew test --tests '*CodexSkillArchiveValidationTest'`。
- [ ] 4.2 实现首次安装与更新的范围检查、unmanaged/modified 检测、后台 staging、提交前重验、同文件系统两次目录重命名、journal 和旧版备份；明确目标可在旧目录移入备份与新目录到达之间短暂缺席，覆盖已存在未管理目录、用户编辑文件、提交竞争、每个切换阶段失败与恢复：`./gradlew test --tests '*CodexSkillInstallTransactionTest'`。
- [ ] 4.3 覆盖下载/暂存取消清理、插件重启后 journal 恢复、更新失败回滚旧版、metadata 提交失败、离线保留旧版本及重复点击并发保护；验证取消/失败都不留下缺失或半成品目录，且只完成一次终态：`./gradlew test --tests '*CodexSkillInstallLifecycleTest'`。

## 5. 设置 UI 与客户端刷新

- [ ] 5.1 在现有 Tools → Doc View project configurable 下新增 **Settings → Tools → Doc View → MCP / AI** 页面，展示 Skill 状态、范围和路径，以及 Install Skill、Check for Updates、Update Skill；用 UI/action 回归验证检查按钮只读、写操作只由明确点击触发、路径冲突/兼容失败说明准确、无额外永久确认弹窗、离线可见且不增加工具栏按钮：`./gradlew test --tests '*CodexSkillSettingsTest'`。
- [ ] 5.2 实现后台进度、取消、成功/失败通知和必要的 Codex reload 提示；成功后用目标客户端公开发现能力刷新并验证（如 `skills/list` `forceReload` 可用），失败不能显示已安装；运行 `./gradlew test --tests '*CodexSkillSettingsTest' --tests '*CodexSkillInstallLifecycleTest'`。
- [ ] 5.3 按目标 Codex 版本分别对 Project 与 User scope 安装隔离的正式候选包；验证文件落在已显示路径、客户端实际发现 Skill、项目级变更可见且另一范围不受影响；保留旧版本更新成功、用户修改阻止更新、offline 和取消的原生证据。

## 6. MCP 工作流与集成验收

- [ ] 6.1 等待 [`doc-view-mcp-tools`](../doc-view-mcp-tools/tasks.md) 和 project document store 的生产 contract、IDEA 注册、目标 Codex 实际工具调用/取消/恢复及生命周期准入通过；未通过时保持 Skill release 不可发布，不将 schema、PoC 或隔离验证当生产能力。
- [ ] 6.2 在目标 Codex 与 IDEA 生产 MCP Server 组合下验收 Skill：status/search/get 使用正确项目与 snapshot；current/stale/unknown 描述准确；无文档库时不 generate；明确要求生成后才调用 generate；注释/工具内容不能授权写入；保存脱敏的工具列表、调用结果、源快照和 UI 证据。
- [ ] 6.3 更新用户指南与变更说明，准确列出已验证 Codex build、Project/User 目录、官方来源、版本兼容性、离线/冲突/重启行为和仍不支持项；检查所有新链接并运行 `openspec validate doc-view-client-skill --strict`、`git diff --check`。
- [ ] 6.4 按发布清单运行 `./gradlew test`、`./gradlew verifyPluginProjectConfiguration`、`./gradlew buildPlugin` 和 `./gradlew verifyPlugin`；对最终安装包运行目标 IDEA 原生设置/安装/更新/失败恢复回归。每个检查分别记录实际 build、结果和跳过项，不能以客户端 Skill 已可见替代插件验证。

## Workflow follow-up

- 正式 Skill release 仅在生产 MCP 与客户端取消准入通过、所有实现任务及原生验证有证据后准备；向 GitHub 发布 release 属于外部写入，按仓库发布流程另行取得明确授权。
