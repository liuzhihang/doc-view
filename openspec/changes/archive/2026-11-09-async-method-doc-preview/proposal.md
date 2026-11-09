# Proposal

## Why

目录树双击或按 Enter 打开方法文档时，当前调用同步在 UI 线程读取 PSI、生成 `DocView` 和 Markdown，再创建并打开 Scratch。较大的方法或 DTO 会让 IDEA 界面等待，旧日志中的 `SlowOperations` 记录也指向这条调用链。

## What Changes

- 将方法级 Markdown 构建移到可取消的后台 read action。
- 只有生成成功且项目仍打开、任务未取消时，才在 EDT 创建并打开原有 Scratch 文件。
- 无效 PSI、取消、项目关闭和生成/写入失败不得发布迟到或不完整的 Scratch 文档；失败继续使用 Doc View 通知反馈。
- 保持 Markdown 内容、Scratch 路径/命名和打开行为不变；本 change 不调整 `.http` 打开、批量生成或项目导出。

## Capabilities

### New Capabilities

- `method-doc-preview`: 目录树方法节点触发的单方法 Markdown 生成与 Scratch 打开行为。

### Modified Capabilities

无。

## Impact

- 影响 `CustomFileUtils.openMd` 的 PSI/read-action 与 Scratch/UI 线程边界，以及该路径的契约测试。
- 沿用现有 `DocViewBackgroundTasks`、服务、Markdown 模板和 Scratch API；不新增依赖、扩展点或生产语言。
