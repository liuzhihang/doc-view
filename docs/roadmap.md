# 路线图

Doc View 的近期目标是稳定“Java 源码 → `DocView` → Markdown/导出/上传”主链路，再扩展工作台能力。每一项都应作为独立 OpenSpec change 设计、验证和回滚，不绑定未经确认的发布日期。

## 推荐顺序

| 顺序 | Change | 目标 |
| --- | --- | --- |
| 0 | `maintenance-workflow-baseline` | 完成 OpenSpec、canonical docs、skill、凭据和发布治理基线 |
| 1 | `markdown-contract-baseline` | 建立真实 PSI fixture、`DocView` 断言和 Markdown golden sample |
| 2 | `openapi3-operation-summary` | 修正 `@Operation.summary` 名称优先级和空值 fallback |
| 3 | `openapi3-schema-required-mode` | 定义 `requiredMode` 三态、其他必填来源优先级和无损写回 |
| 4 | `openapi3-parameter-metadata` | 在现有模型边界内定义 `@Parameter` 元数据映射与冲突规则 |
| 5 | `compatibility-deprecation-cleanup` | 清理 Verifier deprecated API，固化目标 IU 兼容性门禁 |
| 6 | `doc-generation-performance` | 验证并优化 PSI、缓存、后台任务和按需渲染 |
| 7 | `api-tree-navigation` | 强化模块/Controller/path/tag 分组、搜索、刷新和预览导航 |
| 8 | `doc-view-configuration` | 统一文档结构、字段策略、模板 profile 和配置迁移 |
| 9 | `doc-export-workspace` | 形成可选范围、命名和归档规则的批量文档导出工作流 |
| 10 | `platform-upload-stability` | 固化 YApi、ShowDoc、YuQue payload 与安全错误处理 |
| 11 | `doc-import-viewer` | 在 source-neutral 模型成立后评估历史文档导入、查看和对比 |

## 当前优先级说明

### Markdown contract 基线

当前已用真实 PSI fixture 覆盖 Spring 基础链路，并通过 `DocView` 断言和 golden file 固定默认 Markdown 输出。后续运行时 change 继续补充 Feign、Dubbo、递归、泛型、集合/Map、void 和缺少注释场景。

### OpenAPI 方向

Doc View 已读取部分 Swagger 2/OpenAPI 3 注解，但不是完整 OpenAPI 文档工具。OpenAPI 注解兼容按可独立验证的 contract 拆分：

- `openapi3-operation-summary` 只修正 `@Operation.summary` 的名称优先级和空值 fallback，保持 description 既有行为。
- `openapi3-schema-required-mode` 单独定义 `REQUIRED/AUTO/NOT_REQUIRED` 与 Validation、Swagger 2、Javadoc 的优先级，并处理无损写回。
- `openapi3-parameter-metadata` 单独定义参数名称、描述、示例、required 与 Spring 注解的冲突规则。

完整 OAS 导入/导出需要参数位置、响应码、多 media type、components、security、`$ref` 与组合 schema 等 source-neutral 模型，且目标 IU 已提供 OpenAPI draft 能力，因此暂缓到明确差异化需求出现后再评估。

## 横向门禁

- 所有行为 change 先定义 contract；Markdown shape 和模板变量默认保持兼容。
- PSI/UI/网络路径遵守 [性能指南](performance-guide.md)。
- IntelliJ API 与平台升级遵守 [兼容性策略](intellij-compatibility.md)。
- 发布前执行 [发布检查清单](release-checklist.md)。
- AI/MCP、双向同步、多人协作和新生产语言不在近期主线内。
