# Proposal

## Why

Doc View 计划通过 IDEA 自带 MCP Server 向 Codex 提供项目文档查询，但用户还需要知道如何安全、稳定地使用这些工具。一个可安装的 Codex Skill 可以提供统一工作流；当前官方资料和本机安装器对 Skill 发现目录、首次安装及更新的能力并不完全一致，因此需要先把目标客户端的真实版本、发现范围、来源完整性和本地修改保护定义清楚。

## What Changes

- 在 IDEA **Settings → Tools → Doc View → MCP / AI** 提供 Skill 状态、**Install Skill**、**Check for Updates** 和 **Update Skill**。检查只读；只有明确点击安装或更新后才写入，不增加重复的永久确认弹窗。MCP / AI 是计划新增的 Doc View 子设置页。
- 从 Doc View 官方 GitHub release 安装固定提交与路径下的 Skill 文件清单；校验 SHA-256、`toolContractVersion` 和适用的 Doc View 插件版本范围，不接受任意仓库或 URL。
- 默认自动检测 Codex，正常界面只选择“当前项目”或“全局”安装范围；固定安装目录由插件计算并只读显示，不要求用户填写。自动检测失败时才提供手动选择程序的入口。确认实际客户端版本与 Project/User 发现目录，未经验证时说明原因并禁止写入，不修改客户端发现 roots。
- 安装和更新使用有界暂存、路径与文件类型校验、内容冲突检查、同文件系统分步重命名和恢复记录；两步切换之间目标目录可能短暂缺席。已安装文件与已记录内容不一致时停止并保留原目录；离线时保留现有版本。
- 将发行来源与校验记录放在 Skill 正文之外的 Doc View 专用 metadata；Skill 文本不作为安装状态存储。
- 随 Skill 明确 `doc_view_status`、`doc_view_search`、`doc_view_get` 的只读查询规则及用户明确要求后的 `doc_view_generate` 工作流。将 freshness 的 `current`、`stale`、`unknown` 原样解释；没有项目文档库时不得暗中生成；源码、注释和工具结果均作为数据，不得当作授权指令。

2026-11-12 已授权实现插件内的安装与更新；只有用户明确点击安装/更新才写入所选范围，不因拉取仓库或打开设置自动安装。不会自动注册/配置 MCP Server、修改 Codex 连接配置、升级 Doc View 插件或新建 MCP Server。Skill 正式 release 依赖生产 MCP 工具通过其准入验收。

## Capabilities

### New Capabilities

- `doc-view-client-skill`：面向 Codex 的 Skill 来源、兼容性、发现范围、安装/更新事务和工具使用工作流。

### Modified Capabilities

无。生产工具行为由 [`doc-view-mcp-tools`](../doc-view-mcp-tools/proposal.md) 定义；本 change 作为客户端使用层依赖该 contract，不重复定义 MCP 协议或项目文档库格式。

## Impact

未来实现涉及 IDEA 设置 UI、Codex 版本/Skill 发现目录判定、官方 release 查询与下载、安装 metadata、后台安装任务和恢复逻辑；生产实现继续 Java-only。计划中的 source package 路径为仓库 `skills/doc-view/SKILL.md`，它是发行源码，不等于项目级 `.agents/skills/doc-view` 或个人目录，也不会因本 change 自动成为可发现 Skill。任何 Skill 正式发布前，都需要先完成生产 MCP 注册、客户端连接、查询/详情/显式生成和取消生命周期验收。
