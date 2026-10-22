# Spec Delta

## Purpose

规定 Doc View 如何安全保存、呈现并使用 ShowDoc、YApi 和 YuQue 的项目级访问凭据，同时保持普通设置生命周期可预测并避免新版继续写入凭据到项目配置。

## ADDED Requirements

### Requirement: 凭据按项目和平台隔离

系统 SHALL 将 ShowDoc、YApi、YuQue 凭据保存在 IDE 提供的安全凭据存储中，并 SHALL 以项目身份和平台身份隔离；新版保存的项目设置状态 SHALL NOT 包含这些凭据。

#### Scenario: 同一平台的不同项目
- **WHEN** 两个不同项目分别保存同一平台的凭据
- **THEN** 每个项目的上传 SHALL 只读取其自身凭据
- **AND** 一个项目的凭据 SHALL NOT 被另一个项目解析为已配置
- **AND** 替换或清除其中一个项目的凭据 SHALL NOT 覆盖或删除另一个项目的凭据

#### Scenario: 保留普通项目设置
- **WHEN** 新版加载并保存平台设置
- **THEN** URL、YApi 项目 ID、YuQue login/repos 等非敏感值 SHALL 继续使用现有项目设置存储
- **AND** 新序列化的设置状态 SHALL NOT 包含 ShowDoc key/token、YApi token 或 YuQue token

### Requirement: 旧 XML 凭据不迁移或自动清理

系统 SHALL NOT 读取、迁移、显示或自动删除升级前设置 XML 中的凭据值；用户 SHALL 在新版重新录入凭据，未由新版重写的旧 XML 可能继续保留原值。

#### Scenario: 从含旧凭据的配置升级
- **WHEN** 新版加载包含旧平台凭据的项目设置文件
- **THEN** 插件 SHALL 继续读取支持的非敏感设置
- **AND** SHALL 将平台凭据视为未配置并要求用户重新录入
- **AND** SHALL NOT 主动迁移或删除旧凭据

#### Scenario: 新版保存设置状态
- **WHEN** 新版通过常规设置持久化写回平台设置状态
- **THEN** 写出的新状态 SHALL 不包含凭据字段
- **AND** 系统 SHALL NOT 执行专门扫描、清理或改写旧 XML 的操作

### Requirement: 凭据输入保持遮蔽并支持显式清除

平台设置 SHALL 通过密码输入控件编辑凭据；未输入字符（空字符串）且未选择清除 SHALL 表示保留已保存凭据，任何非空输入 SHALL 按原样替换凭据，只有用户显式选择清除并 Apply 后才删除该凭据。

#### Scenario: 更新一个已有凭据
- **WHEN** 用户在 ShowDoc、YApi 或 YuQue 设置页输入新凭据并 Apply
- **THEN** 输入 SHALL 以遮蔽形式显示
- **AND** 对应安全存储值 SHALL 在该平台 Apply 成功后更新

#### Scenario: 未输入字符以保留凭据
- **WHEN** 用户保持凭据输入框字符数为零且未选择清除并 Apply
- **THEN** 已保存凭据 SHALL 保持不变
- **AND** 空字符串 SHALL NOT 被解释为写入新凭据

#### Scenario: 原样替换非空凭据
- **WHEN** 用户输入一个或多个字符并 Apply
- **THEN** 对应安全存储值 SHALL 使用输入原文，不自动裁剪首尾空白

#### Scenario: 显式清除凭据
- **WHEN** 用户选择清除某凭据并 Apply 成功
- **THEN** 对应安全存储值 SHALL 被删除
- **AND** 其他平台的凭据 SHALL 保持不变

### Requirement: 凭据状态读取不阻塞界面且不缓存明文

系统 SHALL 在后台查询凭据状态，并 SHALL 只保留配置状态而不在设置表单或长期缓存中保留凭据明文；插件 SHALL NOT 在 EDT 上直接调用阻塞式安全存储 API。

#### Scenario: 打开平台设置页
- **WHEN** 用户打开或重置一个集成设置页
- **THEN** 凭据读取 SHALL 在后台执行
- **AND** 界面 SHALL 在读取完成后显示已配置、未配置或不可用状态
- **AND** 后台结果 SHALL NOT 覆盖较新的草稿或已关闭页面状态

#### Scenario: 安全存储访问线程
- **WHEN** 插件读取、写入或删除凭据
- **THEN** 安全存储调用 SHALL 在后台线程执行
- **AND** 插件 SHALL NOT 直接阻塞 EDT 调用该 API
- **AND** 由 EDT 发起的 Apply 可使用平台 modal progress 等待后台操作，平台事件循环 SHALL 在等待期间保持可调度

### Requirement: Apply 与 Reset/Cancel 保持凭据状态一致

Apply SHALL 在凭据安全存储操作成功后才提交该平台的非敏感设置；失败 SHALL 保留表单草稿并保持非敏感设置不变，Reset 和 Cancel SHALL NOT 写入或删除凭据。

#### Scenario: 成功应用凭据与普通设置
- **WHEN** 用户 Apply 一组平台设置且安全存储操作成功
- **THEN** 凭据 SHALL 按当前草稿操作保存或清除
- **AND** 同一次 Apply 的非敏感设置 SHALL 随后提交
- **AND** 设置页 SHALL 显示成功后的已应用状态

#### Scenario: 安全存储失败
- **WHEN** 凭据安全存储读写或删除失败
- **THEN** 非敏感设置 SHALL 保持 Apply 前的已保存值
- **AND** 用户输入草稿 SHALL 保留以便重试
- **AND** 错误提示与日志 SHALL NOT 包含凭据明文

#### Scenario: 安全存储抛出致命运行时错误
- **WHEN** 凭据安全存储操作抛出 JVM `Error`
- **THEN** 系统 SHALL 在进度任务完成清理后将该错误传播给平台错误处理
- **AND** SHALL NOT 将该错误转换为普通的凭据存储失败提示

#### Scenario: Reset 丢弃草稿
- **WHEN** 用户在未 Apply 时 Reset 平台设置
- **THEN** 非敏感字段 SHALL 恢复为最近已应用值
- **AND** 凭据输入与清除意图 SHALL 恢复为保留状态
- **AND** 安全存储 SHALL NOT 被修改

#### Scenario: Cancel 丢弃未应用改动
- **WHEN** 用户修改平台设置后选择 Cancel，且这些改动尚未 Apply
- **THEN** 未应用的凭据与普通字段草稿 SHALL 被丢弃
- **AND** 安全存储与已保存普通设置 SHALL 保持不变

#### Scenario: Apply 成功后再 Cancel
- **WHEN** 用户先成功 Apply，再对设置对话框执行 Cancel
- **THEN** 已成功应用的凭据与普通设置 SHALL 保持不变

### Requirement: 上传在后台读取项目凭据

集成上传 SHALL 在后台检查并读取当前项目的平台凭据，缺失凭据 SHALL 保持既有未配置提示且 SHALL NOT 发起平台请求。

#### Scenario: 上传前检查与读取
- **WHEN** 用户从任一上传入口触发 ShowDoc、YApi 或 YuQue 上传
- **THEN** 配置检查和凭据安全存储访问 SHALL 在后台执行
- **AND** 上传 SHALL 使用当前项目、当前平台对应的凭据
- **AND** 上传入口 SHALL NOT 在 EDT 上调用安全存储

#### Scenario: 缺少平台凭据
- **WHEN** 当前项目没有所选平台所需凭据
- **THEN** 系统 SHALL 显示现有平台配置提示并停止上传
- **AND** SHALL NOT 发起外部平台请求

#### Scenario: 上传日志与请求行为
- **WHEN** 插件处理上传或安全存储错误
- **THEN** 插件日志与用户可见错误 SHALL NOT 输出凭据明文
- **AND** 本 change SHALL 保持既有平台请求字段与协议行为
