# 性能指南

Doc View 运行在用户 IDE 进程内。性能 contract 的首要目标是避免阻塞 EDT、无界 PSI 扫描、递归失控和生命周期泄漏。

## PSI 与线程

- PSI 读取在 read action 中执行；写入通过 `WriteCommandAction`。
- action `update` 只读取显示/启用状态，不做项目扫描、递归 DTO 或 Markdown 渲染。
- 扫描、解析、批量导出和上传准备放到可取消后台任务；EDT 只更新 UI。
- Dumb Mode、项目关闭、文件失效和 `PsiElement#isValid` 必须显式处理。
- 限定搜索到当前文件、类、方法、模块或用户选择范围，避免默认全项目扫描。

## 递归与类型展开

- 对对象、数组、collection element、map value 和泛型替换后的 effective type 使用同一递归上下文。
- 上下文记录最大深度和当前路径；检测循环或达到深度时保留当前字段元数据但停止 children 展开。
- invalid/unresolved type 必须降级，不得导致 stack overflow、无限循环或 IDE freeze。
- 示例 JSON 的集合大小和嵌套深度必须有边界。

## 缓存

- 缓存使用 project scope，key 至少考虑 schema role、type identity、PSI modification、相关 settings/template 版本。
- 缓存值保存 DTO snapshot 或派生数据，不长期持有 PSI element。
- 源码、设置或模板变化后必须 miss/失效；clear cache action 必须覆盖用户可感知缓存。
- project dispose 后不得保留条目或后台回调。

## Markdown、导出和上传

- 模板只消费已构建 DTO，不在 Velocity 中触发 PSI 查询。
- preview 多 entry 时按需渲染；重复 preview/export/upload 可以复用已验证的 schema/Markdown。
- 批量导出避免重复解析同一类，文件错误包含安全的路径和原因，并尊重覆盖确认。
- 网络请求设置超时并区分认证、网络、参数和服务端失败；不得在日志中输出 token 或完整敏感 payload。
- 重试必须避免重复创建远端文档。

## UI 生命周期

- 先创建可响应的 UI/loading 状态，再后台构建数据并回投 EDT。
- 后台任务使用同一 `ProgressIndicator` 传播取消，不再启动脱离外层任务的 unmanaged pooled thread。
- 回投结果前检查 project/component 是否已 dispose。

## 验证场景

- 递归：直接自引用、`List<Self>`、`Map<String, Self>`、嵌套泛型和超深 DTO。
- PSI：大 Controller、多个模块、Dumb Mode、文件删除和项目关闭。
- UI：preview、参数编辑、tool window refresh 期间保持响应并可取消。
- 缓存：重复操作命中；修改源码/settings/template 或 clear 后重新生成。
- 导出/上传：批量进度、取消、失败和部分完成状态。

行为或性能优化必须先记录基线场景和完成信号；未经测量不得声称“性能提升”。
