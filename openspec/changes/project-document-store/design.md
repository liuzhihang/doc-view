# 设计

## 背景

动机和范围见[提案](proposal.md)。现有项目导出器从后台 PSI 快照生成 Markdown 页面和 `index.md`，通过 marker、hash、暂存和逐文件原子替换保护已有文件；它不提供结构化查询数据，也不保证整批文件事务。`ProjectDocumentIdentity` 已提供由模块、项目相对源码路径、类名、方法名和参数类型构成的稳定身份。`DocView` 对象图持有 PSI 和 parent 引用，不能序列化或传入磁盘写入阶段。

本 change 在现有 Markdown 之上新增项目绑定的结构化读模型。项目文档库 UI 由 [`project-document-library-ui`](specs/project-document-library-ui/spec.md) 定义；正式 MCP adapter 的工具参数、MCP 响应和业务错误码由 [`doc-view-mcp-tools`](../doc-view-mcp-tools/design.md) 定义。

## 目标与非目标

**目标：**

- 从同一份不可变、无 PSI 的解析快照发布人读 Markdown、机器索引和接口 JSON 详情。
- 以最终替换 `index.json` 作为机器快照提交点，并允许在文件替换边界响应取消。
- 在项目范围内提供有界、确定性的搜索和详情读取，不在查询时访问 PSI 或解析 Markdown。
- 检测文件冲突、同根并发写入和可观察到的外部修改；无法验证时拒绝返回或覆盖数据。
- 按明确环境记录性能数据，并将未测目标保持为未验证。

**非目标：**

- 中途取消、I/O 错误或进程崩溃后保留完整的上一代详情字节。
- 新增 IntelliJ `FileBasedIndex`/`StubIndex`、SQLite、向量检索、运行时依赖或自建 MCP Server。
- 源码编辑后自动重生成、查询时扫描源码、任意文档根目录、自动删除过期文件或跨 schema 读取适配。
- 改变 Spring、Feign、Dubbo 解析、Markdown 模板、接口身份算法、插件 `sinceBuild` 或独立 UI scope。

## 设计决策

### 1. 固定项目根，Markdown 与 JSON 共享稳定文件 stem

文档库根固定为项目根下 `docs/doc-view/`。继续使用 `index.md` 与 `apis/<existing-page-file>.md`，新增 `index.json` 与相邻详情 `apis/<same-stable-stem>.json`。例如现有 API 页面是 `apis/UserApi-find-<identity-hash>.md`，详情必须是 `apis/UserApi-find-<identity-hash>.json`；不另用单独的 hash-only 文件名。`index.json` 是机器查询唯一入口，已提交的 entry 才是当前 API；不得通过遍历 `apis/` 发现旧页面。

路径语义固定为：`source.path` 相对项目根；Markdown 与详情路径相对 `docs/doc-view/` 文档根，例如 `apis/UserApi-find-<identity-hash>.md` 和同 stem 的 `.json`。索引与详情不写本机绝对项目路径。读取时拒绝绝对路径、`..` 越界及逃出文档根的符号链接。

JSON 字段、schema 版本、错误 envelope、规范化与 hash 算法及可复制样例由同目录 [`format.md`](format.md) 维护；用户入口见[`docs/mcp-format.md`](../../../docs/mcp-format.md)。根 index entry 保留搜索摘要，包括存在时的 `httpMethod` 和 `httpPath`；`httpPath` 也属于本版本关键词检索字段。每个详情文件通过现有稳定 identity 关联，记录相同的 identity 字段。

另一方案是把所有字段树塞入 `index.json`。不采用该方案，因为它会让每次搜索载入响应 DTO，也会让大量条目扩大根索引；独立详情文件允许只按需读取单个接口。

### 2. 复用一次 DocView 解析，固定已提交的 Document 文本快照

项目生成先确保相关打开文档的 `Document` 已提交到对应 PSI，再在受保护的后台 read action 中解析接口并捕获本次输入文本。这里的“提交”只表示 IDE 内 `Document` 与 PSI 同步；不得调用自动保存或把用户未保存编辑写回源码文件。不得把磁盘 VirtualFile 字节 hash 当作本次解析内容 hash。

在已批准的 IU 263 基线上使用公开 `ReadAction.nonBlocking(Callable)`，关联项目存活、Document 提交状态及外层取消 indicator；后台工作线程同步取得结果，写锁优先时允许平台中止并重新调度读尝试。每次尝试先核对同一输入 epoch，来源已变化即返回 `SOURCE_CHANGED`，不把不同代解析结果混合；只有一次成功返回的 PSI-free snapshot 能进入发布。解析使用当前读尝试的 indicator，以便写锁抢占与用户取消都能生效，文件发布仍在 read action 外执行。

对每个 API 源文件，在这份一致快照中捕获的确切 Java Document 文本上计算 `source.capturedContentSha256`。在 index payload 顶层用 `hasUnsavedDocuments` 标记生成时当前项目内容根中是否存在未保存 Java 或 XML Document。Dubbo 目录解析会读取项目范围内的 Beans XML 声明，因此 XML Document 也是生成输入；为避免每次生成前扫描 XML 内容来缩窄标记，采用保守项目范围，可能包含未被本次接口引用的 Java 文件或没有 Dubbo 服务声明的 XML 文件。该字段不声称实现了完整 Java DTO 依赖追踪。`source.capturedContentSha256` 只标识对应 API Java 源文件的 Document 文本，与落盘文件 hash、JSON payload hash 及 `snapshotId` 分离。若无法取得已提交的一致文本，整个生成在开始发布前失败，不将 PSI 与磁盘内容混成一份快照。

从同一 `DocView` 实例复制接口身份、显示名/描述、框架、HTTP 元数据、tags、headers、请求参数、请求体、响应字段树及生成时源码位置到 immutable、PSI-free DTO，并从同一实例渲染现有 Markdown。读阶段结束后，发布只接收 DTO、Markdown 文本、hash 和相对路径；不得传递 `PsiElement`、`DocView` 或 parent pointer。使用项目既有 Gson/runtime 配置和显式 DTO，不新增依赖，不直接序列化任意对象图。

Producer、会影响输出的 Settings 和模板输入分别记录稳定 fingerprint；settings fingerprint 不得包含 token、password 等凭据，模板内容只记录 hash。源码内容 fingerprint 同源文件仅计算一次，再由该文件下的方法共享。

Producer 版本从插件专有 `META-INF/doc-view-build.properties` 读取。Gradle `processResources` 用与 `patchPluginXml` 相同的 `pluginVersion` 展开版本，资源中固定插件 ID；测试和最终包审计必须核对它与实际 descriptor 一致。运行时不访问 Internal PluginManager API，也不依赖测试与安装环境不同的 defining classloader 类型；资源缺失、插件 ID 不匹配或版本未展开时明确失败，不使用 development/未知版本回退。

从捕获输入到最终 `index.json` 提交前，若观察到相关源码、模块、Settings 或模板变化，则以 `SOURCE_CHANGED` 停止提交；若此前已有页面或详情替换，按发布规则返回 `PARTIAL_PUBLICATION` 并列出已发布路径。

另一方案是重新解析 PSI 生成 JSON 或解析输出 Markdown。两者都会产生与 Markdown 不一致的来源版本或无法稳定抽取用户模板控制的结构字段，因此不采用。

### 3. 分离内容 snapshot、序列化完整性和源码新鲜度

`snapshotId` 确定性地标识排序后的接口身份、Markdown/detail 内容 hash 及 producer/source fingerprint，不包含时间戳。JSON envelope（排除 integrity）完整性 hash 按 `format.md` 定义的规范化 JSON 计算，并排除其自身字段。Markdown 与 JSON 的文件 byte hash 用于验证 entry 指向的确切文件内容。`source.capturedContentSha256` 只对应已捕获的 Document 文本。四类值用途不同，不能互相替代，也不能用完整性 hash 推断源码实时性。

Freshness 是当前 IDEA 进程可证明的观察状态：本进程成功生成且没有观察到项目内容根内 Java/XML Document、module root、模板或输出相关 Settings 变化时为 `current`；已观察到相关变化时为 `stale`；从磁盘加载的快照一律为 `unknown`，即使其时间戳很新或 source hash 与磁盘相同。XML 按项目内容根保守跟踪，因为其中可能含有影响 Dubbo 候选发现的服务声明。新鲜度不因普通查询而重新扫描源码或自动重生成。它表示本进程未观察到变化，不表示绝对实时。

另一方案是每次查询 hash 全项目源码以推断 current。该方案会使有界索引查询退化为项目扫描且仍有竞争窗口，因此 freshness 只依赖本进程追踪的 epoch 和明确未知状态。

### 4. 用项目读写锁、同根 OS 排他锁和 hash fail-closed 协作

同项目查询在 project-scoped read lock 中选取 immutable index snapshot 并读取详情；写入持有对应 write lock，防止本 IDE 进程中的查询读到正在逐文件替换的中间状态。保留现有项目级 export guard，再用针对规范化 `docs/doc-view/` 根的独占 OS file lock 阻止另一个进程或 IDEA 实例同时写同一根。锁获取在后台执行，等待受期限和取消约束，不得让 EDT 无限阻塞；锁不可用、被占用或等待超限时，在更改受管理文件前返回 MCP design 定义的 `BUSY`，不得退化为无锁写入。锁文件为内部隐藏文件，不出现在索引 entries。

这些锁只协调遵守同一锁协议的 Doc View writer，不能阻止编辑器或其他程序直接改磁盘。writer 保留整批预检快照，并在每个替换前复核目标状态；reader 按 root entry 读取文件的确切字节并校验路径、identity、schema 与 expected hash。观察到外部文件/VFS revision 变化时，使受影响 cache 失效并重读验证；任何不能验证的索引或详情都按 MCP design 中相应的大写错误码 fail closed。不得把命中内存 cache 当成被改文件仍有效的证明。

完整性错误按快照和文档根内的规范相对资源路径分别记录，例如 `apis/<stem>.md`、`apis/<stem>.json` 与 `index.md`。只有该资源后续读取完整通过路径边界、raw-byte hash、适用的 schema/identity 和 UTF-8 校验，并且读取期间没有更新的失败记录时，才清除这一条错误；其它资源的错误继续保留。失败序号单调递增，cache 清理或快照提交不得重置序号，以免较早开始的成功读取清除较晚发生的失败。清除错误会通知状态观察者，但不更改 source/settings epoch 或 freshness；当最后一条完整性错误清除后，状态回到原有的 `current`、`stale` 或 `unknown`。

另一方案是等待或排队相同根的另一个 writer。不采用，因为过时或已取消的 snapshot 不能在稍后被隐式写入；竞争请求明确返回 `BUSY`，由用户重试。

### 5. 按文件边界取消，以最终 index.json 作为提交点

writer 构造一整代输出，在验证资源上限后一次性预检所有 Markdown、详情和索引目标；所有变更先写到各目标目录的临时文件，再次校验目标未被用户或外部程序更改。按 API Markdown/JSON 详情、`index.md`、`index.json` 的顺序逐文件 `ATOMIC_MOVE`。

每个单文件 move 前后检查同一 ProgressIndicator 和 project lifecycle。单次 move 不可安全中断：若取消在该 move 内到达，等待 move 完成，记录该路径，再在下一个文件前停止并清理剩余临时文件。首个成品 move 前取消保持无文件变化。普通页面或人读 index 替换后、机器 index 提交前取消，旧 `index.json` 保留；已更新稳定路径可能与旧 entry 的 hash 不符，因此旧 snapshot 的相应详情必须拒绝读取。该情况返回 `PARTIAL_PUBLICATION` 和已发布路径，不能用普通 `ProcessCanceledException` 经过现有静默处理。

最终 `index.json` move 开始前再检查取消与 project disposal。若检查已观察到取消，停止并保留旧 index。最后一次检查返回后立即进入不可中断的单次 move；该检查与文件系统调用并非原子操作，随后到达的取消按 move 期间到达处理，只有 move 成功才算提交。成功后磁盘提交 wins。project 仍存活时立即将同一 snapshot 安装到内存服务，结果按完成处理，不回滚、不声称已取消。若 project 在最终 move 期间关闭且该 move 成功，保留磁盘提交，不回调已 dispose service/UI；下一次打开标为 `unknown`。

最终 index 前 I/O 失败或进程崩溃时，旧 root index 保留，部分稳定路径可能已换代；hash 不匹配的详情 fail closed，整批重试后收敛。writer 预检使用文件自身 marker/self-hash 与目标 identity 判断所有权，JSON 还须保持格式规定的 JCS+LF 成品字节，额外空白/重排版也视为冲突，不把旧索引 raw-byte hash 当作写入前置；partial 产物自身仍有效时允许重试，否则会永久锁死恢复。自身校验失败仍是 CONFLICT。此协议不保证旧 snapshot 所有详情一直可读。若要保证任意错误/崩溃后旧快照完整可用，需改用 immutable generation 目录和单一 pointer，另立存储迁移与旧版本回收设计。

另一方案是把所有文件 move 视作一个短暂、不可取消区间。不采用，因为一批最多可有 10,000 个接口，用户不应等待整批替换完成才能取消；逐文件检查把取消延迟限制在正在执行的一个原子文件 move。

### 6. 只在已载入摘要上搜索，cursor 绑定查询条件和 snapshot

首次发现或读取时校验 root index 并载入 project-scoped immutable entry 列表。query 和可搜索文本字段按 NFKC 与 `Locale.ROOT` 小写归一化，再按空白拆词；每个词都要命中同一 entry 的至少一个 name、description、qualifiedClass、fullSignature、module、项目相对 source path、`httpPath` 或 tags 子串。词之间为 AND、每词字段之间为 OR。`module`、`framework`、`httpMethod` filter 只与条目中的规范值精确匹配，不对 filter 做小写归一化或模糊匹配。

空 query 合法，可按全部 entries 浏览；结果按现有 identity canonical form 排序，默认每页 20 条、最多 100 条。cursor 携带 snapshotId、归一化 query、filters 和上一页最后的 identity；任一绑定条件改变返回 `CURSOR_STALE`，不得拼接不同代结果。status/search 只验证 index 自身，不逐项扫描页面；已知某页不一致可以报告状态，但有效旧 index 摘要仍可查询。detail 通过 entry 的同 stem `.json` 路径按需读取并验证，不重解析 Markdown、访问 PSI 或扫描 `apis/`。

UI 对状态检查、搜索和打开操作共用 `CANCELLED` 错误映射，因此共享提示必须描述一般的“项目文档操作”已取消，不能把搜索/打开取消标为“状态检查”已取消。项目文档生成有独立的生成取消 key，保留其任务语义。

另一方案是直接新增 trie、数据库或 IDEA 专用索引。不采用；先测量受限 10,000-entry JSON 摘要，未达性能目标前不加另一套失效机制。

### 7. 明确文件、查询和耗时上限

沿用 spec 与 `format.md` 定义的边界：index 最大 32 MiB、最多 10,000 entries、每份 Markdown/详情最大 1 MiB、详情字段树深度最大 32、每接口字段节点最多 10,000、query 最大 256 Unicode code points 且最多 16 词、查询最多 5 秒、生成从操作开始到最终 index commit 最多 120 秒。超限不截断，且在 final `index.json` commit 前显式以 `LIMIT_EXCEEDED` 或对应 MCP 大写错误码失败。详情或 MCP 工具总响应超过协议包装上限时由 MCP adapter 返回 `RESULT_TOO_LARGE`。

10,000 entries 下 warm search p95 ≤200 ms、cold index load ≤2 s、query cancellation p95 ≤500 ms 是待测验收目标。记录机器、IDE build、JDK、fixture 大小、采样数和分位数；light fixture 计时不作为 native IDEA 性能结论。

#### 索引解析与取消轮询

冷索引查询把同一个 `ReadContext.checkActive` 传入格式读取器。严格 UTF-8 解码由有界字符缓冲驱动；JSON 字符块、完整性 canonicalization 和 SHA-256 按固定字符或字节间隔轮询，索引语义校验按条目轮询。Gson 的内存树到 DTO 转换只在调用前后检查，不声称能中断其内部调用。`ProjectDocumentFormat` 仍是无 PSI 的格式边界，不依赖项目或 IDE 服务。轮询中观察到的原始取消/超时异常原样传回查询层，并映射到 `CANCELLED` 或 `TIMEOUT`；不得被 JSON 解析错误处理改写成 `INDEX_INVALID`。默认格式 API 仍使用无操作回调，序列化结果与 schema v1 golden 字节保持一致。

### 8. Dubbo 目录发现按声明范围与接口源码模块归属

项目目录生成继续复用现有 Dubbo PSI 方法解析与 Markdown 字段规则；候选发现和模块归属按本节修复。后续原生卸载发现的 XML 生命周期问题按下一节处理，参数/响应 DTO、模板或页面 identity 不变。

Dubbo 注解从 project-scoped `JavaAnnotationIndex` 取候选，以 `Service` / `DubboService` 短名检索，再按 `DubboConstant.SERVICE_ANNOTATIONS` 的完整 FQN 精确过滤。注解位于接口时收录接口；位于实现类时遍历其父类和接口的 PSI `getSupers()` 闭包，按接口所属模块收录。遍历使用 visited 集合，避免菱形继承重复访问；不引入全项目 `AllClassesSearch`。

XML 文件也在 project scope 发现，因为 provider XML 可以注册另一模块的 API。每条 `<dubbo:service interface="…">` 先由 XML 所属模块确定 declaration scope，再用该 scope 解析 FQN；只有解析所得 Java 接口的源码归属等于当前扫描模块时才进入该模块目录。不得把 project scope 中所有同 FQN 类都加入，也不得按 XML 文件所在模块归属接口。注解、继承和 XML 循环都检查平台取消状态；所有来源最终汇入同一集合并排序，使同模块重复注册只生成一项。

### 9. XML PSI 替换自有 DOM model，保持识别与导航语义

2026-11-08 的真实卸载 heap 证实：平台 `ClassNameReader.EARLY_EXIT` 静态字段持有 CGLIB 异常，其 backtrace 间接保留 Doc View 类加载器。回溯类镜像包含 Doc View gutter、Dubbo 识别与平台 DOM 代理类；具体方法顺序由当前源码推断，heap 未提供可解码的方法帧。较晚 heap 中 `DocViewWindowPanel` 实例已为 0，不据此修改搜索面板或 JList。

修复仅替换本插件 XML DOM 边界：`DubboPsiUtils` 的识别/目录发现与 `DubboDefinitionSearch` 共享公开 `XmlFile` / `XmlTag` PSI 读取；移除 `plugin.xml` 的 `dom.fileMetaData` 注册和三个自有 DOM model 类型，保留原 definitions scoped search extension。根名必须为 `beans`，只读直接子标签 `dubbo:service` 的 `interface` 属性；缺少/空属性跳过，字符串按 XML PSI 解码，不增加别名或递归容器。目录使用 project scope 与声明模块依赖解析，现有直接识别/定义导航保留 all scope 的查找边界；consumer 拒绝后立即结束，循环遵循平台取消。

不修改 SDK、反射清空 CGLIB 静态字段或捕获/吞掉取消；不增加生产依赖、语言或专用索引。XML 候选发现使用公开 `PsiSearchHelper.processElementsWithWord` 的 `IN_FOREIGN_LANGUAGES` 上下文及 XML 文件范围，以 `dubbo` 为候选词，再按精确标签结构校验并对文件去重；该索引路径已由真实 beans XML 和提交但未保存 Document 的 fixture 验证，不全量解析所有 XML，不增加缓存或静态 PSI 状态。`XmlTagNamesIndex` 经目标 SDK 核对实际只索引 XSD element 声明，已排除。具体 API 与测量边界写入 verification。

自动回归覆盖 XML-only、实现类注解、重复和跨模块同 FQN、缺失属性、根/标签边界、未保存已提交 XML、定义导航与 consumer 停止。重新冻结包并通过 Java 25 构建、全部测试和两个精确 build 的 Plugin Verifier 后，用新进程实际触发 XML 识别、生成/查询和导航，再从原生 Plugins 禁用，仅由该进程的日志及 GC 证明卸载结果。

## 风险与权衡

- [部分 move 后取消或 I/O 错误会让旧索引引用到 hash 已变化的稳定路径] → 保留旧 `index.json`、列出已发布路径并对不匹配详情 fail closed；完整重试收敛，不承诺 batch rollback。
- [长批量写出期间发生取消] → 每个 move 边界检查并返回明确的 `PARTIAL_PUBLICATION`，不把 10,000 个文件当成不可取消区间。
- [目标文件系统不支持或拒绝独占锁] → 在受管理文件变化前返回 `BUSY`/`IO_ERROR`，不无锁发布。
- [非 Doc View 程序可在最后复核后改写磁盘] → 每次读取按确切字节校验 hash，发现变化时按资源路径保留错误并失败关闭；恢复原始受管理字节后，只有完整校验成功才清除该资源错误；文件系统没有跨文件事务。
- [重新打开项目后无法证明编辑器关闭期间的来源变化] → 磁盘 snapshot 固定为 `unknown`，时间戳/hash 不会让它变成 `current`。
- [未知 schema 或手工修改使 store 无法查询/覆盖] → 返回统一大写错误码；只有用户显式重新生成且旧目标所有权与完整性可验证时才允许重建，不接管修改文件。
- [JSON 详情与 Markdown 有部分重复] → JSON 只保留搜索摘要和结构化详情所需字段；用代表性大 DTO 验证容量，不复制整份 Markdown 到 root index。

## 迁移计划

现有 `index.md` 与 API Markdown 继续作为普通文档阅读。旧目录只有 Markdown 时，机器查询返回 `INDEX_NOT_FOUND`，只有用户显式执行项目文档生成后才产生 schema v1 JSON；查询时不扫描 PSI、不反解析 Markdown。未知 schema 不自动适配。显式重新生成只有在旧 JSON 所有权与完整性可验证且文件未被修改时才可覆盖；否则返回 `CONFLICT` 并保留原文件。旧页面不自动删除。回退旧插件仍可读取 Markdown，但不承诺读取新 JSON；本 change 不提高 IntelliJ 最低版本。
