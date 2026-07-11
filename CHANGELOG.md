# Changelog

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [1.3.13] - 2026-07-11

### Changed
- 建立 Spring Java PSI 到 `DocView` 和默认 Markdown 的自动化契约测试，并完善 OpenSpec、兼容性与发布检查门禁。

### Fixed
- 修正 OpenAPI 3 `@Operation.summary` 未被读取的问题；启用 Swagger 3 名称来源时使用非空 summary，空值继续按既有顺序回退。
- 清理 IU 2026.1 Plugin Verifier 报告的 7 处 deprecated API usage，并将 deprecated usage 纳入验证失败门禁；Spring/Feign/Dubbo 扫描、tool window 数据与搜索、重复文档名称格式保持不变。

### Security
- 将仓库内 YApi HTTP 示例改为本地私有环境变量占位符，不再跟踪非占位符 endpoint 或 token 形态数据。

## [1.3.12] - 2026-06-13

### Changed
- 建立 UI 后台扫描与 DTO schema 缓存基线，降低 preview、参数编辑器、导出和上传路径的 EDT 阻塞风险。
- 插件兼容基线调整为 IntelliJ IDEA 2026.1+。

### Fixed
- 增加 DTO 递归解析和深度限制保护，避免循环引用或过深嵌套导致 IDE 卡顿。
- 移除预览和参数编辑弹窗工具栏中的 IntelliJ internal `ActionToolbarImpl` API 使用。
- 移除启动通知版本检测中的 IntelliJ internal `PluginManagerCore` 和 `PluginManager` API 使用，改由扩展框架注入插件描述符，确保 1.3.12 Marketplace 兼容性验证不再触发 internal API 拒绝。

## 1.3.11

### Added
- 修改 DocViewData 类以特殊处理集合和 map 类型的参数，保证在展示时忽略 Map 和 List 的属性
