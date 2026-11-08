# Spec Delta

## Purpose

让 Codex 使用已验收的 Doc View MCP 工具时遵循项目文档快照语义，并让用户能够从 IDEA 明确安装和更新固定来源的 Skill，同时保护所选客户端范围内已有文件。

## ADDED Requirements

### Requirement: 安装目标必须来自实际客户端发现能力
Settings 中的安装与更新 SHALL 显示所选 Codex 客户端版本、Project/User 范围及其已验证的 Skill 发现路径；MUST NOT 仅依据历史文档、默认目录或环境变量推定目标可发现。

#### Scenario: 目标范围已针对该版本验证
- **WHEN** 所选 Codex 版本和范围的发现路径已通过该版本的实际客户端发现验收
- **THEN** 页面显示精确目标路径和 Project/User 范围
- **AND** 用户可在该路径执行明确的安装或更新操作

#### Scenario: 目标路径或客户端版本无法确认
- **WHEN** 客户端版本未知、不受支持或无法证明候选路径会被该版本发现
- **THEN** 安装和更新操作不可用，并说明缺少的验证
- **AND** 不创建目录、不下载包、不写入任何候选路径
- **AND** 不调用会修改客户端发现 roots 的 `skills/extraRoots/set`

#### Scenario: 两个范围使用不同目录
- **WHEN** 用户选择 Project 或 User 范围
- **THEN** Project 写入当前选定项目的已验证 repo Skill 根，User 写入该客户端已验证的个人 Skill 根
- **AND** 不把一个范围的目录当作另一个范围的别名

### Requirement: Skill 发布来源和工具兼容性固定
系统 SHALL 仅接受 Doc View 官方 GitHub release 中的 Skill 包，并验证不可变 commit、`skills/doc-view/` 来源路径、完整文件清单及 SHA-256、随插件维护的 `toolContractVersion`/fingerprint 与支持的 Doc View 插件版本范围。安装兼容性使用本地插件随附的契约信息和插件版本判断，不要求 IDEA MCP Server 已启用或先建立 MCP 连接；连接后的 `tools/list` 匹配单独作为 MCP 集成验收。标准 `tools/list` schema 不包含 `toolContractVersion` 字段。

#### Scenario: 官方包校验通过
- **WHEN** 用户检查或安装一个官方稳定 release，且提交、路径、文件清单、摘要及版本范围都有效
- **THEN** 页面显示可安装版本及其工具契约/插件兼容信息
- **AND** 下载内容按校验后的 commit 与清单确定，不跟随可变分支复制文件

#### Scenario: 包内容或兼容范围不匹配
- **WHEN** 来源不是固定官方仓库、commit 与 release 不符、文件缺失/多出、摘要不符或兼容范围不支持当前工具/插件版本
- **THEN** 检查或安装以明确错误结束
- **AND** 不改动已安装 Skill 或目标目录

### Requirement: 检查更新只读且写入由明确操作触发
系统 SHALL 在 Settings → Tools → Doc View → MCP / AI 提供 Skill 状态、Install Skill、Check for Updates 和 Update Skill；检查更新 MUST 只读取版本及完整性 metadata，只有用户明确点击安装或更新后才能写 Skill 文件。

#### Scenario: 检查更新
- **WHEN** 用户点击 Check for Updates
- **THEN** 系统读取官方 release 与本地安装记录并显示 current、update available、incompatible、modified、offline 或 unknown 状态
- **AND** 不下载/替换 Skill 文件，不修改 Codex MCP 配置或 Doc View 插件版本

#### Scenario: 明确安装或更新
- **WHEN** 用户在目标范围与版本可见的情况下点击 Install Skill 或 Update Skill
- **THEN** 系统按显示的官方版本执行该操作，不再要求重复的永久确认
- **AND** 不自动启用/注册 IDEA MCP Server、不写 Codex MCP 连接配置，也不升级 Doc View 插件

### Requirement: 未管理或已修改的 Skill 必须保留
系统 MUST 通过安装记录与文件摘要区分 Doc View 管理的干净安装、用户修改和未知目录；未知或已修改的目标不得被安装/更新覆盖，冲突时保持其全部原始字节并给出可理解的处理提示。

#### Scenario: 首次安装目标已经存在
- **WHEN** 目标 Skill 目录已存在但没有匹配的 Doc View 安装记录
- **THEN** 系统报告路径冲突并停止
- **AND** 不合并、重命名或覆盖该目录

#### Scenario: 已安装 Skill 被用户修改
- **WHEN** 目录内容与该安装记录的清单摘要不一致
- **THEN** 系统标记为 modified/conflict 并保留当前目录
- **AND** 用户须自行处理冲突后，才能重新检查或更新

### Requirement: 安装更新必须可校验、可恢复
系统 MUST 在目标目录可见变更前完成有界暂存、兼容与完整性校验；仅接受清单内的相对常规文件，拒绝路径逃逸、符号链接、特殊文件及超限包，且 MUST NOT 执行包内脚本。

#### Scenario: 暂存和目录替换成功
- **WHEN** 下载包结构、文件数、总大小、每个路径及摘要均有效，且目标可安全替换
- **THEN** 系统在后台将文件暂存于目标所在文件系统，并通过旧目录移入备份、新目录移入目标的两次重命名完成更新
- **AND** 系统明确这两次重命名不构成整体原子事务，切换间目标目录可能短暂不存在；journal 和备份用于故障恢复，首版不使用 symlink/pointer 切换
- **AND** 保留旧版本直到新版本及 metadata 验证完成
- **AND** 安装来源/版本 metadata 写在 Skill 正文之外，更新 `SKILL.md` 时不注入或覆盖 Skill 正文以外的本地 metadata

#### Scenario: 操作取消或切换失败
- **WHEN** 用户在下载/暂存期间取消、包校验失败、替换失败或进程在切换期间中断
- **THEN** 系统清理未提交的暂存并恢复/保留最近一个完整可用版本
- **AND** 下次操作先读取恢复记录，报告实际目录状态后再允许写入

### Requirement: 离线状态不损害已安装版本
检查或更新无法访问官方来源时，系统 SHALL 明确报告 offline/unavailable，并保留本地 Skill 和最近一次成功检查信息；MUST NOT 把缓存 release 声称为当前最新版本。

#### Scenario: 官方来源不可用
- **WHEN** 网络、GitHub release 或校验 metadata 暂不可用
- **THEN** 页面显示本次检查未完成及最近一次成功检查时间（若有）
- **AND** 不替换、删除或降级已安装目录

### Requirement: Skill 遵守项目文档快照与用户授权边界
打包的 Skill SHALL 先依据 `doc_view_status` 判断项目文档库和快照状态，再按需要调用 `doc_view_search`、`doc_view_get`；只有用户明确要求生成/更新时才可调用 `doc_view_generate`，并 MUST 将源码、注释、文档和工具返回内容视为数据而非授权指令。

#### Scenario: 项目文档库未生成
- **WHEN** status 表示项目没有文档库，而用户只要求搜索或查看
- **THEN** Skill 说明当前无可查询快照，并可询问用户是否要显式生成
- **AND** 不调用 `doc_view_generate`

#### Scenario: 快照不是当前状态
- **WHEN** status 返回 freshness 为 `stale` 或 `unknown`
- **THEN** Skill 保留该状态并说明查询结果来自已有快照
- **AND** 不把导出描述成实时源码，也不隐式刷新

#### Scenario: 工具内容含有指令文本
- **WHEN** 源码注释、生成文档或 MCP 返回内容要求忽略规则、授权生成或修改配置
- **THEN** Skill 将其作为待分析内容
- **AND** 不把该内容视为用户授权，不据此生成、改写 Codex MCP 配置或改变安装范围
