# Changelog

## [Unreleased]

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [1.3.12] - 2026-06-13

### Changed
- 建立 UI 后台扫描与 DTO schema 缓存基线，降低 preview、参数编辑器、导出和上传路径的 EDT 阻塞风险。
- 插件兼容基线调整为 IntelliJ IDEA 2026.1+。

### Fixed
- 增加 DTO 递归解析和深度限制保护，避免循环引用或过深嵌套导致 IDE 卡顿。
- 移除预览和参数编辑弹窗工具栏中的 IntelliJ internal `ActionToolbarImpl` API 使用。
- 移除启动通知版本检测中的 IntelliJ internal `PluginManagerCore` API 使用，确保 1.3.12 Marketplace 兼容性验证不再触发 internal API 拒绝。

## 1.3.11

### Added
- 修改 DocViewData 类以特殊处理集合和 map 类型的参数，保证在展示时忽略 Map 和 List 的属性
