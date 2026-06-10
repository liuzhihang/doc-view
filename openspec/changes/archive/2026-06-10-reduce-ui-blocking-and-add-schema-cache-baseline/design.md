## Context

Doc View 从 IntelliJ PSI 构建 API 文档，再渲染 Markdown 或平台上传 payload。现有代码已经在部分 upload、export、tool window 路径中使用后台任务，但一些关键入口仍可能在 UI 构造器或 action 中直接构建 `DocView`、body tree、示例数据或 Markdown。未来 Contract 提取会增加 PSI 遍历成本，因此在继续扩展解析能力前，需要先建立统一的后台执行和缓存基线。

相关当前实现面包括 `PreviewForm`、`ParamDocEditorForm`、`DocViewWindowPanel`、`WindowRefreshAction`、upload/export toolbar actions、`DocViewService`、`SpringDocViewServiceImpl`、`DubboDocViewServiceImpl`、`ParamPsiUtils`、`DocViewData`、`VelocityUtils`、`ExportUtils` 和 startup notification/activity 代码。

## Goals / Non-Goals

**Goals:**

- 保持插件启动阶段不进行全项目扫描。
- 确保重型 PSI 扫描、DTO schema 构建、示例生成和批量 Markdown 渲染不运行在 EDT。
- 为用户触发的 scan/render 流程定义可复用的后台执行边界，支持取消和 stale result 防护。
- 引入 DTO schema 缓存，明确 cache key、失效触发、project lifecycle 清理和 clear-cache 行为。
- 为递归 DTO 解析增加循环检测和 schema 深度限制。
- 在 UI 或集成路径并不立即需要 Markdown 文本时，延迟渲染 Markdown。
- 在 `docs/performance-guide.md` 中记录缓存失效和人工验证方式。
- 通过版本/changelog 更新、Plugin Verifier、手动 `runIde` 检查和 Marketplace 发布准备完成 release readiness。
- 修复 service access timing 或 startup work ordering 导致的启动时报错。

**Non-Goals:**

- 不改变生成 Markdown 结构、模板变量、上传 payload 语义、导出文件命名、设置持久化或 UI 可见工作流；除非为了后台任务所需的响应性/进度反馈。
- 不引入新的生产语言或非 IntelliJ async framework。
- 不新增启动时自动 indexing 或 proactive whole-project prewarming。
- 不在缺少维护者凭据和明确确认时自动发布到 Marketplace。

## Decisions

### 使用 IntelliJ background/read-action API 作为执行边界

重型工作应通过 IntelliJ 支持的后台任务机制运行，例如 `Task.Backgroundable`、`ProgressManager`、pooled thread，以及适用场景下的 read action/non-blocking read action。EDT callback 只负责更新 Swing 状态、通知、选中节点和 preview 内容。

备选方案：在所有调用点直接包一层 ad hoc pooled-thread call。该方式能降低短期 EDT 阻塞，但会让取消、stale result 处理和 project disposal 检查变得不一致。

### PSI 访问保持短生命周期，长期缓存不保存 PSI

Schema cache entry 应保存派生 schema/body 数据或序列化轻量 descriptor，而不是持久保存 `PsiElement` graph。Cache key 可以包含 project identity、framework、qualified class 或 method signature、request/response role、相关 settings/template version 和 PSI modification stamp。

备选方案：整对象缓存 `DocView`。现有 DTO 可能包含 PSI 引用，整对象缓存容易带来 stale PSI retention 和内存泄漏风险。

### 增加 project-level DTO schema cache service

新增一个小型 project service 负责 get/put/invalidate/clear。初始缓存范围限定在 project 内并保持有界；在 project disposal、显式 clear-cache action、相关 settings 变化和 PSI modification 变化时清理。实现可以先使用内存 map 和文档化 eviction 策略，不急于持久化。

备选方案：在 `ParamPsiUtils` 中使用 static utility cache。它更简单，但 project lifecycle 和 settings invalidation 更难推理。

### 在 schema-building context 中显式处理递归保护

DTO 解析应传递 parsing context，包含当前深度、最大深度、已访问 type identity、泛型替换和 role 信息。重复 type path 或深度溢出时，应生成有界 placeholder body node 或停止展开 children，同时保留字段本身。

备选方案：仅依赖现有 parent-chain 检查。Parent check 对简单循环有帮助，但对泛型嵌套、map/collection 和未来 Contract 提取不够显式。

### 基于缓存的 `DocView` 数据进行 Markdown 延迟渲染

Preview 和集成流程应先构建或复用 `DocView`，只有当选中的 preview tab、copy/export action 或 upload integration 需要 Markdown 文本时才渲染。Rendered Markdown 可以按当前 `DocView` 加 template/settings version 做 memoize，并随 schema 或 template cache 一起失效。

备选方案：每次扫描完成后立即 eager render Markdown。它流程最简单，但 class-level scan、selection change 和 batch operation 中只查看或上传部分 API 时会浪费工作。

### 发布验证与实际发布分离

实现可以更新版本元数据和发布文档，运行 build/verifier/manual checks，并准备发布。实际执行 `publishPlugin` 需要维护者提供 Marketplace 凭据并明确确认。

备选方案：将发布作为无条件自动任务。Marketplace token 可用性和发布时间由维护者控制，不能默认自动执行。

## Risks / Trade-offs

- [Risk] 后台 read action 可能与 PSI invalidation 或 project disposal 竞争。-> 检查 `Project.isDisposed()`，使用前验证 PSI，并在更新 UI 前用 request token 或 modification stamp 丢弃 stale result。
- [Risk] Cache key 可能遗漏影响 schema 或示例的 setting。-> 文档化 key 维度，并在相关 settings/template 变化时保守失效。
- [Risk] 如果 `Body` 仍然可变，缓存派生数据可能掩盖错误。-> 可行时缓存不可变 snapshot，或在 UI 编辑前 defensive copy。
- [Risk] 深度限制可能省略很深但有效的 schema。-> 保留被截断字段，并在内部标记/描述截断；除非后续 contract 明确暴露，否则不改变当前 Markdown contract。
- [Risk] 将工作移出 EDT 可能改变 action 时序。-> 只有 backing data ready 时才开放相关 UI command，必要时显示进度，并人工验证 preview/copy/export/upload 路径。
- [Risk] Marketplace 发布可能被凭据或 verifier failure 阻塞。-> 将发布视为依赖成功验证和维护者确认的 release task。

## Migration Plan

1. 审计 startup、action update/action performed、preview、parameter editor、tool window scan、export、upload 和 Markdown render 路径中的 EDT-heavy 工作。
2. 新增或改造 background scan/render helper，在 read action 下运行 PSI 工作，并将 UI 更新回投到 EDT。
3. 增加 DTO schema parse context，包含 cycle/depth guard，并让 Spring/Dubbo request/response body 构建接入该 context。
4. 引入 project-level schema cache service，并将 invalidation 接入 PSI/settings/template 变化和 clear-cache action。
5. 将 preview 和 batch flows 转为 Markdown 延迟渲染，同时保持生成 Markdown 内容稳定。
6. 更新 `docs/performance-guide.md`、release notes 和版本元数据。
7. 运行自动化验证，执行手动 `runIde` smoke test；只有在维护者确认且凭据可用后才发布。
