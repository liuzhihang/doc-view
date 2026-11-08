# Design

## Context

动机见 [proposal](proposal.md)。生产 descriptor 目前没有 MCP 依赖或扩展，独立 PoC 不随生产包交付。现有 Java `DocView`、`Body`、`Param` 含 PSI/父引用，不可直接作为磁盘或 MCP DTO。文档库与 UI 的服务边界由 [project-document-store](../project-document-store/design.md) 定义；本 adapter 只负责协议参数、项目绑定、结果编码和取消传播。

## Goals / Non-Goals

实现同一插件、同一文档库、同一生成链路。第一版只支持固定项目文档根及精确验证的平台；不建立独立进程、HTTP 服务、跨插件公共 API、AI 摘要、任意目录注册或旧版适配。`resources/list`、MCP prompts、任务协议和推送订阅不纳入首版；先完成四个工具的闭环。

## Decisions

### 1. 依赖与注册

通过主 descriptor 的 optional dependency 指向同包 MCP descriptor，后者注册公开 `com.intellij.mcpServer.mcpToolsProvider` 实现。使用显式 Provider 定义工具 inputSchema 与调用，不依赖反射自动推导任意 Java DTO；这是已验证取消探针采用的可行方向。生产注册需按目标 SDK 再验证，禁止使用 Internal 扩展、反射分支或把探针类复制进生产。

MCP 相关类仅在可选 descriptor 生效时加载；普通 UI/service 不引用 MCP 类型。编译使用目标 IDEA bundled plugin，结果通过公开 Text 内容类型返回 Gson 生成的 JSON 文本；不新增 Kotlin/序列化生成插件。协议 `structuredContent`/`outputSchema` 的 Java API 尚未完成生产验证，首版不同时维护两套结果；选用已经实测可工作的 Text(JSON) 并为其内容单独维护 schema。它仍是正式 JSON 工具结果，不是把 `index.json` 原样当整个 MCP 响应。

用户在 IDEA **Settings → Tools → MCP Server** 启用服务并从该页面复制/配置客户端连接。插件不硬编码端口，不创建另一服务，不自动修改 Codex 配置。目录绑定只使用已打开 Project 的规范 base path + `docs/doc-view/`；查询前验证 root、路径、版本和完整性。工具的 `projectPath` 必填；宿主若已注入同名参数，schema 仅保留一个定义，必须通过真实 tools/list 校验无重复或冲突。

### 2. 工具输入和职责

所有输入对象 `additionalProperties=false`；除列出的可选字段外不可省略。项目路径只用于当前机器的路由与响应，不写入可提交的文档产物。

| 工具 | 输入 | 输出 data | 写盘 |
| --- | --- | --- | --- |
| `doc_view_status` | `projectPath` | root、indexState、entryCount、generatedAt、正在生成与否；无需扫描源码 | 否 |
| `doc_view_search` | `projectPath`, `query`（可空）；可选 `module`, `framework`, `httpMethod`, `limit=20`, `cursor` | items（摘要）、nextCursor、totalMatched；按文档库固定规则过滤/排序 | 否 |
| `doc_view_get` | `projectPath`, `apiId`, `snapshotId`；可选 `includeMarkdown=false` | api（结构化详情 payload）、可选 markdown | 否 |
| `doc_view_generate` | `projectPath` | outcome、scanned/created/updated/unchanged/skipped、publishedPaths、conflicts、cleanupFailures | 是，唯一生成/更新入口 |

search limit 1–100，query ≤256 Unicode code points / 16 词；只搜索清单字段（含已解析的 httpPath），httpMethod 使用精确 filter；不对全部 Markdown 做全文扫描，不宣称语义检索。读已有库允许 Dumb Mode；生成必须处于可读 PSI 的项目状态，否则返回 INDEXING，不排队无限等待。无库查询不隐式生成；客户端需要根据用户意图单独调用 generate。查询跟随新的 index 提交失效，cursor 绑定输入与 snapshotId；get 必须使用搜索返回的 snapshotId，避免更新期间读另一代。

### 3. 结果、错误与注解

Text(JSON) 顶层格式：`{schemaVersion:1,status,projectPath,snapshotId,freshness,hasUnsavedDocuments,data,error}`。snapshotId 在无库时为 null；freshness 为 `current|stale|unknown`，不等于网络连接状态。hasUnsavedDocuments 来自所读索引，未知或无有效快照时为 null；status data 另含当前 pluginVersion 与 toolContractVersion，便于客户端连接后核对。status 为 `ok|error`；成功 error=null，失败 data=null 且 error 含 code/message/relativePaths/relativePathsTotal/retryable/details，不能把异常堆栈或私密请求正文放进 message。用于 status 的 `indexState` 为 `not_generated|ready|invalid|unsupported_version|inconsistent`；刷新/生成执行状态独立表达，不滥用 freshness。ready 时 entryCount 为实际数量；not_generated 为0，invalid/unsupported/inconsistent 无法可靠计数时为 null，不能填0冒充空库。

业务错误采用 MCP `isError=true` + 同一 JSON error envelope；输入 schema 错误遵循宿主 JSON-RPC 错误机制。真实协议取消按宿主协议结束，不强行补发一个成功 Text；若客户端仍能接收终态则以取消错误表示，IDE 本地保留发布结果用于排查。

generate 的 outcome 为 generated/updated/unchanged；数量按接口计数（不是文件数），publishedPaths 列出实际替换文件。partial 是错误终态，使用 PARTIAL_PUBLICATION，不伪装成成功 outcome。error.details 固定为 publishedPaths/publishedPathsTotal、indexCommitted（未知为null）及 cleanupFailures/cleanupFailuresTotal；字段和完整枚举以 tool-contracts.json 为准。

错误码：`INVALID_ARGUMENT`、`PROJECT_NOT_OPEN`、`INDEX_NOT_FOUND`、`UNSUPPORTED_SCHEMA`、`INDEX_INVALID`、`INDEX_INCONSISTENT`、`API_NOT_FOUND`、`CURSOR_STALE`、`SNAPSHOT_CHANGED`、`INDEXING`、`BUSY`、`CONFLICT`、`SOURCE_CHANGED`、`LIMIT_EXCEEDED`、`RESULT_TOO_LARGE`、`CANCELLED`、`TIMEOUT`、`PARTIAL_PUBLICATION`、`IO_ERROR`。取消/期限中途已发布文件，通过 error.details 说明 publishedPaths 与 indexCommitted=false，不伪装整批回滚。总结果 JSON ≤4 MiB；Markdown/单详情沿用文档库 1 MiB 限制，文档正文超限拒绝而非截断。诊断路径清单另有公开分页外边界：每组只展示按确定顺序排列的前100条，每路径最多512 Unicode code points，同时必带对应 *Total 实际总数，省略数为 Total - 数组长度，不能伪称清单完整；IDE本地服务保留完整结果。partial 已发布文件只列在 details.publishedPaths，不重复塞入 relativePaths；relativePaths 只列实际错误/冲突位置。所有 relativePaths/publishedPaths/conflicts/cleanupFailures 统一相对 docs/doc-view/ 文档根，adapter 将现有 exporter 相对项目的 docs/doc-view/... 报告验证后转换；拒绝越界，不通过字符串截断伪造路径。输出含原文档的业务描述，默认不写完整内容到日志。

如目标公开 API 支持工具 annotations，则三个读工具标 `readOnlyHint=true, destructiveHint=false, idempotentHint=true, openWorldHint=false`；写工具为 `readOnlyHint=false, destructiveHint=true, idempotentHint=true, openWorldHint=false`，因为会替换受管理文件。这些提示不是权限边界；遵循 IDEA/客户端既有授权流程，不新增一个永远弹窗的重复确认层。写操作可重试收敛，不自动网络重试。

### 4. 生命周期和取消

任务由插件拥有的 project service 负责，方法、递归、查询循环和每个文件发布边界观察同一取消信号。最多四个读任务和一个写任务/项目，超出返回 BUSY；同根多 IDEA 实例由文档库的排他写锁拒绝第二个 writer。不要持有 read action 进行磁盘 IO，不保存 PSI 到后台回调或缓存。

服务端读任务期限5秒、生成120秒；只是防止无界运行，不能代替客户端取消验收。发布取消遵循文档库 design：暂存时零成品变化；已发布部分文件则停止后续 move、旧 index 保留、记录 partial；最终 index 原子提交之后迟到取消不回滚。每个工具只 cleanup 一次，项目关闭和插件卸载必须移除任务、监听器、缓存及工具可见性。

Codex 原因与候选修复见[取消调查](../../../docs/mcp-cancellation.md)。截至 2026-10-24，官方 `0.165.0-alpha.1` app-server 候选已在隔离 IU `263.7031.23` 上完成三轮真实 turn-interrupt 取消、cleanup、无迟到完成和同 session 恢复，候选准入 tasks 1.1/1.2 已通过。`0.163.0-alpha.11.2` 的两轮透明 wire 失败仍保留，不能把结论推广到所有 Codex 版本。候选通过不等于生产插件或生产基线通过；正式实现仍等待文档库最终验收，以及精确 263 生产 build、Verifier 和原生回归。后续根据 initialize 实际协商的协议，记录匹配的通知或响应流取消及服务端 run/cleanup；不只检查 HTTP 202 或 turn=interrupted。

### 5. 验证和版本维护

可执行接口描述在 [tool-contracts.json](tool-contracts.json)，例子在 `examples/`。API 字段/工具名、手册、schemas、golden 和真实 tools/list 必须一起变更；tool-contracts.json 中复制的磁盘 $defs 必须与源 schema 逐项一致并由检查保护，不能独立演进；MCP schemaVersion 与磁盘 schemaVersion 各自独立。第一版拒绝未知版本，不保留旧格式读取分支。

先完成文档库和客户端门槛，再完成精确 IU 生产升级，最后实现此 adapter。先测试 status/search/get 无写盘及 A/B 隔离，随后开放 generate 的真实写入、冲突、取消、partial 与恢复。最终构建、Plugin Verifier、原生 UI 和 MCP 实测分别留证，不能用 unit test 替代。

## Risks / Trade-offs

- [Codex 客户端版本的取消行为不同] → `0.163.0-alpha.11.2` 的实际失败与 `0.165.0-alpha.1` 的三轮候选通过分别记录；生产工具仍要求按实际协商协议和目标平台验收，不把一版候选结果推广到其他版本。
- [Text(JSON) 不提供协议级 structuredContent] → 单一结果格式可由当前公开 Java API 实现，输入/输出内容用 schema 与真实调用验证；若以后升级封装，另改 contract，不同时返回两条不一致路径。
- [已落盘文档可能陈旧] → 每次返回 snapshotId/freshness，current 仅指当前进程观察范围未变，不宣称实时源码。
- [写入取消留下部分页面] → 机器索引最后提交、详情 hash 校验、显式 partial 报告和可重复生成；不承诺跨文件事务。

## Migration Plan

当前无生产 MCP 工具可迁移。首版在准入与目标 build 回归通过后加入同包可选扩展；普通 Markdown 通过显式生成补齐 JSON，冲突仍由用户处理。回退插件不自动删除/转换文档库；旧插件读取新格式不承诺兼容。发布与客户端持久注册另按用户授权执行。
