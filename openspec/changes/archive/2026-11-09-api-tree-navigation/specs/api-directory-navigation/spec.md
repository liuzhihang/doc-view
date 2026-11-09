# Spec Delta

## Purpose

定义 Doc View 接口目录的代码结构展示、业务说明、源码与文档导航、全插件图标映射以及刷新状态，使用户能够辨认重载方法、找到目标接口并在不改变文档输出的前提下控制工具窗口。

## ADDED Requirements

### Requirement: 目录上传使用目录上下文
当项目存在、未销毁、不在 Dumb Mode 且事件包含 Doc View 目录树时，目录 Upload 中的 YApi、ShowDoc、YuQue 动作 SHALL 可用，不得要求当前编辑器或 PSI 文件。更新菜单可用状态 SHALL NOT 生成文档、读取凭据或发起网络请求。目录动作仍使用选中节点的文档，空选择 SHALL 不启动上传；编辑器上传规则、平台凭据校验和 payload SHALL 保持不变。

#### Scenario: 焦点在目录且没有编辑器上下文
- **WHEN** 用户在可用项目的 Doc View 目录中选择方法或类，事件仅提供项目与目录树，没有 EDITOR 和 PSI_FILE
- **THEN** 三种平台上传动作可用且可见，Upload 子菜单可展开
- **AND** 仅展开和按 Esc 关闭菜单不会上传文档或打开平台设置

#### Scenario: 缺少目录上下文或正在索引
- **WHEN** 事件没有项目或目录树，或项目已销毁、处于 Dumb Mode
- **THEN** 目录上传动作不可用，不尝试解析源码或调用上传服务

### Requirement: 工具窗口上传使用所属视图数据
工具窗口 Upload SHALL 依据可用项目、目录 root 和 toolbar 上下文启用。可用项目指存在、未销毁且不在 Dumb Mode。此动作 SHALL NOT 要求外层编辑器或 PSI 文件，也 SHALL NOT 在更新可用状态时生成文档、检查凭据或访问网络。缺少所需上下文时动作 SHALL 不可用；执行时的空数据处理与上传范围保持不变。

#### Scenario: 所属视图数据就绪且没有编辑器上下文
- **WHEN** 工具窗口已有 root 和 toolbar 数据，但事件没有 EDITOR 和 PSI_FILE
- **THEN** 工具窗口 Upload 可见且可用
- **AND** 仅打开列表、按 Esc 退出不会启动上传

#### Scenario: 视图数据缺失
- **WHEN** 对应 root 或 toolbar 上下文缺失
- **THEN** 工具窗口上传动作不可用，不使用外层编辑器内容替代缺失数据

### Requirement: 目录同时呈现源码身份和业务说明
目录 SHALL 按模块、类、方法组织并隐藏重复根标题；类节点 SHALL 以 Java 类名为主文字，方法节点 SHALL 仅以 Java 方法名为主文字，不得在行文字中追加参数括号或类型。不同于主文字的业务标题或接口名称 SHALL 作为主题感知的次要文字；方法 SHALL 具有 IntelliJ 原生方法图标。完整 canonical 方法签名 SHALL 可从 tooltip 查看，并继续包含在速度搜索文本中，重载方法 SHALL 保持独立节点。

#### Scenario: 同名重载方法有相同业务说明
- **WHEN** 类中存在 `find(String)` 与 `find(Long)`，两者业务名称相同
- **THEN** 目录显示两个独立的 `find` 行，并带相同的次要业务说明
- **AND** 每个节点的 tooltip 与速度搜索文本都包含能区分重载的完整 canonical 签名
- **AND** 深色、浅色和选中状态下两种文字均可辨认

#### Scenario: 方法没有业务说明
- **WHEN** 接口名称仅回退到方法名
- **THEN** 目录仅显示方法名，不重复追加同一个次要文字

#### Scenario: 方法行保持紧凑
- **WHEN** 用户查看参数较多的方法
- **THEN** 主行仅显示方法名，业务说明按既有规则作为次要文字
- **AND** 参数类型通过 tooltip 和速度搜索获取，不改变重载节点数量

### Requirement: 插件动作图标遵循 IntelliJ 图标体系
插件所有可见的工具栏、右键菜单、弹出框和动态 action 的通用操作 SHALL 使用目标 IntelliJ 平台中语义对应的 `AllIcons`；目录类/接口/方法/模块继续使用平台节点图标。通用动作 SHALL NOT 使用自绘或复制的通用动作 SVG。Doc View 品牌 SHALL 使用独立、单色并适配主题的品牌图形：通知图标为 16×16，传统工具窗口图标为 13×13，New UI 工具窗口通过 `com.intellij.iconMapper` 映射 20×20 与 Compact Mode 16×16 资源，gutter marker 为 12×12 并为 New UI 提供 14×14 资源。独立的 `META-INF/pluginIcon.svg` SHALL 为 40×40 插件 logo，不得以 16×16 Markdown 文件图标替代插件 logo。工具窗口导出 SHALL 在单个 Export 下拉组中显示“Export Markdown”和“Export Project Docs”两个现有操作。

#### Scenario: 工具窗口导出入口
- **WHEN** 用户查看 Doc View 工具窗口工具栏
- **THEN** 只显示一个 Export 下拉按钮
- **AND** 下拉菜单分别显示“Export Markdown”和“Export Project Docs”
- **AND** 两项仍执行各自原有导出流程和 action 可用状态

#### Scenario: 全插件通用图标映射
- **WHEN** 用户查看预览窗口、参数编辑器、工具窗口、目录右键菜单或 plugin.xml 注册的动作
- **THEN** 刷新、上传、导出、复制、固定、预览、设置、确认、清理缓存、展开/折叠、JSON 与 HTTP Client 使用语义匹配的平台 `AllIcons`
- **AND** 节点图标与 Doc View 品牌图标保持既定例外
- **AND** 各品牌图标资源使用适合其 tool window、gutter、notification 用途的尺寸，浅色和深色主题均可辨认

#### Scenario: New UI 和 plugin logo 资源
- **WHEN** IDE 使用 New UI 或 Compact Mode，或在 Plugin Manager 展示 Doc View
- **THEN** tool-window 品牌 icon mapper 选择对应的 20×20 或 16×16 变体，gutter 使用对应尺寸资源
- **AND** Plugin Manager 使用独立 40×40 SVG logo

#### Scenario: IDE theme 与全局 editor scheme 明暗不同
- **WHEN** IDE UI theme 为深色而全局 editor scheme 背景为浅色，或二者明暗相反
- **THEN** gutter 品牌图标按全局 editor scheme 选择适配浅色或深色背景的变体
- **AND** 工具窗口品牌图标仍按 IDE UI theme 资源规则显示
- **AND** 此结果不声称支持按单个 editor 的 scheme override 分别切换图标

### Requirement: 预览动作工具栏遵循 IDE 界面主题
PreviewForm 中承载动作图标的左右工具栏及其背景面板 SHALL 使用 IntelliJ IDE 界面的主题配色，不得将 Markdown 编辑器的编辑器配色强制应用到动作工具栏。Markdown 源代码编辑器 SHALL 继续遵循当前编辑器配色方案。

#### Scenario: IDE 深色界面使用浅色编辑器方案
- **WHEN** IDE 界面主题为深色且 Markdown 编辑器配色方案为浅色
- **THEN** 预览左右动作工具栏及其背景面板仍使用深色 IDE 界面配色，平台动作图标可辨认
- **AND** Markdown 源代码编辑器继续使用浅色编辑器配色方案

#### Scenario: IDE 浅色界面使用深色编辑器方案
- **WHEN** IDE 界面主题为浅色且 Markdown 编辑器配色方案为深色
- **THEN** 预览左右动作工具栏及其背景面板仍使用浅色 IDE 界面配色，平台动作图标可辨认
- **AND** Markdown 源代码编辑器继续使用深色编辑器配色方案

### Requirement: Gutter 品牌图标遵循全局 editor scheme
Doc View gutter marker SHALL 根据全局 editor scheme 背景明暗选择适配的图标变体，且 SHALL NOT 依赖 IDE UI theme。按单个 editor 的 scheme override 选择图标不属于本 contract 范围。

#### Scenario: IDE theme 与全局 editor scheme 明暗不同
- **WHEN** IDE UI theme 为深色而全局 editor scheme 为浅色，或二者明暗相反
- **THEN** gutter 品牌图标使用适配全局 editor scheme 的变体
- **AND** 工具窗口品牌图标继续遵循 IDE UI theme
- **AND** 不暗示按单个 editor 的 scheme override 切换图标

### Requirement: 同一模块中的类顺序稳定
同一模块的类节点 SHALL 按源码限定名的字典序显示；限定名相同时 SHALL 依次按源文件 VirtualFile 路径、源码偏移量稳定排序，无 VirtualFile 时以 PSI 文件名作为路径回退。该排序 MUST NOT 改变类/方法识别规则或去重行为；单个类中的方法 SHALL 保留现有 `PsiClass.getMethods()` 顺序，声明方法仍按其源码声明顺序展示。

#### Scenario: 重复刷新后的类顺序一致
- **WHEN** 同一模块含有多个 Spring、Feign、Dubbo 或普通接口类，并多次刷新目录
- **THEN** 每次刷新均按限定名、源文件路径、源码偏移量得到相同的类节点顺序
- **AND** 同类方法顺序与 PSI 提供的既有顺序一致，刷新不重新排序方法

### Requirement: 展示变化不得改变生成内容或文件命名
目录的展示快照 SHALL 与既有文档业务命名分离，MUST NOT 因展示类名或方法签名而改变 Markdown 内容、目录导出标题、Markdown 路径或 HTTP 文件路径。

#### Scenario: 有中文业务名称的接口
- **WHEN** 用户从新的代码名目录打开或导出具有中文业务名称的接口
- **THEN** 文档内容和业务文件命名与展示改动前一致

### Requirement: 文档与源码导航均可达
方法节点双击或 Enter SHALL 继续打开接口文档；类与方法节点 SHALL 提供键盘和菜单可达的源码导航。源码无效或节点无源码时 SHALL 禁用源码导航。

#### Scenario: 从方法目录跳转源码
- **WHEN** 用户选择有效方法节点并执行转到源码
- **THEN** 编辑器打开对应 Java 方法
- **AND** 该方法节点的 Enter 操作仍打开其文档

#### Scenario: 原文件被删除
- **WHEN** 当前节点对应源码已经失效
- **THEN** 转到源码不可用且不抛出未处理异常

### Requirement: 刷新具有完整且隔离的状态
目录 SHALL 区分索引等待、正在查找、空结果、失败、取消与成功状态；只有最新请求的完整结果 SHALL 发布到当前有效视图。失败 SHALL 保留之前可用目录并提供重试提示。刷新 MUST NOT 逐次累积重复方法或关闭后回写。

#### Scenario: 索引中刷新
- **WHEN** 用户在索引不可用时刷新目录
- **THEN** 显示等待索引的状态，并在索引可用后执行查找
- **AND** 不把索引等待伪装成没有接口

#### Scenario: 没有支持的接口
- **WHEN** 当前扫描成功返回空目录
- **THEN** 显示没有支持接口的提示及检查扫描设置的指引

#### Scenario: 重复刷新且旧请求较晚完成
- **WHEN** 连续刷新同一个项目且旧请求晚于新请求结束
- **THEN** 目录仅显示新请求结果，无重复累积节点

#### Scenario: 失败或取消
- **WHEN** 当前查找失败或被用户取消
- **THEN** 临时查找状态结束，显示失败或取消提示
- **AND** 如已有可用目录则保留，用户可再次刷新

### Requirement: 默认布局服从 IDE 与用户操作
新安装 Doc View 工具窗口 SHALL 默认位于左侧并允许 IDE 原生 Move To；刷新与后台完成 MUST NOT 显示用户隐藏的窗口或夺取焦点。插件 MUST NOT 重置已保存布局或增加自身布局迁移。

#### Scenario: 首次打开工具窗口
- **WHEN** 新配置下用户首次打开 Doc View
- **THEN** 默认从左侧显示，用户能用 IDE 操作移动到右侧

#### Scenario: 隐藏窗口后刷新完成
- **WHEN** 用户在查找期间隐藏 Doc View，后台随后完成
- **THEN** 窗口保持隐藏，用户再次打开时看到最新结果
