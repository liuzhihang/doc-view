# Tasks

## 1. 编写预览生命周期回归测试

- [x] 1.1 在 `PreviewLifecycleTest` 使用现有 Java fixture 和真实 `EditorFactory`，记录预览及无关 viewer 的身份集合；覆盖未展示 form 不创建 editor、关闭或 show 失败后只释放预览 editor，并在测试兜底清理前完成断言。round7 中 `PreviewLifecycleTest` 10/10 通过；JCEF panel 释放另见 1.4。
- [x] 1.2 在同一测试类覆盖空列表、当前请求失败/取消、旧请求逆序成功/失败、popup 关闭后的回调、新选择期间复制/单文档导出不可用，以及关闭前导出快照仍可用；断言源码 editor 状态可见且不进入导出数据。通过窄的请求完成回调控制次序，不新增通用调度测试层；使用动作实际消费的不可变快照断言导出文件名、正文及批量文档副本。round7 中 `PreviewLifecycleTest` 10/10 通过；HTML 状态替换另见 1.4。
- [x] 1.3 在同一 fixture 中使用虚构 Spring 与 Dubbo 源码分别验证预览输出，并记录模板生成与 UI 应用线程。round7 中 `testSpringRenderOnBackgroundAndApplyOnDispatchThread` 与 `testDubboRenderOnBackgroundAndApplyOnDispatchThread` 通过；预览输出包含对应方法名和路径，模板生成在后台线程，Document 应用在 EDT。HTML panel 路径另见 1.4。
- [ ] 1.4 在 JCEF 可用的目标 IDE 验证 HTML panel 显示 Loading/Rendering、成功及错误/取消状态，状态替换上一份内容，并在关闭 preview 后释放 panel。当前 fixture 显式禁用 JCEF，尚无该路径的自动或原生验证。

## 2. 建立预览资源所有权

- [x] 2.1 将源码 `Document`、viewer 和文档加载延迟到 popup 显示流程；由 popup 所有者释放 viewer 并使关闭后的回调失效。自动测试覆盖未显示 form 不分配 editor、关闭或 show 失败后 viewer 数量恢复基线且无关 editor 保留；round7 中 `PreviewLifecycleTest` 10/10 通过。
- [ ] 2.2 在 JCEF 可用的目标 IDE 验证 HTML panel 仅在需要的 preview 生命周期中创建，并在关闭/失败路径释放；该分支当前未被 fixture 执行。

## 3. 完成预览请求和状态生命周期

- [x] 3.1 在 `PreviewForm` 使用本地 Backgroundable 回调处理 Markdown 渲染的成功、失败和取消；新请求和 popup 关闭时使旧回调失效，所有回调按请求编号及关闭状态校验。round7 中 `PreviewLifecycleTest` 10/10 通过，覆盖终态替换进度提示、旧回调和关闭后的错误保护。
- [x] 3.2 新选择开始时清空可复制/导出的 Markdown；Export 和 Export All 在关闭 popup 前捕获各自所需快照。运行 targeted 测试，确认加载/状态文本不可作为 Markdown，且资源清理后导出快照完整。
- [x] 3.3 补充真实后台文档列表加载任务的失败/取消及关闭后回调测试；现有自动化验证了空列表状态应用，但没有覆盖列表加载任务的完整终态回调链。

## 4. 移出 EDT 生成并完成集成验收

- [x] 4.1 将 Markdown 与可选 HTML 字符串生成放到后台任务，按需在后台读取 PSI/VFS；在 EDT 更新 editor、HTML panel 和状态。round7 中 `PreviewLifecycleTest` 10/10、`SpringMarkdownContractTest` 1/1 通过；Spring/Dubbo 测试断言模板生成在后台线程、Document 更新在 EDT。该 fixture 禁用 JCEF，HTML 分支的实际运行仍见 1.4/2.2。
- [x] 4.2 运行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 并记录自动验证结果。round8 共执行 18/18 个 Gradle task，96 tests passed；IU 261.22158.277 Plugin Verifier Compatible，未报告 deprecated/experimental API。
- [ ] 4.3 在目标 IDE 验证 Spring 与 Dubbo 预览成功、空/失败/取消终态、快速切换后旧结果不覆盖新选择、关闭 popup 后 viewer 数量回到关闭前基线、单项与批量导出可用；JCEF 可用时验证 HTML 状态替换和 panel 释放。记录实际通过、跳过和未覆盖项。
