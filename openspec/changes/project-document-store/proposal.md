# Proposal

## Why

项目文档已写入 `docs/doc-view/index.md` 和 `apis/*.md`，但没有可直接查询的结构化清单、字段详情或明确的新鲜度状态。每次查询重扫 PSI 会慢，反解析用户自定义 Markdown 又不可靠；需要让界面与未来 MCP 共用落盘文档库。

## What Changes

- 保留当前人类可读 Markdown，增加版本化 `index.json` 与接口 JSON 详情；同一不可变、无 PSI 的数据快照同时生成这些产物。
- 以当前项目的固定 `docs/doc-view/` 为唯一文档根；项目打开时只读发现，查询不创建目录、不生成文件、不扫描源码。
- 提供索引摘要搜索、按稳定身份读取详情、分页、内存派生缓存及明确的 current/stale/unknown 状态；磁盘索引与缓存分开。
- 共用一次“生成/更新项目文档”操作维护 Markdown 和 JSON，保留所有权/hash 冲突保护、旧文件不静默删除和部分发布报告；不把单文件原子替换说成整批事务。
- 精简 UI：区分刷新接口目录、生成/更新文档、搜索文档和打开文档；设置/清缓存继续在 `…` 中，使用现有平台图标。
- 修复验收中发现的 Dubbo XML 生命周期问题：用公开 XML PSI 读取已有受支持的服务声明及导航目标，移除本插件的 XML DOM model 注册，避免该路径的运行时代理异常回溯保留插件类加载器；接口发现、模块归属及输出规则保持不变。
- **BREAKING（新查询格式）**：机器查询只接受本 change 定义的 schemaVersion；只有 Markdown 的旧产物仍可普通阅读，但需显式重新生成后才能进入新查询，不提供 Markdown 反解析或跨版本格式适配。

## Capabilities

### New Capabilities

- `project-document-store`：落盘机器清单、结构化详情、项目绑定、完整性、新鲜度和只读检索。
- `project-document-library-ui`：文档生成/更新、查询入口及状态表达。

### Modified Capabilities

无长期 spec 修改。现有项目导出与 Markdown 索引仍在未归档的 [doc-export-workspace](../archive/2026-11-09-doc-export-workspace/specs/project-document-export/spec.md) 和 [doc-export-index-readability](../archive/2026-11-09-doc-export-index-readability/specs/project-document-index/spec.md) 中；本 change 在它们之上新增机器格式，不同步/归档旧 change，也不改变 Markdown 正文模板和页面身份算法。

## Impact

实现将扩展 `service/export` 中的 snapshot/page/exporter，增加 Java-only 的文档库读取与检索 service、无 PSI 的 DTO，以及 tool window/action 文案与状态；使用现有 Gson/平台 API，不新增数据库、向量服务或运行时依赖。原有真实 PSI/Markdown contract、冲突/失败测试与原生 UI 验收需补齐。

本 change 已进入实现和验收阶段，具体状态以 tasks 与 verification 为准。它不依赖 MCP 客户端取消修复，可在当前获准基线上独立实施；不修改生产最低版本。正式 MCP 由 [doc-view-mcp-tools](../doc-view-mcp-tools/proposal.md) 跟踪。任意外部目录注册、独立 Wiki、自动源码写回、自动同步/删除、AI 语义搜索和 Air 集成不在范围内。
