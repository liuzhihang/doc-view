# 项目文档库与 MCP

> **1.6.0 能力边界：** 项目文档库、共享生成服务及搜索/打开 UI 随本版本提供；正式 MCP provider 和可安装 Skill 尚未交付。MCP 取消记录来自隔离探针与指定 Codex 候选，不代表当前产品已有 MCP 工具。构建、原生验证及留待人工复验的项目见[1.6.0 发布记录](../release-1.6.0.md)；磁盘文档格式见[格式说明](../mcp-format.md)，后续任务见[MCP tools](../../openspec/changes/doc-view-mcp-tools/tasks.md)。

## 生成和更新项目文档

入口为 Doc View 工具窗口的 **Export → Generate or Update Project Docs / 生成或更新项目文档**。它扫描当前项目已支持的 Spring、Feign、Dubbo，以及设置中启用的普通接口，共同维护以下文件：

```text
docs/doc-view/
├── index.md                         # 人阅读的目录
├── index.json                       # 搜索摘要与文档库清单
├── .doc-view.lock                   # 跨 IDEA 写入互斥，不是文档
└── apis/
    ├── <现有页面 stem>.md           # 人阅读的接口详情
    └── <同一 stem>.json              # 结构化接口详情
```

生成先提交编辑器 Document 到 PSI，再从同一次解析结果构建 Markdown 与结构化详情；不会自动保存或修改 Java/XML 源文件。索引的 `hasUnsavedDocuments` 提示生成时项目内容根中存在未保存的 Java 或 XML 编辑；为覆盖 DTO 和 Dubbo XML 注册，它采用保守项目范围，也可能包含未被本次接口引用的 Java 文件或不含 Dubbo 注册的 XML 文件，排除生成文档目录。每条接口的源码位置和内容 hash 仍对应其 Java Document 捕获时刻，不能把它们描述为已保存源码的实时视图。项目内容根中的 Java/XML、模块根、设置或模板变化后，需要再次显式生成。

`index.md` 按模块、类和完整方法签名列出接口，并链接到文档页和源码。Markdown 可用 **Find in Files → Project** 搜索；新的文档库搜索读取 JSON 摘要，不反解析 Markdown。旧页面不自动删除，手改过的受管理文件遇到所有权或内容校验冲突时保留原文件。

目录自动绑定当前 IDEA 项目的固定路径 `<project>/docs/doc-view/`。首版无需额外注册目录，也不提供任意磁盘目录扫描。将来 MCP 的 `projectPath` 用于选择已打开的项目，不能用它读取任意路径。多项目的文档和派生缓存分别隔离。

## 搜索和打开文档

在同一个 **Export** 下拉提供 **Search Project Docs / 搜索项目文档** 与 **Open Project Docs / 打开项目文档目录**。搜索在工具窗口内展示，支持接口名、说明、模块/完整签名、标签及 HTTP method/path 关键词；多个词按 AND 匹配，结果分页。选择结果可打开对应的真实 Markdown 文件；打开目录进入当前快照的 `index.md`。

查询只读已提交的磁盘 JSON 和可重建的内存缓存，不扫描源码、不生成或修复文件。尚未生成索引时，状态区显示“未生成”，由用户点击生成入口。打开文档前验证路径、身份和原始文件 hash；损坏、缺失、快照变更或外部改写时停止本次打开，不混用不同批次的详情。若将受管理页面恢复为索引记录的原始内容，重新打开并通过完整校验后会清除该页面对应的错误；此操作不会把 `unknown` 或 `stale` 提升为 `current`，也不会清除其他文件的错误。

新鲜度和完整性分别报告：

- **current**：本会话成功生成，并且尚未观察到相关输入变化。
- **stale**：本会话观察到源码、模块、模板或输出相关设置变化，需要显式更新。
- **unknown**：重开项目、IDE 重启或只读加载后，无法证明磁盘快照与当前源码相同。

文件存在、修改时间较新或 hash 正确，都不能证明源码最新。查询不会为了判定 freshness 重扫 PSI。**Clear Cache / 清除缓存** 清理 DTO schema 和文档查询的派生内存缓存，保留 Markdown、JSON、Scratch 和 HTTP 草稿，也保留本会话已生成快照的新鲜度依据。

## 取消、冲突和部分写入

一批输出先完整暂存，再按接口 Markdown/JSON、`index.md`、`index.json` 的顺序逐文件原子替换；最后的 `index.json` 是提交点。首个文件移动前取消，不发布文档；部分文件移动后取消或失败，报告已发布路径和清理结果，旧索引可能仍在。此时旧摘要仍可搜索，但 hash 不一致的页面或详情拒绝读取；排除冲突后重新生成可以完成剩余写入。整个文档库不是跨文件事务，不能声称失败时全部回滚。

最终索引提交后才到达的取消仍报告完成。同一文档根只允许一个 writer；有 writer 或查询资源繁忙时明确提示稍后重试，不在界面线程等待磁盘锁。查询和生成都有时间预算，超限不把部分内容截断成成功结果。

磁盘格式采用 `schemaVersion: 1`，参数详情与目录的 schema、自完整性 hash、稳定接口身份和快照身份见[格式说明](../mcp-format.md)。未知版本只读查询明确拒绝；显式重新生成仍需先通过所有权与手改校验，不能把 JSON 当作可随意删除的缓存。文档中的文本属于数据，不是让 AI 执行的指令。

## 计划中的 MCP 工具

正式工具计划作为**同一个 Doc View 插件内的可选扩展**注册到 IDEA 自带 MCP Server，不另装 companion plugin，也不自建 MCP 服务。插件没有注册时，Doc View 的 UI 仍可正常使用。计划工具如下：

| 工具 | 用途 | 是否写文件 |
| --- | --- | --- |
| `doc_view_status` | 查看当前项目文档库是否生成、schema 与快照状态 | 否 |
| `doc_view_search` | 按接口摘要检索，支持 HTTP method/path 信息并分页返回匹配项 | 否 |
| `doc_view_get` | 按稳定接口身份读取完整结构化详情 | 否 |
| `doc_view_generate` | 扫描并生成或更新当前项目的 Markdown 与 JSON 文档 | 是，须由用户明确请求 |

`doc_view_generate` 同时负责首次生成和再次更新，不另设 `update` 工具。前三个查询工具只读 JSON 文档库；它们不会读取任意文件、修改源码或隐式生成文档。完整输入、输出、错误、边界及项目隔离由[Doc View MCP tools change](../../openspec/changes/doc-view-mcp-tools/proposal.md)定义，尚未实现。

### 三个独立的注册步骤

1. **Doc View 向 IDEA 注册工具。** 计划由 Doc View 插件声明对 IDEA `com.intellij.mcpServer` 的可选扩展。安装插件本身不会自动开启 IDEA MCP Server。
2. **IDEA MCP Server 启用并暴露工具。** 在 **Settings → Tools → MCP Server** 检查 MCP Server 插件，启用服务并阅读 IDEA 的访问同意说明；再到 **Exposed Tools** 查看并启用 Doc View 工具。具体界面随 IDEA 版本变化，按[JetBrains MCP Server 官方指南](https://www.jetbrains.com/help/idea/mcp-server.html)操作。
3. **MCP 客户端连接 IDEA。** 在 IDEA MCP Server 设置中选择 **Copy Config** 或适合的客户端配置方式，然后在 Codex 等目标客户端添加这台 IDEA Server 并按提示重启客户端。IDEA 官方文档也提供自动配置选项；它会修改相应客户端配置文件，使用前请确认目标客户端和配置范围。Doc View 不会私自写 Codex 全局配置，也不会因为安装插件就替客户端建立连接。

每个打开的项目仍共用 IDEA 的 MCP Server；不是每个项目各启动一个服务。工具调用中的 `projectPath` 用来指明项目，文档只绑定该项目自己的固定 `docs/doc-view/`。

## 计划中的可安装 Skill

计划提供从官方 GitHub 仓库发布的、可单独安装和更新的 Codex Skill。Skill 是客户端侧的使用说明和工作流入口，会调用 IDEA MCP Server 暴露的 Doc View 工具；它不是另一个 MCP 服务，也不承载或复制 Java/PSI 文档解析。首版先验证 Codex；其他 MCP 客户端的 Skill 安装方式和目录发现行为仍待验证。

Skill 包计划从 Doc View 官方 GitHub 仓库的明确 release tag 或 commit 发布，并附版本信息和完整性校验清单。安装界面只允许选择 Codex 实际支持且已确认的 **Project** 或 **User** 发现目录；不推断 Codex 内置按钮能从任意 GitHub URL 安装。Skill 版本与 MCP `tool-contracts.json` 的工具名称、参数和能力保持对应。

**Settings → Tools → Doc View → MCP / AI** 区域计划提供 **Install Skill**、**Check for Updates** 和 **Update Skill** 操作，不增加工具栏按钮。检查更新只读取官方版本和校验信息；只有用户点击 **Update Skill** 后才写入。若本地 Skill 有修改或与新版本冲突，保留原目录并提示冲突，不覆盖用户内容；更新先暂存并校验新目录，再逐步切换并记录恢复日志；单次目录移动可原子完成，但整组切换不是文件系统事务，失败时恢复旧目录。Skill 安装不会自动注册或启用 IDEA MCP Server、修改 Codex 连接配置，也不会升级 Doc View 插件。完整范围和验收见 [客户端 Skill 计划](../../openspec/changes/doc-view-client-skill/proposal.md)。

## UI 与 Skill 入口

开发中的文档库在现有 **Export** 下拉中提供项目文档的 **Generate or Update Project Docs**、**Open Project Docs** 与 **Search Project Docs**。生成/更新明确写入当前项目；打开进入当前项目的 `index.md`；搜索读取 JSON 文档库。Skill 的安装与检查更新位于 **Settings → Tools → Doc View → MCP / AI**，不挤入主工具栏，也不新增顶层按钮或重绘平台图标和品牌图标。

工具窗口现有 **Refresh** 只重新扫描 PSI 接口目录，不生成 Markdown/JSON，也不刷新项目文档快照；开发版名称为 **Refresh API Catalog / 刷新接口目录**，使它与生成/更新区分。实际验证与未覆盖项以文档库 change 为准；Skill 设置区域仍是计划。

## 取消与客户端兼容

MCP 请求取消必须由客户端真实发送协议取消，并由 IDEA 服务端停止对应工作、完成清理。Codex app-server `0.165.0-alpha.1` 候选在隔离 IU 263.7031.23 上按实际协商的 `2025-06-18` 协议连续三轮通过：每轮 `turn/interrupt` 都发送了匹配 request ID 的 `notifications/cancelled`，IDEA 记录任务取消与 cleanup，没有迟到完成，随后同会话 echo 成功。

常用 Codex `0.163.0-alpha.11.2` 的既有实测仍失败：turn interrupt 没有取消服务端长调用，约 30 秒后请求正常完成；客户端退出时发生的取消不能算 turn interrupt 成功。0.165 候选的通过仅适用于该隔离客户端、协议和 IDEA build，不能推广为所有 Codex 版本或生产插件已通过。263 构建和 Verifier 已通过；full-10 新包已有部分原生场景通过，但验收矩阵和文档库最终验收仍未完成，正式 adapter 尚未实现。HTTP 202 或客户端 turn 状态本身都不能证明服务端取消。详细轮次和证据见[取消验收说明](../mcp-cancellation.md)。
