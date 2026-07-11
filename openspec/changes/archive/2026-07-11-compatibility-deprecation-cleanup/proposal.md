## Why

当前插件在声明支持的 IntelliJ IDEA 2026.1 上通过兼容性验证，但 Plugin Verifier 仍报告 7 处 deprecated API usage。继续保留这些调用会增加后续平台升级风险，也无法把 deprecated usage 作为可持续的发布门禁。

## What Changes

- 使用目标平台提供的公开替代 API，清理 Spring、Feign、Dubbo 注解索引扫描中的 deprecated 调用。
- 将 tool window 数据提供迁移到 `uiDataSnapshot(DataSink)`，并通过公开 `TreeUIHelper` 安装树搜索。
- 使用 Apache Commons Lang 当前实例 API 生成既有格式的随机后缀，保持文档名称语义不变。
- 将 deprecated API usage 加入 Plugin Verifier 失败级别，防止同类调用回归。
- 增加真实 PSI/index 与重复名称契约验证，并记录 UI 手工烟测和前向 IDE 验证路径。
- 不改变 Markdown、上传、导出、设置、注解识别规则、重复名称格式或声明支持的 IDE 矩阵。

## Capabilities

### New Capabilities

- `intellij-api-compatibility`: 定义 Doc View 使用公开非弃用平台 API、保持扫描/UI/命名行为以及通过 Plugin Verifier 门禁的兼容性契约。

### Modified Capabilities

无。

## Impact

- 生产代码：tool window、Spring/Feign/Dubbo PSI 工具和 `DocViewService` 的局部 API 替换。
- 构建：Plugin Verifier 的失败级别增加 deprecated API usage。
- 测试与文档：新增契约测试，更新兼容性说明、changelog 和 OpenSpec artifacts。
- 不新增依赖、不修改 `plugin.xml`、不提高 `sinceBuild`，也不发布或上传插件。
