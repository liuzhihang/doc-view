# Proposal

## Why

修复前，模板设置页的 `reset()` 会把项目中的 Spring/Dubbo 模板直接改为内置默认值，而 IntelliJ 会在创建设置页后调用该方法。这使打开设置、取消未保存编辑等操作存在改变自定义模板的风险，应先修复配置可靠性，再进行模板 UI 改版。

提案依据是修复前源码与平台生命周期文档；自动复现、runIde 尝试及官方 IDEA 隔离实例的实际验收见 [verification.md](verification.md)，不把源码推断当作磁盘模板已被覆盖的实测结论。

## What Changes

- 打开模板页和执行平台 Reset 时，将当前保存的模板读入编辑器，不修改项目设置；Reset 表示放弃未应用修改，不表示恢复出厂模板。
- 编辑仅改变 UI 草稿；Apply/OK 才提交。Cancel 保留最近一次已应用状态，不能撤销同次对话框中已成功 Apply 的内容。
- 修改状态检测使用平台允许的不可变文本快照，保持逐字符比较，避免原生设置回调的读锁警告；不增加版本兼容分支。
- 补齐模板页自建编辑器的关闭释放，支持重复打开/关闭并避免遗留编辑器；范围限定本模板页生命周期。
- 以真实 Configurable、项目设置和编辑器建立失败/通过回归，覆盖 Spring/Dubbo、精确文本保持、取消、重开和状态序列化；用实际 IDE 补充保存/重启验收。
- 更新修复记录与验证证据。本 change 不修改内置模板内容、Velocity 变量、现有存储标识或项目作用域；不增加旧插件版本、旧配置格式或降级读取的兼容逻辑。

非目标：模板预览、变量搜索、恢复默认按钮、设置分组/搜索改版、侧栏、首页、解析规则、文档新格式、索引、MCP、Wiki、Air。上述后续工作仅在 [路线图](../../../docs/roadmap.md) 排队，不合并进本 change。

## Capabilities

### New Capabilities

- `template-settings-lifecycle`：项目模板设置的加载、草稿编辑、提交、取消、重开和 UI 资源生命周期。

### Modified Capabilities

无。现有 Markdown 输出和默认模板 golden 保持不变；不改变当前 IntelliJ 目标平台基线。本 change 仍须验证当前目标 IDE API，但不增加对旧版 IDE/JCEF 行为的兼容要求。

## Impact

- 预期生产修改仅涉及 `TemplateSettingForm`、`TemplateConfigurable`；`TemplateSettings` 的存储契约保持不变。
- 新增 Java 契约测试，沿用当前 IntelliJ test framework/JUnit 与 Gradle；不新增依赖或改变 Java-only 策略。
- 不改 `.form` 布局、`plugin.xml`、Gradle 配置、内置模板资源、PSI/DTO/渲染/上传实现；`CHANGELOG.md` 在实施后记录实际修复。
- 用户已于 2026-10-18 确认按更新后的方案 apply。**本轮完成修复与相关验证，暂不提交、发布、sync 或 archive；实际完成状态以 tasks 和验证记录为准。**
