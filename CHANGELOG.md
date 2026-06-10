## [Unreleased]

### Changed

- 建立 UI 后台扫描与 DTO schema 缓存基线，降低 preview、参数编辑器、导出和上传路径的 EDT 阻塞风险。

### Fixed

- 增加 DTO 递归解析和深度限制保护，避免循环引用或过深嵌套导致 IDE 卡顿。

## 1.3.11

### Added

- 修改 DocViewData 类以特殊处理集合和 map 类型的参数，保证在展示时忽略 Map 和 List 的属性

### Changed

### Deprecated

### Removed

### Fixed

### Security
