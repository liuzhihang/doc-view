# Codex Skill 发现与安装 API 核查（2026-11-12；2026-11-13 补验）

## 当前结论

隔离 Codex CLI/app-server `0.167.0-alpha.4.1` 已分别发现 Project `.agents/skills` 和规范 User `$HOME/.agents/skills`。除临时 marker 外，2026-11-13 又把工作树中的 `skills/doc-view/SKILL.md` 分别复制到全新隔离 Project 和 HOME 根，公开 `skills/list(forceReload=true)` 返回精确 `name=doc-view`、`scope=repo` / `scope=user` 和对应绝对文件路径。当前精确客户端 build 的两个目标 scope 均已完成 discovery 验收。

发现/迁移 API 均不提供固定 Doc View Release、SHA manifest、修改保护、更新和恢复这一完整生命周期：`skills/list` 只负责发现，`externalAgentConfig/import` 用于迁移被支持的外部 Agent 条目。内置 `$skill-installer` 是独立 Skill 工作流，脚本可从 GitHub 安装到 `$CODEX_HOME/skills`，默认跟随可变 `main`，只检查目标不存在；没有 Doc View 所需的发行清单、更新记录、已修改文件保护和两阶段恢复。因此 Java 安装器仍须执行来源与文件校验、冲突保护和可恢复事务。此次 scope discovery 不等于正式 release archive 校验、安装/更新事务验收或 Skill 对话调用准入；MCP 工具和 Skill 生产运行时仍分别保留这些门槛。

## 实际客户端验收

| 项目 | 结果 |
| --- | --- |
| 客户端 | ChatGPT.app 随附 Codex CLI，`codex-cli 0.167.0-alpha.4.1`；使用同一 packaged CLI 的 `codex app-server --stdio` |
| 隔离 | 独立 `CODEX_HOME`，关闭 analytics；独立临时 Git project；测试 repo/user roots 全在 `build/mcp-skill-2026-11-12/client-discovery/` |
| Project 发现 | PASS。`skills/list` 返回 `doc-view-project-probe`，scope=`repo`，路径与 fixture 的 `SKILL.md` 完全匹配 |
| 磁盘变更刷新 | PASS。启动后新增第二个 marker，使用 `forceReload=true` 再查询，返回 `doc-view-force-reload-probe` 及其绝对路径 |
| User 根发现（marker） | PASS。使用子进程隔离 HOME/CODEX_HOME，`$HOME/.agents/skills/.../SKILL.md` 被列为 scope=`user`；未访问真实个人 Skill 根 |
| Doc View 实际源文件 discovery | PASS。2026-11-13 将工作树 `skills/doc-view/SKILL.md` 复制到隔离 Project 与隔离 HOME 目标；实际返回分别为 `name=doc-view, scope=repo` 与 `name=doc-view, scope=user`，路径精确匹配各自 `SKILL.md` |
| 其他候选 User 根 | 0.167 marker probe 也观察到隔离 `$CODEX_HOME/skills` 返回 scope=`user`；正式 UI 只使用官方文档规定的 `$HOME/.agents/skills`，不把该历史/替代根当成第二个安装目标。`perCwdExtraUserRoots` 仍不在实际 schema 中 |
| Codex 对话中的 Skill 调用 | NOT VERIFIED。本次只调用 initialize 与 `skills/list(forceReload)`，没有调用 `$doc-view`、`/skills` 或任何模型/turn |
| 模型/账户 | 未创建 thread/turn、未调用模型、未读取凭据值、未读普通 Codex 配置正文 |
| Junie/普通 IDEA | 未操作 |

机器可复验结果和 runner 在 `build/mcp-skill-2026-11-12/client-discovery/`：原 Project marker 证据在 `discovery-result.json`；User marker 证据在 `user-scope-probe-2026-11-12-r1/result.json`；实际 Doc View Skill discovery 在 `actual-skill-source-probe-2026-11-13-r1/result.json`，runner 为 `scripts/probe_actual_skill_scope.py`；当前 schema 在 `app-server-schema/v2/SkillsListParams.json`。所有 runner 只传递最小隔离环境，使用独立 HOME、CODEX_HOME、Git fixture 和 TMPDIR；实际源文件仅复制到 `build/` 下临时根。

## 官方能力边界

- 官方 [Build skills 文档](https://learn.chatgpt.com/docs/build-skills) 说明 Codex 在 repo 当前目录到 repo 根路径寻找 `.agents/skills`，User scope 为 `$HOME/.agents/skills`；Codex 会监听本地 Skill 变化，需要时可重启。
- 官方 [App Server 文档](https://learn.chatgpt.com/docs/app-server#skills) 说明 `skills/list` 可带 `forceReload`；目前网页文档另提 `perCwdExtraUserRoots`。本次读取的实际 0.167 schema 不含此字段，并通过同版本运行时发现其 marker 未被列出，故不把网页上更新的 API 声明当作当前二进制的能力。
- 官方 `externalAgentConfig/detect` + `externalAgentConfig/import` 可迁移已检测的外部 Agent 项目；示例将外部 `SKILLS` 复制到用户 `.agents/skills`。这不是对任意 Doc View Release 固定 commit 的安装/更新 API，也没有设计中的文件 hash conflict、journal 和失败恢复 contract。
- Codex 内置 `$skill-installer` 的当前本机脚本接受 GitHub repo/path 和 `--ref`、`--dest`，检查目的 Skill 已存在时停止并复制所选目录；默认 `--ref main`，目的在 `$CODEX_HOME/skills`（未设时为 `~/.codex/skills`）。它不保存来源 commit/逐文件 hash metadata，不提供 update transaction、改动保护或 journal recovery。因此它不是本 change 的完整 Java 安装器替代品。
- `skills/extraRoots/set` 会改写当前 app-server 进程发现 roots，首版不自动调用。

## 后续准入

1. 仅对已验收的精确客户端 `0.167.0-alpha.4.1` 启用 Project `.agents/skills` 与 User `$HOME/.agents/skills`；其他客户端 build 继续 fail closed，直到补对应 discovery 证据。
2. User scope 的 marker 与实际 `doc-view` 源文件均只在隔离 HOME 下测试；不通过修改普通 `$HOME`、默认 Codex 配置或普通工作区写入来造出通过证据。
3. 当前 `skills/list` schema 不含 `perCwdExtraUserRoots`；不把比本机 binary 更新的网页字段当作该 build 的兼容性保证。
4. 实际正式 release archive、更新/恢复事务和安装后原生 settings/UI 行为仍需单独验证；本次没有执行网络安装或触碰客户端真实目录。

## 同名 Project/User 同时存在（2026-11-13）

在同一临时项目与临时HOME内同时放入当前`skills/doc-view/SKILL.md`源（SHA-256 `01d69274b1ee5bc757cb8b7273ba7082314652ac99a7624dd2c1c804bb5ff99f`），同版Codex的`skills/list(forceReload=true)`返回两条同名`doc-view`，分别为repo/user和各自精确路径，没有在这个发现接口按名称去重。因此安装后校验可以区分两个scope；此结果不推断模型实际选择优先级。证据`build/mcp-skill-2026-11-12/client-discovery/same-name-scope-probe-2026-11-13-r1/result.json`。没有读取或写入真实HOME/CODEX_HOME，没有凭据、模型turn或网络release请求。
