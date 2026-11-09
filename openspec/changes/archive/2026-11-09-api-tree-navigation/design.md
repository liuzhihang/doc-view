# Design

## Context

见 [proposal](proposal.md)。`ClassNode.getName()` 与 `MethodNode.getName()` 当前返回业务名称；同一方法名称还被导出动作使用，不能直接把该方法统一替换为代码签名。`docPath/httpPath` 使用现有业务标题组成路径。`ClassNode.updateNode` 不清除旧方法，重复更新有累积风险。`DocViewWindowPanel` 采用 SimpleTree/StructureTreeModel/AsyncTreeModel，后台直接修改现有 root，空状态被清空，隐藏时调用 `toolWindow.show()`。性能基线复跑还发现 `AllClassesSearch.findAll()` 和注解索引候选集合的类成员顺序不稳定；现目录未定义确定的类顺序，容易令重复刷新产生视觉跳动。

已读完整现行 `intellij-api-compatibility`；其中速度搜索不自动展开节点继续保留。IU261 本地公开 API 确认 SimpleNode 可覆写 `doUpdate(PresentationData)`，TreeSpeedSearch 有 `installOn(JTree, boolean, Function)`，无需 deprecated 构造器。最终选择以编译和 Verifier 为准。

## Goals / Non-Goals

**Goals:** 使用原有树和文档生成链路，分离 UI 标签与业务命名；让方法行紧凑、可通过 tooltip 与搜索查看完整签名；统一插件可见图标并收拢重复导出按钮；保持键盘导航；让刷新具有清楚终态并服从窗口生命周期。

**Non-Goals:** 不重写扫描算法、不改变重复业务名称处理、不修导出覆盖策略、不引入路径过滤或新文档存储。此项不接管预览 popup 生命周期或普通接口设置语义。

## Decisions

1. **节点保留业务身份，增加显示快照。** 在后台 read action 构建节点时计算类简单名、方法名与参数类型签名以及业务说明。通过节点 presentation 添加主文字和 `GRAYED_ATTRIBUTES` 次要片段；说明与主文字相同或为空时不重复显示。方法行的主文字只显示 Java 方法名，不显示参数括号或类型；完整 canonical 签名放入 tooltip，速度搜索快照继续包含完整签名和业务说明。参数类型用于区分 tooltip 与搜索中的重载，不合并重载节点；保留数组/varargs。用原生 Class/Interface/Method 图标。`getName()`、`docPath/httpPath` 的业务语义不变，避免显示改动改变导出结果。renderer 不再读 PSI 或构建 DocView。类更新先清理方法列表和缓存。
2. **稳定同模块类顺序。** 在 `ModuleNode` 聚合现有候选类后，按限定名排序；同名时按源文件 VirtualFile 路径，再按源码偏移量打散。排序只改变同模块类节点次序，不改变扫描范围、识别结果、重复项或每个类内部 `PsiClass.getMethods()` 提供的方法次序。排序键在 read action 中构建，检查取消；不在 renderer 中读取 PSI。
3. **速度搜索复用展示文字。** 使用公开 `TreeSpeedSearch.installOn`，`canExpand=false`，查询 presentation 的文本快照，既包含代码名也包含说明。保持键盘输入定位，不增加全文过滤框、每按键扫描或完整文档生成。
4. **复用平台源码导航。** 当前选择为有效类/方法时，面板数据快照提供相应可导航 PSI；目录菜单引用 IDE 的 Edit Source 动作，沿用用户 keymap（通常 F4）。文档仍由原有双击/Enter 打开。类/方法删除后导航不可用，不对过期 PSI 继续跳转。
5. **新安装布局与显示解耦。** descriptor 的 anchor 改为 left，隐藏重复根节点，保留模块层级及根句柄。删除刷新中的 show 调用；仅用户主动打开工具窗口才显示。IDE 自身维护 Move To 和持久布局，不添加布局配置或迁移。
6. **刷新发布完整快照。** 后台 read action 创建新的 RootNode 及子节点，在 EDT 检查 generation、项目与面板存活后替换 root 并 invalidateAsync。旧刷新完成/失败不能覆盖新刷新。任务检查取消，面板 dispose 时取消任务；模型以面板为 disposable，factory 用 Content.setDisposer 绑定。普通刷新失败保留可用旧树，并显示状态，不将半构建 root 发布到 UI。
7. **状态有可见出口。** 在内容区设置轻量状态文字并配合空树提示：索引中等待结束后刷新、正在查找、没有支持接口（提示检查扫描设置）、查找失败（提示刷新重试）、已取消。成功有内容时清除临时状态。失败内容不包含源码、凭据或内部堆栈；诊断按现有日志范围处理。已隐藏窗口更新数据与状态，不重新显示。
8. **通用动作复用 IntelliJ 图标。** 全仓盘点 `plugin.xml` 声明、Java 中动态创建的 actions、工具窗口/预览/参数编辑器 toolbar、目录右键菜单与通知。刷新、上传、导出/下载、复制、固定、预览、设置、确认、清缓存、展开/折叠等通用语义使用目标 IU 平台已有 `AllIcons`，并在 plugin.xml 与 Java 创建动作处使用相同的语义映射。类、接口、方法、模块目录节点继续使用 `AllIcons.Nodes`。将“生成 HTTP Client”映射到现有 HTTP 文件类型图标，不为通用动作保留复制来的 SVG。
   `DocViewIcons` 只保留 Doc View 产品品牌图标的用途变体：通知复用 16×16 资源（项目对通知的尺寸选择，不是官方通知专属尺寸要求）；传统工具窗口使用 13×13；New UI 工具窗口通过 `com.intellij.iconMapper` 为常规与 Compact Mode 分别提供 20×20、16×16 图标；editor gutter 分别提供 12×12 与 New UI 14×14 图标。工具窗口与 gutter 品牌标记采用主题适配的灰色单色 M↓ 图形。`META-INF/pluginIcon.svg` 是单独的 Marketplace/Plugin Manager 品牌图，使用 40×40 SVG；不可把 16×16 的 Markdown 文件图标当作唯一品牌图。除这些品牌资源外不增加新自绘动作图标。
9. **导出操作收在一个 toolbar 下拉组。** 工具窗口 toolbar 只显示一个 Export 下拉按钮；菜单中以“Export Markdown”和“Export Project Docs”区分两个已有动作。Action ID、可用状态、输出格式和执行流程保持不变。其他工具栏动作使用各自匹配的 IntelliJ 平台图标；目录右键菜单和预览/参数编辑器中的动态动作按相同图标映射更新。
10. **预览工具栏跟随 IDE 界面配色。** 左右动作 toolbar 及其承载面板使用平台的 panel background，避免 Markdown 编辑器的 editor scheme 覆盖 IDE 工具栏表面并降低浅色平台图标的对比度。Markdown 源码编辑器仍由自身 editor scheme 呈现；本次不修改 Markdown 编辑区配色或 HTML 预览内容。
11. **Gutter 图标跟随全局 editor scheme。** `LineMarkerProvider` 只接收 `PsiElement`，创建的 `LineMarkerInfo` 也不提供当前 editor 的配色上下文，因此按 `EditorColorsManager` 的全局 editor scheme 深浅选择 Doc View gutter 图标变体。该方案解决 IDE UI theme 与全局 editor scheme 明暗不一致时的对比度问题；不承诺针对单个 editor 的 scheme override 作独立选择，也不引入自定义 renderer。

### 原生验收补充：目录 Upload 可用条件

Round52 用户提供的 `respGenericTest` 方法右键截图证明菜单能停留，但 Upload 灰显。三种 `CatalogUpload*Action` 继承 `AbstractAction.update()`，其中要求 `EDITOR`、`PSI_FILE`；`DocViewWindowPanel` 的树上下文没有提供这两个键，目录上传执行本身却只依赖项目、树和选中 `DocViewNode`。这是可用条件与执行入口不一致。

只在 `AbstractCatalogUploadAction` 覆写 `update()`，按项目存在、未销毁、不在 Dumb Mode 且 `WINDOW_CATALOG_TREE` 存在判断可用；保持 BGT，更新过程中不读取 Swing 选择、不生成文档、不访问配置或网络。选中节点仍由原执行入口检查，空选择不上传。编辑器上传的原校验不变，不改上传服务或 payload。

先为三种目录平台动作增加只有项目和树、没有编辑器和 PSI 文件的 RED 测试，再修复为 GREEN；覆盖缺项目/树与 Dumb Mode，不调用真实上传。新包原生验证 Upload 展开列出 YApi、ShowDoc、YuQue，Esc 关闭且不启动上传。菜单截图与工具无法保持弹出的现象分别记录，不能混为已通过。

同继承链还影响 `WindowUploadAction`：虽在 descriptor 注册，但截图工具栏没有 Upload，执行入口本身并不依赖编辑器。其 update 按项目、root 与 toolbar 存在启用，沿用项目未销毁且非 Dumb Mode 的限制；只检查数据存在，不在 BGT 访问 Swing 状态或读取 root children。空目录/空选择沿用执行入口的现有处理，不扩大上传范围。针对真实 Window action 的 update 测试覆盖无编辑器事件和缺失必要上下文，保持编辑器上传检查原样。当前 `PreviewForm` 使用动态创建的 `AnAction`，不使用旧的 `PreviewRightUploadAction`/`MenuUploadAllAction` 注册类；本次不修改它们或实际预览上传逻辑。

## Risks / Trade-offs

- [仅方法名不足以在树行直接区分重载] → tooltip 和速度搜索保留完整 canonical 签名，重载仍是独立节点；由目标 IDE 验证 tooltip 与搜索入口可发现。
- [展示名与导出名不同] → 专门用真实 PSI 测试两种名称及路径，避免误用 presentation 作为文件名。
- [后台刷新与树模型同时读取] → 构建新快照后发布，不逐步修改当前显示 root；用请求代数和 owner disposed 检查阻止迟到结果。
- [平台图标在目标 IU 版本中缺少语义匹配] → 在 261 目标 SDK 验证选定的公开 `AllIcons` 字段；仅当确实没有合适的平台图标时保留 Doc View 品牌资源，不复制或自绘通用动作图标。
- [New UI 的 tool-window/gutter 图标需要多尺寸与映射] → 使用官方 iconMapper 文件格式登记同一品牌标识的 20/16/14 像素变体；检查基础版本与主题文件名能被打包，不把单一 16px 图标直接拉伸到所有位置。
- [真实键盘、选中态与窗口布局不能由 headless fixture 证明] → 原生 IDEA 验收覆盖搜索、源码/文档导航、深浅主题、隐藏刷新和 Move To；记录实际 build。

## Migration Plan

无插件级数据迁移。新安装默认左侧，已有布局由IDE处理；旧插件行为由用户选择旧版本。实施顺序为 RED fixture、节点展示与导航、图标引用盘点与映射、品牌图标尺寸和 plugin logo、导出下拉、刷新生命周期、descriptor、GREEN/全量/Verifier和原生验收。文件只在本地保留，不提交或发布。
