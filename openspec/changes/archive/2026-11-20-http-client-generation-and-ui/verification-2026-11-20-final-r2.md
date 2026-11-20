# 2026-11-20 1.7.2 R2 原生验收

## 工件与环境

- R2 ZIP SHA-256 `7eb07fa4e744efe7e30bf4026b78aae3de1d21286a1caaf386bb8c5ac226e5a5`；安装 JAR SHA-256 `681eb66c2d41f316c77878ea857a1b0cd494f8c750cf5c979d2c7944ba5f1fbb`。正式 IU `263.7307.200`，Java 25。英语应用在隔离 HTTP profile 加载 1.7.2（PID 73628）；中文深色应用在另一隔离 profile 加载相同 JAR（PID 75000）。原始日志、截图、回环和 SHA 读回位于 `build/release-1.7.2-20261120/`。
- 发行包 R3 仅更新内嵌插件介绍。[稳定版基线记录](../2026-11-20-intellij-263-baseline-upgrade/verification-2026-11-20-stable-minimum.md#正式发行包-r3-的最终核对)含两个产物的逐项比较和各自的构建结果。
- 菜单修复前的 1.7.2 ZIP `e38d28595f48adf2117e8c0f3202264a13ec9880667837a4be8e774fe05f4571` 裁切英文维护标题。现有 `DocViewActionLayoutTest` 增加精确文案断言，先 RED 后 GREEN；R2 将组标题缩为 `Doc View Maintenance` / `Doc View 维护`。两候选中所有 Java 类字节一致，变化只在 descriptor 及中英 bundle。最终包执行 99 suites / 584 tests，0 失败、错误、跳过。正式最低 build 的 Verifier 为 Compatible，保留 16 项 Experimental API 和 SDK 布局警告。过期 EAP 的报告保存在各历史候选记录。

## HTTP

| 编号 | R2 原生观察 |
| --- | --- |
| A01 | 原生客户端运行 `GET /native/users/42`，回环端收到 `q=a&b 空格`、`X-Trace: native-get`，无 Content-Type 或 body。 |
| A02 | `POST /native/json?dryRun=true`，收到 `X-Trace: native-json`、`application/json` 和 `{"displayName":"","age":0}`。 |
| A03 | `POST /native/form` 无 query，收到 `X-Trace: native-form`、`application/x-www-form-urlencoded`，解码为 `name=Alice & Bob 空格`。 |
| A04–A07 | 从目录生成类/方法常量组合 `/matrix/constant/`、保留 `wire=visible`、保留 required/path 占位符并省略无值可选项、保留显式空值 `emptyValue=`。认证值为变量，数值头为 `X-Numeric: 42`。其他 Header 冲突约束由自动契约覆盖。 |
| A08 | 从 Feign `create(boolean)` 生成 `POST {{host}}/remote/items?dryRun={{query_dryrun}}`，保留主机变量，未运行该请求。 |
| A09/A10 | 多 mapping、multipart、两个 `RequestBody` 的三次原生选择分别显示 `The endpoint mapping is ambiguous or unsupported.`，没有生成对应草稿。其他无效组合由自动契约覆盖。 |
| A11 | 递归 DTO 生成有限 JSON，包含嵌套 `next` 占位，Content-Type 为 `application/json`。 |
| A12 | 在编辑器改路由且未保存，草稿使用新路径 `/metadata-after-unsaved-172`，磁盘源码 SHA 始终为原值；Undo 恢复编辑器。 |

A01–A03 由验收者在 IDE 中把生成后的 `{{host}}` 改成 `http://127.0.0.1:65230`，核对方法/路由后点击原生 Run，三次返回 HTTP 200。`final-native-loopback.jsonl` 的前 3 行是标题修复前的候选，后 3 行为 R2，于 UTC 04:54:07、04:54:25、04:54:50 接收；没有只有生成未点击 Run 就收到的请求。没有向源码中的 Feign URL、其他外部服务或上传目标发送请求。

| 编号 | R2 原生生命周期 |
| --- | --- |
| L01 | 类与 XML Dubbo 仍显示目录，XML Dubbo Enter 打开含 `FictionalXmlService#readXmlUser` 和 Dubbo 类型的 Markdown。无效节点/编辑器状态的执行保护由自动动作测试覆盖。 |
| L02 | A 与 B 使用相同路径/模块及方法名，B 的 String/int 分别生成 `/string-b` 和 `/number-b`，源注释含各自完整签名；A 的 String 草稿保持 `/string`。Window 菜单同时列出 A 和 B，两个草稿目录相互独立。 |
| L03 | 在解析返回后的单次停点内重触发同一方法，IDE 只有一个生成进度。放行后 `caseConstant_2` 只新增一份，11 份旧草稿 SHA 保持；`_1` 的未保存标记仍留在编辑器且未落盘，验收后 Undo。`_1` 原本是在写入失败恢复重试时生成的，不是本轮重复生成结果。 |
| L04 | 在同一解析完成停点，通过原生 Stop 取消后旧草稿逐字保持。独立一轮实际 `File → Close Project` 抵达 `IntelliJ IDEA Home`，才释放后台线程。IDE 日志记录项目 context disposed，未新增 `caseRecursive` 草稿；JDI 已 detach。 |
| L05 | 将单个虚构项目的草稿目录设为只读，原生说明无法保存且旧草稿不受影响，10 份旧草稿 SHA 一致；恢复 mode 后同动作重试生成一个新稿。另一中文 profile 禁用原生 HTTP Client 后仍正常加载 Doc View，Spring 方法菜单的 HTTP 动作为禁用。 |

## 菜单、文档与资源

- 项目 A 的浅色紧凑窄窗显示完整英文 `Doc View Maintenance`，保留平台 Speed Search、View Mode、Move to、Resize；实际从菜单打开所属项目的 Settings。独立中文深色 profile 显示完整 `Doc View 维护`、`设置`、`清除缓存` 和平台项。使用 Find Action → Show Context Menu 的键盘路径打开实际目录菜单，见 `final-r2-keyboard-directory-menu.png`；菜单含源码、Markdown、导出、上传、HTTP，不含 Clear Cache；F4 跳到同一个 Java 方法的 `35:19`。中文方法物理双击与英语 XML Dubbo Enter 都打开原生 Markdown。
- A、B 分别生成 72 项项目文档，两个原生维护菜单分别执行 Clear Cache。两边状态与打开的 HTTP 编辑器保持；清理前后 A/B 共 294 份文档库文件和 15 份 HTTP 草稿 SHA 逐字一致，清单 SHA `709d4f794493ff8e91b607a2cd2b3ede4927ef152e41e6d25471f004cf31d8aa`。同项目派生缓存重载、另一项目内存条目和未保存稿保护的直接观察见[前序专用夹具](verification-2026-11-08-remaining-native.md)。
- 原生 JCEF 预览 Copy 的 Markdown 与备选目录 Export 文件 `caseWire.md` 字节一致，SHA `9efa40f61a0e2f36e0fdb08acaa631a3f0599502ce8b94280f61bebfa04ec58d`。Upload 菜单显示 YApi、ShowDoc、YuQue，用 Escape 取消，无真实上传。Schema 三个纯图标动作可识别为 Copy JSON、Copy as Markdown、Apply to Source。真机 Copy JSON 剪贴板解析字段 `age`、`displayName`。键盘把 `displayName` 的 `false` 设为 `true` 后 Apply 和保存仅改 `NOT_REQUIRED` 至 `REQUIRED`；一次 Undo 后保存，源码字节与原始样例一致。
- 实例跟踪使用隔离 IDEA PID 73628 的 live `GC.class_histogram`。打开参数时可见一个 `ParamDocEditorForm` 和一个 `ParamTreeTableView`，打开预览可见一个活 `PreviewForm`。关闭 A 后 B 仍打开，一个 `HttpRequestService` 和短暂待回收的 `PreviewForm` 仍可见。关闭 B 回到 Home 后，下一次 live histogram 中未见上述三个 Doc View UI 对象、`MarkdownJCEFHtmlPanel`、`RemoteBrowser`、`HttpRequestService` 或 HTTP 匿名后台任务的活实例。原始 `.hist` 按阶段保留。该观察范围为代表场景与 Java 层对象，未测量无关平台原生堆、长期内存趋势或热卸载。跨应用 Finder、Pin、草稿保留和全部旧候选资源轮次见[前序验收](verification-2026-11-08-icon-buttons.md)。

## L06 固定模型原生测量

R2 安装到独立 `fixture-hot` profile。全部动作都从同一个 `hot(HotNode0)` 目录节点经原生 Generate HTTP Request 触发，未执行 HTTP。7 个冷样本均为各进程的首次生成，保留磁盘缓存和持久索引，耗时排除 IDE 启动和 UI 选取。第八个进程先生成一份预热草稿，再串行执行七次。测试观察器只用公开 action/editor 回调和 100 ms Swing Timer，未进入发行包。观察记录用互不重叠的 PID 启停时间窗口关联，延迟用进程内单调时钟计算。

| 样本 | 动作到编辑器打开，单位 ms | p50 / p95 |
| --- | --- | --- |
| 进程首次生成，7 次 | 482.173, 465.703, 472.815, 924.257, 789.983, 981.215, 819.321 | 789.983 / 981.215 |
| 同进程预热后，7 次 | 604.708, 228.677, 211.882, 210.046, 222.826, 199.180, 194.541 | 211.882 / 604.708 |

所有 15 份草稿均为 433,188 bytes，SHA-256 `b5bf65e4d19a2a1312710037194c0730db031f644854b93e783789dcfe9707df`；432,954-byte JSON body 读回相同，包含 1,093 个对象、深度 7。两组 p95 采用 nearest-rank。15 次 action 同步分派 p50 5.067 ms、最大 9.868 ms。生成到编辑器打开的窗口里，4/7 冷样本和 1/7 热样本的 UI Timer 间隔超过 250 ms，最大 514 ms；该记录包含 IDE 调度和编辑器打开成本，无法仅凭 Timer 归因给生成器，也不与不同轮次的观测直接等同为性能变化。L06 要求记录固定模型、时序与资源终态，未设延迟上限。观察器退出和项目关闭结果单列，不把其人工种子用于 GC 结论。原始输出见 `build/release-1.7.2-20261120/l06-hot/runs/`。

## 发行包 R3 冒烟与清理

R3 ZIP `c372c22e1773a0ae1487f2de4d08060edb690cf9fed0c17282c12750ac0dacbc`，安装 JAR `f8de3a0b8dbea35c5357898d30b2f738700dd585b9f507dfae9b56a2fd7e806f`。真实发行包在无调试器的同一正式 IU 启动（PID 79903），IDE 日志读回加载 `Doc View 1.7.2`，识别 72 条已落盘接口快照，刷新工具窗口。实际选择 `nativeGet`，生成带 `{{host}}`、正确转义 query 和 `X-Trace` 的草稿，并用 Enter 打开原生 Markdown。英文深色菜单的 `Doc View Maintenance` 完整，Settings、Clear Cache 和平台项均在。R3 只改插件介绍，真实发送、全部场景及性能数字保留 R2 身份。

最终全量覆盖 Spring/Feign/Dubbo、Markdown 和上传 mock；未进行真实 YApi、ShowDoc、YuQue 上传。R2 直接验证缺依赖、保存失败与重试。Dumb Mode、同一请求在途源码删除、派生内存缓存的中间状态保留在[前序原生续验](verification-2026-11-08-remaining-native.md)，R3 未专门重跑这些历史夹具。中英单独界面、深浅色、窄窗、Compact 与键盘均有实际观察，未对每种操作做全组合。进程与 profile 清理记录在 `final-r2-functional-profile-restored.json`、`final-r3-native-smoke.json`；取消调试、临时 MCP 和回环监听均关闭，Doc View JAR、临时禁用列表、深色主题与源码已恢复，原工作区和 Junie 保留。
