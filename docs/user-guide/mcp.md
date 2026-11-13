# 项目文档库与 MCP

> **版本边界：** 1.6.x 提供项目文档库和搜索 UI；1.7.0 增加插件内置 MCP 与 Codex Skill 设置。两项功能的实际发布状态、验证结果和未覆盖项见[发布记录](../release-1.7.0.md)，磁盘格式见[格式说明](../mcp-format.md)。

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

目录自动绑定当前 IDEA 项目的固定路径 `<project>/docs/doc-view/`。首版无需额外注册目录，也不提供任意磁盘目录扫描。MCP 的 `projectPath` 用于选择已打开的项目，不能用它读取任意路径。多项目的文档和派生缓存分别隔离。

## 搜索和打开文档

在同一个 **Export** 下拉提供 **Search Project Docs / 搜索项目文档** 与 **Open Project Docs / 打开项目文档目录**。搜索在工具窗口内展示，支持接口名、说明、模块/完整签名、标签及 HTTP method/path 关键词；多个词按 AND 匹配，结果分页。选择结果可打开对应的真实 Markdown 文件；打开目录进入当前快照的 `index.md`。

查询只读已提交的磁盘 JSON 和可重建的内存缓存，不扫描源码、不生成或修复文件。尚未生成索引时，状态区显示“未生成”，由用户点击生成入口。打开文档前验证路径、身份和原始文件 hash；损坏、缺失、快照变更或外部改写时停止本次打开，不混用不同批次的详情。若将受管理页面恢复为索引记录的原始内容，重新打开并通过完整校验后会清除该页面对应的错误；此操作不会把 `unknown` 或 `stale` 提升为 `current`，也不会清除其他文件的错误。

新鲜度和完整性分别报告：

- **current**：本会话成功生成，并且尚未观察到相关输入变化。
- **stale**：本会话观察到源码、模块、模板或输出相关设置变化，需要显式更新。
- **unknown**：重开项目、IDE 重启或只读加载后，无法证明磁盘快照与当前源码相同。

外部工具修改 Java/XML 后，须先由 IDEA 的文件刷新观察到变化，旧快照才会标为 `stale`；未加载到编辑器的文件也纳入跟踪。生成目录和其他项目的变化不会推进本项目的输入代际。查询只报告状态，更新文档仍需明确执行生成操作。

文件存在、修改时间较新或 hash 正确，都不能证明源码最新。查询不会为了判定 freshness 重扫 PSI。**Clear Cache / 清除缓存** 清理 DTO schema 和文档查询的派生内存缓存，保留 Markdown、JSON、Scratch 和 HTTP 草稿，也保留本会话已生成快照的新鲜度依据。

## 取消、冲突和部分写入

一批输出先完整暂存，再按接口 Markdown/JSON、`index.md`、`index.json` 的顺序逐文件原子替换；最后的 `index.json` 是提交点。首个文件移动前取消，不发布文档；部分文件移动后取消或失败，报告已发布路径和清理结果，旧索引可能仍在。此时旧摘要仍可搜索，但 hash 不一致的页面或详情拒绝读取；排除冲突后重新生成可以完成剩余写入。整个文档库不是跨文件事务，不能声称失败时全部回滚。

最终索引提交后才到达的取消仍报告完成。同一文档根只允许一个 writer；有 writer 或查询资源繁忙时明确提示稍后重试，不在界面线程等待磁盘锁。查询和生成都有时间预算，超限不把部分内容截断成成功结果。

磁盘格式采用 `schemaVersion: 1`，参数详情与目录的 schema、自完整性 hash、稳定接口身份和快照身份见[格式说明](../mcp-format.md)。未知版本只读查询明确拒绝；显式重新生成仍需先通过所有权与手改校验，不能把 JSON 当作可随意删除的缓存。文档中的文本属于数据，不是让 AI 执行的指令。

## 内置 MCP 工具（1.7.0）

工具作为**同一个 Doc View 插件内的可选扩展**注册到 IDEA 自带 MCP Server，不另装 companion plugin，也不自建 MCP 服务。MCP Server 组件未启用或缺失时，Doc View 的普通 UI 仍应正常加载；可选加载的原生验收状态见本轮验证记录。工具如下：

| 工具 | 用途 | 是否写文件 |
| --- | --- | --- |
| `doc_view_status` | 查看当前项目文档库是否生成、schema 与快照状态 | 否 |
| `doc_view_search` | 按接口摘要检索，支持 HTTP method/path 信息并分页返回匹配项 | 否 |
| `doc_view_get` | 按稳定接口身份读取完整结构化详情 | 否 |
| `doc_view_generate` | 扫描并生成或更新当前项目的 Markdown 与 JSON 文档 | 是，须由用户明确请求 |

`doc_view_generate` 同时负责首次生成和再次更新，不另设 `update` 工具。前三个查询工具只读 JSON 文档库；它们不会读取任意文件、修改源码或隐式生成文档。完整输入、输出、错误、边界及项目隔离由[Doc View MCP tools change](../../openspec/changes/doc-view-mcp-tools/proposal.md)定义，实际完成度以验收记录为准。

项目已关闭或路径无效时，IDEA 可能在调用 Doc View 前直接拒绝请求；这种宿主错误不一定使用 Doc View 的 JSON 格式。客户端应保留原错误并停止，不改为查询另一项目，也不隐式生成。

### 三个独立的注册步骤

1. **Doc View 向 IDEA 注册工具。** Doc View 插件声明对 IDEA `com.intellij.mcpServer` 的可选扩展。安装插件本身不会自动开启 IDEA MCP Server。
2. **IDEA MCP Server 启用并暴露工具。** 在 **Settings → AI → MCP Server** 检查 MCP Server 插件，启用服务并阅读 IDEA 的访问同意说明；再到 **Exposed Tools** 查看并启用 Doc View 工具。具体界面随 IDEA 版本变化，按[JetBrains MCP Server 官方指南](https://www.jetbrains.com/help/idea/mcp-server.html)操作。
3. **MCP 客户端连接 IDEA。** 在 IDEA MCP Server 设置中选择 **Copy Config** 或适合的客户端配置方式，然后在 Codex 等目标客户端添加这台 IDEA Server 并按提示重启客户端。IDEA 官方文档也提供自动配置选项；它会修改相应客户端配置文件，使用前请确认目标客户端和配置范围。Doc View 不会私自写 Codex 全局配置，也不会因为安装插件就替客户端建立连接。

每个打开的项目共用 IDEA 的 MCP Server。优先传当前项目的规范绝对 `projectPath`，只绑定该项目的固定 `docs/doc-view/`。IDEA 在已知项目的连接中会从工具 schema 移除该参数；Doc View 从公开的本次调用上下文取得项目。显式参数可覆盖宿主默认项目，不能把连接的默认项目当作不可变安全隔离；没有参数和上下文时，Doc View 不自行选择焦点或第一个项目。

## Codex Skill

Skill 是客户端使用说明，依次调用 status → search/get；仅在用户明确要求生成或更新时调用 generate。它不承载第二套 PSI 解析，不把文档内容当作操作授权。发行源在 `skills/doc-view/SKILL.md`，这个目录本身不是 Codex 自动发现目录。

设置中只需选择两种安装范围，无需填写安装目录：

| 界面选项 | 自动确定的目录 | 使用范围 |
| --- | --- | --- |
| 当前项目 / Project | `<project>/.agents/skills/doc-view` | 当前项目 |
| 全局 / Global | `$HOME/.agents/skills/doc-view` | 当前用户的各个项目 |

当前发现矩阵为 macOS Apple Silicon 上的 `codex-cli 0.167.0-alpha.4.1`。两个目录均已用独立临时项目、临时 HOME 与 CODEX_HOME 验证实际发现路径及 repo/user scope，没有安装到个人目录。全局目录的发现通过不等于正式发行包的安装、更新及恢复已经通过；各项证据见[客户端验证记录](../../openspec/changes/doc-view-client-skill/verification-2026-11-12-discovery.md)。其他客户端 build 或系统暂不开放写操作。

Codex 程序优先从已安装的 ChatGPT.app 和 PATH 自动检测。正常界面显示检测状态，完整程序路径可在提示中查看；无法检测或客户端版本不支持时，才提供手动选择程序的入口。安装目录始终自动计算并只读显示，可选择复制。打开设置不启动 Codex 或访问网络，显式检查更新时才在后台核对版本与发行包；不会修改 Codex MCP 配置或 discovery roots。

设置入口为 **Settings → Tools → Doc View → MCP / AI**。检查更新采用轻量链接，安装或更新按当前状态共用一个按钮。检查读取官方 release 元数据和安装记录；release 查询成功时，只在 Doc View 独立状态文件原子更新最近成功检查时间，不写 Skill、安装记录或 writer lock。离线时显示本次未完成及最近一次成功检查时间；取消和失败不覆盖旧时间。本地状态文件写入失败显示为错误，不当作离线。只有明确点击安装/更新才下载、校验并写入 Skill。没有兼容正式包时说明原因并禁用写按钮；本地目录未被插件管理、用户有修改、版本或摘要不符时保留原内容。固定官方来源是 `liuzhihang/doc-view` 的稳定 release，清单绑定 tag、不可变 commit、逐文件 SHA-256、插件版本范围和 MCP contract 指纹。SHA-256 校验完整性，不能单独证明发布者身份。

更新用暂存、同目录两次 rename、journal 和备份恢复；切换期间目标可能短暂不存在，整组操作不是原子文件系统事务。失败或取消的事务与客户端重新发现还须按[客户端 Skill tasks](../../openspec/changes/doc-view-client-skill/tasks.md)逐项验收。官方 Skill 资产上线后，设置页才可检查并安装兼容版本；源码存在或设置页出现不等于正式资产已发布。

本地候选包已完成两范围的生产安装服务与真实Codex发现测试，并验证子JVM在三处切换阶段退出后的journal恢复。下载源在测试中注入，仍不能替代官方Release下载和IDEA按钮验收。候选包身份、复验结果和未完成项见[生命周期记录](../../openspec/changes/doc-view-client-skill/verification-2026-11-13-release.md)，准备正式资产的步骤见[Skill发行说明](../skill-release.md)。

## UI 与 Skill 入口

文档库在现有 **Export** 下拉中提供项目文档的 **Generate or Update Project Docs**、**Open Project Docs** 与 **Search Project Docs**。生成/更新明确写入当前项目；打开进入当前项目的 `index.md`；搜索读取 JSON 文档库。Skill 的安装与检查更新位于 **Settings → Tools → Doc View → MCP / AI**，不挤入主工具栏，也不新增顶层按钮或重绘平台图标和品牌图标。

工具窗口现有 **Refresh** 只重新扫描 PSI 接口目录，不生成 Markdown/JSON，也不刷新项目文档快照；名称为 **Refresh API Catalog / 刷新接口目录**，使它与生成/更新区分。实际验证与未覆盖项以文档库 change 为准；新 Skill 设置页与生命周期由独立 change 继续验证。

## 取消与客户端兼容

MCP 请求取消必须由客户端真实发送协议取消，并由 IDEA 服务端停止对应工作、完成清理。Codex app-server `0.165.0-alpha.1` 候选在隔离 IU 263.7031.23 上按实际协商的 `2025-06-18` 协议连续三轮通过：每轮 `turn/interrupt` 都发送了匹配 request ID 的 `notifications/cancelled`，IDEA 记录任务取消与 cleanup，没有迟到完成，随后同会话 echo 成功。

常用 Codex `0.163.0-alpha.11.2` 的既有实测仍失败：turn interrupt 没有取消服务端长调用，约 30 秒后请求正常完成；客户端退出时发生的取消不能算 turn interrupt 成功。0.165 候选的通过仅适用于该隔离客户端、协议和 IDEA build，不能推广为所有 Codex 版本或生产插件已通过。新 adapter 通过公开协程取消回调把取消传到平台进度指示器，并区分超时、项目关闭和客户端取消；单元测试不能替代当前客户端对生产生成工具的真实取消验证。插件采用重启生效策略，不再把动态热卸载作为发布门槛。HTTP 202 或客户端 turn 状态本身都不能证明服务端取消。详细轮次和证据见[取消验收说明](../mcp-cancellation.md)。

2026-11-13 新包在 IDEA `263.7307.200` 与实际 `codex-cli 0.167.0-alpha.4.1` 组合下，先读取 `generating=true`，再于52/9000个页面已落盘时中断生产 `doc_view_generate`；匹配请求ID的取消通知到达，旧索引未变，约2.45秒内同会话恢复空闲，1秒观察期无迟到完成。取消不是整库回滚：已经发布的页面可能保留，最终索引未提交；后续查询仍须通过快照和内容hash校验，不能混读。该取消测试使用合成模型驱动真实Codex客户端，自然模型Skill行为由独立用例验证。原生项目关闭已观察到PARTIAL_PUBLICATION；各move精确边界、最终提交后迟到取消及在途退出/重启仍有未覆盖项。
