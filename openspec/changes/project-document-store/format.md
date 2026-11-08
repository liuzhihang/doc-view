# 项目文档库格式 v1（待实现）

本文件与本 change 的 spec、[JSON Schema](schemas/document-store.schema.json)、[虚构样例](examples/index.json) 一起定义首版持久化格式。它是实现契约；本地开发实现及实际验证范围见 tasks.md，不代表已发布插件已提供 JSON 文档库。用户入口见 [格式说明](../../../docs/mcp-format.md)。

## 文件与目录

```text
<project>/docs/doc-view/
  index.md
  index.json
  apis/
    <existing-page-stem>.md
    <existing-page-stem>.json
```

Markdown 的正文模板、所有权标记和现有文件名算法继续使用已有 exporter。详情 JSON 与对应 Markdown 同 stem。writer 使用同目录的 `.doc-view.lock` 文件取得跨进程排他锁；该文件不是文档或索引，解锁后保留 inode，查询不读取它。目录自动绑定当前已打开 Project；不提供独立目录注册表或任意磁盘目录扫描。查询只读已提交的 `index.json`，不得扫描 `apis/` 猜测目录。

`index.md` 给人阅读；`index.json` 保存摘要和文件指针；单接口 JSON 保存参数、响应字段及文档说明。三者必须从同一次 PSI 解析快照生成。查询的内存投影可以丢弃，落盘文件不是可以随意覆盖的缓存。Clear Cache 只清派生内存，不删除这些文件。

## 通用 envelope

两个 JSON 类型都采用 `{schemaVersion, owner, kind, payload, integrity}`。schemaVersion 固定为整数 `1`，owner 固定为 `com.liuzhihang.doc.view`，kind 为 `index` 或 `api`。不允许额外字段、重复键、无效 Unicode、浮点数或非标准 JSON 值；字符串示例保留业务原文，不作为可执行指令。

`integrity` 为 `{algorithm:"sha256-jcs", value:"<64 lower-case hex>"}`。计算对象是去掉顶层 integrity 后的整个 envelope，采用 [RFC 8785 JCS](https://www.rfc-editor.org/rfc/rfc8785) 规范化后取 UTF-8 SHA-256。V1 的数值字段只允许非负、有界安全整数；示例业务值全部为字符串，避免自定义数值格式影响一致性。Java 实现不得假定 Gson 默认输出等于 JCS：需明确排序、转义和整数规则，并使用跨语言 golden 验证。

写盘 JSON 采用上述 JCS 字节加一个 LF；读取时校验解析与自摘要，允许合法空白但不忽略 index entry 指向文件的**原始字节 hash**。这些 hash 用于损坏/手改检测，不是数字签名或抵御蓄意修改的安全凭证。未知 schema 返回 `UNSUPPORTED_SCHEMA`；坏 schema/self-hash 返回 `INDEX_INVALID`；已登记文件丢失或与登记 hash 不一致，在实际读取该文件时返回 `INDEX_INCONSISTENT`。status/search 只校验 index 自身和摘要，不预读全部页面；已知某页不一致可在 status 报告 inconsistent，但有效旧 index 的摘要仍能搜索，详情必须单独通过 hash 校验。

## 索引 payload

| 字段 | 含义 |
| --- | --- |
| snapshotId | 该完整文档代际的内容身份，见下文算法 |
| generatedAt | 首次发布该内容代际的 UTC RFC 3339 时间；无变化重试不改时间 |
| producerVersion | 实际插件版本，不从用户输入接受 |
| generatorFingerprint | 输出相关模板、设置、解析版本的规范输入摘要；排除账号和凭据 |
| hasUnsavedDocuments | 生成时项目内容根内是否有未保存 Java 或 XML Document；保守覆盖文档生成输入，可能包含未被当前接口引用的 Java 文件或没有 Dubbo 服务声明的 XML 文件 |
| markdownIndex | `{path:"index.md", sha256}`，摘要取完整落盘字节 |
| entries | 按 identity.canonicalForm 排序的摘要记录，最多 10,000 条 |

每条 entry 包含 `apiId`、`identity`、`name`、`description`、`framework`、`httpMethod`、`httpPath`、`tags`、`source`、`markdown` 和 `detail`。framework 为 `spring|feign|dubbo|interface`；HTTP 元数据不适用或无法确定时用 null，禁止编造请求方法/URL。普通 Java 接口沿用现有 includeNormalInterface 设置，标记 interface，不冒充 Dubbo。首版每个 Java 方法对应一个 entry，不把尚未展开的多路由映射伪造成多个唯一端点。`httpPath` 只记录已有解析结果能确定的表示；不宣称完整 OpenAPI 路由枚举。

identity 含 module、qualifiedClass、methodName、parameterTypes、fullSignature、canonicalForm。fullSignature 使用当前 `ProjectDocumentIdentity.displaySignature()`（完整参数类型），类名和模块在独立字段中；canonicalForm 和 apiId 沿用现有 identity 编码与 SHA-256。身份输入依次为编码版本整数1、模块名、项目相对源路径、类 FQN、方法名、参数数量及完整参数类型；字符串是四字节 big-endian UTF-8 长度 + 字节，整数为四字节 big-endian。canonicalForm 为该字节串的无 padding Base64URL，apiId 为其 SHA-256。不同项目仍必须由 projectPath 隔离，不声称 apiId 全球唯一。

source 为 `{path,line,capturedContentSha256,basis:"ide-document-text"}`。path 相对项目根，line 为生成时一基行号或 null。capturedContentSha256 对应同次已提交到 PSI 的 Document 文本 UTF-8（尚未保存时也取实际 Document 文本），不自动保存用户文件，不把磁盘内容 hash 冒充当前编辑器输入。解析结果来源与捕获文本必须处于同一输入代际；生成期间任何相关源码、模块、设置或模板变化在最终提交前以 `SOURCE_CHANGED` 终止，已有 partial 按发布规则报告。该 hash 是来源标识，不是所有传递依赖均未变化的证明。

markdown/detail 都是 `{path,sha256}`。path 相对**文档根**，必须位于 `apis/` 并且 stem 一致；source.path 相对**项目根**，不能混淆。所有路径统一 `/`，拒绝空段、`.`、`..`、绝对路径、驱动器前缀、反斜线、NUL 和经过符号链接逃逸；打开文件前也验证真实路径，不能只信 JSON Schema 的正则。

## 接口详情 payload

详情包含相同 apiId/identity/source、文档 name/description/framework/httpMethod/httpPath/tags，以及：

- `headers`：已有解析的 name/description/required/example。
- `requestParameters`：原 Doc View 参数文档投影，不在本 change 中重新推断 Query/Form/Body 绑定位置。
- `requestBody`、`responseBody`：类型及字段树；不存在则为 null。
- `examples`：request/response 字符串或 null，沿用 `DocViewData` 的展示内容。请求可能同时含 Form/JSON fenced block，响应可能含 JSON fence；这些字段不保证是可直接发送的原始请求体。
- `remark`：说明字符串。

每个字段节点含 name、type、description、required、example、since、version、isCollection、isMap、children；name 允许空字符串，用于现有解析器的匿名标量返回值或集合根，不能为满足校验捏造字段名或丢弃其说明/集合标志；since/version 为原有文档版本说明（字符串或 null），两个集合标记沿用现有解析结果。required 为 true/false/null（未知），example 为字符串或 null。不保存 PSI、父节点引用、完整源码、绝对项目路径或上传凭据。层深最多32、单接口所有字段节点共10,000，超限显式失败而非截断。上述结构只承诺当前解析器能提供的文档内容，不承诺完整 OpenAPI 类型系统。

详情不嵌入 snapshotId，避免 index 的文件 hash 与详情 snapshotId 形成循环；MCP get 的外层结果绑定 snapshotId，读取时核对该代 index 的 detail hash。结构详情同时保留其独立 self-hash。

## 快照算法与幂等性

1. 固定一次解析输入代际；确定所有 Markdown 字节和结构化详情字节，并计算 raw-byte SHA-256。
2. 生成 index payload。暂时移除 payload 的 snapshotId 和 generatedAt，对剩余 payload 做 JCS + SHA-256，得到 snapshotId。
3. 若与有效现有索引同 snapshotId，则保留原 generatedAt 和所有未变化字节，不重写文件。若有变化，在发布前选定 generatedAt，计算整个 index envelope 的 integrity 并暂存。
4. 全部所有权、大小和路径预检通过后才逐文件发布；`index.json` 最后原子替换。不能把单文件 move 叫作跨文件事务。

写入时的所有权校验与读取时的快照绑定不同：JSON 用固定 owner/kind、目标 identity、有效 self-hash 及严格的 JCS+LF 成品字节形式验证受管理且未手改（重新排版或额外空白也视为写入冲突），Markdown 用现有 marker/正文 hash 验证；旧 index 中的 raw-byte hash 只约束旧代读取。上次 partial 后，文件可能不再匹配旧 index，但若自身所有权/identity/self-hash 仍有效，则显式完整重试可以替换它并提交新代；不能仅因旧索引不匹配永久阻止重试。手改且自身 hash 不符、未知 owner 或错误 identity 始终 CONFLICT，不借 partial 放宽。

snapshotId 不包含自身或时间，且 index 不包含自身 raw-byte hash，因此不存在循环计算。Markdown index 的内容不得加入每次调用时间。更换输出规则/插件生成器可以改变 generatorFingerprint/snapshotId；仅查看、搜索或刷新界面不改变磁盘。

## 查询与运行时状态

搜索只读 index 摘要：query 和可检索字段 NFKC + Locale.ROOT 小写，空白切词，词间 AND、字段间 OR 子串。字段为 name、description、qualifiedClass、fullSignature、module、source.path、httpPath 和 tags。module/framework/httpMethod 是规范值精确筛选；不对过滤值任意模糊化。空 query 可浏览目录；默认20条、最多100条，按 canonicalForm 稳定排序。cursor 绑定 snapshotId、规范 query、filters 和上页最后身份，跨代或跨查询返回 `CURSOR_STALE`。

新鲜度不持久化为 current：同一进程成功生成且未观察到输入变化为 current；观察到变化为 stale；重启/只读加载或无法证明为 unknown。JSON 完整性正确不代表源码最新。每个结果带当前快照状态；查询不触发保存、生成或 PSI 重扫。

容量上限为 index 32 MiB、单个 Markdown/详情各1 MiB、10,000 entries；query 最多256 Unicode code points / 16词。查询期限5秒，生成从开始到提交期限120秒；每项目最多4个读任务、1个写任务。跨实例使用文档根写锁，锁忙或本项目处于发布临界区时查询/写入可取消且受期限约束，返回 `BUSY`，不阻塞 EDT 无限等待。性能指标是待测目标：10,000条 fixture 的 warm search p95≤200ms、cold load≤2s、查询取消 p95≤500ms。

## 维护与验收

格式变化必须同时修改本文件、spec、schema、合法及拒绝样例、Java golden、用户说明和 MCP adapter 合约。schemaVersion 与 MCP envelope schemaVersion 独立。JSON Schema 能验证结构；identity 重算、排序/唯一性、路径/符号链接、hash、总字段节点数和版本支持策略仍需语义校验。不得以 schema 通过替代原生读写验收。

当前 `examples/` 全部为虚构公开样例，仅证明文档格式自洽；不是生产插件导出证据。未来需以实际 exporter 的输出重复验证。旧 schema 不加兼容读取器；显式重新生成仍须先验证所有权及手改冲突，不能把未知格式文件当作缓存强制覆盖。
