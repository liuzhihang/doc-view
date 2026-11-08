# 派生索引、HTTP 异常与原生资源续验（2026-11-08）

接续[HTTP/双项目/搜索记录](verification-2026-11-08-http-cache-search.md)。本轮业务文件仍只写独立整合工作区，原工作区和 Junie/Air 保留。原生输入使用获准的隔离 IDEA Integration Test 和虚构项目 Finder；第二项目启动出现了普通 IDEA 误路由事件，详见下节。发布继续以全部必需验收完成为条件。

## 产物与环境

前半轮沿用 search 候选 ZIP `50a956be5ee2534be9a2f1cf14fdc8766dc5b4b66c208f3027f2d0ef5928d94e`，JAR `d094f2b7c1d7bf7a706ea1844036595db6a64e933a9b373fbbafc1e7692273f0`。启动前再次核对安装 JAR；没有因验收脚本问题修改生产实现。该包既有自动门禁为 77 suites / 403 tests、两个精确 build 的 Verifier Compatible，未将此描述为本轮重跑。后半轮修复原生发现的既有 Dubbo 目录遗漏后，另冻结下述新候选；两个包的原生证据分别记录。

原生 IU `263.7307.200` / JBR `25.0.4.1+1-b635.79`，首次 PID 76757，未登录诊断 profile `unload-clean`。A 为 `http-cache-search/fixture-a`，B 为 `fictional-project`，两者均在 ignored `build/native-validation/2026-11-08/` 下。观察器及 UI 证据放在该目录的 `remaining-native/`，不得进入生产 ZIP。

## 复制、写回与关闭判定

英语 Islands Dark / Compact：Shift+Tab 聚焦后 Space 复制 JSON（39 字符、有效 JSON）与 Markdown（122 字符）成功，剪贴板恢复。字段描述通过 Ctrl+Tab → Tab → Space 写回 `Keyboard source acceptance`，原生 Undo + Save 恢复原文件 SHA `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91`。

首组三轮资源脚本未正确关闭参数弹窗：Find Action 的鼠标落点产生平台 `Read only` tooltip，首个 Escape 只关闭最上层提示。AX 回读显示参数窗口仍存在，三轮累积三个参数表单，因此这组数据不能作为关闭后 GC 结论。单独复现证明第二个 Escape 关闭参数窗；精确 SDK 的 popup 栈按从顶到底分发 Escape，与观察一致，无生产焦点修复依据。脚本改为识别参数动作/预览动作，Escape 后回读其窗口确实消失再采样。

修正后第二轮曾遇到 Find Action 行尚无 geometry，守卫在任何点击前拒绝；保存当时 AX 状态，增加可见 geometry/稳定路径检查后复跑。不得将测试工具的失败写成插件失败或省略。

## 修正后的资源与跨应用结果

同一 PID 完成三轮参数窗口打开/确认关闭及实际 JCEF 预览打开/确认关闭。三次关闭后的 live GC 中 `ParamDocEditorForm` 和 `ParamTreeTableView` 均为 0；FD 三次均为 518，RSS 分别为 4,179,424、4,228,432、4,239,248 KiB。预览新增 renderer 关闭后退出；启动前已存在的一个 Markdown renderer 与平台 CEF server/helper 保留，不把它们归为本次预览泄漏。此处是有限重复观测，不宣称长期内存稳定或性能提升，也不替代类加载器热卸载检查。

新包参数/预览各 Pin 开、关完成四组真实 Finder 往返。桌面截图显示重叠区域无 Doc View 遮挡，返回后参数草稿 `Finder draft retained` 保留，预览控件可用；测试草稿已取消，Pin 恢复开启。每组 popup 为 800×500，与 Finder 的实际重叠面积为 146,500 平方点。读取 Finder AXDocument/AXURL 得到空值，这一读法未能再次证明路径；既有允许的窗口标题及桌面目录内容可见，路径结论不来自这次空值读取。

## 当前包目录与热卸载

目录刷新/展开实际呈现 60 个方法，Speed Search 定位 nativeForm 后通过 Find Action → Show Context Menu → Enter，菜单显示源码、Markdown、导出、上传与 HTTP 五项；Escape 取消再 Enter，实际打开 `nativeForm_2.md`，正文读回 `/native/form`、POST 与表单信息。

同一 PID 通过原生 Plugins 中仅禁用 Doc View，`20:51:51` 开始动态卸载。`20:52:16` live GC 里 Doc View 实例行数为 0，`VM.class_hierarchy DocViewWindowPanel` 无该类，日志无本轮 still referenced 告警；随后原生恢复启用。没有通过重启/切换主题伪装此次卸载成功，Junie/Air 保持不变。平台 Markdown 编辑器另有 renderer，插件类已回收不能据此宣称所有平台 CEF 资源消失。

主题/语言第一次通过 AX 点击候选行没有改变选中值；读取后仍是 Islands Dark，未将操作意图记成浅色或中文通过，随后改用实际下拉框键盘选择并回读。

此结果证明当前包在本机精确 IDEA/JBR 的已执行目录、参数和预览路径可回收，不宣称上游 JNI 问题被全面修复，也不代替其他未执行生命周期。

## 中文浅色与窄窗

同一生产 JAR 在 PID 83013 切到简体中文、Islands Light。Compact 开启时真实 geometry 为 600×380，三个底部图标及中文列标题完整；三个 hover 的实际 tooltip 分别含“复制字段示例 JSON。”、“将当前编辑的字段复制为 Markdown 表格”、“将参数描述和必填信息写回源码。”，已分别保存 tooltip 窗口截图与 AX 文本。

中文字段描述 `键盘写回验收` 经局部 Ctrl+Tab → Tab → Space 写回成功，Undo + Save 恢复上述源文件基线哈希。关闭 Compact 后，600×380 的按钮仍完整，键盘复制 Markdown 得到 122 字符并恢复剪贴板。复制成功按既有行为关闭弹窗；后续截图脚本第一次错误期待窗口仍在，回读确认已正常关闭，再重新打开取图，没有改生产行为。

测试后参数窗口恢复 800×500、Compact 开启、主题选择恢复 Islands Dark、语言选择恢复英语并正常退出；后续启动需继续回读恢复状态。中文进程安装了临时 cache observer，但没有执行其 warm/prepare 动作；本节仅作为 UI 结果，热卸载和 GC 证据仍引用前一个无 observer 的 PID。

## 第二项目启动误路由

`21:09:47` 打开 B 的命令遗漏 `IDEA_PROPERTIES`，被普通 IDEA 的单实例入口接收，普通 IDEA 打开了同一个虚构 `fictional-project`。隔离实例仍只有 A；没有把这个状态当作双项目验证。发现后停止对普通实例的任何原生输入，通过日志只核对误开时间和项目标识，并向维护者请求仅关闭误开的虚构项目；既有 UI 授权限定隔离应用，不能据此扩大到普通 IDEA。

后续启动命令必须显式传入已核对的隔离 properties，并在原生窗口与 probe 的 project allowlist 中再次核对。维护者随后明确授权只关闭误开的虚构项目。专用原生守卫核对普通 IDEA PID、bundle、唯一窗口与精确项目路径后，仅按其窗口关闭按钮；回读目标窗口消失、普通 IDEA 进程保留。该时刻其他项目窗口数为 0，没有操作 Junie。首次守卫只检查 AX title，未识别描述属性中的路径，因此拒绝输入；补齐完整 AX label 后才关闭。`misrouted-close-result.json` 保存结果。没有回滚普通 profile 的最近项目记录，因此不声明普通 profile 字节完全不变。

## 派生索引缓存与设置项目归属

为避免普通/隔离实例并发打开同一项目，本轮把 B 的虚构源码、模块配置和文档复制为 `remaining-native/fixture-b`，没有复制 workspace 或账号设置。新的 observer 仅允许 A 与这个 B 副本。PID 93688 的两个项目窗口和精确根目录均核对；打开 B 的命令显式传入隔离 properties，CLI 在等待原生 New Window 对话时超时，但原生完成选择后两个项目确实在该隔离 PID 中打开，不能把 CLI 超时当作失败或通过的功能结论。

通过真实 `… → Clear Cache` 完成双向检查：清 A 时 A 的 1 个 `EntryData` 对象重新加载，B 的 57 个对象保持引用；再次预热后清 B，则 B 的 57 个对象重建、A 保持。非空查询和重复查询复用对应的已加载条目，快照与 `index.json` SHA 均稳定。两轮共保护 152 个文档、HTTP 和 Scratch 文件哈希。记录在 `cache-probe-clone/output/cache-observation.log` 的两条明确 `PASS`、`cache-clear-file-baseline.json` 和 `cache-clear-file-result.json`。

观察器只比较公开 `search` 返回的 `EntryData` 引用，未通过反射读取私有 `SearchProjection`。非空查询的重复结果与身份变化证明可观察缓存隔离，不宣称已取得私有投影对象身份。

设置准备为 A `mergeExport=false`、B 默认 `true`。从各自真实 `… → Settings` 进入 Template and Output → Output options，原生回读 A 未选、B 已选 Merge exports，标题分别对应本项目；两边 Cancel。`settings-routing-result.json` 及两张 output 截图保存证据。与上一份同候选的 DTO cache、已保存/未保存编辑保护合起来，HTTP/UI **5.4 完成**；旧 clear-cache 目录/工具栏入口已被新窗口菜单替代，不伪称旧入口另外通过。

当前包另实际导出 `nativeForm.md`（607 bytes，包含 `/native/form`），随后打开三平台上传子菜单并 Escape 取消，没有选择平台或上传。XML-only Dubbo `readXmlUser` 直接预览生成成功，但默认目录仍遗漏该接口；源码审计确认这是既有注解枚举边界，非本轮新增回归。按维护者“发现问题先修复再复验”要求，正在补充模块扫描契约与回归，修复后的新包须重新取得对应构建和原生证据。

## HTTP 拒绝与重试

PID 90807 仍加载同一 production JAR，并单独安装 Java 25 test-only HTTP probe（ZIP `aca5c3a3863f5ab6df3df67bbda94d2d1a309ad57b485e83413d24f3365fae6e`）。它只接受两个精确虚构项目的路径哈希，使用公开平台 API、文件命令通道，无网络端口；一次性新文件监听器和 DumbModeTask 最长 120 秒，具备释放/取消命令。启动注册使用此 build 的 Obsolete StartupActivity，仅存在于测试包，不属于生产引用。辅助构建出现过 JBR 路径、无 jar 工具及旧 listener 弃用问题，均已修正后 javac `-Xlint:all` 无警告；不是重新执行生产门禁。

A 原生目录出现四个临时负例方法。多路径 mapping、带 CR/LF 的 Header 和 JSON InputStream Body 分别显示 unsupported mapping、unsafe header、unsupported parameter 的明确通知。三次均无新增 `.http`，既有草稿文件集合/哈希保持不变；每次随后选择 validRetry，恰好生成一个完整 GET 草稿，既有 21、22、23 个文件分别保留。没有点击 Send。

第一次写入失败准备时，辅助脚本打开的是编辑器上下文菜单而非 Doc View 方法菜单，守卫未执行任何 HTTP 动作；一次性监听器在 120 秒后正常过期。脚本已加入目录焦点、准确 Speed Search 字段及菜单身份检查后重试，不能把这一轮当作写入失败已测。

第二次一次性监听器实际命中新建的 A HTTP 文件，在 production `saveText` 前令其只读。原生通知显示“draft could not be saved”；新增残缺文件已由生产逻辑清理，24 个既有文件集合/哈希与当前编辑器含测试备注的文本均保持。备注随后原生 Undo + Save 恢复；再生成恰好一个新草稿。首轮准备时备注未写到磁盘，但实际故障轮未额外采集 Document dirty flag，因此不把该证据扩大为完整未保存状态矩阵。

公开 `DumbService.queueTask` 将 A 保持在真实索引中状态。原生方法菜单的 Generate HTTP Request 为 disabled，文件集合/哈希不变；独立释放命令后回读 `isDumbMode=false`、任务 `idle`，再次生成恰好一个新草稿且旧文件保留。探针不调用生产方法代替原生点击。

生成期间的源码变更和方法删除分别完成原生测试。仅临时扩大 A 的虚构 DTO，原生点击 Generate HTTP Request 后，守卫确认仍选中精确 Controller 行，再通过键盘替换 mapping 或删除 annotation + method 两行；输入开始距生成点击分别约 234 ms、238 ms。两次均显示源码已变化的提示，等待终态后 HTTP 文件集合及哈希不变。删除后的完整 AX editor 文本确认方法已消失，未保存磁盘当时仍保留原方法。mapping 替换后的原生 Undo 恢复由下一次精确选中原行确认；方法删除后的 Undo 最初只比对磁盘，检查不足，退出后的最终校验发现删除仍被保存，已撤销该项“原生 Undo 恢复”结论。隔离 IDE 退出后从已核对 SHA 的本轮基线恢复 Controller，77 个 fixture 输入重新逐一比对。辅助 DTO 同样恢复；独立 validRetry 成功生成一个新草稿。此清理脚本问题不否定两项 stale-source 拒绝与无残留请求证据，也不冒充已查明的产品 Undo 缺陷。

源码变更测试准备阶段曾因菜单消失、AX 树路径在点击后变化而被守卫拒绝；修改辅助脚本为点击后重新读取路径，再复跑成功。这些是测试工具的准备失败，不是生产 HTTP 故障。HTTP probe 最终状态另存，待所有任务为 idle 且没有 armed listener 后才退出隔离实例。

另建 `remaining-native/no-http-client` 空白隔离 profile，只复制同 SHA 生产插件并禁用 `com.jetbrains.restClient`，没有复制账号配置或卸载 Junie。PID 96312 启动日志确认 HTTP Client marked disabled、Doc View 已加载；平台另自行禁用无许可 Ultimate，不将其误记为手工禁用项。从真实目录选 validRetry，Generate HTTP Request 为 disabled，状态栏实际显示 `HTTP Client is unavailable in this IDE installation.`，没有生成草稿，随后正常退出。同一临时 profile 仅恢复 HTTP Client 后重启为 PID 12458；目录刷新/展开后动作 enabled，实际生成一个完整 validRetry 草稿，再次正常退出。首次重试因目录未展开，菜单身份守卫拒绝执行，调整准备步骤后才通过。恢复验证对应这个临时 profile，不泛称原普通 profile 已检查。

## Dubbo 修复候选的全量与原生复验

既有目录漏掉 XML-only 与实现类注解接口，修复范围及 RED/GREEN 见 [store 记录](../project-document-store/verification.md#2026-11-08dubbo-项目目录遗漏修复)。新候选 ZIP SHA `aa5441c8b3851aa04105310c46f3cd069fee2614580fd112ca05bc0c5a85516c`，JAR SHA `2134d09f764cb5d208c5b7cffe9547fb1334d94543de14c33a03970b2dbf4cc3`。`test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-parallel` 实际执行 18 tasks；78 suites / 406 tests，0 failures/errors/skipped。单一生产 JAR 内 340 classes 均 major 69，无探针；最低 build `263.7031.23` 与实际 `263.7307.200` 均 Compatible、16 Experimental usages，无 compatibility/internal/deprecated/override-only 报告。构建仍报告 Gradle 10 兼容性弃用提示，不称全工具链零警告。冻结证据在 `remaining-native/dubbo-candidate/`。

同一 ZIP 安装到 `unload-clean`，安装哈希回读一致，PID 93962 / IU `263.7307.200`，无测试 observer。首个短生命周期 launcher 未进入 IDE startup 即退出，之后使用持续 exec session 显式传入隔离 properties 启动；没有再次路由普通 IDEA。刷新、展开目录实际出现 62 个方法，XML `readXmlUser` 与实现类注解 `annotatedCatalog` 各一条；两者原生 Open Markdown 均生成对应 `Interface#method`、Dubbo 方式及 `id` 参数，方法菜单均没有 HTTP 入口。模块归属和不可见同 FQN 冲突另由上述 light fixture 测试证明，不把单模块原生观察扩大为多模块原生性能结论。

同包键盘复制 JSON 为 39 字符有效 JSON，剪贴板恢复。描述写回后真实编辑器含新值；Undo + Save 后同时比对完整 AX editor 与磁盘字节，恢复 SHA `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91`。HTTP 表单生成一个完整新草稿，`application/x-www-form-urlencoded`、`X-Trace` 与中文编码正确，27 个既有草稿 SHA 保持；未 Send。实际 Export 生成 `nativeForm.md`，607 bytes，SHA `bf0e00b8143a94bf3e0669fab1689ae824df0f938196fd4d0cc5c6fbe47377f4`。

辅助脚本出现的准备失败分别保留：Expand 按钮名称与脚本不符；工具窗口打开后未等待布局；Select Path 无 label 输入框守卫拒绝。最后一项失败后脚本错误继续资源步骤，文本输入留在选择器内，未开始资源验收；取消错误提示、回读选择器并用角色限定的输入框重试后导出成功，资源脚本另行启动。以上失败不作为产品通过或产品缺陷。

三轮参数和实际 JCEF 预览打开/确认关闭后，live GC 的 `ParamDocEditorForm` / `ParamTreeTableView` 均为 0，FD 均为 519，RSS 分别为 4,054,784、4,063,936、4,073,456 KiB；新预览 renderer 关闭后退出，两个已打开 Markdown 页的 renderer 留存。四组参数/预览 Pin 开关 Finder 往返有 146,500 平方点实际重叠区域，逐张检查桌面截图未出现 Doc View 遮挡；参数草稿和返回后的预览控件保留。这不是长期内存稳定结论。

实际 Generate/Update 生成 62 条唯一索引，124 个 Markdown/detail 引用 hash 匹配。初次立即出现 Stale，下一次生成只有 61 条且标记未保存；完整编辑器回读查明 Find Action 辅助函数在点击结果已执行动作后多发 Enter，将 Spring 方法名拆开。修正辅助函数、原生 Undo + Save 并同时比对编辑器与磁盘后，再生成得到 62 条、Current、`hasUnsavedDocuments=false`；再次更新所有文档 hash 不变，原生提示 No content changes。XML 方法搜索命中 1/1。异常过程保留于 `store-anomaly-state.json`，修正结果为 `store-corrected-results.json`，不把辅助输入造成的 Stale 误报为插件缺陷。

另有两个实际失败，必须修复后以新包复验：

- **XML freshness**：在 IDE 中将 `fictional-dubbo.xml` 的注册接口改为不存在的类型，确认未保存编辑已生效、磁盘旧字节不变；Search 后仍显示 Current、62 条及旧 XML 搜索结果。源码确认事件监听仅接受 Java，XML 来源未递增 epoch。原生 Undo 后全文与磁盘恢复。证据为 `xml-freshness-result.json`，不是上述多余 Enter 引起的现象。
- **文档库搜索后的热卸载**：22:53:45 仅禁用 Doc View 并 Apply/OK，22:54:04 平台报告 still referenced；live GC 仍有 1 个 `DocViewWindowPanel`。这与此前只使用目录/参数/预览的通过路径不同。新 heap `unload-after-store.hprof` 为 932,602,189 bytes，仅保留 ignored 本机目录，分析限定类、字段和引用关系，不上传或输出应用字符串。没有把表单关闭归零当作类加载器释放。设置页 Marketplace 列表超过原 AX snapshot 的 800 节点上限，导致第一轮找不到 Apply；扩大测试辅助快照上限后才实际应用卸载，平台日志为判定依据。

该进程已原生正常退出；退出后确认 B 副本全部原有源码/模块配置未变，只存在本次预期的文档索引更新。仅将隔离配置中本轮添加的 Doc View disabled 条目恢复，以便下一候选启动；其他 disabled 条目与 Junie/Air 保留。

同 SDK 内的 `com.intellij.diagnostic.hprof.Analyzer -plugin com.liuzhihang.doc-view` 离线读取该 heap，9.675 秒完成，结果保存于 `hprof-classloader-report.txt`。输出的最短链从 Global JNI 经平台 `PathClassLoader`、某个 class 静态对象、CGLIB `ClassNameReader$EarlyExitException.backtrace` 到插件类加载器。它没有报告 JList，因此不能把搜索列表的可疑销毁代码当作已确认根因；面板本身的根链另查。分析器在 `java.lang.Class` 节点上的字段标签仍需与 CLASS_DUMP 元数据交叉核对，不直接将报告中的 `cachedConstructor` 当作真实静态字段名。首次两次离线运行缺少平台 JVM 所需模块开放参数而失败，补齐 `sun.nio.ch` / `jdk.internal.ref` 后才成功；这些仅属于本机诊断，生产代码不引入诊断 API。

后续独立核对显示，同一 dump 的 `DocViewWindowPanel` 类元数据存在，但官方 Histogram 实例计数为 0，完整扫描 10,568,173 个对象 ID 也未找到该类实例。报告为 `diagnostics/panel-root-path-report.txt`。这证明 heap 采集时面板已释放，不能把较早 live GC 的 1 个实例描述为持续面板泄漏，也不能据此修复或归因于 JList；CGLIB 到类加载器的保留是另一条仍未通过的路径。

XML 修复的中间候选 `5f60e67c…03bec` / JAR `60cefc1a…5e66e` 已通过 78 suites / 408 tests、18 tasks 实际执行、构建与两个精确 build 的 Verifier（同为 Compatible、16 Experimental）。复核发现未保存输入检查被改为提前返回，会漏检后续尚未提交的输入；正式生成路径有提交保护，但仍需恢复快照服务原有完整遍历防御并补测试。该中间包没有安装，也不是最终可发布候选。


## XML freshness 最终修复包原生闭环

恢复完整输入遍历并补齐后续未提交 XML 防御后，最终候选 ZIP SHA `a08ae7e28bbd94b1b14b67ea98cd5630877d092ddbe09c11abcf9d517ebd6319`、JAR SHA `1e2a65b9d96cba9cd3fe91b4673bfe87a9fa334890cd30179549e2b42b5143c1`。全量实际执行 78 suites / 409 tests，0 failures/errors/skipped，18 tasks；340 个生产 class 均 major 69，无探针。两个精确 build 的 Verifier 均 Compatible，16 Experimental，无 compatibility/internal/deprecated/override-only 问题。证据位于 `remaining-native/xml-freshness-final/`；不把未安装的 408 项中间包作为本次结果。

安装后核对 JAR hash，显式隔离配置启动 PID 3786 / IU `263.7307.200`，保留 Junie/Air、无测试 observer。先生成 62 条 Current 快照，再用原生编辑器把 XML interface 改为不存在类型，Search 显示旧 62 条为 Stale；未保存状态生成得到 61 条、`hasUnsavedDocuments=true`，磁盘 XML 原字节保持。原生 Undo + Save 后同时核对完整编辑器文本与磁盘 SHA，重新生成恢复 62 条和 `hasUnsavedDocuments=false`；再更新所有受管理文件 hash 不变。`xml-native-result.json` 的九项布尔/数量断言均满足预期。

首轮准备时 Find Action 的唯一结果行没有 AX geometry，点击守卫拒绝，未执行动作；截图确认唯一选中结果后，辅助函数调整为单候选时只发一次 Enter，无额外点击或编辑器输入，重跑通过。该准备失败保留，不算产品失败或验收通过。

## CGLIB 引用链交叉核对与修复范围

对 406 候选的原生失败 dump 读取 CLASS_DUMP 元数据，确认静态字段为 `net.sf.cglib.core.ClassNameReader.EARLY_EXIT`，对象类型为 `ClassNameReader$EarlyExitException`，而非分析器显示的 `cachedConstructor`。`hprof-cglib-static-field-and-frames.txt` 记录字段及对象 ID；异常 backtrace 中的类镜像包含平台 DOM/CGLIB 和 Doc View 的 Dubbo/gutter 类。allocation stack frame count 为 0，因此具体方法调用顺序仅由源码相关性推断，不冒充 heap 解码结果。

已先补齐 `project-document-store` proposal/design/spec/task 1.6：使用公开 XML PSI 保留识别、目录与定义导航，移除自有 DOM model 注册，避免该路径生成的静态异常回溯持有插件类加载器。不得修改 SDK 或反射清空平台字段；新实现必须保持取消、模块作用域和查询开销边界。当前 409 项包仍包含旧 DOM 路径，XML freshness 通过不等于热卸载问题已修复。


## 2026-11-09：XML PSI 替代的定向验证

`DubboXmlPsi` 用公开 `PsiSearchHelper.processElementsWithWord`、`IN_FOREIGN_LANGUAGES` 和 XML file-type scope 收集 `dubbo` 候选文件，再对 `beans` 根及直接 `dubbo:service` 子标签做精确检查。候选收集使用方法局部并发 Set，处理前按文件路径排序，不保存长期 PSI/cache。`XmlAttribute.getDisplayValue()` 读取解码后的 interface；缺少/空值安全忽略。Dubbo 识别、目录和定义导航共用此边界；删除三个 DOM model 和 `dom.fileMetaData` 注册，不新增依赖或 extension。

原生卸载 heap 是生命周期 RED。另有两个旧实现的真实行为 RED：缺少 interface 时 NPE，以及定义导航在 consumer=false 后继续返回第二条声明。修复前 JUnit/log 均保留在 `unload-api-timing/project-document-store-xml-psi/red/`。公开索引选择的探测失败单独保留：`XmlTagNamesIndex` 实际只索引 XSD element 声明；word-search 默认 context 不含 XML 标签，改用 foreign-language context 后已保存/提交未保存 XML 均命中。这些 API 选择实验不作为产品功能失败。

首轮新实现定向 18 tests 有 2 项失败：普通属性读取不解码数值字符引用；取消 fixture 的 indicator 在 runProcess 启动前被取消后又被生命周期重置。改用公开 display value，并在任务启动后取消；保留失败日志与 JUnit。再次定向回归及补充真实 XmlTag 导航目标断言后的最终复跑均通过：4 suites / 18 tests，0 failures/errors/skips，分别 5 个 XML PSI、4 个注解目录、1 个多模块目录、8 个 snapshot 测试。测试编译有 test-only 弃用提示，不称整个工具链零警告。`git diff --check` 和两个受影响 change 的 OpenSpec strict 验证通过。

最终定向日志为 `green/final-targeted-green.log`，每个 suite 的 JUnit 为 `*-final.xml`。当前生产源码已经冻结，全量与原生结果在取得后另外记录；定向通过不等于热卸载已通过。官方 [XML DOM API 说明](https://plugins.jetbrains.com/docs/intellij/xml-dom-api.html) 描述了模型缓存及运行时生成访问代码；本轮采用其公开 XML PSI 层绕开自有 DOM 路径，不修改平台 CGLIB、不反射清空静态字段，也不宣称 JetBrains 已发布对应上游修复。

## 2026-11-09：XML PSI 最终候选的构建与原生回归

冻结 ZIP SHA `67cdbe75cfed62279075bfd980efa39194e9e67c98deaeca10bbc4d62706b9c7`，生产 JAR SHA `db1e4e32e8466bd64965bd57a35830dcf7d123fb35bedd667c2709ebb5288f32`。全量实际执行 79 suites / 414 tests，0 failures/errors/skips，18 tasks；338 个生产 class 全部 major 69，未打包测试 observer 或已移除 DOM model/注册。精确最低 `263.7031.23` 和正式 `263.7307.200` 两个 Plugin Verifier 均 Compatible、16 Experimental，无 compatibility/internal/deprecated/override-only 问题。测试编译仍有 18 条提示和 Gradle 10 弃用警告，不称整个工具链零警告。证据冻结于 `remaining-native/xml-psi-candidate/`。与 409 项包逐 entry 比较，除 XML PSI 替换涉及的三处 class/注册、一个新增 class 和三个删除 model 外，其余生产 JAR 内容相同；这不是最终包全矩阵通过声明。

同 SHA 安装回读后，显式隔离 properties 启动 PID 13788 / IU `263.7307.200`，无 observer、MCP 关闭，保留 Junie/Air。已通过：

- XML-only `readXmlUser`、实现类注解 `annotatedCatalog` 各一条目录记录，正确 Dubbo Markdown、无 HTTP 菜单；从 XML 接口原生 Go to Implementation 跳到实际 `fictional-dubbo.xml` 编辑器。证据 `xml-psi-native-paths.json`。
- XML 原生未保存改名后旧 62 条 Stale；生成 61 条且 `hasUnsavedDocuments=true`、磁盘未变；Undo/Save 同时核对完整编辑器和磁盘后生成 62 条、无 unsaved；再更新文件哈希不变。证据 `xml-native-result.json`。
- 对虚构 `docs/doc-view/index.json` 临时设置 immutable 标志，原生修改一个 Javadoc 并生成，确实出现两个页面文件已发布、最终 index 发布失败。旧 index 字节保持，无遗留临时文件；旧索引拒绝打开已变更的 Markdown。清除标志后原生重试成功，62 条/124 个引用哈希全部匹配。Undo/Save 恢复原源码和原 snapshotId，文件标志归零。证据 `partial-native-result.json`。首次准备试图粘贴过长全文被 500 字符守卫拒绝，未设置标志、未开始故障测试；改为有界单行注释后才执行。
- 键盘复制 JSON 为 39 字符有效 JSON、Markdown 为 122 字符；描述写回成功，重新定位变化后的 AX editor 再 Undo/Save，编辑器全文和磁盘恢复 SHA `ab9d766cf7e040391066154d5e1068093e3ff5896b93367f262b39532db65e91`。剪贴板有效可读条目恢复；系统对原剪贴板既有 `content` UTI 有警告，不输出其内容。
- HTTP 表单生成恰好一个新草稿，28 份既有草稿不变；Content-Type、X-Trace 和中文表单编码正确。当前 clean profile 的 Ultimate 未激活，未执行 Send；不能替代最终包回环发送。Export 原生输出 `nativeForm.md` 607 bytes，SHA `bf0e00b8143a94bf3e0669fab1689ae824df0f938196fd4d0cc5c6fbe47377f4`。

资源验收的准备脚本曾未等待 Go to File 选择完成，在 PaginationController 插入换行；发现后原生 Undo/Save 恢复全文件基线 SHA `97168b94be9a9cac167f468885d8f8d0b675a6ebc85b6e28ca955688eb6d3a17`。辅助脚本改为确认搜索 tab、查询值、唯一文件结果与聚焦编辑器后继续；Find Action 的同名 Settings 行另经截图区分。上述准备失败不计为产品失败或通过，资源循环另起记录。`resource-preparation-restored.json` 保存恢复证据。

## XML PSI 候选的资源、跨应用和热卸载结果

同一 PID 13788 三轮参数/实际 JCEF 预览打开关闭后，live GC 的 `ParamDocEditorForm` / `ParamTreeTableView` 均为 0，FD 均为 520，RSS 为 3,808,480 / 3,818,528 / 3,825,408 KiB。新预览 renderer 关闭退出；三个已打开 Markdown 页的 renderer 仍在，不能声称所有 CEF 子进程退出。参数/预览 Pin 开关四组真实 Finder 往返均有 146,500 平方点交叠区域，四张截图逐一核对没有遮挡；返回后参数草稿/预览控件保留，Pin 恢复开启，草稿取消。证据 `resource1-gc.json` 至 `resource3-gc.json`、`crossapp-visual-result.json`。这是有限三轮资源验证。

00:30:47 原生 Plugins 页仅禁用 Doc View 并 Apply/OK，00:31:06 平台报告仍被引用；live GC 中表单/面板实例已为 0，但还有 66 条插件类对象直方图记录、目录面板类元数据仍存在，**完整热卸载失败**。最初辅助日志过滤只保留含 plugin ID 的行，漏掉上一行 warning header；已补读相邻两行并更正 `stillReferencedWarning=true`，不得据最初 false 声称没有告警。

新 `unload-xml-psi.hprof` 为 929,558,181 bytes，保留本机 ignored 目录、不上传。目标 SDK 官方 Analyzer 10.10 秒完成，最短根为 `Local JNI → NodeRenderer.myTree → SimpleTree.uiTreeExpansionListener → DefaultTreeUI$MyTreeExpansionListener.myOriginal → BasicTreeUI$Handler.pressedPath → CachingTreePath → StructureTreeModel$Node.userObject → ClassNode → PluginClassLoader`。该报告不支持宣称 XML PSI 包已完全卸载，也不支持宣称 CGLIB/JNI 上游缺陷已修复；它定位了另一条可以继续处理的 Java 引用边。

已先补基线 change 的 design/spec/task 6.3：永久销毁树时用公开 Swing 生命周期解除 UI delegate 与展开监听器对旧路径的引用，先 RED/GREEN 再新包全量及原生卸载。不得使用反射清私有字段或关闭无障碍。此问题修复前，store 1.6、基线 R2/4.2 与整体发布门槛均保持开放。

## 树 UI 生命周期修复的 RED/GREEN

核对精确 IU `263.7031.23` 和 `263.7307.200` 的平台字节码，两者 `Tree.setUI(TreeUI)` 都先清空自身 UI 展开监听器字段，再调用 JDK `JTree.setUI`。生产只在永久销毁目录树的 EDT 清理中增加 `catalogTree.setUI(null)`，结束 UI delegate 生命周期；不读写私有字段，不关闭辅助功能，不改变在用目录行为。[JDK 25 JTree 文档](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/swing/JTree.html#setUI(javax.swing.plaf.TreeUI)) 提供此公开 setter。

现有 `ApiTreeRefreshTest` 增加公开 `getTreeExpansionListeners()` 的前后 identity 断言：旧实现 7 tests / 1 failure，dispose 后仍保留 1 个原监听器；修复后同 suite 7 tests、0 failures/errors/skips，14 tasks 实际执行、BUILD SUCCESSFUL。测试同时验证旧 UI、模型、parent、鼠标监听器与 selection 生命周期，不通过反射读取 pressedPath。证据为 `remaining-native/tree-ui-red/` 与 `tree-ui-green/`。该 GREEN 只证明自动行为，后续新包原生卸载结果另记。

## 树 UI 修复包的完整自动门禁

新 ZIP SHA `03f483361591f7bbd2a7f139ef457f27fdc76c0eb15419e1d2427dcbd504205b`，JAR SHA `20fdd64ad3ac775655edb633f198c04d57ae001abf8d8112005b93e9ed62a74b`。79 suites / 414 tests，0 failures/errors/skips，18 tasks 实际执行，构建耗时 1m53s；338 个 class 均 major 69。两精确 build Verifier 均 Compatible、16 Experimental，无 compatibility/internal/deprecated/override-only 问题。原 18 条测试编译提示和 Gradle 10 弃用警告保留。

`remaining-native/tree-ui-candidate/production-jar-diff-from-414.json` 的逐 entry 比较显示，仅 `DocViewWindowPanel.class` 变化，无新增/删除 entry；未包含 observer，已移除 DOM models/注册未回归。完整证据、源码 manifest 与安装 hash 回读冻结在该目录；原生新包结果另记，不能仅据该范围比较宣称剩余验收自动通过。

安装哈希回读后，使用显式隔离 properties 启动 PID 19963 / IU 263.7307.200，无 observer，原工作区和 Junie 保留。新包 XML-only/注解实现类目录与 Markdown、XML 定义导航，以及 XML freshness 的 62→61 未保存→Undo/Save→62 和幂等更新闭环均通过；证据在 `tree-ui-candidate/xml-psi-native-paths.json`、`xml-native-result.json`。首次 paths 脚本漏传输出目录，在任何测试操作前退出；随后目录 AX path 布局变化被守卫拒绝，改为重新读树并确认焦点后复验通过，没有把准备失败计为功能缺陷。

## 树 UI 修复包原生热卸载通过

PID 19963 同包完成目录/两类 Dubbo/XML 导航、文档库搜索与 XML freshness，再三轮参数/实际 JCEF 预览打开关闭。三轮 live GC 的参数表单/表格均为 0；新增预览 renderer 关闭退出，两个已打开 Markdown 页的 renderer 保留。FD 为 513 / 513 / 518，RSS 为 3,933,728 / 3,941,280 / 3,981,360 KiB；这是有限样本，不称 FD 恒定或长期内存稳定。

00:51:03 原生仅禁用 Doc View 并 Apply/OK；00:51:33 live GC 中 Doc View histogram rows=0，`VM.class_hierarchy` 不再含 `DocViewWindowPanel`，该轮日志未出现插件 still-referenced 告警。**此包此条使用顺序的热卸载通过**，证据为 `tree-ui-candidate/unload-result.json`、`unloaded-gc.json`、`unloaded-class-hierarchy.txt` 和 `unload-log.txt`。平台仍有 PSI invalidation 警告，不写成整份 IDEA 日志零警告；卸载过程打开的 Plugins 页使 FD 增至 552，不与 popup 关闭样本混为同一资源基线。

这个结果证明公开树 UI 生命周期清理在当前精确 build 的上述复现路径中解除类加载器保留；不宣称修复了 macOS/JBR 的 JNI 实现，也不自动关闭 HTTP L01–L03、最终包 Send、Schema/预览/凭据等独立原生门槛。四组 Finder 遮挡沿用前一 414 包的实测；逐 entry 比较确认弹窗实现未变化，没有伪造在当前包重跑四组的记录。

## 同包 GET / JSON / Form 原生回环发送

同一 tree-ui-candidate JAR 安装到已激活的隔离 `profile`，PID 21400 / IU `263.7307.200`。只替换该 profile 的 Doc View 插件；未读取或复制许可/凭据，保留原工作区和 Junie。使用仅绑定 `127.0.0.1` 的虚构接收端，分别生成并在 IDEA HTTP Client 主动执行 GET、JSON POST、Form POST。三次原生响应均为 200，接收端恰好收到三次请求：GET 路径 `/native/users/42`、查询 `q=a&b 空格`；JSON 查询 `dryRun=true`、`application/json` 和 `{"displayName":"","age":0}`；Form 无查询、`application/x-www-form-urlencoded`、`name=Alice & Bob 空格`。三者 X-Trace 分别为 native-get/native-json/native-form。

生成与发送分阶段核对接收计数，生成动作没有自动发送；新文件创建不覆盖已有草稿，实际发送并保存响应记录后，生成前既有 5 / 7 / 8 份 `.http` 文件哈希分别保持。仅对新草稿把 host 替换为本机字面地址。JSON 粘贴时 IDEA 自动将缩进改为两空格并整理末尾换行，首次严格字节断言拒绝继续；保留该未发送草稿，再生成独立新稿，核对请求头逐字及 JSON 值相同后发送，不称 JSON 正文逐字未变。目录与 HTTP 响应窗口出现同名 AX tree 时，辅助脚本先安全拒绝，随后限定到 Doc View 工具栏所在区域；这两次准备问题不记为产品缺陷。

证据为 `tree-ui-candidate/http-loopback/summary.json`、三个 `*-result.json`、原生响应快照和本机接收日志；测试服务已结束。该结果补齐此冻结包的 A01–A03 Send，不代表后续编辑器修改后的新包已复跑全矩阵。

## 同包 L01 类、模块及无文件编辑器场景

PID 21400 原生选中 HttpAcceptanceController 类节点、fixture 模块节点，菜单均没有 HTTP 动作；打开/关闭菜单前后，选择、目录行、编辑器集合与 9 份请求文件哈希一致。关闭当前虚构项目全部文件标签后，模块菜单仍隐藏 HTTP。此时平台 AX 的部分树行名称为空，辅助脚本的名称比较安全失败；改用菜单读回、文件/编辑器/行数和限定 Doc View 区域截图交叉检查，前后目录截图字节完全相同，人工核对 fixture 模块保持选中、展开状态未变。

随后明确展开目录并选中 nativeForm；没有文件编辑器时，HTTP 菜单仍 enabled，未执行生成，9 份文件不变。该行为符合 catalog action 使用所选有效方法的 contract；“没有编辑器”本身不是禁用条件。前一次目录类节点处于折叠状态，speed-search 未到方法，脚本拒绝错误菜单；展开后复验，不把准备定位失败写成产品失败。证据 `l01-class-module-result.json`、`l01-no-editor-module-result.json`、`l01-no-editor-method-result.json`。空目录、L02 和 L03 仍待验。

测试服务结束后正常退出 PID 21400；原工作区 HEAD 与 584 个冻结文件、原 fictional-project 15 个文件和 Schema 准备态 4 个文件均逐项哈希一致。证据 `main-profile-preservation-after-exit.json`；未操作普通 IDEA 或 Junie。

## 新发现：Schema 描述输入丢失实际换行

同 PID 的独立 `schema-current-project` 原生描述单元格粘贴真实 LF，提交后的单元格把 LF 变成空格；点击 Apply Source 并 Save 后，解码 Java 注解字符串仍是空格。初始聚焦控件的 AX value 不可读，不据该空值判定失败；提交后的单元格与实际写回源码才是 RED。一次原生 Undo/Save 已同时核对编辑器全文和磁盘恢复。证据 `tree-ui-candidate/schema-multiline-input-result.json`；OpenAPI change 已补 contract/tasks，修复及新包原生回归仍在进行，不能发布。 多行编辑的 8 项定向测试已通过；首轮全量 418 项有 1 项旧复制测试仍假定 JTextField。更新复制回归时又确认描述 LF 会拆散共享 Markdown 表格，已先补 OpenAPI 4.3 的普通/分表输出 contract，修复冻结后重新执行完整门禁。


## 多行编辑候选：419 项门禁与实际原生结果

冻结候选 `remaining-native/multiline-candidate/`：ZIP SHA `2f067769a174b8b2b6bda04f1b903bd04c206ded619c9abe133fe6382ff8b6cf`、JAR SHA `946feb0351a56f0f05ef61be4c29dfdb383f2f34c3be4fe850c11a36c6ca69a3`。79 suites / 419 tests，0 failures/errors/skips，18 tasks 实际执行；342 个生产 class 全部 major 69。最低 `263.7031.23` 和正式 `263.7307.200` 两个 Verifier 均 Compatible、16 Experimental，无 compatibility/Internal/deprecated/override-only findings；测试编译及 Gradle 弃用提示仍保留。324 个构建/测试输入在门禁前后哈希一致。未打包 observer。

显式隔离配置启动 PID 32358 / IU `263.7307.200`，同 JAR 安装回读后，实际描述编辑器已保留粘贴/Enter 输入的连续及结尾 LF、引号、反斜线；Mac Command+Enter 提交，Tab/Shift+Tab 与 Ctrl+Tab/Ctrl+Shift+Tab 提交及焦点离开通过。原生 Copy Markdown 的描述换行变为单元格内 `<br>`，实际 IDEA Markdown Preview 显示正确两行数据表、连续空行及独立 age 行，源码保持不变。旧 Schema 缺少 requiredMode 的原生 Apply 明确拒绝，保留两个字段草稿，编辑器/磁盘均未改变，不产生普通 Javadoc 的部分写入。详细记录见 OpenAPI change verification。

发现两项真实原生失败并保持 RED：编辑中 Esc 直接关闭整个 popup；两个 Schema 字段写回后，一次 Undo 仅恢复后一个字段，第二次才恢复全部。Mac Ctrl+Enter 被 IDEA New 动作消费，已明确本平台提交键是 Command+Enter，不再宣称两种组合都通过。失败证据分别为 `schema-escape-result.json`、`schema-undo-red.json` 和 `schema-control-enter-conflict.json`。修复及新候选复验另记，不把此 419 包视为发布就绪。

## 多行编辑候选：HTTP 空目录与双项目重载

L01 空目录：同 PID 的独立旧 Schema 项目无任何接口行，菜单没有 HTTP 入口，未创建请求或打开编辑器，空目录状态不变；证据 `l01-empty-result.json`。类、模块、无文件编辑器场景仍绑定前述 tree-ui 候选，未伪造本包重跑。

L02：同时打开 `fixture-http-a` / `fixture-http-b`，均声明 `poc.HttpIsolationController`、同名 `lookup` 和相同文档标题。逐项原生选中 `lookup(java.lang.String)`、`lookup(java.lang.Long)` 共四次，生成结果分别为 A 的 `/native-a/lookup-string?value=A-string`、`/native-a/lookup-long?value=101` 和 B 的 `/native-b/lookup-string?value=B-string`、`/native-b/lookup-long?value=202`。每次核对两个项目窗口仍打开、新文件恰好一份、项目标识/完整签名/URL 正确，前台编辑器全文与文件相同。两个项目使用不同目录，四份草稿路径互异，既有 29 份请求哈希与 18 个 fixture 输入全部保持；未触发 Send。证据 `l02-overload-result.json`。L03 在途重复点击及旧未保存草稿仍待验。


## 421 项候选：新增原生通过与卸载失败

ZIP `cbc2c7142094ec1905fc8443a435b23026eb34052a1b4a1418228f5fc39a9a4d`、JAR `4b587f4884f45653eca10e6817aec3ae063c3dd7de684cc134aab788790dbe3f`：79 suites / 421 tests，0 failures/errors/skips，18 tasks 实际执行，1m55s；342 个 class 全部 major 69。两个精确 build Verifier 均 Compatible、16 Experimental，compatibility/Internal/deprecated/override-only findings 为 0。324 个构建/测试输入哈希前后一致；原测试编译和 Gradle 警告保留。冻结证据 `remaining-native/batch-escape-candidate/`。

同 SHA 原生 PID 46071 / IU263.7307.200：已通过首次 Esc 仅取消 cell/第二次关窗、真实多行输入、requiredMode 双向及成员保留、正在编辑时 Apply、一次 Undo/Redo 整批恢复、键盘导航/复制，以及缺少 requiredMode 的整批拒绝/草稿保留。参数/预览 Pin 开关四次真实 Finder 往返各有 146,500 平方点交叠，四图逐张确认无跨应用遮挡，返回后草稿/控件保持。具体 Schema 证据见该 change verification。

整类 Spring 三方法和 Dubbo 三方法各自然重复三次，首轮逐项核对正文；空预览与无效模板的源码/JCEF 状态、Copy 不复制状态页、模板恢复后成功生成均取得证据。三次目录刷新终态观察为 1.612/1.576/1.649 秒，每次展开核对全部 65 方法；均为原生事件到 AX 读回的上界，非纯 PSI 或 SLA。首轮脚本错误要求刷新保持展开，看到 9 个折叠类/模块行后安全失败；按现有刷新 contract 重测终态及展开完整性，未改生产以迎合测试。详见 preview change verification。

三轮关闭后的参数表单/表格为 0，FD 都是 515，RSS 为 3,899,040 / 3,908,464 / 3,914,656 KiB，关闭样本无 renderer 子进程；PreviewForm 仍有瞬时保留，另采 heap 诊断。02:38:34 原生仅禁用 Doc View；02:39:10 live GC 留有 51 条插件对象记录，目标类仍存在，日志有明确 still-referenced warning，因此本包此次使用顺序的热卸载失败。`unload-result.json`、`unload-log.txt` 和本地 `unload-after-preview-errors.hprof` 保留，heap 不上传。不能据表单实例归零宣称类加载器已释放，也不据旧包通过关闭此次失败。

测试模板已原生 Apply 恢复并重开全文回读一致，隔离 PID46071 已正常退出；Junie 未操作。正在定位引用根，尚未对未确定原因增加运行时代码。HTTP L03、取消/迟到回调、凭据与最终新包矩阵仍待验，未提交、打 tag 或发布。

## 状态边界

本记录持续补充实测结果。429 候选已补 JCEF 状态、预览取消/迟到回调、Copy/单项 Export、三轮关闭后 renderer 退出及 selection 日志证据；同候选热卸载仍失败，当前 heap 根链指向 UniversalFileChooser 工具栏创建回溯持有 Doc View 类。取消提示资源变更的 R2 包已完成 429 项自动测试、构建与两个精确 build 的 Verifier，原生新文案复验仍待完成。不得把分包证据合并为最终包全矩阵通过；没有升级公开版本、提交、推送、打 tag、创建 GitHub Release 或发布 Marketplace。正式 MCP 与 Skill 仍是独立待实施功能，本轮不声称已交付。


## 421 候选受控在途取消与 L03 补验（2026-11-09）

原生进程 PID52395，IU263.7307.200，仍安装冻结的 421-test JAR `4b587f4884f45653eca10e6817aec3ae063c3dd7de684cc134aab788790dbe3f`。隔离启动显式指定 `unload-clean/idea.properties`，额外 JDWP 仅绑定 `127.0.0.1:58337`，端口所有者已核对；没有安装诊断插件、修改生产包或普通 IDEA。公开 JDI 仅在目标工作线程设一次断点，不读取局部变量、不调用生产方法；原生菜单/键盘仍由隔离 AX 操作触发。以下是**受控交错的功能验收，不是自然延迟或性能数据**。证据位于 `build/native-validation/2026-11-08/remaining-native/batch-escape-candidate/`。

- **目录取消/恢复通过**：在 `DocViewWindowPanel$8.run:761` 注册 indicator 后、构建快照前暂停；通过平台 Shift+Cmd+F2 选择 `Searching Doc View interfaces`，实际进程显示 Stopping，平台日志记录 UI cancellation；恢复后显示 Refresh cancelled。旧目录的 `cold01`–`cold07`、`hot`、`cancelStress` 九个方法仍完整可见，迟到观察仍维持取消状态，再次 Refresh 成功。AX 返回部分方法行空 label，初始自动断言未通过；两张仅裁取 Doc View 的原生截图逐张核对证明成员没有丢失，不能将 AX 文字缺失写成目录折叠或数据丢失。见 `controlled-catalog-2-result.json`、`controlled-catalog-2-cancelled-panel.png`、`controlled-catalog-2-retry-panel.png`。
- **预览加载取消通过**：在 `PreviewForm$10.run:757` 暂停，通过同一原生停止入口取消 `Doc View preview`，恢复后源码视图与真实 HTML 均显示 `Loading Doc View was cancelled.`；Copy 保留剪贴板 sentinel，helper 恢复用户剪贴板，Esc 正常关闭。见 `controlled-preview-cancel-result.json` 和逐张查看的 `controlled-preview-cancelled-html.png`。它不代替快速选择的迟到渲染结果互操作。
- **L03 受控在途重复调用/未保存草稿通过**：先原生编辑既有 cancelStress 草稿为 88 字符虚构内容，读回编辑器与磁盘不同；在 `HttpRequestService$1.run:115` 暂停首项生成后，再次从原生目录菜单触发同一方法，原任务仍在途且尚无新文件。恢复后只新增一个 7,929,881-byte 完整请求文件，新草稿获得焦点，33 个旧文件的完整哈希均不变。返回旧草稿时未保存内容完整保留；原生 Undo/Save 后恢复原 7,929,881-byte 文件全量哈希。见 `l03-unsaved-before.json`、`l03-second-inflight.json`、`l03-generated-result.json`、`l03-restoration.json`。这里证明在途合并与草稿保护，不把暂停期间耗时写成自然压力性能。

诊断限制/失败保留：第一轮目录断点期间，未带省略号的 Action 查询未匹配，后续 AX/Thread.print 等待；恢复并 detach 后 UI 恢复，平台记录一次约94秒 UI freeze。本轮把它归为受控诊断造成的待解释现象，不作为产品性能测试，不标取消通过；第二轮使用已观察到的直接平台快捷键才取得上述有效取消证据。所有调试连接均在各轮恢复目标线程后退出。

另有**新的生产缺陷**：先前整类预览切换方法虽然可见结果正确，但日志在 `PreviewForm.lambda$applyDocViewList$1:812` 报告无 read action 的 EDT PSI 访问。该路径目前只有视觉通过，完整验收失败，正在按 RED/GREEN 修复。旧421包新发现的 Tool Window/JNI 热卸载失败也仍开放。不得依据本节通过项发布、升级版本或宣称全矩阵完成。

### 421 HTML 状态页日志缺陷补充

上述空内容与取消场景的文字/截图可见，但**HTML 完整验收未通过**：在 02:28:41（空内容）和 02:58 左右（取消状态后），Markdown JCEF 报 `First body child is expected to be the root of the document!`。冻结 SDK 的 ScrollSync 需要标准 Markdown 根节点源位置信息，Doc View 手写 `<body><p>` 状态页没有这些信息。不能把可见截图或无 Doc View Java 栈当作无错误；正在补状态页 contract 与 RED/GREEN，重建后复验真实 JCEF 和新日志。当前实际使用的公开 facade 是 `org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil.generateMarkdownHtml`；邻近 `md5` 方法的 Internal 注解不能误归给该 renderer。本项未修复/复验前不发布。

受控调试进程 PID52395 已通过原生 Confirm Exit 正常退出；各轮 JDI 已恢复并断开，退出后仅删除哈希匹配的 L03 新测试草稿，33 个原有草稿哈希保留，旧编辑器测试内容已通过原生 Undo/Save 恢复。详见 `debug-exit-cleanup.json`。


## 2026-11-09：项目关闭与预览修复候选（428 项门禁）

本轮冻结包 `remaining-native/project-close-candidate` 的 ZIP SHA-256 为 `d377299230eb6ac6ff773f25283664839cd54b4b17c4622ddcee62cb9230efc8`，JAR SHA-256 为 `a7bcdf905c920a48900fba0680d1b46699e8d8513963b05ddda94e5ef4892b53`。80 suites / 428 tests 全部实际执行，0 failures/errors/skips；344 个生产 class 均为 major 69。两精确 build `263.7031.23`、`263.7307.200` 的 Verifier 均 Compatible、16 Experimental，compatibility/Internal/deprecated/override-only findings 为 0。18 条既有测试编译提示及 Gradle 10 弃用警告仍保留。325 个构建/测试输入前后哈希相同。

与421候选的逐 JAR entry 比较仅涉及 `PreviewForm` 及其内部类、`DocViewToolWindowFactory` 及新生命周期 listener；新增 `PreviewListSnapshot`，没有删除其他 entry。修复包括后台捕获类名避免选择监听器裸读 PSI、状态 HTML 使用公开 Markdown renderer 并拒绝过期回调，以及原生项目关闭时移除 Doc View 工具窗口注册项。定向预览测试17/17、项目关闭测试2/2的 RED/GREEN 证据分别在 `build/native-validation/2026-11-09/preview-status-html/` 与 `project-close-lifecycle/`。

安装 JAR 哈希读回一致后，以显式 `unload-clean/idea.properties` 启动隔离 IU `263.7307.200`，PID66650，无 observer。原生回归正在进行；不把421及更早包的通过项改记为本包已通过。版本仍是仅用于整合验证的1.5.0，未升级、打tag或发布。


### 428 候选原生项目关闭、预览和热卸载

PID66650 同包完成 `fixture-close-unopened` 从未展开 Doc View 工具窗口的原生 Close Project；已使用的 `fixture-b` 与 `schema-current-project` 同时打开后，关闭前者不影响后者目录刷新。原生 Confirm Exit 的 Cancel 保留项目和目录，此项是退出确认取消，不冒充任意 BGT projectClosing 或注入 veto。A 再次打开后生成65条文档；关闭/重开后显示 Freshness is unknown，133个文档文件哈希保持，未自动扫描源码或改写文档。证据 `unopened-close.json`、`a-close-b-kept-result.json`、`exit-cancel-result.json`、`freshness-generated-result.json` 和 `freshness-reopened-result.json`。

Spring/Dubbo 类预览各三轮成功、首轮逐项选择全部正文，原生观察上界分别约1.49–1.51秒与1.46–1.48秒（包括事件/AX观察，不作纯解析时间/SLA）；整段日志无对应 ThreadingAssertions。参数与预览两类弹窗的 Pin on/off 四组实际 Finder切换均无跨应用遮挡，返回后参数草稿/预览控件保留，四张Finder截图分别人工查看。初次 helper 在仅含DTO的schema项目查找 SpringController，定位守卫退出；切换正确 fixture-b 后完成预览两组，不计产品失败。

空状态源码与实际 JCEF 页面显示 No Doc View content，Copy哨兵保持；旧JCEF根节点结构错误未再出现，但正常取消的 AsyncPromise sentinel 被误写成 WARN，所以这项只记部分通过。源码已用公开 promise.isCancelled() 修正，18项定向测试通过；新修正版原生结果另记。

三轮参数/预览关闭和live GC（JCEF面板均创建，原生快照确认第1/3轮显示HTML，第2轮显示源码；不把切换模式说成三轮都可见HTML）：ParamDocEditorForm/ParamTreeTableView均0、PreviewForm均1，FD515/515/515，RSS4363056/4362416/4371664KiB；新增预览renderer关闭退出。当前仍打开两个虚构项目，DocViewWindowPanel均2。不把有限采样说成长期内存稳定，也不把关闭后PreviewForm仍存活说成完全回收。

03:52:54 原生只禁用 Doc View 并Apply/OK，03:53:30 live GC中Doc View histogram rows=0，目标DocViewWindowPanel类已不存在，日志无still-referenced告警。**428在上述未展开/已使用项目反复关闭重开、双项目和多轮预览顺序中的热卸载通过。** 证据 `unload-result.json`、`unloaded-gc.json`、`unloaded-class-hierarchy.txt`、`resource-cycle-summary.json`。这说明公开工具窗口清理解除本轮闭项目JNI根链对插件的保留，不宣称修复macOS/JBR全部JNI问题。平台仍记录beforePluginUnload/pluginUnloaded的PSI invalidation警告；打开Plugins页后FD539不与popup基线混算。

## 2026-11-09：429 候选 JCEF 预览、导出与热卸载失败

### 产物与原生状态

隔离正式 IU `263.7307.200` / JBR `25.0.4.1+1-b635.79`，PID 71085，安装 `status-cancel-candidate`。ZIP SHA-256 `6c789b81e371656db0cdbfca005c82a4175541bb98ae04106500feece46f8370`，JAR SHA-256 `9828f7d3f864f60fd28521e25c0a36782e24cfbb56a13aa2c5e3157110b33aa2`；`artifact.json` 记录 80 suites / 429 tests、0 failures/errors/skips、344 个 class 全为 major 69。IDE build、安装读回与 artifact 分别见 `status-cancel-candidate/native-install.json`、启动 `unload-clean/log/idea.log` 和该候选 `artifact.json`。

真实 JCEF 记录覆盖 Loading、Rendering、空状态、模板错误/恢复、预览取消、旧 render 迟到与 popup 关闭中的在途 render。空预览 HTML 显示 `No Doc View content.`，无 `First body child`、Markdown preview update 或意外 status warning；故意使用模板 `#if(` 显示失败状态、Copy 哨兵保留，随后原生恢复并精确读回 459 字符模板（SHA `472710c8f7c689789c64368c536cbe57d9b40aca83051d6ad7353dd79297879c`），再次生成 HTML 成功。受控预览取消两次均由 IDEA 原生 Stop 执行，源码/HTML 取消终态正确、Copy 哨兵不变；辅助线程只暂停后台 worker，不调用产品取消 API，不作延迟/性能声称。旧 render 停在 `PreviewForm` 行 879，在新的 JSON 源码与 HTML显示后恢复，旧结果没有覆盖；另一次在 render indicator 注册后暂停、先按原生 Escape 关闭 popup、再恢复任务，没有重建 popup。所有 JDI 断点仅控制顺序。

单项 Copy 读回当前 JSON（624 字符），旧 GET 未复制且用户剪贴板恢复；单项 Export 导出 778 bytes，hash 与该当前 JSON 一致，旧 GET 未导出。当前结果未覆盖批量 Export。`status-and-selection-log-check.json` 对 preview-status warning、ThreadingAssertions、JCEF 根节点、Markdown preview update 与 EDT read-access 五类模式均为 0 命中；故意模板异常属于预期日志。相关汇总与结果分别见 `empty-result.json`、`controlled-preview-cancel-result.json`、`late-render-result.json`、`close-render-result.json`、`preview-copy-result.json`、`preview-export-result.json`、`template-failure-recovery-result.json` 与 `status-and-selection-log-check.json`。

### 资源与热卸载

主线逐张核对三张 `resource1-html.png` 至 `resource3-html.png`。三轮关闭后 `resource-summary.json` 记录 FD 均为 512；`ParamDocEditorForm` / `ParamTreeTableView` 为 0，`PreviewForm` / `DocViewWindowPanel` 各为 1；每轮实际 HTML 打开时观察到的新 CEF renderer 子进程在关闭后退出。有限三轮和稳定 FD 不是长期资源基线，也不能证明插件 classloader 已释放。

PID 71085 禁用 Doc View 后仍有 **56** 条 Doc View 类直方图记录，目标类未消失，IDEA 报 plugin still referenced，见 `unload-result.json` 与 `unload-log.txt`。离线 heap 分析 `diagnostics/classloader-roots.txt` / `toolbar-trace-audit.txt` 定位到 `Local JNI → NioFileSystemTree$FileRenderer → Tree → UniversalFileChooser$Panel$FileView → ActionToolbarImpl.myCreationTrace → Throwable.backtrace → ExportUtils / PreviewForm$6 → PluginClassLoader`。同时 `preview-form-roots-no-class-roots.txt` 的目标 `PreviewForm` 实例数为 0；该事实不能抵销 56 行或 unload warning。

PID 78877 同包无调试器对照通过原生 Export 生成 618-byte `nativeGet.md`，`export-result.json` 明确 `debuggerEnabled=false`。禁用后仍有 20 条 Doc View 类直方图记录和 still-referenced warning，heap 重现同一 file chooser toolbar creation-trace 链。因此该引用不是由打开调试器造成；尚未修改生产代码，也未证明可安全解除。两个 HPROF 及分析数据留在本机 ignored 目录。

### 取消提示 R2 自动门禁及待验边界

首轮 `cancel-wording-candidate/full-gates.log` 有 429 项中的一项测试失败：`HttpRequestProjectIsolationTest.testIdenticalModuleAndMethodIdentityUsesDifferentProjectScratchPaths` 在请求任务 inactive 后立即检查 editor manager，实际 `FileEditorManager.requestOpenFile` 尚未完成异步打开。失败不是生产请求生成缺陷。专项修正首轮另有 2 项 fixture setup failure：Java module fixture builder 尚未注册；精确 263 SDK 字节码显示 `JavaTestFixtureFactory` 构造时注册该 builder。修正测试先初始化 factory，并等待实际目标文件 `isOpenIn` 后，`HttpRequestProjectIsolationTest` 2/2 通过；这些为测试夹具/异步等待修复，不改生产实现。证据分别为 `failed-test-results/TEST-com.liuzhihang.doc.view.service.http.HttpRequestProjectIsolationTest.xml`、`targeted-fixture-registration-red/`、`263-java-fixture-registration-bytecode.txt` 和 `targeted-editor-open-wait-green/`。

R2 候选 `cancel-wording-r2-candidate` 已完成 18/18 Gradle tasks；80 suites / 429 tests 全通过，344 classes major 69，325 个构建/测试输入哈希未变。最低 IU `263.7031.23` 与正式 IU `263.7307.200` Verifier 均 Compatible、16 Experimental、0 blocking findings。与 429 原生候选逐 JAR entry 比较仅改变 `DocViewBundle.properties` 与 `DocViewBundle_zh_CN.properties`。ZIP SHA `7661036f4edb4c7cb53ecb0bc729466f00188eed75500b376230352e62ad253a`，JAR SHA `5618784756441003feeb18de1bd890fe28bdd450782140d6c098feaace8ef997`；证据 `cancel-wording-r2-candidate/artifact.json`、`build-test-inputs-unchanged.json`、`production-jar-diff-from-status-cancel.json` 与两个 Verifier 目录。

R2 的英语/中文通用取消提示仍待隔离 IDEA 原生读回；429 原生搜索取消结果记录的是旧文案 `Project document status check was cancelled.`，其取消功能及 3/3 搜索重试已通过，但不得写成新文案已验证。429 候选热卸载也没有通过；在堆根因修复并按新包复验、且其余原生门槛关闭前，不推进发布。
