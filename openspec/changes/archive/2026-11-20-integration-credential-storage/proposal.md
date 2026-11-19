# Proposal

## Why

ShowDoc、YApi 与 YuQue 的凭据目前保存在项目级设置 XML 中，并以普通文本字段显示。将 API key/token 改由 IntelliJ `PasswordSafe` 管理，可避免新版继续把凭据序列化到项目设置，同时让设置页和上传路径按平台要求在后台读取安全存储。

## What Changes

- **BREAKING**：从三个项目设置状态中移除 ShowDoc key/token、YApi token、YuQue token；沿用现有项目设置存储与非敏感字段，用户升级后需要重新填写凭据。
- 新增项目级凭据服务，以项目身份和平台作为 `PasswordSafe` 凭据键；ShowDoc key/token 作为一个安全存储值更新，YApi 和 YuQue 各用一个值。
- 设置页以密码控件输入凭据；未输入字符（空字符串）表示保留已存值，任何非空输入按原样替换，只有显式选择清除并 Apply 才删除。凭据存在状态在后台读取，界面只保留状态，不缓存明文。
- PasswordSafe 读写只在后台执行。Apply 使用模态进度等待安全存储操作成功，再提交该平台的非敏感设置；失败保留草稿且不提交设置。Reset、Cancel 不写入凭据。
- 上传检查与凭据读取放入后台路径；PreviewForm 当前绕过上传后台包装的调用需与 `fix-preview-lifecycle` 的文件所有者协调后接入。
- 不读取、迁移或主动删除旧 XML 中的凭据，不新增依赖、不访问真实平台服务。旧 XML 可能在新版首次保存前继续留有明文值；本 change 不提供自动清理。

## Capabilities

### New Capabilities

- `integration-credentials`：约束项目级 PasswordSafe 身份隔离、凭据表单语义、安全读取与应用、上传时的后台访问及设置持久化中不含凭据。

### Modified Capabilities

无。当前 OpenSpec 没有覆盖集成平台凭据存储与生命周期的 capability。

## Impact

- 生产候选范围：三个集成设置状态、设置表单、项目凭据服务、上传服务与三个平台服务；PreviewForm 上传入口由其当前文件所有者协调接入。
- 测试候选范围：项目/平台键隔离、密码字段与零字符保留/非空替换/显式清除语义、Apply/Reset/Cancel、PasswordSafe 失败、后台线程约束、设置状态 XML 不含凭据的 fixture 测试；真实 PasswordSafe 由隔离 IDEA 原生验收。
- 保留现有设置 XML 文件和非敏感设置字段；不迁移旧 XML 凭据、不自动清除旧数据。PasswordSafe 的跨存储写入与项目设置落盘无法构成崩溃级事务，设计中说明其边界。
- 不改变平台请求协议、YApi URL 参数行为或设置布局，不增加第三方依赖，不提交或发布。
