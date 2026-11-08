# 已批准：显式要求重启的发布选项

状态：用户于 2026-11-09 批准该生命周期策略。源 descriptor 已声明 `require-restart="true"`，定向测试 RED 为 3 tests / 1 expected failure，GREEN 为 3 tests / 0 failures。最终 ZIP 属性读回和隔离 IDEA 的重启提示/状态回读仍未验证。该策略不是热卸载修复；429 候选及无 JDWP 对照的失败证据继续保留。

## 已确认问题

- 正式 IU 263.7307.200、429 候选 ZIP `6c789b81e371656db0cdbfca005c82a4175541bb98ae04106500feece46f8370` 在预览 Export 后热卸载失败。PID71085 的强引用链：Local JNI → IDEA UniversalFileChooser 的 FileRenderer/tree → FileView.topToolbar → ActionToolbarImpl.myCreationTrace → Throwable.backtrace → ExportUtils / PreviewForm$6 的 Class mirror → Doc View PluginClassLoader。
- 该堆中 PreviewForm 实例为0；不是据实例归零推断类加载器释放，也不是新的状态 promise 根链。已修复的闭项目 ToolWindow 根链属于另一问题。
- 无 JDWP 的 PID78877 同包、仅预览及成功导出后也报告 still-referenced，卸载后20条 Doc View histogram rows、目标类仍存在。第二个 heap 用同样的 strong-only、class-root=false 分析器确认相同文件选择器引用链，Class mirror 仍为 ExportUtils 与 PreviewForm$6，PreviewForm 实例为0；因此 JDWP 不是该问题的原因。
- 诊断只读对象类型、字段引用及 class-object metadata，不读取凭据或任意堆字符串。未向外部上传 heap。

本机证据：`build/native-validation/2026-11-08/remaining-native/status-cancel-candidate/diagnostics/` 与其 `no-debug-export-control/`。

## 已选择方案

1. 将 `src/main/resources/META-INF/plugin.xml` 根节点设为 `<idea-plugin require-restart="true">`，采用平台公开机制，不反射内部字段、不清除 JNI、不修改全局文件选择器设置。
2. 安装、更新和卸载要求重启成为本次声明的插件生命周期策略。该策略不等同于动态热卸载成功，也不抹除既有失败记录。
3. 补充 descriptor 契约测试并同步本 change 的 design/spec/tasks、兼容性文档和 CHANGELOG。隔离 IDEA 中安装/更新/禁用/卸载的原生提示与重启后状态回读尚未验证，须如实标注；按用户 2026-11-09 的决定，若功能验收完成而该原生项无法完成，可在发布说明中列明未验证并发布后收集用户反馈，不将其伪报为通过。
4. 热卸载相关历史失败和引用链继续留在验证记录；其他功能、凭据及最终版本/ZIP 状态分别报告，不因 `require-restart` 自动通过。已知功能失败仍须先修复；用户允许在功能验收完成后将无法执行的原生检查列为未验证，发布后收集反馈。

用户影响：安装、更新、禁用或卸载可能需要重启IDE；普通文档生成、Copy/Export、参数写回及HTTP请求不改变。该选项是公开支持的生命周期限制，不声称解决了JBR/macOS无障碍根问题。

## 官方依据与边界

- [Dynamic Plugins](https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html)：公开允许在根节点声明 require-restart=true；卸载失败时平台也可能要求重启。
- [Plugin Configuration File](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html)：require-restart 默认false，影响安装/更新/卸载是否需要重启。
- [File and Class Choosers](https://plugins.jetbrains.com/docs/intellij/file-and-class-choosers.html)：推荐 callback 重载支持原生macOS选择器。改用此入口只能改变默认选择器路线，不能仅凭默认native路径通过就声称用户选择IDEA文件选择器时也能热卸载；不把它作为未经验证的完整修复。

这是对原先热卸载验收策略的明确调整，用户已选择以公开重启声明作为生命周期契约。它保留热卸载失败事实，不声称解决 JBR/macOS 根链；未验证项必须在发布说明中保留。
