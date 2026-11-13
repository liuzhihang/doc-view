# 2026-11-12 MCP/Skill 续进

- 公开 Marketplace API 回读 1.6.0 / 1215457：approve=true、listed=true，since=263.7031.23。证据 `build/mcp-skill-2026-11-12/marketplace-1.6.0-status.json`。
- 原工作区已按本轮开始时的 HEAD、所有 tracked/untracked 非忽略文件 SHA 冻结，未重置或迁移。当前隔离分支从最新 develop `aa4a696`（已有1.6.2）创建。Junie 不操作。
- Java PoC 已归档；1.6.0 最终构建430tests和两个Verifier、文档库代表性烟测见发布记录，后续1.6.2已有独立发布记录。本轮适用最新基线，保留旧完整原生验收缺口。
- 本轮明确开发授权对应 proposal/design/spec/tasks 的窄范围准入修订；不将历史失败勾成通过。MCP 新运行时仍须独立通过协议/取消/隔离验证；Skill源包与安装验证不等于正式Skill发行。

## 验证进度

实现与验证进行中，未执行项不计通过。


### 只读生产注册与实际调用（首个候选）

- Java 25 / 263.7031.23 SDK 定向22项测试、buildPlugin、verifyPluginProjectConfiguration通过。先修复测试本地目录/后台线程使用，再修复真实not_generated entryCount=null问题为0。复验日志mcp-read-candidate-r4。
- 首个仅注册status/search/get候选ZIP SHA-256 `b1a5c6c214ab284e9e6bf0043fce3f224417e9ff991d21449d8c320e1938757b`，JAR `e8ee03f72c4dee9f6b926d3f852342108644ee6889095a1b90c8657c707b7c45`。隔离官方IDEA263.7307.200载入1.7.0-SNAPSHOT；没有将这个原生结果外推到最低263.7031.23。
- 已启用原生MCP Server（保留命令确认），HTTP Stream initialize协商2025-06-18，tools/list真实返回三个工具。MCP直连未绑定项目时projectPath出现一次；指定header项目时宿主移除该字段，adapter通过公开call project补回。
- 当前Codex会话实际发现并调用三项生产工具：status ready/57条、search分页2条、get同snapshot含Markdown862字符；freshness=unknown如实返回。结果为`native/protocol/codex-*.json`。
- 两个真实打开的虚构Project，各117个文档文件。独立HTTP协议客户端完成13项调用：A/B状态和详情、绑定参数省略、显式选择B、未知参数、过期snapshot、超限limit、空cursor、B手改Markdown拒读及同时A不受影响。所有Text(JSON)结果按schema校验，恢复故意修改的B测试文件后全部234个文件hash与调用前一致。
- 初始harness误把会话header当作不可变项目锁，实际宿主明确projectPath优先并将call project更新为B；核对同build源码后纠正测试预期，保留routing-observation.json。插件一致性检查比较“本次调用已解析的项目”，不宣称session固定项目。真实wire不输出additionalProperties，额外字段由adapter拒绝。
- 至此满足先真实只读调用再接写入口的顺序，继续注册generate；本轮仅只读候选不覆盖写入、目标Codex turn取消、Dumb Mode原生或最低build原生，3.5/4.5/5.x不据此整体勾选。

### 四工具候选、问题修复与生成验收（2026-11-13）

- `full-candidate-r2` 实跑494项，4项失败；不是全量通过。失败分别是：英文资源测试意外回退系统中文、损坏索引status的null隐式拆箱、Skill文件hash不匹配误报为干净、恢复fixture未规范化macOS `/var`路径。修复后 `fixes-r3` 定向28项、buildPlugin、verifyPluginProjectConfiguration通过。全局Skill交互继续调整，最终整包全量复验另记。
- 修复共享发布结果：页级created/updated/unchanged在writer lock内根据实际文件状态计算，MCP返回本次真实提交的snapshotId；提交后的可选状态刷新失败不把已提交写入改报失败。status的generating包含UI与MCP共享生成guard，已加回归。
- 四工具候选ZIP SHA-256 `064d16b578bc6bd5d503aa0178b252342c894a57be833d686620fde336ed61ec`；JAR `15b4dab01426676372602ee74aecf7a92b59cb0171584ac40404a1fe0329b638`。隔离263.7307.200重启加载，tools/list实测status/search/get/generate四项。
- 当前Codex通过IDEA公开 `execute_tool` 路由实际调用 `doc_view_generate --projectPath <project-a>`：成功提交62条，created5/updated55/unchanged2，freshness=current。后续HTTP协议客户端7项通过：A重复生成不改bytes/mtime；B未生成状态、首次生成62条、重复无变化、搜索、手改冲突保留全部原bytes、解除冲突后重试。B操作不改A，A操作不改B。证据 `native/four-tool-candidate/codex-generate.json`、`write-acceptance.json`。
- 第一轮B harness在重启后未重新打开B，宿主返回非envelope的“项目未打开”错误。已恢复被临时移走的B文档，在隔离原生UI重新打开B后完整复验通过；不把该宿主错误写成Doc View解析错误。
- 四工具中间包的Plugin Verifier1.411在263.7031.23 SDK上结果为Compatible；16项Experimental均为已有FileEditorOpenRequest链路，无Internal/Deprecated/Override-only或兼容性问题。SDK布局缺失提示仍保留在日志，不能外推到未来build。最终含两范围Skill的整包仍需重跑Verifier。
- 10,000-entry共享查询fixture在Java25/IU263.7031.23下30次warm search p95=20.996ms；报告 `r2-performance.json` 标记light-fixture，这不是原生IDE性能结论。
- 第一轮当前0.167客户端到生产generate的取消及同session恢复已通过协议检查：`tools/call id=2` 发往实际隔离IDEA，`turn/interrupt` 后通知 `requestId=2` 匹配；同一Codex app-server/thread/MCP session继续调用status id=3，返回generating=false，恢复turn完成。B的127个文档文件逐字节未变。证据 `codex-cancellation/runs/20261112T162901Z-20846-b413a48d/result.json` 与双侧事件。此轮尚未在取消前读取generating=true，因此最终包复验还需补上在途状态与迟到结果观察。
- 取消harness使用真实Codex 0.167.0-alpha.4.1的MCP客户端和实际IDEA生产工具，本地合成Responses provider仅提供预定工具调用，不使用模型账户或凭据；不代表自然语言模型或Skill工作流验收。临时配置只为generate/status两个测试工具设置allowlist和scoped approval，命令仍为on-request，没有扩大普通Codex设置或权限。原始热卸载门槛仍按已批准require-restart策略处理。

### 双范围 Skill 整包与原生反馈（2026-11-13）

- `full-candidate-r4` 使用Java25、263.7031.23 SDK实跑510项，0失败、0错误、0跳过；buildPlugin与verifyPluginProjectConfiguration通过。新增覆盖包含MCP非规范/无效项目路径、Skill服务无发行包检查不写目标/metadata/锁、离线保留字节、后台CLI发现与dispose迟到回调。
- `native/global-ui-candidate` ZIP SHA-256 `0f6e5397ce727805c2ec303d8f6f35f4bb9a6a136c33c5474fae3ff653819d30`，JAR `1472eeb8b4b8346f90a4b90bf5ce116ceb0b44da2fcf5f4e0d9a9e5f7f86bf73`。只有一个生产JAR、406个class、全部major69、契约指纹一致、optional MCP descriptor与require-restart均读回正确，无PoC/Test类。
- 该包在263.7031.23（Gradle verifyPlugin）及263.7307.200（官方安装包+Verifier1.411）均Compatible，只有16项既有Experimental引用，无Internal/Deprecated/Override-only/兼容性错误。最初并行运行两个Verifier共享默认提取目录，引发DirectoryNotEmptyException；待另一个结束后顺序复验通过，未改动生产API，保留失败日志 `final-minimum-verifier.log` 与通过的 `final-minimum-verifier-r2.log`。未来Verifier须顺序运行或使用独立缓存。
- 隔离263.7307.200原生已确认：自动检测本机Codex候选、默认隐藏程序路径选择、键盘选择Global时目标变为当前用户的固定`.agents/skills/doc-view`；Global检查更新确认实际0.167.0-alpha.4.1并提示没有兼容正式Skill包，安装按钮禁用；检查前后Skill目标、metadata与writer lock均未创建。
- 原生发现并待修复：ActionLink禁用时默认自动隐藏，导致检查期间按钮跳位；完成后Codex版本/目标路径重复显示；Global按钮tooltip仍误称project path；设置窗口从1800×1400放大到5120×2706再恢复时，平台外层滚动容器保留大宽度，造成横向滚动条与右侧裁切。自动布局测试只覆盖内层pane，不能证明该原生场景通过。修复包需重新构建、Verifier并原生复验，本包不标为最终UI通过。

### 最终 UI 修复包（2026-11-13）

- `full-candidate-r5` 的512项测试中，一个人为包装 Settings 的 headless 模拟断言失败；它没有复现原生平台宽度缓存，已删除该伪复现，保留单滚动层与宽窄布局边界检查。该失败记录不改写为通过。
- `full-candidate-r6` 强制重跑且不使用 build cache：511 tests，0 failures/errors/skips；`buildPlugin`、`verifyPluginProjectConfiguration` 通过。包名1.7.0-SNAPSHOT，ZIP SHA-256 `06f16f0efac61e240972b2a89b76356715237a43cfac545b385f6816cdaadc16`，JAR `956905a6c90c55d67e2a02c438bdf84d0ffcc035e7e6e953b916e1a6917d6ce3`。单个生产JAR、406个class、全部major69；optional MCP descriptor、require-restart、契约指纹及无PoC/Test类均读回通过。
- 同一包顺序通过263.7031.23与263.7307.200的Plugin Verifier，均为Compatible，16项既有Experimental引用，无Internal/Deprecated/Override-only/兼容性问题。日志为`fixed-minimum-verifier`、`fixed-release-verifier`；最终冻结源码hash与build/distributions包再次核对一致。
- 原生263.7307.200复验确认：自动CLI检测、项目/全局固定目录、只读目标、稳定的检查按钮位置、无重复Codex版本；Settings放大再缩小后内容重新适配，左右滚动无整页偏移。检查更新OFFLINE后显式重试恢复，无兼容稳定Skill包时写按钮继续禁用。详见[设置页记录](../doc-view-client-skill/verification-2026-11-13-implementation.md)。

### 最终包的目标客户端取消与收尾

- 同一最终JAR在隔离263.7307.200 / PID99134运行。真实ChatGPT.app随附 `codex-cli 0.167.0-alpha.4.1` app-server连接实际IDEA生产generate工具，客户端请求、服务端选择及HTTP header协议均为 `2025-06-18`。
- 在同一MCP session通过独立status探针读到 `generating=true` 后执行 `turn/interrupt`；generate `tools/call id=2` 与 `notifications/cancelled requestId=2` 匹配，turn为interrupted。同一个app-server进程、thread和MCP session后续由Codex调用status `id=3`，返回 `generating=false`，恢复turn正常completed。1秒观察期未见迟到的generate item/turn完成；该时长不能代替历史PoC的31秒观察，也不证明无限期无迟到响应。
- project-b文档库127个文件前后完全一致，未进行恢复写入；项目A未被取消harness触碰。机器结果为 `codex-cancellation/runs/20261112T171453Z-39890-4d9dc84d/result.json`，保留wire与Codex事件。使用本地合成Responses provider只为驱动实际Codex工具客户端，不使用模型账户或凭据，不代表自然语言模型或Skill指令遵循已验收。
- 首轮独立status探针因只发送 `Accept: application/json` 收到HTTP406，缺少取消前在途证据；对照Codex实际请求改为 `application/json, text/event-stream` 后完整复验通过。生产代码未为该测试错误改动，失败轮次仍保留。
- 最终包再次通过四工具 `tools/list` 及两项目各status/search/get共6次schema校验；A/B各127文件前后hash一致。结果见 `native/global-ui-fixed-candidate/readonly-final-smoke.json`。本次launch日志无ERROR；不将此前项目的历史错误指示器计作本包新异常。
- 原生取消勾选临时MCP Server开关并保存，正常退出隔离IDEA。readback确认PID99134已退出、64343端口关闭，见 `native/global-ui-fixed-candidate/cleanup.json`。没有操作普通IDEA或Junie。

仍未执行：精确最低263.7031.23的生产包原生、MCP组件缺失/禁用时的可选加载原生、Dumb Mode完整矩阵、发布各move/commit边界的真实取消、在途关闭项目/重启生命周期、自然语言Skill工作流。4.3–4.5、5.2–5.3按完整任务口径保留未完成，本轮通过不等于整个change已可归档或正式发布。

最终文档检查：两个change的status与strict validate通过，112个本地相对链接有效，`git diff --check`通过。原工作区与本轮开始时的HEAD、584个tracked/untracked非忽略文件hash和Git状态一致；没有提交、推送、打tag、发布、sync或archive。证据为`final-openspec-checks.json`、`final-relative-links.json`、`original-workspace-final.json`。

## 2026-11-13 最新 r11 包复验

最新ZIP `2a8adad9610058f1b94466a0b755b7f6515d05e042613dc2d948e1310a37e95e` 完成531项全量检查和两build Verifier。宿主MCP禁用时Doc View设置/目录加载，启用后A/B只读6次调用不改254个文件。真实Codex取消出现一次“通知已到、立即状态仍生成中”的时序失败；保留失败并修正harness为有界观测异步清理，两次分别5.703/70.351ms恢复，随后Codex同session查询false、文档不变，各1秒内无迟到完成。生产取消实现未改，不能据此勾选发布边界和项目关闭全部生命周期任务。完整证据及收尾见[最新候选验收](../doc-view-client-skill/verification-2026-11-13-final-candidate.md)。
