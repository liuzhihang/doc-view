# 2026-11-17 取消、客户端与生命周期续验

## 范围与 Git

- 工作区：`/Users/liuzhihang/workspace/doc-view-release-integration-20261107`，分支 `codex/mcp-ui-lifecycle-20261114`，基点 `6514a9eff12b395119be53edf80c37b5a7bf12a4`，候选 `1.7.2-SNAPSHOT`，未发布。
- 按用户要求执行 `git fetch origin`，本地 `master` 从 `00f1f94` 快进46个提交至 `6514a9e`，与 `origin/master` 一致；当前验收分支已经包含该提交。同步前后三个工作区714/577/660个文件摘要、HEAD及未提交状态一致。未切换原工作区分支，未提交、推送或发布；Junie未操作。
- 同步证据：`build/remaining-acceptance-2026-11-17/remote-sync-result.json`。首次验收前的原工作区清单与同步前清单取样时间不同，不能将计数差异当作本轮改动。
- 结束前再次核对原工作区577个文件和polish工作区660个文件：摘要、HEAD、未提交状态均与同步前一致，见 `preserved-worktrees-final.json`；本轮实现和证据只保存在隔离集成工作区。

## 实际客户端与安装记录

- 本机官方 ChatGPT 随附 CLI 已变为 `codex-cli 0.169.0-alpha.11.1`；SHA-256 `50ab38ba21d0d9f8346f32f41848382f15b556190f3c7a07e885a4fb73e379c8`。未找到保留的旧 `0.167.0-alpha.4.1` binary，不伪造版本，也不将旧验收算作新版结果。
- 实际 app-server `initialize → initialized → skills/list(cwds, forceReload=true)` 在隔离 HOME/CODEX_HOME 下分别返回 Project的 `scope=repo` 与 User的 `scope=user`，两者均为精确 `.agents/skills/doc-view/SKILL.md`、enabled=true。普通用户 Skill、Codex 配置未读写；无模型回合或外部MCP调用。
- 证据：`build/skill-cancel-2026-11-17/current-codex-scope-probe/result.json`。
- 回归先RED：新增 `readsRecordedInstallVersionsAcrossTheVerifiedClientUpgrade` 因新版本安装记录被当前精确版本校验拒绝而失败（1 test / 1 failure）。只替换当前客户端常量会反过来拒绝旧安装记录，因此将当前运行准入与同格式安装来源记录分开：运行只接受新build，metadata只允许本次与此前已验证build，未知版本仍拒绝。
- 修复后定向GREEN：26 tests，0 failures/errors/skips；包含真实新版客户端两范围本地发行源的18项流程检查，以及两范围、三个事务阶段的6项独立子JVM退出/恢复。历史metadata由测试明确注入，检查不改写该记录，更新后记录实际当前客户端。
- 以上发行源为测试注入的本地不可变候选，真实Codex发现走实际程序；下载取消为受控源信号，不是GitHub网络中断，也不是IDEA原生按钮验收。
- 证据：`build/remaining-acceptance-2026-11-17/metadata-client-{red,green}.json`、`metadata-client-red.xml`、`metadata-client-green-results/`、`real-client/{real-codex-lifecycle,jvm-crash-recovery}.json`。

## 旧包观察与先失败后修复

- 核对隔离安装 `IU-263.7307.200`、`JBR-25.0.4.1+1-635.79-nomod`，使用新隔离profile与虚构 `project-a`，MCP初始Disabled。初次运行的是11月14日最终ZIP（SHA-256 `4264fd845da4453b34826dc6dc6908d3a3d3b399f6bb3be5efaa7656145c2ae9`），不是本轮修复后的新包。
- 旧最终包在设置页自动识别实际新版CLI并显示明确不支持提示；可见操作为检查链接，安装按钮隐藏，MCP设置快捷链接可见。该观察证明未验证版本fail-closed，不证明新版受支持安装完成。
- 读取窗口900×700 points；尝试缩小后实际尺寸未改变，因此本次不记录窄窗缩放通过。隔离IDEA随后正常退出；退出日志不证明精确下载/暂存或在途生成生命周期通过。
- 首次生成freshness的前几次夹具准备失败分别为事件队列等待阻塞、缺少实际可扫描端点、根目录未注册为content root，均不计产品根因证据。修正夹具后，实际生成成功并提交epoch=9；首次VFS刷新仅报告 `docs/` 创建，epoch推进至10、快照误报stale，RED已保存。
- 直接忽略 `docs/` 创建的初版修复被反例否定：首次刷新前预放 nested Java/XML 后仍返回current。最终改为after阶段检查直接子项，仅有输出目录 `doc-view` 时忽略，其余保守stale；未使用Internal API或递归扫描。最终完整 `ProjectDocumentIndexServiceTest` 22 tests，0 failures/errors/skips。证据为 `freshness-fixture-red5.xml`、`freshness-counterexample-red1.xml`、`freshness-index-service-final.xml`（均在本轮build证据目录）。

## 最终包自动检查（11月18日完成）

- 最终全量 `test` 使用实际客户端 opt-in init script、`--rerun-tasks --no-build-cache --no-parallel`：100个suite、564 tests，0 failures/errors/skips。配置检查与构建均成功。测试编译仍有既有弃用提示，不能描述为所有构建输出零警告。
- 首次全量失败为 `ProjectDocumentGenerationLifecycleTest` 的 skipped 数量多1；保留候选扫描诊断后，确认新freshness测试删除物理文件但未清除VFS/注解索引中的 `freshness.FirstGenerationApi`。修复夹具清理：删除源码和输出、刷新根目录、等待索引并断言PSI/扫描均不再命中。随后22项定向与564项全量均通过；不是把该失败解释为生产取消缺陷。
- 最终 ZIP `Doc View-1.7.2-SNAPSHOT.zip` SHA-256：`f0d5ad184d97b393a9b737488f09e75d4a66cdd36feb4e061e187882cf881dcb`。`final1-launch.json` 记录同一SHA、隔离 PID 11112及 `263.7307.200`；本节后续原生结果属于该包，而非前面的11月14日旧包。
- Plugin Verifier：`263.7031.23`、`263.7307.200` 均 Compatible，各报告16处Experimental API；没有Internal、Deprecated、Override-only或兼容性问题。最低build第一次运行因两个Verifier并发使用提取缓存出现 `DirectoryNotEmptyException`，不是兼容性结论；改为串行重跑通过，保留失败日志。
- 证据位于 `build/remaining-acceptance-2026-11-17/`：`full-red-results/`、`full-diagnostic.log`、`full-final-results/`、`full-final-counts.json`、`final-build-summary.json`、`final-build.log`、`minimum-verifier-report/`、`release-verifier/`。

## 最终包原生结果

- 首次项目生成：62个API、更新126个文档输出；首次VFS刷新后UI显示 `Current for this session`，没有误报stale。索引与JSON/Markdown输出已回读，见 `first-generation-readback.json`，根因及反例详见[文档库验证](../2026-11-19-project-document-store/verification-2026-11-17-first-generation-freshness.md)。
- 设置自动识别真实 `0.169.0-alpha.11.1`，从官方源检查得到Skill 1.7.1；`final1-skill-ready.png` 显示仅有安装主按钮，未知摘要和手动CLI路径不占据常规界面。AX会列出隐藏控件，不能只按AX列表判断按钮是否同时可见。
- MCP快捷入口鼠标和键盘均进入宿主的 `Settings → AI → MCP Server`。Tab可访问范围、只读路径、检查操作和快捷链接，Space触发跳转；未自动安装Skill或配置客户端。见 `keyboard-tab-flow.json` 与 `ui-actions.jsonl`。
- 窗口实际从1200×800缩至900×700 points，设置内容左对齐、未出现横向面板滚动或按钮裁切，见 `ui-wide-attempt.png`、`ui-narrow-after-wide.png`。请求760×620被IDE最小窗口尺寸限制，不能记录为760宽通过；宽→窄→宽完整循环尚未完成。后续尝试选择浅色主题未实际改变主题，不能记浅色通过。
- 项目和全局范围的精确暂存取消分别通过：用外部JDI行断点观察 `writeStage` 已完成、尚未校验提交，实际点击设置Cancel后释放工作线程，观察取消异常。目标和metadata仍不存在，journal/stage/backup已清理，另一个scope未改变。全局指隔离HOME，未修改普通用户目录。证据均在 `build/skill-cancel-2026-11-17/`：`project-stage-r2.jsonl`、`global-stage.jsonl`、`project-stage-r2-before.json`、`project-stage-r2-after.json`、`global-stage-before.json`、`global-stage-after.json`。
- 首次暂存探针使用MethodExit导致明显调试开销，取消晚于提交；该轮不作为精确取消或正常安装证据。改为单一行断点后复验通过。下载阶段的观察虽确认CDN请求在途，但关闭设置前future已完成，因此仅证明应用取消路径/清理，不证明在途网络请求取消。
- 取消后正常安装重试受到官方GitHub公共API `403` 限流（60/60）影响；设置页显示离线且保留上次成功检查时间，未留下半成品。证据 `github-public-headers.txt`、`project-retry-status-ax.json`。未使用凭据或替代发行源绕开；本地不可变源的自动化结果不能替代该原生路径。
- 新版真实Codex与最终IDEA工具的 `turn/interrupt` 取消、同session查询恢复，以及项目关闭/重开已通过。关闭项目后Full GC，设置面板、Skill服务、文档索引服务、MCP服务和Run实例计数均为0。不是JNI热卸载结论；具体时序和调试器边界见[MCP生命周期记录](../2026-11-20-doc-view-mcp-tools/verification-2026-11-17-lifecycle.md)。
- 测试结束在原生设置关闭MCP Server，回读127.0.0.1:64343拒绝连接，隔离配置不再启用服务；命令执行确认保持开启，未点自动配置。证据 `mcp-disabled-readback.json`。
- 隔离IDEA随后通过原生退出确认正常结束，PID与本轮调试探针均不再运行，见 `final-environment-cleanup.json`。三个活动change的严格OpenSpec验证及 `git diff --check` 通过；本轮修改文档的137条相对链接目标均存在，见 `final-document-links.json`。

## 待完成

- 新包两范围官方源完整安装/更新重试、在途下载取消、更新取消以及原生IDEA安装journal重启恢复。两范围暂存取消已通过，不将聚合任务一并勾选。
- 新包手动程序切换后的自动检查、中英文/浅色/宽→窄→宽及键盘全部操作组合。
- 新客户端自然对话中的Skill调用与完整MCP发布边界/超时/重启生命周期；本轮真实Codex取消的工具选择由本地测试模型确定，不冒充自然模型验收。
- 最低build本轮没有重新启动原生UI；其本轮Verifier与历史原生证据分开保留。

活动change的聚合tasks保持未完成，不因局部检查通过而归档。
