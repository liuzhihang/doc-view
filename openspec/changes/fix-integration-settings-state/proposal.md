# Proposal

## Why

ShowDoc、YApi 与 YuQue 设置页对空值、YApi 项目 ID 和 URL 尾斜线的比较/提交规则不一致，导致空白页被误报为已修改、合法清空不可提交，或 Apply 后仍显示未保存；YApi 无效 ID 还可能在报错前写入部分设置。YuQue 缺少配置时会打开错误的设置页，YApi 配置页的注册 ID 也与其 `SearchableConfigurable` 不一致，影响设置查找和后续维护。

## What Changes

- 统一三个集成设置表单的空值比较与 URL 规范化，使 Reset、Apply 和 `isModified()` 对同一输入得到一致、幂等的状态。
- 允许用户保存不完整或全空的集成设置；不在 Apply 时发起网络校验。YApi 项目 ID 为空表示未配置，非空时必须是正的十进制 `long`；校验失败时不写入任何 YApi 设置。
- Reset 始终从已保存状态恢复所有字段，包括将空的 YApi 项目 ID 显示为空。
- 将 YuQue 缺少配置时的导航指向 YuQue 设置页，并统一 YApi descriptor 与 configurable 的 ID。
- 为表单状态、YApi ID 校验原子性、重置、路由和 descriptor ID 增加无外部服务依赖的回归验证。

非目标：调整设置页面布局或术语、迁移凭据到 PasswordSafe、增加连接测试、改变真实上传行为/请求/payload、改变凭据存储格式或为旧配置格式增加兼容层。

## Capabilities

### New Capabilities

- `integration-settings-lifecycle`：定义 ShowDoc、YApi、YuQue 设置的空值语义、草稿比较、Apply、Reset、YApi 可选项目 ID 验证，以及缺少配置时的设置导航。

### Modified Capabilities

无。当前 OpenSpec 没有覆盖这三类集成设置的行为 capability。

## Impact

- 生产候选范围：`ShowDocSettingForm`、`YApiSettingForm`、`YuQueSettingForm`、`YuQueServiceImpl`、`YApiSettingsConfigurable` / `plugin.xml`。
- 测试候选范围：IntelliJ project fixture 下的设置表单契约测试和插件 descriptor 注册检查；YuQue 导航用隔离 IDE 场景验证。
- 不增加依赖，不访问 ShowDoc、YApi 或 YuQue 服务，不修改 `ShowDocSettings`、`YApiSettings`、`YuQueSettings` 的持久化格式。
- 本 change 只准备方案与验证任务；实施和运行时改动由后续按任务执行。本批不提交或发布。
