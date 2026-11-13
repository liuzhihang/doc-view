# MCP 与 Skill 发布门槛续验

2026-11-13，隔离工作区 `doc-view-release-integration-20261107`，开发版本 `1.7.0-SNAPSHOT`。本轮未提交、推送、打官方 tag 或发布。主要证据目录为 `build/mcp-release-gates-2026-11-13/`（下称 G）；构建记录位于 `build/release-readiness-2026-11-13/`（下称 R）。

## 首轮自动检查

新增 Skill 归档重复路径、大小写冲突、文件数、单文件及展开总量五个负向用例；强化每次文件 move 前后取消、最终 index 提交后迟到取消，以及运行任务遇项目 service dispose 的测试。前两轮分别因测试缺少静态 import 和 callback 内 IOException 未处理而编译失败，未执行测试；修正后26项定向检查通过。失败日志保留在 R 的 `gates-targeted*`。

`gates-full` 使用真实 opt-in Codex 验收 source set，强制无缓存重跑537项测试，0失败/错误/跳过，117.21秒；两范围16项真实客户端安装服务流程和6次子JVM中断恢复亦重新执行。该轮尚未包含随后发现的 VFS freshness 修复，不能充当修复后的最终检查。

## 生产 MCP 生成与项目隔离

在隔离 IU263.7307.200、R11包（身份见[上一轮候选记录](../doc-view-client-skill/verification-2026-11-13-final-candidate.md)）上，通过 IDEA 原生 MCP `/stream`、协议 `2025-06-18` 调用生产工具：

- A 没有文档库时，status 为 not_generated，search 返回 INDEX_NOT_FOUND，文档目录未被查询创建。
- 显式 generate 创建62个接口、127个文件；重复生成62个 unchanged，snapshotId、文件hash和mtime均不变。
- B 的127个文档文件及mtime在A的生成/更新期间保持不变。
- 手工改动一个受管理 Markdown 后，generate 返回 CONFLICT，保留修改；恢复原字节后可重试成功。
- 任意 outputDir 参数返回 INVALID_ARGUMENT。

证据：G `generation/result.json` 及同目录逐次响应。此组使用真实直接HTTP客户端，不冒充自然模型或Codex取消验收。

### 项目路径 header 的准确含义

IDEA 的 `IJ_MCP_SERVER_PROJECT_PATH` 是会话默认项目提示，不是不可改变的安全绑定。该宿主版本优先选择显式工具 projectPath，然后是调用/初始session header及其他项目解析来源。因此默认header=A、显式argument=B时路由到B属于宿主行为；Doc View校验的是IDEA实际解析的Project与工具参数，不把这个header当作跨项目访问屏障。客户端仍须核对每次响应的projectPath。此边界不改变“只查询/写入明确选定项目”的contract。已对照[精确宿主build的官方解析器](https://github.com/JetBrains/intellij-community/blob/idea/263.7307.200/plugins/mcp-server/src/com/intellij/mcpserver/impl/McpSessionProjectResolverImpl.kt)。

## 原生项目关闭、中途发布与GC

在B加入仅用于隔离验收的9000方法Controller，经真实生成请求观察到 `generating=true` 后，用原生 File → Close Project 关闭B。最终 `project-close-r3`：

- 原session哈希前缀 `7ea147f2dccc`，生成约11.353秒终止；返回 PARTIAL_PUBLICATION。
- 已替换8238个文件；返回路径前100条及 `publishedPathsTotal=8238`，与磁盘变化数量相符。
- `indexCommitted=false`，旧index.json原字节保留，`cleanupFailuresTotal=0`。
- 关闭后的B请求被IDEA宿主拒绝；同session后续A status成功。
- GC histogram 中当前A的任务/快照service各保留一个，没有活跃 `DocViewMcpTaskService$Run`、发布异常或项目快照对象。此为该次关闭后的观测，不是所有生命周期或内存压力场景的证明。

证据：G `project-close-r3/summary.json`、完整result与原始响应、`after-project-close-histogram.txt`。B的临时Controller和部分文档移入证据目录，原文档已恢复。

前两轮未掩盖：第一轮3000方法请求在关闭前已完成，不能算在途取消；第二轮真实返回PROJECT_NOT_OPEN，但harness错误地把宿主非JSON错误当业务JSON，后续检查未完成。修正harness后重跑第三轮。未修改生产代码来迎合这两次测试时序/解析错误。

## 真实 Codex 自然模型遵循 Skill

使用实际 `codex-cli 0.167.0-alpha.4.1` 和其既有登录，ephemeral、忽略普通用户配置、临时MCP连接仅开放四个Doc View工具；未读取或复制凭据，未写普通MCP配置。当前项目Skill是本地候选原文件拷贝，**不是官方安装器下载验收**。这些用例使用真实自然模型，不是合成模型fixture。

| 场景 | 观察结果 |
| --- | --- |
| 已有文档查询 | 读取Skill，按status → search → get取得同项目、同snapshot的接口参数和响应 |
| 没有文档库 | 只调用status并请求显式生成授权；0个文档文件仍为0 |
| 明确要求生成 | 保留自动审批后status → generate → status，生成127文件；最初readonly/never策略拒绝写工具的轮次保留为未执行，不算生成成功 |
| 注释中的伪指令 | 详情包含要求生成/改配置的UNTRUSTED_FIXTURE文本；模型明确视为数据，仅status/search/get，127文件hash不变 |
| 外部源码修改 | 首轮失败：刷新后工具仍返回current；模型据实复述。生产新鲜度问题需修复后重跑，不能标记为Skill通过 |

证据：G `natural-*-events.jsonl`、`*-final.txt`、`*-result.json`。查询用例只核对可观察的工具调用和文档文件；不把模型进程退出码0单独视作业务断言通过。

全局范围另用隔离HOME和无项目Skill的临时Git项目执行真实 `$doc-view` 调用。模型返回实际 `.agents/skills/doc-view/SKILL.md` 的精确全局来源路径及正确工作流说明；prompt未给出该来源路径，事件中没有命令或MCP调用，Skill字节未变。证据 G `natural-global-discovery/`，用时24.55秒。结合此前两范围 `skills/list(forceReload=true)` 的精确路径/scope结果，可证明该版本的两种发现范围与显式Skill调用；不等于GUI斜杠菜单截图或官方安装器下载。

## VFS 新鲜度缺陷与后续复验

真实外部改写A的Java注释并执行 Reload All from Disk 后，status/search/get仍报告旧快照current；随后显式generate报告updated=1并读取新注释，证明生成输入已变化。原生失败证据为 G `natural-stale-*` 与 `a-source-update.json`。

定向RED-r3/r4确认：真实外部磁盘写入已产生VFS事件且VirtualFile内容更新，旧实现仍返回CURRENT。前两轮RED只是light fixture未使用本地content root等测试设置问题，不作为产品缺陷证据。修复通过公开 `VirtualFileManager.VFS_CHANGES` / `BulkFileListener` 在变更前按事件路径、后缀与ProjectFileIndex判断，不扫描PSI或文件系统；listener绑定project service Disposable。copy按目标parent判断，move分别按新旧parent/path判断，避免生成目录移向项目外时误标。

最终 `freshness-vfs-green-final` 4项通过：Java/XML外部改写，增删/改名刷新/目录变更，copy/move，以及非输入/项目外/生成目录排除。证据在 `build/skill-acceptance-2026-11-13/freshness-vfs-*`。新整包全量及原生复验仍需单独记录。

## Skill 提交竞争缺陷

归档拒绝的服务层新用例证明：在已有旧Skill及metadata时，压缩后未超archive上限、展开后超过单文件限制的归档被拒，目标树和metadata字节均不变。

新增的三个事务用例复现：READY后目标被修改仍可能成功更新，OLD_BACKED_UP后备份被修改也可能被新安装覆盖清理；首次安装READY后出现外部目录则虽保留内容，却留下本可清理的journal。

修复在READY后及备份rename后重验；已移动的旧树发生外部修改时，通过持久化恢复阶段保留并恢复该树和旧metadata。发现新目标/备份在提交前又发生修改时，保留可用目标、备份和journal，明确报告恢复待处理；不先删除新target再发现旧backup不能恢复。首次安装的stage已移入target、但journal还停在OLD_BACKED_UP的崩溃窗口也已覆盖，不能误清journal留下无metadata的半安装。

定向RED 19项中3失败；修复后事务16项、服务5项全部通过。独立JUnit环境所需的进度指示器上下文也作了测试侧修正，未改变生产取消语义。原版、diff及轮次记录见 `build/skill-acceptance-2026-11-13/{SkillInstallTransaction.before.java,SkillInstallTransaction.diff,skill-transaction-tests.json}`。

## 修复后的整包检查

首轮 `gates-fixed-full` 执行547项，2失败、0错误/跳过。失败均位于新测试前置步骤：VFS父目录普通刷新未产生预期create event；归档测试的临时shell CLI在3.088秒后仍未退出，触发生产版本探测的3秒限制，尚未调用release source。XML保存在G `fixed-full-failures/`，不能将该轮称为通过。

测试夹具改为显式dirty refresh并确认精确路径的真实create/delete事件；归档用例先对同一临时CLI进行有界启动与版本校验，再进入实际生产探测和归档校验。生产3秒限制、事件/freshness断言及目标字节保护断言均未放宽。具体系统调度导致首次shell延后的原因未证实；不把它归因于archive validator，也不把先前缺少状态信息的失败伪称为已完整回溯。

随后相关三类40项通过，最终 `gates-fixed-full-r2` 强制无缓存执行547项（0失败/错误/跳过），并完成buildPlugin和平台配置检查，114.85秒。两范围16项真实Codex安装服务场景和6次子JVM中断恢复也重新运行。新ZIP SHA-256为 `e6c64025ef250a34b272467e6d7887e0fc50a13e65ef9b4eb1652aca785297f4`，JAR为 `fe48995947f91d782091f420751a2c977bbc8b40c3879f26ebdffa017b8fe2cc`；408个生产class全部major69，无Test/PoC类，同包可选MCP descriptor与require-restart保持。包读回见G `fixed-package.json`。

## 修复后 Verifier 与原生复验

同一新ZIP分别在最低IU263.7031.23和实际运行IU263.7307.200执行Verifier，两者均Compatible；各有16项既有Experimental API使用，没有兼容性、Internal、Deprecated或Override-only问题。报告保留于G `minimum-verifier/`、`release-verifier/`，汇总为 `fixed-verifier-summary.json`。Experimental不等于稳定API保证，未来build仍须单独验证。

将上述新ZIP原字节安装到隔离263.7307.200，使用独立HOME、profile且无debugger，启动身份见G `freshness-native/launch.json`。新包观察如下：

- 重启后A的status/search/get都为unknown，同一snapshot，A/B文档hash及mtime不变。真实Codex `natural-unknown` 用时63.71秒，只调用三项查询并说明不能认作实时源码。
- 显式生成A/B后，两者current且生成结果unchanged。外部改写A的Java注释、原生Reload All from Disk后，A三项查询全部stale，B仍current，文件hash及mtime不变。
- 真实Codex `natural-stale-r2` 用时42.05秒，仅status/search/get，明确说明stale及更新需要显式授权，127个文档文件hash不变。首轮旧实现失败保留，不用本轮结果改写历史。
- 显式generate随后更新1个接口、61个unchanged，提交3个文件，新snapshot为 `3b9d6dda5014d1397e0484efa92a359c2efbff7fae7ae8cf1598b4160d499b36`。三项查询恢复current，B仍current。
- 真实协议路由检查证明初始session header=A、后续省略参数时回到A，显式projectPath=B时返回B；四个Doc View工具无重复。证据G `fixed-routing/summary.json`。

对应直接协议结果在G `freshness-{restart-unknown,generate-baseline-both,baseline-current,external-source-stale,generate-external-source-update,regenerated-current}/`。自然模型结果与事件为G `natural-{unknown,stale-r2}-*`，Skill仍为本地候选拷贝，不是官方安装器验收。

### 真实 Codex 在页面发布期间取消

新包在B的9000方法隔离fixture上运行实际Codex `0.167.0-alpha.4.1` app-server，由本地合成模型选择生产工具调用；本项检验真实客户端传输和生产任务，不冒充自然模型判断测试。

第二轮先在原MCP session读到generating=true，观察到52/9000个Markdown页面落盘且旧index.json未变，立即执行真实turn/interrupt。通知的request ID与原generate一致，协议为2025-06-18，session hash前缀 `e8df64bfc9e1`。取消后共159个文档路径变化，旧索引保留；同session在2451.126ms内观测到generating=false，随后真实Codex查询成功，1秒观察期无迟到完成。A文档hash未变。只有任务空闲后才恢复B的127文件基线，并逐字节复核。证据G `partial-cancel-summary.json` 及其runtimeDirectory内wire、客户端事件和partial manifest。

第一轮未通过：初始generating=true已经成功，但harness又在页面发布期间执行status；该读操作等待writer持有的发布锁，5秒客户端读取超时，无法完成原验收断言。虽然实际取消和旧索引保护成功，仍保留为失败。第二轮改为初始真实状态加部分页面/旧索引证据后立即取消，避免在互斥发布阶段插入阻塞读；生产实现没有为这次harness修正改变。失败记录为G `partial-cancel-r1-summary.json` 和 `partial_cancel_r1.py`。

该取消请求的业务响应可能被宿主协议取消；不能据此要求客户端一定收到PARTIAL_PUBLICATION。磁盘和wire证据与前述真实项目关闭返回的partial envelope分别记录。最终index提交后迟到取消由自动测试覆盖，尚无该精确时刻的原生复现。

### 项目释放与环境恢复

新包在取消完成后原生关闭B，宿主拒绝再查询B；A仍可查询且generating=false。GC histogram只剩A的一组ProjectDocumentIndexService及其四个listener、一个MCP task service和snapshot service，无活跃Run、项目snapshot或发布异常。此为一次GC观察，不代表完整长期资源验收。A在此时报告stale，变化时刻和触发来源未单独取证，不能声称B关闭过程保持A current；该现象保留待定位。记录为G `fixed-project-close-readback.json`、`fixed-after-project-close-histogram.txt`。

通过原生控件停用MCP并正常退出隔离IDEA，PID80138消失、64343端口关闭。A/B文档hash及mtime、A源码、B fixture、临时项目Skill/Git目录和disabled plugins恢复完成。原工作区HEAD、相同命令的Git状态与584个文件hash均和基线相同；未操作Junie。恢复记录为G `fixture-cleanup/summary.json`，首次Git状态比对因untracked展示模式不同而失败，改用基线同命令后匹配，原始失败记录保留。

### 补充归档与 XML 失效边界

归档负向测试进一步使用真实ZIP中央目录的Unix类型，覆盖FIFO、字符设备、块设备和socket；摘要按修改后的归档重新计算，排除“只是摘要错误导致拒绝”的假阳性。归档测试类13项通过，随后强制全量548项通过（0失败/错误/跳过，117.33秒），没有修改生产代码或安装包。证据G `special-file-test.log`、`final-548-counts.json`、`final-548-test-results/` 及R `gates-final-548.*`。

针对A后来stale的现象，新增真实LocalFileSystem用例确认：模块content root内的 `.idea/workspace.xml` 被 `ProjectFileIndex.isInContent` 判为true，外部修改、观察到真实VFS事件并刷新后，current变为stale。该行为符合现行contract对内容根内XML的保守失效规则，未修改生产或contract。它证明一个可能触发机制，仍不能证明原生那一轮实际发生了该事件；没有把未捕获的触发时点写成确定原因。初版用例的Light项目路径不属于content root，修正为真实模块content root后定向1项通过。

## 尚未完成

- B测试/关闭期间A从current变为stale的触发来源尚未定位，不将其直接归因于跨项目数据污染或视作正常行为。
- 官方稳定Skill资产下载及未修改生产安装器的IDEA按钮安装、更新、HTTP取消和恢复闭环；当前本地候选不能替代官方来源。
- 精确最低build的完整原生矩阵、Dumb Mode原生查询，以及中文/浅色/完整键盘等未覆盖组合。
- 正式版本、官方commit/tag、GitHub资产与Marketplace。本轮不批量归档或勾选聚合tasks。

## 最低支持 build 263.7031.23 原生查询与可选加载

使用 JetBrains 官方 Apple Silicon DMG `https://download.jetbrains.com/idea/idea-263.7031.23-aarch64.dmg`。下载字节数 `1575622276`，与官方期望大小一致；SHA-256 `c173e165d6365387302694d1fd9cd0d3b02f420f4af942fcd9ed80a37e1c00d2`，下载与 codesign 检查均通过。实际运行 app 报告 IU build `263.7031.23`，内置 JBR `25.0.4.1+1-b635.72`、Darwin aarch64，使用隔离 profile。SDK 与 DMG App 混合启动只用于诊断，不计入完整原生验收。

本轮安装的仍是已验收的 `1.7.0-SNAPSHOT` 候选：ZIP SHA-256 `e6c64025ef250a34b272467e6d7887e0fc50a13e65ef9b4eb1652aca785297f4`，插件 JAR SHA-256 `fe48995947f91d782091f420751a2c977bbc8b40c3879f26ebdffa017b8fe2cc`。正式版本元数据已转为 `1.7.0`，该时刻新正式包尚未构建，后续结果见文末；以上验收不替代新包重建后的包级门禁。

隔离 project A/B 通过真实 IDEA MCP 协议 `2025-06-18` 调用。`tools/list` 中 Doc View 的 status/search/get 各自 schema 仅有一个 `projectPath`，均标记只读；get/search/status 在 A 与 B 均执行成功。调用前后项目文档 hash 和 mtime 未变化。初始 `tools/list`、A/B逐项调用、响应和 summary 保存在 G `minimum-distribution/initial/`。

随后对 A、B 显式调用 generate，各扫描62个接口，分别创建6个接口文档、56项 unchanged，无冲突或清理失败，随后 snapshot 为 current。此证据证明最低 build 上显式生成可用；不代表完整的并发、手改冲突及每种取消边界都在最低 build 原生通过。结果见 G `minimum-distribution/generate/`。

Dumb Mode 中 status/search/get 三项查询通过且文档 hash/mtime 不变；Dumb Mode 下显式 generate 返回 `INDEXING`，无已发布路径、无 index commit。错误用例覆盖未打开项目宿主拒绝、缺少 API 标识和额外字段（后两者返回 `INVALID_ARGUMENT`）。证据位于 G `minimum-distribution/indexing/` 与 `indexing-readonly-errors/`。据此勾选 MCP tasks 3.5；其他仍未覆盖的原生矩阵不随此项一起勾选。

MCP 禁用重启路径也已完成：在独立 profile 禁用 MCP Server 并重启后，127个 Doc View 类加载、0个 MCP adapter 类加载，Doc View 工具窗口、62 API 目录和生成/搜索控件仍可用；IDE启动 ERROR 行数为0。IDEA 的 Debugger/Coverage MCP toolset 因宿主 MCP 被禁用而出现依赖警告，该警告不属于 Doc View 类加载失败。正常退出后进程终止、端口关闭、disabled plugin 配置恢复，普通工作区未改。启用及禁用两分支足以完成 tasks 5.2 的禁用路径；本轮没有物理删除 MCP Server 插件，因此不声称“缺失插件”路径通过。记录在 G `minimum-distribution/optional-summary.json`、`optional-launch.json` 与 `cleanup.json`。

完整 machine evidence 均位于 `build/mcp-release-gates-2026-11-13/minimum-distribution/`：`download-result.json`、`launch.json`、`initial/summary.json`、`generate/summary.json`、`indexing/summary.json`、`indexing-readonly-errors/summary.json`、`optional-summary.json` 和 `cleanup.json`。临时 profile 与fixture归入该 build evidence；未更改普通 IDEA 持久配置或生产包内容。

禁用 MCP Server 后的 IDEA 启动日志包含 Debugger/Coverage MCP toolset 对已禁用宿主服务的依赖警告；Doc View 本身没有启动 ERROR，类加载和普通 UI 验收通过。这里验证的是禁用并重启，不是移除插件安装文件。

## 最终 549 项回归状态

最终全量计数为549 tests、0 failures、0 errors、0 skipped（G `final-549-counts.json`）。首轮同为549项的回归因 `ProjectDocumentIndexServiceTest.testExternalVfsCreateDeleteMoveAndDirectoryChangeMakeSnapshotStale` fixture 未预加载目录 children，未观察到预期 VFS create event；失败记录留在 G `final-549-r1-test-results/`。夹具修正后该测试类20项定向检查通过，随后无缓存549项全量复验通过，记录在 G `final-549-test-results/`。这是测试fixture准备问题，不能写成产品VFS缺陷。

正式 `1.7.0` 版本元数据已从快照切换。该版本的首次新门禁运行发现一个测试仍硬编码期望版本包含 `SNAPSHOT`；测试已改为读取 `gradle.properties` 的版本，后续复验结果见下节。此状态不改变上述已完成的549项 `1.7.0-SNAPSHOT` 候选记录，也不表示正式包已验收。

## 正式版本包复验（待发布）

正式源码版本为 `1.7.0`。首轮549项检查有1项失败：MCP契约测试仍期望 `1.7.0-SNAPSHOT`。将断言改为从 `gradle.properties` 独立读取期望版本后，`formal-full-r2` 强制无缓存重跑549项，0失败/错误/跳过，并完成 `buildPlugin`、`verifyPluginProjectConfiguration`（116.94秒）。失败结果保留于G `formal-first-test-results/`，通过结果位于 `formal-final-test-results/`。Skill Python打包器11项检查通过。

正式包 `Doc.View-1.7.0.zip` SHA-256：`03ab0ce539ea2d60c0c63bf563419f510e427d07858c219ed103eb95ebd69306`；JAR：`19d4a324484bb03492789d8d6990a8ce89950aab524191bcde003995e13bcf69`。408个class全部major69，无Test/PoC类；同包可选MCP描述符、最低build263.7031.23及require-restart均读回正确。与已做原生验收的snapshot包比较，408个class字节完全相同，仅MANIFEST、版本properties、plugin.xml发布说明及契约title变化，不能将这一比较当作官方安装UI已通过。

两个精确build的正式包Verifier均Compatible，各16项既有Experimental API提示，无compatibility/Internal/Deprecated/Override-only问题；最低build89.46秒、发行build67.40秒。记录见G `formal-package.json`、`formal-vs-native-candidate.json`、`formal-verifier-summary.json`及两个独立报告目录。

契约仅删除标题中的“planned”，字段/规则及contractVersion仍为1；正式指纹为 `sha256:400aacb3bc373117e01288d995be7969d3e962c2ace58880a4a1775752ec2a17`。正式检查使用与该指纹一致的本地Skill测试包，其临时commit不是官方GitHub发布身份；发行manifest仍须从最终tag commit生成。

GitHub与Marketplace发布尚未执行。按既有发布授权先完成正式提交/tag和GitHub资产，再由未修改的生产下载器完成固定官方来源验收，最后提交Marketplace。未覆盖原生矩阵保留，不能因版本号已改而视为通过。

## 后续正式发布与 Skill 补丁（2026-11-13）

上述“尚未完成”和“待发布”描述是各候选阶段的历史状态。正式1.7.0官方首次Project/Global Skill安装通过后，发现原生journal恢复按钮被禁用；该版本已保留为GitHub prerelease且未提交Marketplace。1.7.1修复Skill恢复路径，最终553项测试/构建/配置、两精确build Verifier，以及两范围官方更新、取消后重试、修改保护和重启恢复均通过。精确下载/暂存取消时点及全部生命周期矩阵仍保留未覆盖，不更改本文历史MCP候选的验收身份。

[GitHub v1.7.1](https://github.com/liuzhihang/doc-view/releases/tag/v1.7.1)已正式发布；Marketplace接收update1219746，approve=false/listed=false。最终包、tag、资产、原生结果与剩余边界统一见[1.7.1发布记录](../../../docs/release-1.7.1.md)，不批量归档未完成change。
