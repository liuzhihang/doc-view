# Doc View 1.6.0 发布记录

> **状态：`v1.6.0` 已打 tag，GitHub Release 已公开；Marketplace 已接收，尚未审核/上架。** 2026-11-09 已回读两个渠道的下载内容；自动门禁和代表性原生烟测通过，完整原生矩阵按维护者批准继续保留未验证项。

## 版本与兼容基线

- 目标版本：`1.6.0`。
- 生产 Java 基线：Java 25，classfile major 69。
- 最终 ZIP 回读的最低 IntelliJ build 为 `263.7031.23`。Plugin Verifier 使用的 build 与发布版 IDEA 原生回归 build 分别记录，不能互相代替。
- 2026-11-09 发布前实时预检：GitHub 最新为 `v1.5.0`、Marketplace 最新为 `1.5.0`，两处均未占用 1.6.0；远端 `develop` 与 `master` 均为 `877f0c9cafefc065a92650d99989b42a53ff1cde`。证据在 `build/release-1.6.0/publication-preflight-current.json`。创建发布提交/tag 与上传时仍须按实际操作需要重新回读。

## 交付范围

下列内容与已发布包一致；正式 MCP 与 Skill 继续单独开发。

| 范围 | 交付内容 | 使用边界 |
| --- | --- | --- |
| IntelliJ 与 Java 基线 | Java 25 / IDEA 263 | 不再支持低于最终 descriptor 所示最低 build 的 IDE；不据此声称未来 build 已验证 |
| HTTP 请求草稿 | 对适用 Spring Controller / Feign 方法，以 Doc View PSI 规则生成可编辑 `.http` 草稿并交给 IDEA HTTP Client 打开 | 生产路线为 Java 自生成单一路线；生成动作不发送请求。Dubbo、普通非 HTTP 接口及不支持的绑定形式不生成伪请求；未支持输入须明确拒绝 |
| 操作与 UI | 收敛常用工具栏与维护菜单、校准动作名称/适用范围、减少参数操作的常驻文字并保留图标提示与无障碍名称 | 最终的中英、主题、窄窗、键盘、弹窗遮挡和菜单表现以同包实测为准 |
| OpenAPI Schema | 使用 `@Schema.requiredMode` 表达字段必填状态，按字段写回并保留未编辑的 Schema 成员；修复描述内容的字符串及多行编辑行为 | Schema 类型缺少所需成员时保留草稿并提示；本版不保证所有 Schema 组合均已原生验收 |
| 项目文档库 | 通过显式生成/更新将人类可读 Markdown 与版本化 JSON 索引及接口详情写入项目 `docs/doc-view/`；工具窗口支持搜索和打开生成文档 | JSON 表示落盘快照，不是实时源码索引或语义检索；搜索/详情依赖已生成的文档库。旧 Markdown 不会被自动反解析为 JSON |

**本版不交付正式 Doc View MCP 查询工具，也不交付可安装或更新的 Doc View Skill。** 相关工作仍由独立 OpenSpec changes 跟踪，不得在版本摘要或 Marketplace 介绍中表述为现有能力。

## 最终 1.6.0 候选包与自动门禁

候选目录：`build/native-validation/2026-11-08/remaining-native/release-1.6.0-candidate/`。该候选通过了本轮完整自动门禁与 ZIP/JAR 元数据审计，同包代表性原生烟测已完成，完整矩阵缺口见下文。

| 项目 | 结果 |
| --- | --- |
| 插件 ZIP | `Doc View-1.6.0.zip`；SHA-256 `0f58a4365ac7c026ed588838a34cd71aaf54dfb17b1af7608f24302ba82c4cc4` |
| 插件 JAR | `doc-view-1.6.0.jar`；SHA-256 `868f87dee6f86727af49b32621c2fc759010eb9b4565d27ba6fdd89ce4abadde` |
| descriptor 与包清单 | 版本 `1.6.0`，`sinceBuild=263.7031.23`，未设置 `untilBuild`，`require-restart=true`；ZIP 仅有插件目录、`lib/` 与该 JAR；JAR 409 项 |
| Java classfile | 344 个插件 class，major 69 |
| 版本资源 | 四份英/中文欢迎页与更新页均打入；build info 版本匹配，change-notes 为当前版本 |
| 全量自动门禁 | 80 suites / 430 tests，0 failures / errors / skipped；18 个 Gradle actionable tasks 全部实际执行，`BUILD SUCCESSFUL`，1m54s |
| Plugin Verifier | `263.7031.23` 与正式 `263.7307.200` 均为 `Compatible`，各报告 16 项 Experimental API、0 项 blocking finding |
| JAR 测试/PoC 名称检查 | ZIP/JAR 结构为单一插件 JAR；对 JAR 条目名检查未发现测试、fixture 或 PoC 类路径 |

实际命令为 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache --no-parallel --console=plain`，使用隔离 IDEA 的 JBR。证据文件为 `artifact.json`、`commands.json`、`package-audit.json`、`full-gates.log` 与 `verifier-summary.json`。Verifier 的 Experimental API 数量按报告原样披露，不描述为零警告。

## 发布门禁状态

| 项目 | 当前状态 | 最终记录要求 |
| --- | --- | --- |
| 版本、平台和 descriptor | 目标为 1.6.0、Java 25、最低 IDEA build 263.7031.23；源码 descriptor 已声明 `require-restart="true"` | 从最终 ZIP 读回版本、最低 build、Java 字节码、重启属性和 change-notes |
| 最终插件包 | **已构建并审计** | 上表记录同一 ZIP/JAR 的路径、SHA-256、descriptor、classfile 与清单 |
| 自动测试与项目配置 | **通过** | 最终候选 80 suites / 430 tests，0 failures / errors / skipped；完整门禁命令成功 |
| Plugin Verifier | **通过，含 Experimental API** | 同一最终候选针对最低 build 与正式发布 build 均 Compatible；每个报告 16 项 Experimental API、0 blocking findings |
| 原生 IDEA 验收 | **部分通过，矩阵未完成** | 已通过的 Copy、项目文档生成/搜索和停用后重启场景见下文；其余未运行场景单列保留 |
| Git 提交与 tag | **完成** | #113 合入 develop，#114 合入 master；`v1.6.0` 指向 `67d54b700d56d2a662107e81b8ee039bbafdd25c`，与冻结源码 tree 一致 |
| GitHub Release | **已公开且下载校验通过** | Release ID `438524712`，正式非预发布；公开 ZIP 与最终制品 SHA 完全相同 |
| Marketplace | **已接收，待审核/上架** | update ID `1215457`；owner API 回读 `approve=false`、`listed=false`，公开更新列表未出现 1.6.0；下载内部文件与最终包一致 |

自动门禁和制品读回已完成；具体任务状态见[1.6.0 发布 change](../openspec/changes/archive/2026-11-09-release-doc-view-1-6-0/tasks.md)。原生结果与实际发布回读见下文，不把自动门禁替代原生验收。

## 最终包原生烟测

在官方 IU `263.7307.200`、无 JDWP 的隔离 `unload-clean` profile 中，对本节所列同一最终 ZIP（SHA-256 `0f58a4365ac7c026ed588838a34cd71aaf54dfb17b1af7608f24302ba82c4cc4`）执行了部分原生验收。实例 PID 为 89512；停用后重启复验 PID 为 90274。

| 场景 | 结果与边界 |
| --- | --- |
| 预览复制 | 原生 Copy 得到当前预览 `nativeGet` Markdown，482 个字符 / 618 字节，与预览编辑器内容逐字一致；测试后恢复 clipboard。首次 helper 断言误用了 DTO 展示名，改用实际 GET fixture 后通过，确认是验收 helper 输入问题，不是产品缺陷。 |
| 项目文档生成/更新 | 生成 65 个 API、跳过 0 项；既有文档库升级仅更新 1 个文件。`index.json` 显示 `producerVersion=1.6.0`、`hasUnsavedDocuments=false`。 |
| 项目文档搜索 | 搜索 `lifecycle` 返回 3/3 匹配，界面显示当前会话 65 个 API；搜索前后 133 个文档文件哈希不变。 |
| 停用与重启 | 对 Doc View 点 Apply、OK 后显示 “Restart IntelliJ IDEA to apply changes in plugins?”；日志记录需要重启。PID 89512 正常退出后显式重启隔离 IDEA，PID 90274 中插件设置显示 Disabled，主窗口无 Doc View 工具窗口，随后正常退出。该结果仅覆盖停用并重启，不代表安装、更新、卸载组合都验证通过。 |
| 环境清理 | HEAD 与原工作区 584 个文件未变，Junie 未操作；20 个既有 fixture 源文件未变，3 个临时源文件已删除，117 个既有文档库文件恢复，测试 profile 的插件启用状态离线恢复，325 个构建输入哈希未变。 |

原生证据位于 `build/native-validation/2026-11-08/remaining-native/release-1.6.0-candidate/`，包括 `native-summary.json`、`preview-copy-result.json`、`after-generate.json`、`search-readonly-result.json`、`restart-prompt.json`、`normal-exit-result.json`、重启后 Settings/Main snapshots 和 `cleanup-preservation.json`。以上结论绑定最终候选哈希，不覆盖未执行场景。

## 明确保留的验证边界

维护者于 2026-11-09 批准本次在部分原生验收仍未完成时发布：已知功能缺陷仍须修复并复验，完整自动构建/测试/Verifier 门禁仍须通过；确实未完成的原生场景可留作后续维护者人工验证和用户反馈。发布时必须按场景记录未验证项并保持相应功能 task 未勾选。该例外只适用于本版。

以下场景在最终包上**未执行**，按本次例外保留为未验证，由维护者后续人工验证并结合用户反馈处理：

- OS 凭据授权拒绝、异步写入失败后的重试；
- 尚未覆盖的原生资源/GC 情况，以及主题、窗口尺寸和交互方式组合；
- HTTP Client 实际 Send、完整异常/取消/性能矩阵；
- OpenAPI Schema 写回组合；
- 中英/浅色/窄窗/键盘/跨应用弹窗的完整 UI 矩阵，以及剩余资源/GC 矩阵；
- 上文已通过的项目文档生成与搜索以外的 JSON 文档库和项目隔离场景；
- 完整安装、更新、卸载生命周期组合；本轮只验证停用提示和显式重启后的 Disabled 状态。

OS 凭据授权拒绝、异步写入失败后的重试也未在最终包原生验证。对应活动 change 的 task 保持未勾选；以上未验证项不会被表述为通过。此前候选的结果仅作为历史参考，不能替代最终包结果。

此前的热卸载验收出现插件类仍被保留的结果；本版以 `require-restart="true"` 描述安装、更新和卸载后的重启生效方式，**不宣称修复了热卸载引用，也不承诺原位热更新或热卸载**。若最终包没有读回该 descriptor 属性，则不能按本计划发布。

## 历史候选参考（不代表 1.6.0 最终验收）

`cancel-wording-r2-candidate` 历史包 SHA-256 为 `7661036f4edb4c7cb53ecb0bc729466f00188eed75500b376230352e62ad253a`，其 JAR SHA-256 为 `5618784756441003feeb18de1bd890fe28bdd450782140d6c098feaace8ef997`。该候选的 429 项自动测试和英语查询取消提示原生回读（133 个文档文件未改变、重试 3/3）属于历史证据；中文新提示未单独原生验收。以上结果均不可替代最终 1.6.0 包的重新构建、门禁和原生验收。

## 发布与回滚

本次已在最终自动门禁通过、制品读回且未验证原生场景列明后，完成合并、tag、GitHub Release 和 Marketplace 上传。Marketplace 的人工审核与上架仍待平台完成；不得将上传成功描述为已上架。

如发布后发现阻断问题，先停止继续发布，保留制品、日志和远端状态，按发布清单回滚或准备新的 patch 版本；不重写已公开 tag 或替换已发布 ZIP。

## 2026-11-09 发布渠道回读

- 冻结源码提交：`ca51becdd2d0499018f696d96448604cccb94aa3`，232 个 literal paths。通过 [#113](https://github.com/liuzhihang/doc-view/pull/113) 合入 develop，再由 [#114](https://github.com/liuzhihang/doc-view/pull/114) 合入 master。
- `v1.6.0` 指向发布提交 `67d54b700d56d2a662107e81b8ee039bbafdd25c`；该提交与冻结源码 tree 相同，325 项构建/测试输入哈希逐项匹配。未移动原工作区的本地 develop/HEAD。
- [GitHub Release](https://github.com/liuzhihang/doc-view/releases/tag/v1.6.0) 已公开。GitHub 将空格归一化，下载名为 `Doc.View-1.6.0.zip`；已修正 `SHA256SUMS` 对应文件名，公开下载并执行 `shasum -a 256 -c SHA256SUMS` 通过。ZIP SHA-256 仍为 `0f58a4365ac7c026ed588838a34cd71aaf54dfb17b1af7608f24302ba82c4cc4`，未替换插件 ZIP。
- [Marketplace](https://plugins.jetbrains.com/plugin/15305-doc-view) 上传任务成功，update `1215457` 版本 `1.6.0`、最低 build `263.7031.23`、未设最高 build，审核和上架字段均为 false。发布后的变更说明回读含本版 HTTP、JSON 文档库和重启策略；其中打包前“重启流程仍未验证”的文字属于当时快照，本记录补充的是最终包停用/重启路径通过，完整生命周期组合仍未验证。
- Marketplace 下载端点已可取得包；外层 ZIP 被平台重新压缩，SHA-256 为 `1d3c2ddd4f18bfeb01f73647a41087dee919ee9df77d47d77b190da578cf6dc0`。解压后全部文件内容与本地最终包一致，JAR SHA-256 为 `868f87dee6f86727af49b32621c2fc759010eb9b4565d27ba6fdd89ce4abadde`，descriptor 版本、最低 build、重启标志也相同。可下载不等于公开列表上架。
- `publishPlugin` 成功执行，上传前后本地 ZIP SHA 未变；其附带 `patchChangelog` 只产生本地格式改写，已保存诊断副本并恢复冻结源文件，未夹带到发布后文档提交。
- 公开包内容与原生烟测所安装包相同；没有重新执行第二轮完整原生矩阵。正式 MCP、Skill、未完成的原生验收继续保持独立活动 change，不批量完成/归档。

发布证据位于 `build/release-1.6.0/`：`release-commit-binding.json`、`github-release-readback.json`、`marketplace-publish-result.json`、`marketplace-owner-update-readback.json`、`marketplace-artifact-readback.json`。回滚选择既有 1.5.0 或准备新 patch，不改写本次 tag 或已公开的插件包。

## 路线图迁入的历史进度摘要

以下为 2026-11-09 文档整理时从旧路线图迁入的历史摘要，保留当时的包、平台和验证边界；不是本次重新验证，也不覆盖上文最终发布包记录。旧文中的“待验”“不发布”和“正在”均描述对应阶段，不表示当前状态。任务现状以各 change 的 tasks 与 verification 为准。

### 2026-11-07 至 2026-11-09 候选续验

2026-11-09 历史候选门槛（发布前）：429 候选（ZIP `6c789b81…8370`）已通过 80 suites / 429 tests、构建及最低 `263.7031.23` / 正式 `263.7307.200` 两个精确 build 的 Verifier。原生补齐 HTML 加载/渲染/空/失败/取消状态、旧回调防覆盖、Copy/Export，以及项目文档生成和搜索取消后重试；测试临时模板和源码已恢复。三轮实际 HTML 关闭后 renderer 子进程退出、FD 均为512，但随后热卸载仍有56条 Doc View histogram rows、类加载器仍被引用，正在分析新 heap。428 的特定原生序列曾卸载成功，不能覆盖429这次失败。搜索取消提示误称“状态检查”已改为通用文案，下一候选构建与原生复验进行中。OS 凭据拒绝/失败重试、最终发布包原生验收和完整资源门槛继续保留；未升级版本、打 tag 或发布，正式 MCP 尚不推进。原工作区584个冻结文件与 HEAD 已复核未变，Junie 保留。

2026-11-08 派生缓存/异常/UI 续验：同一 d094 候选补齐双项目派生索引缓存隔离和不同设置值读回，HTTP/UI 5.4 完成，累计 35/41；HTTP 拒绝、写入失败、Dumb Mode、源码变更/删除、依赖禁用与恢复重试取得原生证据，Copy/写回/导出、中英窄窗及有限三轮 GC/一次热卸载也已补验。完整矩阵仍保留。另确认 XML-only/实现类注解的 Dubbo 目录发现属于既有缺口，正在补契约、测试和修复，修复后新候选必须重新核对自动门禁与受影响原生路径。普通 IDEA 误开的虚构项目已在单独授权后关闭，未操作 Junie；最近项目记录不宣称字节未变。详见[本轮记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。未升级版本、打 tag 或发布；下文为相应旧包的历史进度。

2026-11-08 HTTP/缓存/搜索续验：修复完整方法说明尾部不可检索；新包 403 tests、构建和两个精确 build 的 Verifier 通过。原生目录及落盘文档搜索、双项目 DTO 缓存/文件保护、HTTP 取消重试与在途关闭已取得证据；固定模型进程首次/热生成各 7 次，p50/p95 分别为 394.121/419.352 ms 与 137.470/180.368 ms。HTTP/UI 完成 34/41，派生索引缓存双项目隔离、完整 L01–L05 和新包 UI/GC 矩阵仍保留，尚不发布。详见[本轮记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md)。下文保留对应旧包的历史边界。

2026-11-08 中文/键盘/JNI 续验已推进：修复参数按钮焦点、表格 Ctrl+Tab 返回按钮，以及目录销毁时的模型、父组件和鼠标路径引用。最新包 402 tests、构建与两个精确 build Verifier 通过；原生键盘写回/Undo、目录菜单/Enter、四种 Pin 状态跨应用，以及同进程两轮目录热卸载通过。中文深浅色、Compact/非 Compact、窄窗和三轮资源结果按各包身份记录；完整取消、跨项目和性能矩阵仍待验，不发布。详见[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-keyboard-resources.md)。以下旧包失败和旧待验描述保留历史边界。

2026-11-08 辅助功能续验：在获准的隔离窗口中补齐参数/预览 Pin 开关的真实 Finder 往返，以及设置、导出和上传选择取消，HTTP/UI 5.8 完成。发现按钮说明未进入浮动提示，已用公开 TooltipDescriptionProvider 修复；最终包 398 tests、构建和两个精确 build Verifier 通过，英语深色的三按钮提示、复制、Schema 写回与 Undo 原生通过。中文/浅色/窄窗/键盘和完整资源矩阵仍待验；JNI 热卸载仍未通过，不发布。详见[纯图标验收记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-icon-buttons.md)。

2026-11-08 卸载 API 续验：已新增仅在 Doc View 自身 `beforePluginUnload` 调用公开 `IconLoader.clearCache()` 的修复。新包在正式 263 的无 UI 卸载对照中类加载器已释放；打开目录后仍被 macOS 无障碍 Local JNI 引用，官方新堆报告确认热卸载未通过。未找到可供插件释放该 JNI 根的公开 API 或官方已发布修复。剩余弹窗验收继续保留；构建、测试修正和两 build Verifier 的精确进度见[本轮记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-08-unload-api.md)。原工作区与 Junie 保留，不发布。下方未清缓存等描述是此前旧包阶段。

2026-11-08：独立工作树已整合已发布 1.5.0 与 263/HTTP/UI/文档库改动；首轮按钮测试和索引取消问题已修复，同一整合包此前 395 tests、构建和两个 Verifier 通过，本轮补齐 Copy/Export、Schema 写回与 Undo、文档分页/冲突保护及有限资源回归。隔离实例由维护者激活后，原生 HTTP Client 的 GET/JSON POST/表单 POST 回环执行通过，部分桌面菜单可见结果已补齐；真实激活 Finder 后，参数弹窗 Pin 开启这一遮挡场景通过。热卸载仍失败，官方本机分析已分别取得无障碍 JNI 与平台图标缓存保留 ClassLoader 的引用链，修复方案尚未验证。完整弹窗、菜单与剩余矩阵未通过，未升级或发布。详见[发布前核对记录](release-preflight-2026-11-07.md)和[新整合包原生记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md#历史证据边界)；下方 full-17 及更早结果仍是对应旧包证据。

截至 2026-11-07，full-17 已修复七个 Keymap 组的中英文标题，70 suites / 362 tests、构建及 EAP `263.7031.23` / 正式 `263.7307.200` 两个 Verifier 通过，仍有 16 Experimental usages。同包原生已验中英 Keymap、参数 Copy JSON、预览 Copy/Export、中文 Schema 写回与一次 Undo、上传菜单取消、目录 Enter，以及 Pin 开/关跨应用草稿保留和前台交互。三轮关闭后静置约 150 秒，表单、Markdown JCEF panel、RemoteBrowser 和客户端 Java 对象全部归零，FD 无逐轮增长；正常重启/退出后对应 IDEA 与 CEF 进程已消失。完整桌面视觉遮挡、`…`/目录右键菜单可见结果、CEF native peer/listener/性能及其余矩阵仍未通过，R2/4.2 保持未勾选，正式 MCP 不推进。Junie 保留，临时语言与源码已恢复；最低 build 仍为 `263.7031.23`。详情见[full-17 续验记录](../openspec/changes/intellij-263-baseline-upgrade/verification-2026-11-07-full17.md)；full-13/14/16 和下方旧日期均保留为历史阶段。

2026-11-09 补充：421 包已补受控原生目录取消/恢复、预览加载取消、HTTP 同方法在途合并和未保存草稿保护。新增线程断言正在修复；热卸载根已确定为已关闭项目仍保留 Doc View ToolWindowEntry，公开 API 清理方案需 RED/GREEN 与最终包原生复验。受控断点数据不作性能结论；发布门槛仍未通过。

2026-11-09 最新状态：多行输入、Markdown 换行、批量 Undo/Redo 与 Escape 修复包 `cbc2c714…a9a4d` 已通过 421 tests、构建和最低/正式两个精确 build 的 Verifier。原生 Schema 必填/转义/成员保留/拒绝/单次 Undo/Redo、导航/复制、四组 Finder 遮挡，以及整类 Spring/Dubbo、空预览、模板异常/恢复已补验；HTTP L01 空目录与 L02 双项目重载有对应 419 包证据。当前 421 包在新增整类/模板异常使用顺序后热卸载再次失败，已保存日志及 heap，正在定位新的类加载器引用；不能沿用旧 414 包的卸载通过。HTTP L03、受控取消/迟到回调、凭据 OS 拒绝/重试等独立门槛仍开放，未升级或发布。 详见[续验记录](../openspec/changes/http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。下文保留旧包历史边界。

### 早期 full-10 / full-17 与客户端候选

**项目文档库与 UI（历史阶段）**

固定 `docs/doc-view/` 下的 JSON 摘要/详情及共享生成、搜索和打开 UI 已实现。IU263/Java25 的 full-10 自动回归 67 suites / 352 tests 全绿，构建和严格 Verifier 通过；另有 15 条 test-only deprecated/removal warnings 与 16 处 Experimental API 使用，分别记录。近期修复了恢复后的 Markdown 状态仍显示 `INCONSISTENT`；针对性自动回归已纳入 full-10。full-10 ZIP SHA-256 为 `b238f5412d134ff4311ba5422fb78d50562bfecf80826964858984d5d69bcc0b`。该包部分原生场景已通过：精确还原 Markdown 后重新加载、Search/Open 成功，banner 自动显示 `CURRENT · 30 API(s)`，63 个文件 bytes/hash 与 30 个条目 schema/hash 有效；Spring/Feign/Dubbo 预览、Welcome 关闭重开、GET/form 草稿生成并在 HTTP Client 打开也已通过。no-op 场景重开后初始显示 `unknown`，之后 Generate 返回 `30 API(s) / No content changes`，不点 Refresh 即自动变为 `CURRENT`，约 +4.7 秒与 +20.45 秒读回仍为 current，未复现持续刷新问题。HTTP Client Run 未执行。2026-10-25 同包新增通过文档库20+5分页/末页打开、用户编辑冲突保护与恢复、真实I/O部分发布/重试、8,030接口生成在途取消/取消后重试及关闭重开freshness。上述为 full-10 阶段证据；full-17 已补代表性 Copy/Export、写回/Undo 和有限资源回归，独立菜单、完整参数/HTTP取消及资源矩阵仍待完成。具体包与收尾读回见[验证记录](../openspec/changes/intellij-263-baseline-upgrade/verification.md)。

**Codex 取消候选实测（与第 1 项并行）（历史阶段）**

官方 `0.165.0-alpha.1` app-server 候选在隔离 IU 263.7031.23、协议 `2025-06-18` 上三轮真实 turn interrupt 取消通过；旧 `0.163.0-alpha.11.2` 失败记录保留，不能推导所有 Codex 版本行为相同。证据见[取消说明](mcp-cancellation.md)。

**精确 263 生产基线（历史阶段）**

用户已授权按 [`intellij-263-baseline-upgrade`](../openspec/changes/intellij-263-baseline-upgrade/proposal.md) 使用最低 `IU-263.7031.23` 与 Java 25。最新 full-17 的 362 tests、构建、配置检查及 EAP/正式版两个 Verifier 通过，production compatibility/deprecated/internal/override-only 为 0，保留 16 Experimental 和 15 条 test-only 警告。同包代表性 Copy/Export、中文写回/Undo、中英 Keymap 与有限资源回归通过；独立菜单、视觉遮挡及完整生命周期矩阵未完。文档库取消/partial/retry 的 full-10 历史证据另行保留，不混成同一包全量验收。此为未发布开发包，不推导未来 build 兼容性。

### 功能验收摘要（历史阶段）

**[`fix-preview-lifecycle`](../openspec/changes/fix-preview-lifecycle/proposal.md)**

预览生成、后台任务列表和渲染切换已实现并完成自动验证；原生打开/重开与主题场景已验。JCEF 预览窗口关闭及释放的剩余边界见[验证记录](../openspec/changes/fix-preview-lifecycle/verification.md)。

**[`integration-credential-storage`](../openspec/changes/integration-credential-storage/proposal.md)**

已修复 PasswordSafe 同 serviceName 的跨项目覆盖：生成键按平台与项目位置隔离，并由真实 IU261 InMemoryCredentialStore 取得 RED/GREEN。IU261 原生已验证 A/B 保存、同包重启回读及清除 A 后 B 仍保留；OS 授权/拒绝和后端失败重试仍待验。真实上传本轮排除，旧 XML 不迁移。详见[验证记录](../openspec/changes/integration-credential-storage/verification.md)。

**[`openapi3-schema-required-mode`](../openspec/changes/openapi3-schema-required-mode/proposal.md)**

`requiredMode` 三态、优先级、安全写回及 DTO golden 已实现；421 项候选补齐原生多行/必填/成员保留/缺少成员拒绝/批量 Undo/Redo。其余资源与发布门槛独立跟踪。详见[验证记录](../openspec/changes/openapi3-schema-required-mode/verification.md)。

**[`doc-generation-performance`](../openspec/changes/doc-generation-performance/proposal.md)**

已处理缓存 epoch、过期结果写回和递归取消。IU261 原生目录扫描取消后保留旧树、不发布部分结果已验；历史候选包补过整类 Spring/Dubbo 源预览和刷新的一次可见时间上界；取消后的可见终态、重复测量及 HTML 矩阵仍待补。详见[验证记录](../openspec/changes/doc-generation-performance/verification.md)。

**[`mcp-java-feasibility`](../openspec/changes/archive/2026-11-09-mcp-java-feasibility/proposal.md)**

IU 261 的历史协议取消失败保留；IU 263.7031.23 / SDK 0.15.0 隔离探针的取消、项目/服务/插件生命周期与有限资源检查通过。Codex 0.163.0-alpha.11.2 的 turn interrupt 未取消服务端任务；官方 0.165.0-alpha.1 候选随后在隔离 IU 263 上三轮真实取消互操作通过，PoC 2.4 已完成。当前开发配置已提高到 IU263/Java25，完整自动矩阵通过，原生回归仍在进行；尚未发布。详见[验证记录](../openspec/changes/archive/2026-11-09-mcp-java-feasibility/verification.md)和[取消验收说明](mcp-cancellation.md)。

**[`doc-view-mcp-tools`](../openspec/changes/doc-view-mcp-tools/proposal.md)**

正式 MCP 放在同一个 Doc View 插件内，可选注册到 IDEA MCP Server；与界面按钮共用文档服务。只读 status/search/get 与显式 generate 的共享文档库 contract 已定义，生产工具尚未实现。Codex 候选取消门槛已通过，精确263的构建和Verifier已通过，但 task 1.3 仍等待原生回归和文档库最终验收；详见 [tasks](../openspec/changes/doc-view-mcp-tools/tasks.md)。

**Air 可选协作评估**

已核对官方 ACP/MCP 边界并定义只读流程；原生账户/会话和文件读取未验。保持可选评估，不把 Air、ACP 或 MCP 视为已集成能力。见[评估记录](ai-context-assessment.md#可执行的只读验收流程)。

**`doc-import-viewer`**

[源码评估完成](ai-context-assessment.md#历史文档导入与查看路线图-17)：不重复建设 Markdown 查看器。先完成第 11/14 项原生阅读验证；只有历史目录/差异存在实测缺口时，才另立只读快照 change。

**[`clear-cache-safety`](../openspec/changes/clear-cache-safety/proposal.md)**

Clear Cache 已改为只清当前项目 schema 缓存，不再删除手写 Scratch；旧版自动回归及工具栏入口已验，目录菜单入口仍未完成；当前 HTTP/UI change 已将入口移至窗口 `…`，A/B 自动缓存隔离和文件保护已补验；原生新入口及 A/B 原生场景仍待验。详见[验证记录](../openspec/changes/clear-cache-safety/verification.md)。
