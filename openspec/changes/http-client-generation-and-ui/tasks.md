# Tasks

2026-11-08 最新续验完成 5.4，累计 **35/41**；派生缓存双向隔离、不同项目设置读回、HTTP 拒绝/写入失败/Dumb Mode/源码变更与删除，以及 UI/GC 子场景见[续验记录](verification-2026-11-08-remaining-native.md)。4.6 的取消/重试/在途关闭、冷/热各七次等见[上一轮记录](verification-2026-11-08-http-cache-search.md)。整体原生矩阵及发布收口继续保留，未发布。

本列表记录实施与验收任务；2026-10-23 用户已明确启动本 change 的 apply。P1 的原生/自生成是互斥选路，不需要实现两个后端；第 3–4 组实现选定路线，不能跳过失败项或把未选分支的未运行检查记为通过。

对应行为与验收编号见 [design](design.md) 及两份 specs。第 1 组完成后，第 5 组的布局准备可与第 2 组并行；HTTP 最终文案和第 3–4 组必须等待 2.7 决策。

第 2 组为候选评估：仅当已有上游硬拒绝证据时，下游候选任务可用“评估不适用 + 证据引用 + 未运行子检查清单”完成评估，不声称未运行检查通过；环境受阻不能使用此出口。选定生产路线在第 3–6 组的必需验收不可因此跳过。

实施时使用以下命令组；H/L/U 测试类已建立，若调整名称，须先同步本文件且核对真实测试数：

```bash
# H：HTTP 语义与支持范围
./gradlew test --tests 'com.liuzhihang.doc.view.ui.window.HttpRequestGenerationContractTest' --rerun-tasks
# L：HTTP 后台、取消、写入和身份
./gradlew test --tests 'com.liuzhihang.doc.view.service.http.HttpRequestLifecycleTest' --tests 'com.liuzhihang.doc.view.service.http.HttpRequestProjectIsolationTest' --rerun-tasks
# U：动作布局、状态和本地化
./gradlew test --tests 'com.liuzhihang.doc.view.contract.DocViewActionLayoutTest' --tests 'com.liuzhihang.doc.view.contract.ClearCacheActionTest' --tests 'com.liuzhihang.doc.view.contract.PluginActionLocalizationTest' --tests 'com.liuzhihang.doc.view.contract.PluginDescriptorLocalizationTest' --tests 'com.liuzhihang.doc.view.contract.PluginIconContractTest' --rerun-tasks
# R：受影响的既有文档与异步基础回归
./gradlew test --tests 'com.liuzhihang.doc.view.contract.SpringMarkdownContractTest' --tests 'com.liuzhihang.doc.view.contract.OpenApi3ParameterMetadataTest' --tests 'com.liuzhihang.doc.view.ui.window.MethodDocScratchPreviewTest' --tests 'com.liuzhihang.doc.view.utils.DocViewBackgroundTasksTest' --rerun-tasks
# G：最终集成门禁
./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks
# S：规划、规范和差异校验
openspec validate http-client-generation-and-ui --strict
openspec status --change http-client-generation-and-ui --json
git diff --check
```

## 1. P0 基线与失败证据

- [x] 1.1 读取 apply context、当前 Git/Gradle/descriptor、关联 active change；保存本轮 literal 文件范围与 `git status --short`/HEAD 基线，在验证记录中列出原有未提交改动和不触碰范围，确认 runtime apply 已获授权后才改代码。
- [x] 1.2 建立 H/L/U 所需最小 fixture 与受支持/不支持样例，先对旧生产 HTTP 路径执行 H/U 的缺陷用例并记录实际 RED；核实失败针对参数位置、header、Content-Type、空示例或节点误展开，而不是测试环境错误。
- [x] 1.3 固定源码/模型回归基线并运行 R，记录真实 suites/tests、失败/跳过数和生成输出 hash；HTTP wire name/required 测试与现有文档 Param.name/required/reqParamKV 断言分开，不先改旧 golden 来消除失败。
- [x] 1.4 准备虚构样例和隔离 IDEA profile，记录实际 SDK build/config/system/plugins/log；验证普通 profile 未改变，使用 `doc-demo` 时冻结并核对文件及设置基线，更新 verification 的 A/L/U 对照表。

## 2. P1 原生 HTTP Client PoC 与路线决策

- [x] 2.1 新增不进入生产 source set 的 `tools/http-client-poc/` Java 探针及 README，显式支持目标 IDE、输出目录和 `--verify`；验证 `python3 tools/http-client-poc/run.py --help` 可运行，说明构建/安装/启动/验收分离，不自动发送请求或改普通 profile。
- [x] 2.2 从目标 SDK 记录候选 Spring→URL context→生成器/Action 的完整调用链、类/包 API 状态和精确 plugin/module ID；运行探针的目标编译与隔离包检查，实际命令/SDK/输出路径写入 README 和 verification，不能以 `public` 或单个方法无 Internal 判定整链可用。
- [x] 2.3 在隔离目标 IDEA 安装探针并核对实际加载日志，从 Doc View 等价方法选择执行原生候选；分别验证 Spring 和 Feign 的 A01–A08/A11/A12，保存生成文件与实际结果，单纯打开 HTTP Client 不算通过。
- [x] 2.4 验证原生候选对 A09/A10 不支持输入及 A07 头部冲突的反馈；确保外围校验可以完整拒绝不支持形式，结果不得遗漏参数后报告成功，逐项记录支持/拒绝/未验证。
- [x] 2.5 验证原生候选 L02–L06：重载/项目隔离、重复点击、已保存和未保存编辑保护、取消/源变化/关闭项目、写入失败及依赖缺失；记录可观察完成边界，无法管控的 fire-and-forget 行为作为准入失败证据。
- [x] 2.6 执行 `python3 tools/http-client-poc/run.py --ide-home "$DOC_VIEW_HTTP_POC_IDE_HOME" --output-dir "$DOC_VIEW_HTTP_POC_OUTPUT" --verify`，变量先按 design/README 指向核实的 SDK 和隔离输出目录；记录候选 ZIP 的配置、打包、Verifier 与加载边界证据。确认实际验证的是探针 hash 而非主工程旧包，且探针不混入生产包；上游硬失败时按候选评估出口记录，不放宽门禁。
- [x] 2.7 根据 design 的五项准入条件形成唯一决策并填写 verification：全通过采用原生，否则以实际失败证据采用自生成；环境未验证不作为不可行证据。同步 design 的依赖、加载方式、最终文案和选定实现，运行 S，确认没有运行时 fallback/双模式任务。

## 3. P2 请求语义实现与回归

- [x] 3.1 依据 2.7 接入唯一生产实现，建立最小无 PSI 请求快照或原生语义适配，显式声明实际使用的依赖；运行 H 的基本 Spring/Feign 用例及 `./gradlew verifyPluginProjectConfiguration buildPlugin`，确认无第二套生产生成器。
- [x] 3.2 实现/核对单有效 mapping、path/value、编译期常量和类方法组合；拒绝多路径/method、动态/继承/歧义映射、通配/正则和非空 params/headers 条件，执行 H 的 A04/A09 RED→GREEN。
- [x] 3.3 实现 wire name/location 与文档字段分离，Spring/Java 名称优先，Path 不混入 query；执行 H 的 A01/A05，包含 `@Parameter.name/in` 冲突与路径占位符缺失/重名负例。
- [x] 3.4 实现标量 example/defaultValue/Spring required、空 default/哨兵、可选省略与可选路径变量保留；执行 H 的 A06，断言无 Java 空引用生成的 `null`，文档 required 配置不改变 HTTP 请求。
- [x] 3.5 实现 URI/form 的 UTF-8 组件编码、变量保留与名称消歧；执行 H 的 A01/A03，覆盖 `&`、空格、中文、路径值斜杠，解码后值一致且没有额外参数或双重编码。
- [x] 3.6 实现 Header、JSON 和 form body 组合及有效 consumes；执行 H 的 A02/A03/A07/A11，覆盖类级继承、方法级覆盖、Content-Type 匹配合并/缺值推导/冲突拒绝、无 body 不自动加头、认证变量与 CR/LF 拒绝。
- [x] 3.7 完成不支持 body/绑定/媒体类型的明确失败路径及源码当前编辑内容读取；执行 H 的 A08/A10/A12，确认不扫描全项目、不渲染响应或整份 Markdown、不发布部分请求。
- [x] 3.8 将 H 全套转为 GREEN 并执行 R；对共享 PSI 的改动补齐对应 Spring/Feign/Dubbo 回归。更新相关用户指南的支持范围与“生成后主动执行”步骤，写入 Unreleased 的实际 HTTP 行为变化并验证文档链接。

## 4. P2 后台生命周期与草稿保护

- [x] 4.1 复用受管后台 read task 或经 PoC 证明等效的原生调度，捕获所属项目/方法并限制 action update 工作；执行 L，验证后台解析、当前编辑内容提交到 PSI且不强制保存磁盘。
- [x] 4.2 实现同方法在途合并和请求代次检查；执行 L02/L03 的自动用例，包含两个项目、同标题重载、编辑器/目录焦点不同和选择变化，身份与发布目标不得串用。
- [x] 4.3 实现发布前取消、源失效/变更和项目关闭检查，确保取消透传且不按普通失败通知；执行 L04 的可控延迟用例并检查 Scratch/Document 写入次数为零。
- [x] 4.4 实现每次完成后再生成的新草稿/独立新块策略，保护已保存和未保存旧内容；执行 L03/L05，注入创建/写入失败并核对旧内容逐字不变、不打开或遗留本次残缺请求；完整 UTF-8 请求超过平台可编辑上限时明确拒绝，保持旧草稿不变。
- [ ] 4.5 对依赖禁用、Dumb Mode、空 project/失效 PSI、后台异常和再次重试执行 L；在目标隔离 IDEA 验证原生 L01–L05，将自动与可见终态分别写入 verification。
- [x] 4.6 更新用户指南的重复生成、取消、不支持和失败说明；用固定大模型完成 L06 冷/热各 7 次观测、取消/关闭后任务与监听器检查，记录原始样本和 p50/p95，不以字段置空声称 GC 或性能提升。

## 5. P3 UI 精简与原生交互

- [x] 5.1 先建立主工具栏、目录菜单、窗口菜单的 U 失败断言，再将设置/清缓存移入原生 `…`，保留五个常用入口和单一导出下拉；U 验证分隔线、action identity、平台窗口菜单及既有快捷键，目录不再有项目级清缓存。
- [x] 5.2 调整 Markdown/HTTP action 的轻量 update 和执行保护，非方法节点无目录展开副作用，Dubbo 保留文档而无 HTTP；U 覆盖类/模块/空/有效/失效节点、无 editor、Dumb Mode 与依赖缺失。
- [x] 5.3 按 2.7 的选定路线统一 HTTP 名称，更新“打开 Markdown 文档”、中英文 tooltip/不可用/取消/错误消息，复用平台图标；执行 U 的本地化/图标回归，确认不修改品牌 SVG 或另增生成模式。
- [x] 5.4 核对移动后的设置/清缓存读取所属窗口项目，保留既有缓存安全语义；执行 U03 的 A/B 项目自动与原生场景，HTTP 草稿、手写 Scratch、导出文档及未保存编辑均保留。同一 d094 候选补齐 A/B 副本的公开 EntryData 双向重载/保留、非空重复查询及 152 个文件哈希保护，A/B Merge exports 不同值从真实窗口入口读回。未直接读取私有搜索投影身份；旧 clear-cache 目录/工具栏入口按新 U03 替代，不另称旧入口通过。详见[续验](verification-2026-11-08-remaining-native.md)。
- [ ] 5.5 在目标 IDEA 完成 U01/U02/U04：中英、深浅、Compact、窄窗、键盘菜单，Enter/双击、源码导航、预览/复制/导出及上传菜单取消；保存实际观察和必要截图，不进行真实平台上传。
- [x] 5.6 同步用户指南中的入口名称/位置和 Unreleased UI 变化，更新 roadmap 的实际进度；核对本地链接、术语及全部入口一致，运行 S 与 U，保留未验证项。

- [x] 5.7 按 2026-11-07 用户追加要求，完成全部 icon 引用审查及保留/替换清单，修复设置说明层级与参数底部短文字/图标；保留原有动作、设置、写回语义，记录针对性失败与修复后回归。
- [x] 5.8 使用经精确 SDK 核对的公开正常窗口层级 API，修复预览/参数弹窗跨应用置顶；保留未确认草稿和 Pin，分别验证应用切换、返回继续编辑、设置/导出子窗口和取消。2026-11-08 提示修复包在正式 IU 263.7307.200 完成参数/预览 Pin 开关的真实 Finder 往返、草稿保留及设置/导出/上传选择取消；详见[续验记录](verification-2026-11-08-icon-buttons.md)。此项不包含整体资源/热卸载门槛。
- [ ] 5.9 对新增 UI 修复重建最终包，完成全量测试、配置检查、两个精确 build 的 Verifier、原生中英/深浅/窄窗与 Copy/Export/写回/GC 复验；自动与原生证据分开，未完成项如实保留。
- [x] 5.10 补齐全部注册动作组的中英 Keymap 标题，先记录缺标题 RED，再保持 ID/顺序/popup/compact 不变完成修复；在最终隔离包打开 Keymap 核对可读标题及日志无 Doc View 空标题警告，不启用 dormant 浮动工具栏。

- [x] 5.11 按 2026-11-08 用户反馈将参数底部改为三个可区分的纯图标按钮，保留中英 tooltip/无障碍名称与业务语义；更新既有展示测试，构建新包并原生检查外观、复制和写回，再续验跨应用矩阵。

2026-11-08 后续已完成 5.11 的纯图标、提示与键盘修复，以及代表性中英/深浅/Compact/窄窗、复制/写回/Undo 和跨应用复验。最新包 402 tests、两个 Verifier，并取得两轮目录热卸载通过；各包身份、失败修复过程和未重跑组合见[续验记录](verification-2026-11-08-keyboard-resources.md)。累计 33/41；5.9 与完整取消/资源/跨项目矩阵继续保留，不代表可发布。

历史记录（2026-11-08 提示修复包）：发现仅设置 AnAction description 不会进入 263 浮动 tooltip，已通过公开 TooltipDescriptionProvider 修复并取得 RED→398 tests GREEN、构建与两个精确 build Verifier。新包英语深色的三按钮提示、JSON/Markdown 复制、Schema 描述/必填写回与 Undo 均实测通过；中文、浅色、Compact、窄窗和键盘仍不计通过，5.11 与 5.9 保留，不能用 5.8 完成替代其他矩阵。

## 6. P4 最终包集成与交付

- [x] 6.1 执行 G，核对真实 suites/tests、零意外跳过、实际 Gradle 执行及配置目标 Verifier；失败先定位并修复后重跑受影响检查，记录日志和确切 build，不把缓存或 0 tests 称为通过。
- [x] 6.2 审计最终 ZIP/JAR 清单、descriptor、依赖和 SHA-256，确认无 PoC/test-only 类、失败候选、未用依赖或敏感数据；核对只包含选定的一条生产生成路线。
- [ ] 6.3 将上述最终 ZIP 安装到隔离目标 IDEA并核对加载日志，执行 A01–A12/L01–L06/U01–U04 的适用原生矩阵；A01/A02/A03 使用 loopback 虚构接收端由验收者主动发送，记录接收值，生成动作自身不得发网。
- [ ] 6.4 复核文档/Markdown/上传 mock 等既有业务回归和最终 UI一致性；核对 doc-demo/普通 profile 基线，恢复本次临时设置并关闭本机服务与隔离实例，保留失败/未验证证据，不将其他 change 的验收自动完成。
- [ ] 6.5 汇总 verification、任务状态和 roadmap，运行 S 并检查 literal diff 范围；明确已通过/跳过/未运行/受阻及最终包身份。只有所有必需实现和验收完成才勾选本项；提交、推送、发布和 sync/archive 均不在本任务内自动执行。

## Workflow follow-up

- 2026-11-07 full-17 完成 5.10：七组标题真实 RED→GREEN，中英 Keymap 和日志原生复验通过；全量 362 tests、两个 Verifier Compatible。累计 31/40。Copy/Export、中文写回/Undo、菜单取消和有限资源回归同包补验；5.8/5.9 的视觉遮挡、独立菜单和完整矩阵继续待验，详见 [full-17 记录](../intellij-263-baseline-upgrade/verification-2026-11-07-full17.md)。

- 2026-11-07 新增并完成 5.7：全部 icon 引用审查、参数动作文字/图标和设置说明字体/布局修复。full-16 为 70 suites / 361 tests、两个精确 build 的 Compatible；设置深浅主题、侧栏变宽、关闭重开与草稿取消原生通过。累计 30/39 项；5.8 的跨应用合成遮挡和 5.9 的全矩阵仍待验，详见 [11 月 7 日记录](../intellij-263-baseline-upgrade/verification-2026-11-07.md)。

- 2026-10-23 续验后完成 29/36 项。4.2 的两个真实项目按顺序创建/关闭，验证相同模块/路径/签名的身份及编辑器归属；选择变化和编辑器/目录上下文差异已自动验证。两项目同时打开的原生路由仍在 6.3 待验，不把顺序 fixture 说成同时打开。
- 剩余 4.5、4.6、5.4、5.5、6.3–6.5 涉及完整原生/性能验收及最终收口；Clear Cache 的同时 A/B 项目与文件保护已自动通过，但原生新菜单入口尚未可靠验收。G4 为 53 suites / 288 tests，全量门禁通过；具体部分完成、未运行与许可阻塞见 verification。
- MCP 的 Java PoC 和真实 Codex 候选取消门槛已由 `mcp-java-feasibility` 验证；正式四工具由独立 `doc-view-mcp-tools` change 管理，仍等待 `intellij-263-baseline-upgrade` 和 `project-document-store` 的剩余验收。本列表不创建 MCP 工具、索引或 Air 集成。
- 完成后是否 sync/archive、提交及发布，由后续明确请求和发布检查清单决定；不因 proposal apply-ready 自动执行。
