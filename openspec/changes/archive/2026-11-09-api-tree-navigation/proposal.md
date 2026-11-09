# Proposal

## Why

当前目录用业务说明替代 Java 类名和方法名，重载方法难以区分；空树没有说明，刷新会重新显示用户隐藏的窗口。用户希望从源码结构找到接口文档，并能在文档与源码之间切换，因此先改善导航，再整理设置与首页。

## What Changes

- 新安装默认将 Doc View 放在左侧；用户仍通过 IDE 原生 Move To 移动，不增加插件位置设置或布局迁移。
- 隐藏重复根节点，以模块 → 类 → 方法展示。类名和方法名为主文字，业务标题/接口名用主题感知的次要文字呈现；完整方法签名显示在 tooltip 中，速度搜索仍可匹配参数类型。
- 统一插件可见图标：通用动作复用 IntelliJ `AllIcons`，目录节点继续用平台节点图标，Doc View 品牌标记保留为唯一专用品牌图标。
- 将工具窗口中的两个导出入口收在一个有明确名称的导出下拉菜单中，避免工具栏重复出现相同导出图标；导出动作本身保持不变。
- 键盘速度搜索同时匹配代码名称、完整签名和业务说明，保留不自动展开分支的现有行为，不在 renderer 或搜索过程中构建完整文档。
- 保留双击/Enter 打开文档，增加键盘可达的转到源码操作；无有效源码时动作不可用。
- 明确索引中、查找中、无接口、失败/取消状态；刷新不主动显示隐藏窗口，过期/已销毁视图不回写。
- 刷新不重复累积方法；显示名称的变化不改变现有文档内容、业务命名和导出路径。
- 修复原生验收发现的目录 Upload 灰显：目录上传动作依据项目和目录树上下文启用，不再要求当前编辑器与 PSI 文件；不改变上传目标选择、凭据校验或平台 payload。
- 同步修复相同继承检查导致的工具窗口 Upload 隐藏问题，按工具窗口自身数据启用；实际 Preview 的动态动作保持原样。

非目标：请求路径检索、新过滤器、自动跟随编辑器、导出目录重构、MCP/索引实现、重排工具栏和菜单的整体布局、除目录 Upload 可用条件修复之外的动作语义变化、解析规则修改。后续设置 change 独立处理普通接口扫描开关。

## Capabilities

### New Capabilities

- `api-directory-navigation`：源码结构与业务说明共同展示、源码导航、目录状态与窗口布局。

### Modified Capabilities

- `intellij-api-compatibility`：目录树速度搜索继续使用公开平台 API、不自动展开分支，并扩展为同时匹配主名称和业务说明。

## Impact

- `ui/window` 节点、面板与工厂，目录动作、`plugin.xml` 的默认 anchor/动作注册，必要的消息资源与用户指南。
- 复用 Java/Swing、SimpleTree、后台扫描和现有文档打开链路；不新增语言或依赖。
- 验证覆盖真实 PSI 节点标签/tooltip/重载/刷新、导出命名不变、全插件图标引用与工具栏导出分组、原生搜索/源码跳转/隐藏刷新、主题和左右移动。Plugin Verifier 检查目标平台公开 API。
- UI 依据：[JetBrains UI overview](https://plugins.jetbrains.com/docs/intellij/ui-overview.html)、[Tool Windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)。默认左侧是产品选择，不是平台强制要求。
