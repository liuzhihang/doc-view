# Tasks

用户已于 2026-10-18 确认按更新后的方案 apply，实施与验证已完成；不提交、不发布、不 sync、不 archive。只有实际完成并取得验证证据的任务才勾选，结果见 [verification.md](verification.md)。场景 T1–T9 与手工步骤见 [design.md](design.md)。

## 1. 取得旧实现的失败证据

- [x] 1.1 新增 `TemplateSettingsLifecycleTest`，使用真实 project service、Configurable 和 Editor Document 覆盖 T1 的初始化路径；保存/恢复原设置并在 finally 清理测试资源。运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --rerun-tasks`，确认至少一个实际执行的用例因自定义模板被 Reset 覆盖而失败，而非依赖、GUI 初始化或测试发现错误；在本 change 的 `verification.md` 记录命令、断言、数量与 RED 结果。
- [x] 1.2 用 `./gradlew runIde` 或设计中说明的官方 IDEA 隔离实例，在临时项目执行旧实现手工步骤 1–2，记录实际 IDE build、Spring/Dubbo 标记及打开后直接 Cancel 的生成结果；只使用虚构模板，明确区分页面文本、内存状态和实际落盘证据。若未能复现，先解释与自动复现的差异并更新设计，不把预期当结果。

## 2. 修复草稿与保存状态的数据方向

- [x] 2.1 扩展真实设置页回归，覆盖 T2–T6 的 Reset 幂等、单项/双项 Apply、逐字符保留、空字符串、编辑后取消，以及 Apply B 后编辑 C 再取消保留 B；检查每种操作后的保存值、编辑器值和 `isModified()`。Cancel 自动用例明确模拟“不调用 Apply 而关闭”的回调序列，真实按钮留给原生验收；在 `verification.md` 记录新增用例与对应场景。
- [x] 2.2 仅修正 `TemplateSettingForm.reset()` 为在正确 EDT/文档写入上下文回读最近保存值，保留现有 Apply 和比较语义，不新增 Cancel 回滚、默认恢复按钮或文本转换。运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --rerun-tasks`，确认 T1–T6 实际通过且测试兜底清理不被描述为生产资源释放已完成；记录 GREEN 与执行数量。
- [x] 2.3 补齐 T7–T8：验证当前版本配置格式经现有 XML 序列化与 `loadState` 往返后保留已应用值、排除未应用草稿，不做跨版本迁移或降级读取适配；以最小虚构 Spring/Dubbo 输入验证新生成文档选择对应模板。运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --tests 'com.liuzhihang.doc.view.contract.SpringMarkdownContractTest' --rerun-tasks`，确认新增用例和既有默认 Spring golden 通过且未重写 golden；在 `verification.md` 分开记录序列化与真实磁盘验证的证据范围。
- [x] 2.4 在状态回归通过后更新 `CHANGELOG.md`，准确描述打开/Reset 不覆盖自定义模板、取消保留最近 Apply 的值；核对文案与规格和实际 GREEN 一致，不声称恢复已丢失模板或已完成真实 IDE 重启验收。

## 3. 补齐设置页编辑资源的释放

- [x] 3.1 增加 T9，跟踪本页 Spring、Dubbo、description 三个 editor，覆盖关闭、重开、未创建即清理、重复清理和其他 editor 不受影响；先运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --rerun-tasks`，取得生产清理缺失的 RED。资源断言必须早于测试 finally 兜底清理，在 `verification.md` 记录实际失败证据。
- [x] 3.2 在 `TemplateConfigurable.disposeUIResources()` 与 form 内补齐仅属于本页的 editor/DialogWrapper 清理，清空引用并保护重复调用；保留 `.form` 与其他设置页。运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.TemplateSettingsLifecycleTest' --rerun-tasks`，确认 T1–T9 通过、三个 editor 均释放、其他 editor 保留且草稿未被保存；在 `verification.md` 更新结果，并按实际修复补充 changelog 生命周期说明。

## 4. 集成回归与原生 IDE 验收

- [x] 4.1 运行 `./gradlew test --rerun-tasks`，读取 `build/test-results/test/` 和 `build/reports/tests/test/`，确认全量测试实际发现并执行、数量非零、无未解释的跳过，已有 Markdown contract 仍通过；将命令、数量与结果补入 `verification.md`，不把缓存或 0 tests 视为通过。
- [x] 4.2 运行 `./gradlew verifyPluginProjectConfiguration buildPlugin verifyPlugin`，检查配置、插件包和 Verifier 报告中的实际目标 IDE/API 结果；在 `verification.md` 记录通过、失败、环境阻塞与未运行项，保持现有 Java-only、平台、依赖和 extension 边界。
- [x] 4.3 用 `./gradlew runIde` 或设计中说明的官方 IDEA 隔离实例，执行修复后手工步骤 1–7，验证真实 Apply/OK/Cancel、Reset 可用性、两种模板生成、重复打开关闭、深浅主题基础操作、正常保存/退出/重启与两个临时项目隔离；核对虚构模板标记在实际配置文件、重开页面和新文档中一致，并记录 IDE build、日志与结果。未完成的手工项保留为未验收，不由 fixture 或序列化测试代替。
- [x] 4.4 核对最终 diff 仅含本 change 所需代码、测试、changelog 与验证记录，存储标识、默认模板、变量和既有 golden 未变化；运行 `git diff --check` 与 `openspec validate fix-template-settings-lifecycle --strict`。使 `verification.md`、tasks 勾选和实际证据一致，向用户交付结果与剩余缺口；不自动 sync、archive、提交或发布。
- [x] 4.5 修复原生验收发现的 isModified 读锁警告：仅将两项草稿读取改为公开不可变快照 API，保留精确比较；重跑生命周期、全量测试、打包和 Verifier，并在同一 IU-262.10968.63 原生回调中复验焦点切换/编辑/Apply/Cancel 后不再出现该警告。
