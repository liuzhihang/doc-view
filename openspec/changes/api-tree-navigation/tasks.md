# Tasks

## 1. 节点展示与命名边界

- [x] 1.1 更新真实 PSI 的 `ApiTreeNavigationTest`，覆盖方法行仅显示方法名、tooltip/速度搜索保留完整签名、业务次文字、重载节点独立、无说明不重复，以及原有业务 getName/docPath/httpPath 不变；运行 `./gradlew test --tests '*ApiTreeNavigationTest'` 记录修改前 RED。
- [x] 1.2 更新节点展示：方法主文字只显示方法名，完整签名和已有说明进入 tooltip，搜索仍匹配签名；保留方法图标、主题次要文字及类方法列表刷新。运行同一 targeted 测试取得 GREEN；核对 renderer 不生成文档。
- [x] 1.3 在用户指南记录代码名/业务名和文档文件名的区别，核对示例与 fixture；修改 `CHANGELOG.md` 的对应条目并执行 `git diff --check`。
- [x] 1.4 为同一模块类节点增加稳定顺序：限定名升序，同名按源文件路径和源码偏移量；保留原扫描成员和每类 `PsiClass.getMethods()` 方法顺序。用真实 PSI 测试多次刷新类顺序一致且源码声明方法顺序不变。

## 2. 搜索、导航与窗口状态

- [x] 2.1 补充速度搜索文本、源码数据上下文/失效节点、刷新最新结果与失败/取消/销毁边界测试，运行 `./gradlew test --tests '*ApiTreeNavigationTest' --tests '*ApiTreeRefreshTest'` 取得目标 RED。
- [x] 2.2 实现组合展示文本搜索和原生源码动作、隐藏重复根、左侧默认 anchor，保持 Enter 文档行为；运行上述 targeted 测试确认 GREEN并检查descriptor。
- [x] 2.3 实现完整刷新快照、generation/取消/owner检查、可见状态与隐藏不重显，运行上述 targeted 测试确认最新结果胜出且 dispose 后不回写。
- [x] 2.4 同步用户指南中的搜索、源码导航、刷新状态和 Move To 操作；核对与实际入口一致，检查本地链接和 `git diff --check`。

## 3. 集成验收

- [x] 3.1 针对本次节点展示、动作图标和工具栏改动完成自动门禁：Round48 全量 202 tests 通过且 IU261 Verifier Compatible；Round49/50 分轮补做图标与 Preview 主题 targeted 测试、构建和 Verifier。各轮范围及包审计见 verification.md；Round50 targeted 不是全量 202 tests 的重跑。
- [x] 3.2.a 在新 IDEA profile 打开 Doc View Tool Window，确认首次显示在左侧。
- [x] 3.2.b 在原生树中确认类名/方法名是主文字、业务说明以灰色次文字呈现、完整签名可从 tooltip/速度搜索获取，并确认重载方法仍是不同节点（Round49）。
- [x] 3.2.c 在 Dubbo 方法节点按 Enter，确认打开对应 Scratch 文档。
- [x] 3.2.d.a 在 IU262 展开 `UserFacade` 并输入 `queryUserAccount`，确认速度搜索显示并命中 `queryUserDateAccount` 方法。
- [x] 3.2.d.b 按 Escape 后对选中方法执行 F4，确认真实打开 `UserFacade.java` 并定位单参数重载第 26 行。
- [x] 3.2.d.c 验证重复刷新/隐藏后完成、索引与空结果状态、Move To、多项目/深层导航，以及完整主题矩阵；不把一次速度搜索、源码跳转或主题切换当成这些场景已通过。目录行为已由 Round54/55 及用户手工确认分项补齐；Round56 补齐 Islands Light/Dark × Compact 开/关的目录、工具栏、弹出框及通知品牌图标，类/方法右键和 Preview Upload 四组展开与 Esc 由用户明确确认。各表面证据边界见 verification。
- [x] 3.2.d.d 在专用 doc-demo 使用原生 Move To 将目录从左侧移至右侧，验证刷新/导航仍可用，正常重开后位置保留；再移回左侧。Round54 右侧移动由用户操作、工具独立观察，刷新/源码导航和重启保留位置已通过。移回左侧最初因工具控制限制保留待验；用户随后回复“已恢复到左侧”，并在对象核对后明确确认“我已经手动测试完毕”，据此补齐该末项。最后一项的证据来源为用户手工验收确认，没有新增工具截图或隔离 profile 磁盘复核。
- [x] 3.2.d.e 在真实目录连续刷新，核对类/方法数量及顺序不累积；使用可恢复的临时源码变化确认后续刷新读到最新结果，恢复后再核对原目录。Round54 三次刷新及清理后均为同序5类/接口、23方法；额外暂停旧请求，验证新增第三个方法的新结果不会被随后恢复的旧请求覆盖。
- [x] 3.2.d.f 取得刷新尚未完成的原生证据后隐藏工具窗口，验证完成后不自行显示或夺取编辑器焦点，重新打开时目录为最新完整结果；快速完成后才隐藏不能算通过。Round54 以线程级 JDI 断点暂停真实后台请求，原生隐藏后恢复，窗口保持隐藏且编辑器焦点不变，重开后显示新增接口及两个方法。
- [x] 3.2.d.g 在 doc-demo 使用平台受控 Dumb Mode，原生点击 Refresh，确认显示索引等待而非空结果；退出后自动查找并恢复目录。Round55 原生画面、AX 和平台进入/退出日志通过；不将受控等待当作真实大型项目索引性能测试。
- [x] 3.2.d.h 在 doc-demo 构造真实无支持接口的扫描结果，确认空提示和设置指引可见、旧目录不残留；恢复源码并刷新后恢复原目录。Round55 临时移出5个候选类/接口，随后按原路径恢复，最终目录为5类/接口、23方法，34个既有文件 hash 不变。
- [x] 3.2.d.i 同时打开 doc-demo 与独立临时项目，使用相同限定名/重载签名、不同业务标记验证目录、源码跳转和文档内容隔离；验证深层包及额外模块的方法导航。Round55 A/B 的 locate(long) 和 peer-deep 的 read(long) 均定位源码9:12，Enter 文档含所属标记及 long 参数；切回 A 刷新、按完整签名搜索后仍定位 A。临时源码与生成文档已移入证据目录，普通 IDEA 已恢复。
- [x] 3.3 更新本 change verification，运行 `openspec validate api-tree-navigation --strict`、`git diff --check`，核对仅授权文件变化且未提交/发布。缺外部条件仅记录未验证，不勾选未完成验收。
- [x] 3.4 运行真实 PSI 排序 targeted 测试及目录刷新回归，确认类排序确定、类内方法顺序和目录成员语义保持不变；更新 verification 后再完成 strict validation。

## 4. 全插件图标一致性

- [x] 4.1 完成 `plugin.xml`、`DocViewIcons`、动态 action、目录右键菜单、预览/参数编辑器 toolbar、工具窗口/通知/gutter 的可见图标清单，并核对所有选定 `AllIcons` 字段存在于目标 IU 261 SDK。
- [x] 4.2 将通用动作图标统一为语义对应的 IntelliJ `AllIcons`；为 Doc View 品牌标记提供通知16px、传统工具窗口13px、New UI工具窗口20/16px、gutter12/14px主题资源并登记公开 `iconMapper`；将 `META-INF/pluginIcon.svg` 独立调整为40×40 plugin logo；删除不再引用的通用 SVG 资源。
- [x] 4.3 工具窗口 toolbar 将“Export Markdown”和“Export Project Docs”收纳为一个 Export 下拉组，保留原 action ID、可用状态和执行语义；核对右键菜单及动态 action 的映射一致。
- [x] 4.4 扩展真实 PSI 展示测试及 plugin descriptor/icon contract 测试；运行 `./gradlew test --tests '*ApiTreeNavigationTest' --tests '*PluginIcon*Test'`，记录 RED/GREEN。
- [x] 4.5 在目标 IDEA 新 profile 验证 New UI/Compact Mode、暗/浅主题下工具窗口、gutter、通知、预览、参数编辑器、目录菜单与弹出框图标和导出下拉可辨认，并检查 Plugin Manager 的40×40 logo；该项需原生验收，headless 自动测试不能代替。Round56 对 Islands Light/Dark × Compact 开/关逐格取得主体、Export/Upload、Preview、参数编辑器和品牌成功通知证据；四组右键/Preview Upload 与 Esc 由用户手工确认。Plugin Manager 浅/深色已安装列表 logo 可读；信息/警告/错误通知沿用同一最终包的 Round53 两主题验收，没有声称本轮重跑所有通知类型的四格组合或 Marketplace 详情加载验证。
- [x] 4.5.a 在深色、浅色主题分别打开 API 树类/方法节点的右键菜单，确认菜单项和平台图标可见、可读；记录实际鼠标触发结果。Round52 深色由用户手工确认并提供方法菜单截图；Round53 用户回复“核对没问题”，确认浅色 UserController 类、respGenericTest 方法菜单通过。浅色结论来自手工反馈，没有新增截图。
- [x] 4.5.b 在两种主题分别展开目录右键 Upload 子菜单和 Preview Upload 平台列表，确认 YApi、ShowDoc、语雀等实际平台选项可读；只打开列表，不发送真实文档。Round52 深色用户手工确认与两张截图通过；Round53 浅色实际 Preview 列表和 Esc 保留预览已由工具验证，用户随后确认浅色类/方法目录 Upload 子菜单及 Preview 列表均正常。
- [x] 4.5.c 从目录 Upload 子菜单和 Preview Upload 列表按 Esc，确认返回所属界面，未选择平台、未启动上传或跳转平台设置。Round52 深色及 Round53 浅色均获用户手工确认；工具另行确认工具窗口列表 Esc 返回和浅色 Preview 保留，原生控制限制与产品缺陷分别记录。
- [x] 4.5.d 在 Islands Light / Islands Dark 下验证上传成功通知的 Doc View 品牌图标，以及 INFORMATION、WARNING、ERROR 平台图标。Round53 通过本机隔离 HTTP 夹具触发真实通知，项目导出成功/冲突触发信息与警告；只连接 loopback，不代表真实平台上传验收。截图、清理与恢复见 verification。
- [x] 4.5.e 在浅色主题打开工具窗口 Export / Upload 和实际 Preview Upload 列表，确认图标及文字可读、三平台选项完整；Esc 返回所属界面且未选择上传平台。Round53 原生截图与 AX 通过，不替代目录右键菜单。
- [x] 4.6 验证 PreviewForm 左右动作工具栏和背景面板遵循 IDE 界面主题、Markdown 源码编辑器保留其 editor scheme；覆盖深色 IDE/浅色 editor scheme 与浅色 IDE/深色 editor scheme，记录新包原生结果（Round50）。
- [x] 4.7 验证 gutter 品牌图标按全局 editor scheme 而非 IDE UI theme 选择明暗变体；覆盖二者明暗不一致的组合。逐 editor scheme override 不在本任务支持范围；Round51 新包原生核对与点击入口结果见 verification。
- [x] 4.8 修复目录 Upload 错误依赖编辑器上下文：三种平台动作无 EDITOR/PSI_FILE 的回归先 RED 后 GREEN，覆盖缺项目/树及 Dumb Mode；保持更新过程无文档生成或网络调用。完成全量测试、构建和 Verifier 后记录结果，原生子菜单仍单独按 4.5.b/c 验收。
- [x] 4.9 修复同继承链的工具窗口 Upload 可用条件，以项目、root 和 toolbar 上下文补 RED/GREEN；不改变上传执行、空数据处理和 payload，实际 Preview 动态动作保持不变。与 4.8 一并完成全量、构建、Verifier，原生复验单独记录。
