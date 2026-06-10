## Why

未来 Contract 提取能力会增加更多解析工作；如果当前 PSI 扫描、DTO 递归解析和 Markdown 渲染路径同步运行在 EDT 上，就会放大 IDE 卡顿风险。本变更在继续增加解析工作前，先建立后台扫描、缓存和递归保护的性能架构基线。

## What Changes

- 识别启动、action、tool window、preview、export、upload 等路径中可能在 EDT 上执行重型 PSI 扫描、DTO 解析或 Markdown 渲染的位置。
- 将耗时扫描和解析迁移到可取消的 IntelliJ 后台任务中，EDT 仅负责 UI 状态更新。
- 设计 DTO schema 缓存机制，明确 cache key、生命周期、失效触发点和 clear-cache 行为。
- 增加 DTO 递归解析保护，包括循环引用检测和可配置或文档化的 schema 深度限制。
- 在 preview、export 等适用场景下实现 Markdown 延迟渲染，避免在内容尚未被请求时提前渲染。
- 更新 `docs/performance-guide.md`，补充后台任务、缓存失效、递归保护、延迟渲染和人工验证指南。
- 纳入发布准备任务，包括插件版本升级、changelog/release notes、Plugin Verifier、手动 IDE smoke test 和 JetBrains Marketplace 发布。
- 修复启动时报错，重点处理 service 访问时机或 startup work ordering 问题，同时保持文档生成语义稳定。

## Capabilities

### New Capabilities

- `ui-background-scan-and-schema-cache`：定义重型 Doc View 扫描/渲染流程的响应性、后台执行、DTO schema 缓存、递归保护、Markdown 延迟渲染、启动行为和验证要求。

### Modified Capabilities

- `intellij-build-compatibility-baseline`：扩展兼容性基线要求，覆盖发布该性能基线前必须完成的发布验证和启动兼容性检查。

## Impact

- 受影响的生产区域包括 action handler、tool window/preview 刷新路径、Spring 和 Dubbo service 解析入口、DTO body/schema 构建工具、Markdown 渲染调用点、export/upload 准备流程、startup notification 或 startup activity 代码，以及 clear cache UI wiring。
- 预期更新 `docs/performance-guide.md`；实现阶段的 release readiness 可能触及 changelog 或版本元数据。
- 验证需要在可行时增加 DTO cycle/depth 的针对性单元或 fixture 覆盖，并运行 `./gradlew test`、`./gradlew buildPlugin`、`./gradlew verifyPlugin`，再通过 `runIde` 人工验证启动、预览、刷新、导出和缓存失效行为。
