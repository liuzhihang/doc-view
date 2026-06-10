## 1. 审计与基线确认

- [x] 1.1 审计 startup、action `update`、action `actionPerformed`、preview、参数编辑器、tool window refresh、export、upload、Markdown 渲染和通知路径中可能在 EDT 上执行的重型 PSI、DTO、渲染、网络或文件工作。
- [x] 1.2 在实现记录或 code review summary 中记录已审计的热点路径和计划迁移目标。
- [x] 1.3 确认插件启动时不会执行 Spring/Dubbo 全项目扫描、DTO schema 预热、Markdown 预渲染、export、upload 或 cache 预加载。

## 2. 后台执行边界

- [x] 2.1 新增或改造 Doc View 后台扫描/渲染 helper，使用 IntelliJ background task、progress、cancellation、read action、project disposal 检查和 EDT 结果回投。
- [x] 2.2 将 preview `DocView` 构建移出 UI 构造和 EDT-only 代码，同时保持现有 preview selection 和 notification 行为。
- [x] 2.3 将参数编辑器 DTO body 构建移出同步 UI 初始化流程，或用后台执行和安全 UI 结果应用机制保护该流程。
- [x] 2.4 验证 tool window refresh/catalog scanning 使用后台 read action，并且只在 EDT 上更新 Swing tree 状态。
- [x] 2.5 确保 export 和 upload 批量准备流程中的 scan、DTO 构建、Markdown 渲染和 payload 准备都不在 EDT 上运行，并具备 cancellation-aware 行为。
- [x] 2.6 保持 action `update` 方法轻量，不执行递归 DTO 解析、Markdown 渲染、网络调用、export 写文件或全项目扫描。

## 3. DTO Schema 缓存

- [x] 3.1 引入 project-level DTO schema cache service，并提供明确的 get、put、invalidate 和 clear 操作。
- [x] 3.2 定义 cache key，包含 project scope、framework、class 或 method identity、request/response role、相关 settings/template version，以及 PSI modification stamp 或等效失效信号。
- [x] 3.3 缓存派生 schema 数据或安全快照，不长期持有 PSI element graph。
- [x] 3.4 接入 PSI 变化、schema 相关 settings 变化、template 变化、显式 clear-cache action 和 project disposal 的缓存失效逻辑。
- [x] 3.5 将重复的 request/response DTO schema 构建接入缓存，同时不改变生成的 `DocView`、Markdown、export 或 upload 语义。

## 4. 递归与深度保护

- [x] 4.1 新增显式 DTO parsing context，跟踪当前深度、最大 schema 深度、已访问的有效 type path、泛型替换和 schema role。
- [x] 4.2 对直接对象循环、继承字段、collection、array、map 和 generic value/element type 应用循环检测。
- [x] 4.3 当触发循环或深度限制时停止展开子节点，同时保留当前字段元数据并避免 stack overflow 或无限循环。
- [ ] 4.4 在可行时增加针对直接循环、嵌套泛型、map/list 嵌套和 max-depth 截断的自动化覆盖或 fixture 验证。

## 5. Markdown 延迟渲染

- [x] 5.1 重构 preview 流程，先构建 `DocView` 数据，只在当前请求或选中的 entry 需要时渲染 Markdown。
- [x] 5.2 仅针对当前 `DocView` 加 template/settings/schema version memoize 已渲染 Markdown，并在 source、settings 或 template 变化时失效。
- [x] 5.3 保持等价 `DocView` 和 template 输入下 preview、copy、export、upload 路径的 Markdown 文本不变。
- [x] 5.4 确保 copy/export/upload action 在 lazy rendering 尚未产出 Markdown 时可以安全请求或等待渲染结果。

## 6. 启动兼容性修复

- [x] 6.1 复现或定位 startup notification/activity 或 service access timing 中的启动时报错来源。
- [x] 6.2 修复不安全的 service access 或 startup work ordering，同时不改变通知标题、内容、action 或触发条件。
- [x] 6.3 确认 startup hook 不触发全项目扫描、DTO 解析、Markdown 渲染、export、upload 或 cache 预热。

## 7. 文档与发布准备

- [x] 7.1 更新 `docs/performance-guide.md`，补充 background task 边界、cache key/invalidation 策略、递归/深度限制、lazy rendering 预期、cancellation 行为和人工验证步骤。
- [x] 7.2 为性能基线发布更新插件版本元数据和 changelog/release notes。
- [x] 7.3 文档化 JetBrains Marketplace 发布需要先完成验证、获得维护者明确确认，并具备可用凭据。
- [x] 7.4 对接 Gradle `publishPlugin` 配置，通过 Gradle property 或环境变量安全注入 JetBrains Marketplace token，并支持配置发布 channel。

## 8. 验证

- [x] 8.1 运行 `./gradlew test` 并记录结果。
- [x] 8.2 运行 `./gradlew buildPlugin` 并记录生成产物或失败信息。
- [x] 8.3 运行 `./gradlew verifyPlugin` 并记录兼容性结果。
- [ ] 8.4 运行 `./gradlew runIde` 并人工验证：启动无已报告错误、启动时无全项目扫描、preview 打开、class/method 切换、refresh、clear cache、export preparation、upload preparation、递归 DTO 处理、深度限制行为、lazy Markdown rendering，以及 cancellation/project-close 行为。
- [ ] 8.5 如果发布已获批准且凭据可用，运行 `./gradlew publishPlugin`；否则记录 blocker 并保持未发布状态。
