# Skill 实现与设置页验收（2026-11-13）

本轮在隔离工作区实现1.7开发候选；原工作区和Junie保留，未安装到真实用户Skill目录，未创建或发布Skill Release。发现矩阵见[客户端发现记录](verification-2026-11-12-discovery.md)，MCP包身份和调用证据见[MCP记录](../doc-view-mcp-tools/verification-2026-11-12.md)。

## 已实现

- 当前项目和全局两个范围，目标分别为`<project>/.agents/skills/doc-view`与`$HOME/.agents/skills/doc-view`。目标只读显示，不接受任意安装路径。
- 后台查找ChatGPT.app随附Codex和PATH候选；打开设置不启动CLI、不联网。明确检查时核对客户端build，无法检测或版本不支持时提供程序选择；平台不支持单独说明。
- 固定官方GitHub Release来源、tag/commit/manifest/契约校验、有限压缩包校验、用户修改保护、隐藏writer lock、metadata/journal、目录切换与恢复。Global metadata跨项目/profile共享，Project metadata按项目隔离。
- 一项检查更新链接和按状态切换的安装/更新按钮，范围切换不复用其他范围或未核验程序的状态；关闭页面丢弃迟到回调，已提交安装结果不被迟到取消改写。

## 自动检查

`build/mcp-skill-2026-11-12/full-candidate-r4`：Java25 / 263.7031.23 SDK，510 tests、0 failures/errors/skips；buildPlugin、verifyPluginProjectConfiguration通过。Skill相关用例包括`CodexClientProbeTest`、`CodexSkillTargetTest`、`SkillReleaseManifestTest`、`SkillPackageArchiveTest`、`SkillInstallMetadataTest`、`SkillInstallTransactionTest`、`CodexSkillInstallServiceTest`、`CodexSkillSettingsTest`。这不等于所有task列出的负向矩阵及原生安装事务均已验收。

发行包工具10项Python测试通过；四个MCP输入schema、4个成功样例和8个错误schema检查通过。两个OpenSpec change严格验证和103个相对链接检查通过。

中间包`native/global-ui-candidate`在263.7031.23和263.7307.200的Verifier通过，仅16项既有Experimental API引用，无Internal/Deprecated/Override-only/兼容性问题。后续UI修复改变包身份时须重新记录最终包结果。

## 原生检查与问题修复

隔离IDEA 263.7307.200，macOS Apple Silicon；实际Codex为0.167.0-alpha.4.1。

- 自动发现CLI并隐藏程序路径选择；初始未知版本摘要隐藏。键盘可选择Global，路径自动变为当前用户固定目录。
- Global检查更新正确返回“没有兼容稳定Skill发行包”，安装按钮禁用。检查前后目标Skill目录、Global metadata和writer lock均未创建，路径前后状态存于`native/global-ui-candidate/skill-paths-*.json`。
- 中间包发现：检查时ActionLink默认隐藏使按钮跳位；Global按钮提示仍写project path；完成结果重复显示Codex版本及目标路径。
- 中间包在隔离IDEA原生复现用户横向滑动问题：Settings从1800×1400放大到5120×2706再恢复后，平台外层滚动区保留大宽度，显示水平滚动条并裁切内容。真实原生截图是该症状的复现依据。
- `full-candidate-r5` 的 headless 外层包装模拟没有复现平台 Settings 的宽度缓存；512项测试中仅该固定溢出断言失败。已移除该伪复现，不给组件强设宽度。自动测试保留 `Configurable.NoScroll`、单一自有滚动容器和宽→窄→窄窗→宽的布局边界；平台缓存问题另由下述最终包原生复验确认。

## 最终修复包复验

- `full-candidate-r6` 使用 `--rerun-tasks --no-build-cache --no-parallel`，511 tests、0 failures/errors/skips；Java 25 构建、`buildPlugin`、`verifyPluginProjectConfiguration` 通过。最终包 `native/global-ui-fixed-candidate/Doc View-1.7.0-SNAPSHOT.zip` SHA-256 为 `06f16f0efac61e240972b2a89b76356715237a43cfac545b385f6816cdaadc16`，JAR 为 `956905a6c90c55d67e2a02c438bdf84d0ffcc035e7e6e953b916e1a6917d6ce3`；冻结源码 hash 与实际构建产物再次核对一致。
- 此包在最低 SDK 263.7031.23 与官方安装包 263.7307.200 的 Plugin Verifier 均 Compatible，只有16项既有 Experimental 引用，无 Internal/Deprecated/Override-only/兼容性问题。两个 Verifier 顺序运行；最低版本原生回归仍未执行。
- 隔离 IDEA 263.7307.200 / PID 99134 加载最终 JAR。Settings 从1800×1400放大到5120×2706再恢复后，右侧内容重新适配，未出现水平滚动条或裁切；在内容区向左、向右滚动均不带动整页偏移。修复使用公开 `Configurable.NoScroll`，由自有单层滚动容器跟随 viewport 宽度。
- 检查更新期间灰色链接保留，安装按钮位置稳定；Codex版本仅显示一次，状态不重复完整目录。键盘切换“当前项目 / 全局”会自动更新只读目录；尝试键入不会改写目录。全局安装按钮提示使用“显示的目录”，不再误称项目目录。
- 当前项目及首次全局检查曾返回 OFFLINE，写按钮禁用；全局显式重试后恢复为 NO_COMPATIBLE_RELEASE。相同 JBR 25、请求头、超时与生产有限 body subscriber 的单次公开 API 对照均 HTTP 200，未发现固定 JBR 或请求实现故障；本轮没有因此更换网络实现。检查前后真实全局目标、metadata、writer lock 和测试项目目标均未创建，未安装到个人目录。
- 公共 releases API 回读未发现兼容的 Skill 资产；“没有兼容稳定发行包”是实际结果，不以临时源码 discovery 伪造可安装状态。
- 两个change的最终strict validate、112个本地相对链接与`git diff --check`通过；原工作区HEAD、584个文件hash和Git状态未变。临时MCP服务已关闭，隔离IDEA正常退出，未操作Junie。

## 尚未通过或未执行

后续候选包、两范围真实Codex发现、受控安装/更新/取消及独立JVM失败恢复已在[发行续验记录](verification-2026-11-13-release.md)补充；下列正式GitHub资产及原生IDEA矩阵仍保留未完成。

- 正式stable Skill Release尚不存在；实际发行包的Project/Global安装、更新、离线、取消及重启恢复的完整原生矩阵未执行。临时源码被`skills/list`发现不替代该矩阵。
- Codex对话内`$doc-view`或`/skills`可见性及自然语言驱动的完整Skill工作流未执行。
- 当前发现矩阵只覆盖精确Codex build和macOS Apple Silicon；不外推到其他客户端版本、系统或架构。
- 设置页中文、浅色及完整键盘矩阵未做本轮原生验收；中英资源和相关控件逻辑有自动测试。
- 尚未提交、推送、打tag、发布、sync或archive；tasks只在对应实现和全部验收条件都有证据时勾选。
