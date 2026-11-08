# Doc View 文档与 MCP 格式

状态：**1.6.0 提供 JSON 文档库；正式 MCP 工具仍是计划**。当前生产最低 IDEA 版本仍以 Gradle 配置为准。自动和原生结果分别记录在[文档库任务](../openspec/changes/project-document-store/tasks.md)。

采用 Markdown + JSON，各自有明确用途：

| 产物 | 作用 | 维护方式 |
| --- | --- | --- |
| `docs/doc-view/index.md` | 人读目录 | 已有项目导出维护 |
| `docs/doc-view/apis/<stem>.md` | 人读接口文档 | 保留现有模板与稳定文件名 |
| `docs/doc-view/index.json` | 可搜索摘要、文件位置和内容摘要 | 同一次生成发布，最后提交 |
| `docs/doc-view/apis/<stem>.json` | 参数、响应字段等结构化详情 | 与同 stem Markdown 同源生成 |
| MCP Text(JSON) | 客户端请求的结果、分页、错误及 freshness | adapter 从已提交文档库返回，不等同整份 index |

查询从磁盘索引加载内存投影，之后在摘要中筛选；按需读取一个详情文件。这样无需每次扫描源码，也无需反解析用户自定义 Markdown。没有生成过 JSON 时明确返回未生成，用户可通过 UI 显式生成；MCP generate 仍待正式 provider 实现。`current/stale/unknown` 与文件是否损坏分开，重启后不能把旧快照叫作实时源码。

当前 JSON v1 保留参数与实体字段的 `since/version`，尚未单独暴露接口方法级的这两个字段；1.5.0 已有的方法版本标签仍由 Markdown 模板展示。因此“同源生成”不表示 JSON 包含解析模型的每个属性。后续若扩展方法级元数据，需要同步格式、Schema、示例和回归测试。

唯一规范在 OpenSpec：

- [磁盘格式、身份与 hash 算法](../openspec/changes/project-document-store/format.md)
- [磁盘 JSON Schema](../openspec/changes/project-document-store/schemas/document-store.schema.json) 和 [虚构 index 样例](../openspec/changes/project-document-store/examples/index.json)
- [MCP 工具设计](../openspec/changes/doc-view-mcp-tools/design.md)、[工具输入/结果 schema](../openspec/changes/doc-view-mcp-tools/tool-contracts.json) 和 [虚构调用结果](../openspec/changes/doc-view-mcp-tools/examples/search-response.json)

变更必须联动 schema、示例、golden、工具契约及使用说明；不会在本指南再复制一套字段定义。首版使用 JSON Schema 2020-12。磁盘和 MCP schema 各自带版本，遇未知版本明确拒绝；生成受管理文件之前仍检查内容是否被手工修改。参数说明中的文本是文档数据，不是让 AI 执行的指令。

入口和操作流程见 [MCP 使用说明](user-guide/mcp.md)，取消机制与已知未通过项见 [取消调查](mcp-cancellation.md)。
