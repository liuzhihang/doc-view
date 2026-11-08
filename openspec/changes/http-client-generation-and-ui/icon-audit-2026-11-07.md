# Doc View UI 图标与 Settings 说明样式审计

日期：2026-11-07。初始审查范围只读：`AGENTS.md`、`doc-view-java-maintenance`、架构/Contract/性能指南、`api-tree-navigation` 既有图标 contract，以及当前 `src/main/java`、`src/main/resources`。初始审查未操作 IDE、未运行 Gradle，后续实现与原生结果另见下方链接。另对缓存的 IU 263.7031.23 `AllIcons` facade 做了 `javap` 字段存在性核对。

## 结论

- 截图中 Settings → General behavior 的两段说明由 `SettingsForm.wrappingHelp()` 创建。修复前 helper 没有显式 font/foreground，导致继承 LAF 的 `TextArea.font` / `TextArea.foreground`，与截图所见等宽大字、和正文同亮度一致。当前读取的源码已在该 helper 设置 `JBUI.Fonts.smallFont()` 与 `UIUtil.getContextHelpForeground()`；字体已在 full-14/full-15 的深浅主题原生界面确认；随后发现的设置外层布局裁切已在 full-16 修复并原生复验，详见[原生记录](../intellij-263-baseline-upgrade/verification-2026-11-07.md)。
- 同一 helper 只在 `SettingsForm` 中调用两次：行号说明和“包含普通接口”说明。没有其他页面调用此 helper。
- 图标方面唯一决定的活动替换是参数编辑器的“复制 JSON”：用 `Actions.Copy` 表示复制操作，并显示 `Copy JSON` / `复制 JSON`。确认写回按钮保留 `Actions.Checked`，显示 `Apply to Source` / `写回源码`。其他活动 `AllIcons` 语义合理；Doc View 自有 SVG 保留为品牌资源。未启用的旧 Editor Floating Toolbar `Save` action 仍有独立的陈旧绑定观察，本轮不扩散为 runtime 修改。

## Settings 说明文本根因（修复前审计）

[SettingsForm.java](../../../src/main/java/com/liuzhihang/doc/view/ui/SettingsForm.java) 放置两段说明。修复前的 `wrappingHelp` 创建只读、透明、自动换行的 `JBTextArea`，但不设 `font` / `foreground`，所以采用当前 LAF 的 `TextArea` 默认字体和文字颜色。这解释了截图里的等宽大字号与正文同亮度。

当前源码 [wrappingHelp helper](../../../src/main/java/com/liuzhihang/doc/view/ui/SettingsForm.java) 已设置 `JBUI.Fonts.smallFont()` 与 `UIUtil.getContextHelpForeground()`，与 [GenerationRulesForm.summary](../../../src/main/java/com/liuzhihang/doc/view/ui/GenerationRulesForm.java) 和 [TemplateSettingForm.wrappedHint](../../../src/main/java/com/liuzhihang/doc/view/ui/TemplateSettingForm.java) 一致。字体和颜色已原生复验，布局宽度问题按上述记录独立追踪。

[SettingsGuideHelp](../../../src/main/java/com/liuzhihang/doc/view/ui/SettingsGuideHelp.java) 创建的是 IDEA 原生 `ContextHelpLabel`，用于标题旁帮助图标和链接，与两段 `JBTextArea` 说明无关。精确使用页为 Settings 根页、生成规则、集成总页、YApi、ShowDoc、语雀、模板设置；它们复用的是同一原生帮助 affordance，不会造成说明文本的字体/颜色问题。没有其他页面调用 `SettingsForm.wrappingHelp()`。

`TemplateSettingForm.variableDescriptionArea` 是另一处只读 `JTextArea`，属于模板变量详情面板，未设置字体/前景色；它不调用上述 helper，不能算同一问题的已证实复现点。应独立按实际视觉需求评估，不在本轮推定为缺陷。

## 动作与节点图标清单

| 表面 / 来源 | 当前图标 | 判断 |
|---|---|---|
| 工具窗口树：[RootNode](../../../src/main/java/com/liuzhihang/doc/view/ui/window/RootNode.java)、[ModuleNode](../../../src/main/java/com/liuzhihang/doc/view/ui/window/ModuleNode.java)、[ClassNode](../../../src/main/java/com/liuzhihang/doc/view/ui/window/ClassNode.java)、[MethodNode](../../../src/main/java/com/liuzhihang/doc/view/ui/window/MethodNode.java) | `Nodes.ModuleGroup`、`Nodes.Module`、`Nodes.Class` / `Nodes.Interface`、`Nodes.Method` | 保留。使用 IDE 原生结构节点图标，语义准确。 |
| 工具窗口工具栏与目录菜单：[plugin.xml](../../../src/main/resources/META-INF/plugin.xml) | Refresh、Export、Upload、Expandall、Collapseall、Settings、ClearCash、Preview、Find、FileTypes.Http | 保留。刷新、导出/上传、展开/折叠、设置、清缓存、查看/打开、搜索、HTTP 草稿图标分别对应操作。`Open Project Docs` 使用 Preview 是查看文档；`Generate or Update Project Docs` 使用 Export 且位于 Export 子菜单，符合该输出入口的上下文，不建议单独换成“新建”图标。`ClearCash` 是 263 SDK 的平台字段名，代码用其表示 Clear Cache，并非项目拼写错误。 |
| 当前 Preview 弹窗动态 toolbar：[PreviewForm](../../../src/main/java/com/liuzhihang/doc/view/ui/PreviewForm.java) | Settings、Pin、Preview、Upload、Export、Copy、Export All、Upload All | 保留。齿轮、图钉、预览、上传、导出、复制均与动作一致；复用同一图标保持一致性。 |
| 参数编辑器 toolbar（只列图标，不重复 API 审查）：[ParamDocEditorForm](../../../src/main/java/com/liuzhihang/doc/view/ui/ParamDocEditorForm.java) | Settings、Pin、修复前 FileTypes.Json → 当前 Actions.Copy、Actions.Checked | 图标审计决定唯一替换项：复制动作改为 `Actions.Copy`，可见名称改为 `Copy JSON` / `复制 JSON`；`Checked` 保留并配 `Apply to Source` / `写回源码`。这是图标/可见命名决定，参数编辑 API 与行为由独立审查覆盖。 |
| 设置标题旁帮助：[SettingsGuideHelp](../../../src/main/java/com/liuzhihang/doc/view/ui/SettingsGuideHelp.java)、[GenerationRulesForm](../../../src/main/java/com/liuzhihang/doc/view/ui/GenerationRulesForm.java) | IDEA `ContextHelpLabel` 原生帮助图标 | 保留。无需另造问号 SVG。 |
| HTTP 草稿动作：[plugin.xml](../../../src/main/resources/META-INF/plugin.xml) | `FileTypes.Http` | 保留。能指向 HTTP request 文件类型。 |
| 非活动旧 Preview action groups：[plugin.xml](../../../src/main/resources/META-INF/plugin.xml) | Export、Upload、Copy 的对应平台图标 | 图标本身合理；当前 PreviewForm 通过 Java 内联 actions 构建 toolbar，源码未发现这些旧组的活动 toolbar 消费者。保留定义时不需要重画或换图标。 |

目标 IU 263.7031.23 的缓存 `intellij.platform.util.ui.jar` 中已核对以下字段全部存在：`Actions.Upload/Preview/Copy/Refresh/ClearCash/MenuSaveall/Checked/Find/Expandall/Collapseall`、`General.Settings/Pin`、`FileTypes.Http/Json`、`ToolbarDecorator.Export`、`Nodes.ModuleGroup/Module/Class/Interface/Method`。这是 SDK facade 字段存在性核对，不等同于本轮原生 UI 主题可见性验收。`api-tree-navigation` 已有 IU261 的图标语义与主题矩阵验收记录；该历史结果不冒充本轮 IU263 原生截图。

## Dormant 声明（仅观察）

[plugin.xml 的 Editor Floating group](../../../src/main/resources/META-INF/plugin.xml) 把 `liuzhihang.doc.preview.editor.save` 声明成 `text="Save"`、`MenuSaveall` 图标，却绑定到 `MenuExportAllAction`；该类实际执行批量 Markdown 导出。对应 `editorFloatingToolbarProvider` 扩展当前在 [plugin.xml](../../../src/main/resources/META-INF/plugin.xml) 被注释，故未确认该入口可见。这里只记录 descriptor 中的 dormant mismatch，不列入本轮活动图标替换项，也不提出运行时代码修改。

## 品牌与内容图标

- [DocViewIcons](../../../src/main/java/icons/DocViewIcons.java) 的 `DOC_VIEW`、`DOC_VIEW_TOOL_WINDOW`、`DOC_VIEW_GUTTER` 分别用于通知、工具窗口条和 gutter；均为 Doc View 的 M + 下箭头品牌标记。浅/深主题资源成对提供。保留为品牌，不替换成通用平台图标。
- `pluginIcon.svg` 是 40×40 蓝底插件 logo；`DocViewIconMappings.json` 将 New UI 的 tool-window / gutter 映射到专用品牌 SVG。工具窗口提供传统 13px 与 New UI 16/20px 文件，gutter 提供 12/14px 资源；保留尺寸和主题变体。
- `welcome.html`、`welcome_zh_CN.html`、`updates.html`、`updates_zh_CN.html` 含同一 16×16 inline SVG 品牌标记；欢迎页另用 `↗`、`{ }`、`✓` 做装饰性 feature 标记，分别配目录导出、模板、设置文案，语义可读，不需要新建一套图标。
- [SupportForm.form](../../../src/main/java/com/liuzhihang/doc/view/ui/SupportForm.form) 放置 200×200 `image/wechat.png`，对应关注公众号说明；这是支持页面内容/二维码资产，不是操作按钮图标。保留为内容资产。

## 替换清单

- **唯一活动图标替换决定：** `ParamDocEditorForm` 的复制 JSON 按钮从 `AllIcons.FileTypes.Json` 换为 `AllIcons.Actions.Copy`，并显示 `Copy JSON` / `复制 JSON`；动作本身是复制到剪贴板。写回按钮保留 `AllIcons.Actions.Checked`，可见文本为 `Apply to Source` / `写回源码`。复制图标和两个可见名称已实施，full-14 原生 Copy JSON / 写回及 Undo 已复验；完整主题/布局矩阵仍按对应 tasks 追踪。
- **其他活动图标：** 继续复用平台图标；现有自有 SVG 用于品牌。项目文档生成入口共享 Export 图标，因为它位于 Export 下拉并写出 Markdown/JSON 文档。
- **Dormant 声明：** `Save` → `MenuExportAllAction` 仅作为未启用的 descriptor 观察记录；本轮不将其扩展为运行时代码变更。
