# 11 月 19 日客户端发现和回归复查

- 实际 CLI 为 `codex-cli 0.169.0-alpha.11.1`，可执行文件 SHA-256 `50ab38ba21d0d9f8346f32f41848382f15b556190f3c7a07e885a4fb73e379c8`；与[两范围绝对来源路径与 `skills/list(forceReload=true)` 记录](verification-2026-11-17.md#实际客户端与安装记录) 是同一构建。终端分别使用隔离项目、HOME、CODEX_HOME 与同摘要 Skill：项目范围及全局范围的 `/skills → List skills` 和 `$doc-view` 补全均明确显示 `doc-view [Skill]`，未提交模型回合。只验证客户端可见性，未把测试文件冒充正式包。原始屏幕输出在 `build/openspec-native-closeout-20261119/` 的 `skill-tui/project/mention.txt`、`skill-tui-list/project/skills.txt`、`skill-tui-user-confirmed/user/skills.txt` 和 `skill-tui-user-mention/user/mention.txt`。
- 新增测试断言无当前项目、目标非目录或发现结果无路径时拒绝准入；与既有路径/范围不匹配、未知版本拒绝写入测试一起执行。六个定向测试类共 53 项，通过且无跳过。证据为 `skill-focused.log` 和 `skill-junit/`。
- 设置组件补验：自动发现不支持的程序后，按文件选择器写入路径的方式传入另一台已验证的测试程序；后台仅检查一次，不初始化安装目录。更新后的 `CodexSkillAutoCheckTest` 单独执行 6 项通过；其他自动发现、切换范围、重复操作和取消仍由同套 fixture 覆盖。这不是系统文件选择器的原生烟测。
- 启用验收源集后，两项实际客户端验收均通过：本地固定来源的两范围安装/更新经真实客户端发现，故障和取消回滚保留旧版本，18 个检查通过；测试子 JVM 在事务中被终止后恢复，6 个检查通过。记录分别为 `real-client/real-codex-lifecycle.json` 和 `real-client/jvm-crash-recovery.json`。
- 安装确认和发现结果只验证本地候选；本轮没有重新完成官方 GitHub 下载阶段取消、同一候选包的原生 IDE 重启 journal 恢复，也没有重跑完整 MCP 自然对话与发布边界。相关整体验收仍未通过。
