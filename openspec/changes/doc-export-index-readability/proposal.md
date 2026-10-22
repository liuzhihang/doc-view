# Proposal

## Why

在 IDEA Markdown 预览中，当前项目 API 索引把 64 位身份哈希放在六列表格首列，导致接口名和链接被挤到首屏之外。改为按模块、类和方法纵向阅读，让用户无需横向滚动即可查看接口入口，同时继续保留机器可读取的完整身份和稳定页面链接。

## What Changes

- 将项目 API `index.md` 从宽表格改为按模块、类分组的纵向条目。
- 每个方法条目首屏展示可读签名、接口 Markdown 链接和源码链接。
- 在 Markdown 隐藏注释中保留完整 canonical identity 与 SHA-256；页面路径、重载区分和身份算法保持不变。
- 更新项目文档指南，说明新索引的阅读方式及机器身份仍可从原始 Markdown 中读取。

## Capabilities

### New Capabilities

- `project-document-index`: 项目 API 索引的纵向阅读结构，以及与稳定接口身份关联的可读链接。

### Modified Capabilities

无。

## Impact

- 影响 `ProjectDocumentExporter` 生成的 `docs/doc-view/index.md` 正文格式、该 renderer 的自动化测试和项目文档用户指南。
- 不修改逐接口页面正文、身份编码、哈希算法、文件名、冲突保护、索引 marker、导出入口或 IDEA 索引/MCP 能力；不新增依赖。
