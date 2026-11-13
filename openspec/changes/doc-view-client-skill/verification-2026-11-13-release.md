# Skill 候选发行包与两范围生命周期验收

本轮在隔离工作区 `doc-view-release-integration-20261107` 继续，原工作区、普通用户 Skill 目录与 Junie 保留。开发插件仍为1.7.0-SNAPSHOT；没有正式发布、推送、修改仓库tag或安装到个人目录。

## 候选包

候选ZIP SHA-256：`f0f50211f7492167f99f391d0e740c4880bf69146d2989d6342b6267333955f9`。manifest SHA-256：`769fa25fb8eec8174dd6413b45f9bc3647f1e5539f69bd2b391ce16e3226c634`。仅包含当前 `doc-view/SKILL.md`，绑定临时仓库commit `2f5d64a30ade5e3cfaa50c8c42b58273f497d37a`，两次构建字节一致。临时仓库的v1.7.0不是官方仓库tag，commit不能从GitHub取得；不能把此包描述为已发布资产。完整来源与命令见[发行说明](../../../docs/skill-release.md)。

## 发现并修复

1. Python标准ZIP写入器对纯ASCII文件名不设置UTF-8标志，生产Java校验器却强制要求此标志。实际候选包首次进入Java安装流程即被拒绝。修复为允许纯ASCII名字；非ASCII路径仍要求明确UTF-8，两处header均校验；加密、路径逃逸、文件类型和摘要检查保持。新增正反向测试及实际候选复验。
2. 下载器原先只验证tag commit与manifest、自报ZIP摘要，没有验证内容来自该commit。现新增官方commit→tree→Skill blob完整集合校验；响应有界、拒绝截断/身份不符/非常规文件。5项测试覆盖自报SHA正确但源码不符等路径。
3. 打包器原先可从非官方origin输出声称属于官方仓库的manifest；新增origin检查，11项Python测试通过。origin是本地配置，仍不构成远端真实性证明；正式发布必须核对官方仓库tag和资产回读。

## 两范围服务与真实客户端检查

使用macOS Apple Silicon、Java25、IDEA263.7031.23测试框架，以及实际 `codex-cli 0.167.0-alpha.4.1`。本地release source仅在opt-in测试源码注入，生产入口仍只有官方GitHub；实际安装服务、ZIP校验、metadata、文件事务、Codex版本探测及安装后 `skills/list` 均执行生产代码。

Project使用临时Git项目，Global使用临时home；Global按生产规则写 `.agents/.doc-view-skill-state`，Project metadata使用独立测试config目录。每次CLI启动使用隔离CODEX_HOME，不读取普通客户端配置、凭据或模型账户。更新1.7.1/1.7.2是明确标注的测试fixture，不是已发布版本。

| 场景 | 当前项目 | 全局 | 证据边界 |
| --- | --- | --- | --- |
| 检查不创建目标/metadata/锁 | 通过 | 通过 | 生产service，本地发行源 |
| 候选安装后实际Codex发现 | 通过 | 通过 | 精确name/path及repo/user scope |
| 新版本更新、文件和metadata一致 | 通过 | 通过 | 实际文件与真实发现 |
| 下载阶段取消保留旧版 | 通过 | 通过 | 注入取消信号；非真实HTTP中断 |
| 下载离线保留旧版 | 通过 | 通过 | 注入网络错误；非断网测试 |
| Codex发现错误名称，回滚新文件及metadata | 通过 | 通过 | 实际Codex负向发现；备份/journal清理 |
| 新service实例重试更新成功 | 通过 | 通过 | 实际文件及真实发现 |
| 手改冲突、另一范围字节不变 | 通过 | 通过 | 前后hash比较 |

以上共16项场景，机器记录 `build/skill-acceptance-2026-11-13/real-client/real-codex-lifecycle.json`。普通JUnit还覆盖两scope的未管理目录、暂存取消、四个更新阶段故障、metadata原子提交失败、恢复待处理、新store恢复、并发锁拒写、metadata损坏/超限/软链接保护。

## 独立进程中断与恢复

对两scope各在READY、OLD_BACKED_UP和NEW_IN_PLACE阶段，测试子JVM执行 `Runtime.halt(73)`，不运行catch/finally回滚。父进程确认退出码和残留journal，再以新store调用生产 `recoverPending`：共6次通过。两次rename中间目标确实可能缺席；恢复后目标与旧manifest逐字节一致、metadata版本正确，journal/stage/backup清理完成。记录 `real-client/jvm-crash-recovery.json`。这是实际子JVM终止与跨进程文件恢复，不等于IDEA应用重启UI验收。

## 检查轮次

- r1：并行实现过程中编译遇到新增测试所需方法尚未落地，没有执行测试。
- r2：24项、3失败。实际候选编码误拒2项；另1项为测试错误地修改Skill目录外README条目的mode，已改为精确Skill路径。
- r3：测试补充断言缺少静态import导致编译失败，已修复。
- r4–r6：修正验收harness的独立JVM模态初始化，以及IDEA线程池吞掉PCE/Java25 CompletableFuture对取消的包装。未为harness错误修改生产取消语义；失败报告保留。
- r7：31项定向测试，0失败/错误/跳过；16项真实客户端流程与6次子JVM恢复均通过。
- r8：开启opt-in集成source set后，强制重跑全量526项测试，0失败/错误/跳过；`test buildPlugin verifyPluginProjectConfiguration --rerun-tasks --no-build-cache --no-parallel`通过，用时112.24秒。Python打包工具11项测试另行通过。
- timestamp-targeted：Skill install metadata、检查状态/服务和设置UI三个测试类共24项，0失败/错误/跳过。最近成功检查时间现在写在独立scope状态文件；成功release查询更新它，离线保留旧值。本地状态写入失败的回归验证为`ERROR`，不伪装成`OFFLINE`。这组定向结果不替代接下来的最终全量构建。
- 新插件ZIP SHA-256为 `8c922d5695041f7298ae777ed46ebfc5de1bb598629efccc1a1895ca75aae225`，JAR为 `24c80606df2eb45d0a08436bf3c3670f97ee5e0fa251115d147442bd43e5cdb0`；406个生产class全部major69，包内无Test/PoC类，可选MCP descriptor存在。这个新包没有借用上一包的原生UI结果。
- 新包顺序通过263.7031.23的Gradle Plugin Verifier和263.7307.200的Verifier1.411，均Compatible；16项既有Experimental API引用，无Internal/Deprecated/Override-only/兼容性错误。证据为`minimum-verifier`、`release-verifier`日志及报告。
- OpenSpec strict validate、Git diff空白检查、128个本地相对链接通过。原工作区HEAD、Git状态及584文件hash与基线一致；普通用户Skill目标、metadata和writer lock仍不存在。没有普通IDEA、Junie、外部发布或仓库tag操作。

## 未完成

- 官方GitHub稳定资产的真实下载、来源验证、离线/HTTP取消，以及最终IDEA按钮的安装/更新/恢复原生矩阵尚未执行。当前没有可下载的官方Skill资产；本地source注入不替代它们。
- 中文/浅色/完整键盘矩阵、IDEA退出重启触发恢复、自然语言Skill工作流和`$doc-view`菜单可见性仍未验证。
- 上述旧轮次之后，最新包531项全量构建、两个精确build的Verifier及原生设置/可选加载/MCP复验已完成，见[最终候选记录](verification-2026-11-13-final-candidate.md)。正式GitHub发行包上的安装按钮流程仍未执行。
- 生产MCP的发布边界取消和项目生命周期仍有独立缺口，不因本轮文件事务通过而勾选。
