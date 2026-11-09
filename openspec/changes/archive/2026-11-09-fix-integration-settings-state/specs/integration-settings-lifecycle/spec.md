# Spec Delta

## Purpose

约束 ShowDoc、YApi 与 YuQue 设置页在空值、草稿、应用、重置和缺少配置导航中的可观察行为，避免未配置状态被误报为已修改或校验失败时部分写入设置。

## ADDED Requirements

### Requirement: 空值与 URL 修改状态保持一致

系统 SHALL 将持久化字符串 `null` 与空输入视为同一未配置值；URL 字段 SHALL 在比较和提交时使用相同、幂等的空白与尾斜线规范化，其他字段 SHALL 保留各平台当前的外层空白处理语义。

#### Scenario: 空设置页首次打开
- **WHEN** ShowDoc 或 YApi 的字符串设置均为 `null`、YuQue 的可选字段为 `null`，且界面显示对应空值
- **THEN** 设置页 SHALL 报告未修改

#### Scenario: 应用尾部带多个斜线的 URL
- **WHEN** 用户输入 ` https://api.example.test/// ` 并应用 ShowDoc、YApi 或 YuQue URL 字段
- **THEN** 持久化值 SHALL 为 `https://api.example.test`
- **AND** 应用后设置页 SHALL 报告未修改
- **AND** 再次打开并重置页面 SHALL 保持相同结果

#### Scenario: 空值与已保存空串比较
- **WHEN** 已保存字段为空串且界面字段为空，或已保存字段为 `null` 且界面字段为空
- **THEN** 设置页 SHALL 报告未修改

### Requirement: 允许保存未配置或部分配置

系统 SHALL 允许 Apply 保存空白或不完整的 ShowDoc、YApi、YuQue 设置；Apply SHALL NOT 发起连接、认证或外部服务请求。

#### Scenario: 清空一个平台设置
- **WHEN** 用户清空某个平台设置页的全部字段并 Apply
- **THEN** 表单 SHALL 成功应用空值，YApi 空项目 ID SHALL 保存为未设置状态
- **AND** 设置页 SHALL 报告未修改

#### Scenario: 保存部分配置
- **WHEN** 用户只填写某个平台的部分字段并 Apply
- **THEN** 已填写值 SHALL 保存，未填写值 SHALL 保持为空或未设置
- **AND** Apply SHALL NOT 因缺少上传字段而拒绝保存

### Requirement: YApi 项目 ID 可选且校验原子

YApi 空项目 ID SHALL 表示未设置；非空项目 ID SHALL 是可解析为正 `long` 的十进制数字，且所有 YApi 字段 SHALL 在校验成功后才一起应用。

#### Scenario: 首次配置时项目 ID 为空
- **WHEN** 用户将 YApi 项目 ID 留空或清空并 Apply
- **THEN** 项目 ID SHALL 保存为 `null`
- **AND** URL 与 token 的输入值 SHALL 按正常 Apply 规则保存

#### Scenario: 有效项目 ID
- **WHEN** 项目 ID 是 `1` 到 `9223372036854775807` 范围内的十进制数字
- **THEN** 设置 SHALL 成功应用该 ID 及本次其他字段

#### Scenario: 带前导零的有效项目 ID
- **WHEN** 用户输入 `001` 并 Apply
- **THEN** 项目 ID SHALL 保存为数值 `1`
- **AND** 项目 ID 输入框 SHALL 显示规范化文本 `1`
- **AND** Apply 后设置页 SHALL 报告未修改

#### Scenario: 无效或溢出的项目 ID
- **WHEN** 非空项目 ID 含非十进制数字、不是正数或超出 `long` 范围
- **THEN** Apply SHALL 显示项目 ID 校验错误
- **AND** SHALL 保留当前表单草稿
- **AND** SHALL 保持已保存的 URL、项目 ID 与 token 全部不变

#### Scenario: 输入无效 ID 时修改状态可见
- **WHEN** 用户把已保存项目 ID 改成空值或无效文本
- **THEN** 设置页 SHALL 报告存在未应用修改
- **AND** 对非空无效值的拒绝 SHALL 发生在 Apply 校验阶段

### Requirement: Reset 恢复全部已保存值

设置页 Reset SHALL 将每个界面字段恢复为该平台最近已保存值，MUST NOT 因某个持久化值为 `null` 而保留旧草稿。

#### Scenario: YApi 已保存项目 ID 为空
- **WHEN** 表单中存在项目 ID 草稿而已保存项目 ID 为 `null`，用户执行 Reset
- **THEN** 项目 ID 输入框 SHALL 清空
- **AND** Reset 后设置页 SHALL 报告未修改

#### Scenario: 各平台恢复已保存值
- **WHEN** 用户修改任一平台的多个字段后执行 Reset
- **THEN** 所有字段 SHALL 恢复为该平台已保存值
- **AND** Reset SHALL NOT 修改持久化设置

### Requirement: 缺少配置时打开对应平台设置

上传前置检查发现配置不完整时，系统 SHALL 打开触发该检查的平台设置页，并保留现有缺配置提示和不继续上传的行为。

#### Scenario: YuQue 设置不完整
- **WHEN** YuQue 上传前置检查发现任一必需字段为空
- **THEN** 系统 SHALL 显示 YuQue 配置提示
- **AND** SHALL 打开 YuQue 设置页
- **AND** SHALL NOT 发起 YuQue 请求

#### Scenario: YApi 设置不完整
- **WHEN** YApi 上传前置检查发现 URL、项目 ID 或 token 缺失
- **THEN** 系统 SHALL 打开 YApi 设置页
- **AND** SHALL NOT 发起 YApi 请求

### Requirement: YApi 设置页使用一致的 configurable ID

YApi 设置页在插件 descriptor 中注册的 ID SHALL 与其 searchable configurable 返回的 ID 完全一致。

#### Scenario: 检查 YApi 设置页注册
- **WHEN** 加载插件 descriptor 并读取 YApi configurable 的搜索 ID
- **THEN** 两处 ID SHALL 均为 `liuzhihang.api.doc.YApiSettingsConfigurable`
