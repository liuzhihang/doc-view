# Proposal

## Why

Doc View 现有 HTTP 入口会丢失 header、混合路径/查询/表单参数、错误标注 Content-Type，并在动作调用链同步解析源码；用户得到的请求可能无法正确调用接口。目录中的泛化命名、项目级清缓存入口和密集工具栏也容易造成误操作，应在下一轮先完成请求正确性与操作入口收敛，再推进独立 MCP 查询能力。

## What Changes

- 2026-11-08 用户依据实机截图要求参数底部改为纯图标：JSON、Markdown 和写回分别使用不同的平台图标，完整动作名保留在 tooltip/无障碍名称中；取代此前 Copy JSON/Apply to Source 的常驻文字设计，操作与写回语义不变。

- 2026-11-07 用户追加并授权 UI 修复：全量审查图标语义/尺寸/主题，调整参数编辑器的复制与写回入口，修复设置说明文字层级，以及预览/参数弹窗切换其他应用时遮挡前台窗口的问题。继续使用现有平台图标，不重画品牌、不新增设置或另一套界面；验收以重建最终包为准。
- 同日继续修复原生日志暴露的 Keymap 组标题缺失：补全注册组的中英文可读标题，保留动作结构与快捷键；同时补验跨应用、菜单和资源释放，未通过项保留，不提前推进正式 MCP。

- 在目标 IDEA 版本中验证原生 Spring/Endpoints → HTTP Client 生成链路，比较公开服务与 Action 上下文的接入方式；以支持范围、API 合规、请求语义和生命周期证据决定生产路线。
- 原生路线通过全部准入条件时采用原生生成；未通过时采用一个基于现有 PSI 能力的 Java 请求生成实现。两者是实施阶段的互斥决策，不增加运行时双实现、自动切换或用户模式开关。
- 明确 Spring Controller 和 Spring MVC 注解风格 Feign 的最小支持集合：静态单路径、明确单 HTTP method、标量 Path/Query/Header、JSON body、显式 URL 编码表单；补齐绑定名称、编码、示例/默认值、媒体类型和空值规则。其他形式按 contract 明确拒绝，不生成貌似完整的错误请求。
- 从当前源码生成可编辑请求草稿；解析在可取消后台完成，文件发布遵循项目/源码生命周期，重载、同名跨项目和已有用户编辑不会相互覆盖；生成动作不执行 HTTP 请求。
- 将设置与项目级清缓存收纳到工具窗口原生 `…` 菜单；保留常用工具栏操作。`Open` 改为“打开 Markdown 文档”，HTTP 文案随最终路线确定；动作按节点和项目状态启用，通用图标继续使用平台资源。
- **BREAKING**：停止对 Dubbo、普通非 HTTP 接口和未支持绑定生成伪 HTTP 请求；停止将全部参数拼成 query、输出缺失示例的字面量 `null` 等旧行为。HTTP 草稿不承诺旧业务标题路径，已存在的草稿保留；不影响 Markdown 文档路径及内容。
- 规划阶段仅新增本 change artifacts 并更新路线图；运行时、用户指南和 changelog 的实际变更在后续 apply 阶段完成。

## Capabilities

### New Capabilities

- `http-request-generation`：从当前方法生成语义正确、可取消且不会覆盖用户内容的 HTTP Client 请求草稿，包括支持集合、原生复用门槛与单一路线约束。
- `doc-view-action-layout`：工具窗口常用动作与项目维护操作分组、目录动作命名/可用状态、语言和图标一致性。

### Modified Capabilities

无长期 spec 修改。已读取 `intellij-api-compatibility`、`openapi3-annotation-compatibility` 等现有规范；本次保留其行为。`api-directory-navigation`、`method-doc-preview` 和 `plugin-ui-localization` 当前位于尚未归档的其他 change，本次只补充 HTTP 与操作布局的新 contract，不重复复制或提前同步这些 delta。HTTP 文件的独立身份规则属于本次明确的行为变化，不改变由目录展示变化引起的路径保持约束。

## Impact

- 预计实现范围：`CatalogHttpClientAction`、`CatalogOpenAction`、`CustomFileUtils` 的 HTTP 路径、`MethodNode` 的 HTTP 草稿身份、最小请求生成 service/快照，以及工具窗口 action 分组和中英文消息；具体文件以实施时的最小正确改动为准。
- Java-only、当前 Gradle/IDE 基线保持不变。若采用原生 API，须在决策记录中锁定目标 SDK 的 plugin/module ID 并显式声明所需 bundled dependency；不引入第三方 HTTP 客户端、生产 Kotlin、internal/反射调用或新索引。
- 继续复用现有 PSI 类型和注解处理能力；文档显示字段、Markdown/Velocity、上传 payload、参数编辑器写回规则保持不变。请求所需 wire name/location 与文档显示名分离。
- 非目标：完整 OpenAPI 导入导出、multipart/文件上传请求生成、自定义元注解解析、批量生成所有端点、HTTP 执行/响应 UI、环境/凭据编辑器、图标重绘、正式 MCP 工具、专用索引、Air/Wiki 产品扩展。
- 验证包括针对性 RED/GREEN、HTTP 文本/语义断言、本机虚构服务接收值、IDE action 与生命周期、现有文档回归、完整构建与 Plugin Verifier；自动结果不能替代最终包原生验收。详细矩阵与依赖路线见本 change 的 design/specs/tasks。
- 本次不修改版本、提交/推送、发布、sync/archive，也不把已有 1.4.0 验收缺口标成已完成。
