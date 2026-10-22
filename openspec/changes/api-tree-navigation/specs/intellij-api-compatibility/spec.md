# Spec Delta

## MODIFIED Requirements

### Requirement: 目录树速度搜索行为保持稳定
系统 SHALL 通过公开平台 API 为目录树安装键盘速度搜索，并 MUST 匹配节点展示的代码名称、方法签名及业务说明，保持不自动展开节点的行为。搜索 MUST NOT 在每次输入时解析或生成完整接口文档。

#### Scenario: 键盘定位目录节点
- **WHEN** 用户聚焦目录树并输入唯一节点文本
- **THEN** 选择移动到匹配节点
- **AND** 搜索不会额外展开未展开节点

#### Scenario: 按代码名称或业务说明定位
- **WHEN** 用户输入可见节点的代码名称、方法签名或业务说明中的唯一文本
- **THEN** 选择移动到对应节点
- **AND** 搜索不触发完整文档生成

## ADDED Requirements

### Requirement: 图标扩展使用目标平台公开接口
插件 SHALL 使用目标 IU 261 平台中存在的公开 `AllIcons` 字段呈现通用动作；自有品牌工具窗口与 gutter 图标 SHALL 按 IntelliJ 图标规范提供对应尺寸与主题资源。New UI 工具窗口品牌图标 SHALL 使用公开 `com.intellij.iconMapper` extension point 注册 20×20 与 Compact Mode 16×16 资源映射。图标路径 SHALL 在最终插件包中存在，且不得依赖测试或非 bundled plugin 资源。

#### Scenario: 目标平台兼容性
- **WHEN** 插件使用刷新、复制、导出、设置等通用动作图标并声明 New UI tool-window icon mapping
- **THEN** 每个 `AllIcons` 符号和 `iconMapper` extension 均可在 IU 261 目标平台解析
- **AND** `buildPlugin` 与 Plugin Verifier 检查不报告图标字段、extension point 或资源缺失
