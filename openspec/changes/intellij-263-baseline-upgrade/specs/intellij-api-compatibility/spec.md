# Spec Delta

## MODIFIED Requirements

### Requirement: 正式矩阵与前向验证分离
系统 SHALL 以仓库中固定的 `pluginVerifierIdeVersions` 作为声明验证矩阵，并 MUST NOT 因一次性预发布 IDE 验证而扩大正式支持声明。生产最低版本升级到 IU 263 时，插件 SHALL 声明精确 `since-build="263.7031.23"`。平台解析与 Plugin Verifier 使用固定官方工件输入 `263.7031.23-EAP-SNAPSHOT`，其实际解析/Verifier 报告 MUST 确认目标为 `IU-263.7031.23`；不得用无法解析的裸 Maven 版本 `263.7031.23`、`263` 分支号、通配符或滚动 EAP 代替固定输入与实际 build 核验。

#### Scenario: IU 263.7031.23 基线升级
- **WHEN** 基线升级变更完成
- **THEN** 插件 descriptor 的最低 build 精确为 `263.7031.23`
- **AND** 平台与 Plugin Verifier 固定解析工件输入 `263.7031.23-EAP-SNAPSHOT`，并从实际元数据/报告读回 `IU-263.7031.23`
- **AND** 不把 IU 263 之前的 build 声明为受支持版本

#### Scenario: 单个精确 build 的验证范围
- **WHEN** 只对 `IU-263.7031.23` 完成 Plugin Verifier 与原生回归
- **THEN** 验证记录只声称覆盖该精确 build
- **AND** 不声称其他 IU build、IDE 产品或更早版本已通过验证

#### Scenario: 精确 build 前向验证
- **WHEN** 维护者通过命令行属性对额外的精确 IDE build 执行 Plugin Verifier
- **THEN** 结果记录为前向兼容证据
- **AND** 仓库中的正式验证矩阵和插件兼容范围保持不变

## ADDED Requirements

### Requirement: 参数表渲染器归属表格实例
参数表 SHALL 为每个表格创建独立 renderer，不得通过插件静态字段持有会引用最后渲染树的 renderer。表格关闭后，不应因 Doc View 静态 renderer 而保留表格、父组件或参数表单；并行参数表 MUST 保持各自的渲染状态。

#### Scenario: 参数弹窗关闭后的静态引用
- **WHEN** 参数表渲染完成并关闭弹窗
- **THEN** 插件静态字段不再提供 renderer 到已关闭表格的强引用路径
- **AND** 原生重复打开/关闭、EDT 空闲与 GC 样本独立记录，不以单元测试代替完整无泄漏结论

#### Scenario: 同时构造多个参数表
- **WHEN** 创建两个参数表实例
- **THEN** 两者使用不同 renderer，参数名称展示及编辑规则保持不变
- **AND** 构造阶段不访问尚未初始化的实例 renderer

### Requirement: 弹窗内部点击不触发外部取消
预览和参数弹窗的 mouse-out 取消回调 SHALL 先识别事件组件是否属于弹窗内容树。来自内容根组件或后代控件的事件 MUST NOT 触发外部点击取消，即使事件屏幕坐标与组件位置不一致。对于外部组件的鼠标按下事件，系统 SHALL 保持未固定时取消、固定时保留的既有行为；不得吞掉内部事件或直接执行工具栏动作。

#### Scenario: 内部按钮的屏幕坐标不一致
- **WHEN** Copy、Export、Pin 或参数控件收到鼠标事件，但事件屏幕坐标落在弹窗几何范围之外
- **THEN** mouse-out 回调不取消弹窗，事件继续由原控件处理
- **AND** 复制、导出或编辑的成功与否仍通过真实动作结果验证

#### Scenario: 真正外部点击与固定状态
- **WHEN** mouse-out 回调收到内容树外组件的鼠标按下事件
- **THEN** 仅在未固定时请求取消
- **AND** 非按下事件不请求取消，Escape 和窗口生命周期保持平台原行为

### Requirement: 显式声明现有 XML 功能的插件依赖
插件 SHALL 在构建和生产 descriptor 中声明 bundled `com.intellij.xml` 依赖，以使现有 Spring XML/Dubbo DOM 功能所用公开类在目标 IDE 中可解析。不得将平台 XML JAR 打入插件包，或仅靠 test classpath 掩盖生产依赖缺失。

#### Scenario: 正式版 IU 263.7307.200 检查最终包
- **WHEN** 对重建的最终 ZIP 在 `IU-263.7307.200` 上运行 Plugin Verifier 并启动隔离实例
- **THEN** XML/DOM 依赖由 descriptor 明确解析，Verifier 不报告对应 missing class 或 missing dependency
- **AND** 插件正常加载，现有 Spring/Dubbo 功能保持可用
- **AND** 记录实际 build 和最终 ZIP hash，不用候选 ZIP 或历史 EAP 结果替代

### Requirement: 异步编辑器打开保留结果反馈
系统 SHALL 观察 IU 263 编辑器打开 Future 的完成结果，不得把成功提交打开请求当成实际打开成功。已保存文档和 HTTP 草稿在打开失败时 MUST 保留；项目关闭引起的取消 MUST NOT 产生错误通知，过期请求不得覆盖新请求状态。

#### Scenario: 打开保存成功的文档失败
- **WHEN** 文档或 HTTP 草稿已保存，但异步编辑器打开异常结束
- **THEN** 用户看到打开失败提示，已保存文件保持不变
- **AND** 项目文档 UI 不显示“已打开”，欢迎页失败也可观察

#### Scenario: 打开完成前关闭项目或发起新请求
- **WHEN** 打开 Future 因项目关闭而取消，或旧请求在新请求之后完成
- **THEN** 不向已关闭项目发送错误通知，旧请求不覆盖新请求的 UI 状态
- **AND** 不在 EDT 阻塞等待异步完成

### Requirement: 263 基线升级前的 MCP 取消互操作准入
系统 SHALL 在修改生产最低 IDE build 前，使用记录了精确官方版本、下载摘要、实际二进制路径及 SHA 的 Codex 客户端，经临时 Codex app-server MCP 配置和 turn interrupt 验证完整的在途取消与会话恢复链；旧 CLI `0.163.0-alpha.11.2` 的失败记录 MUST 保留。官方 TypeScript SDK 只能补充验证，不能替代该门槛。仅收到 HTTP 202 或查看补丁源码不满足该门槛。

#### Scenario: Codex 客户端完成取消闭环
- **WHEN** Codex 客户端通过 turn interrupt 取消同一 session 中仍在执行的探针调用
- **THEN** 根据实际保存的 initialize protocolVersion，服务端观察到与该调用匹配的真实取消信号
- **AND** 对2025-06-18和2025-11-25验证带有原 request ID 的 `notifications/cancelled`，对2026-07-28验证该请求 SSE response stream 的取消关闭，不把普通连接关闭当成取消
- **AND** 服务端记录该任务取消及 cleanup，且没有迟到的成功完成
- **AND** 同一 session 随后的正常工具调用成功
- **AND** 记录 Codex turn、session、JSON-RPC request ID、服务端 run ID 与时间戳以关联两侧证据
- **AND** 连续三轮在途调用均满足上述条件，未通过项仍明确保留

#### Scenario: 2025-06-18 协商版本的候选客户端取消
- **WHEN** 隔离 Codex app-server 与 IDEA MCP Server 协商 `2025-06-18`，并通过 `turn/interrupt` 中断仍在执行的工具调用
- **THEN** wire 记录中对原 JSON-RPC request ID 收到匹配的 `notifications/cancelled`
- **AND** 服务端停止对应工作、释放资源且不为已取消调用返回迟到成功
- **AND** 同一 MCP session 后续正常工具调用成功
- **AND** HTTP 202 只作为通知传输结果记录，不单独作为取消成功证据

#### Scenario: 仅发现包含修复的官方版本
- **WHEN** 官方 PR、tag 或发行元数据显示某个客户端包含取消修复，但未运行对应实际安装包
- **THEN** 该版本只作为待复验候选
- **AND** 准入保持未通过，不修改生产最低版本或将旧客户端失败改为成功

#### Scenario: 只有取消 HTTP 状态码
- **WHEN** 只观察到 HTTP 202、自制 HTTP harness 或官方 TypeScript SDK 结果，或服务端取消、cleanup、同 session 后续调用任一项缺少证据
- **THEN** 取消互操作门槛保持未通过
- **AND** 生产平台最低版本保持原值

### Requirement: IU 263 使用 Java 25 生产基线
系统 SHALL 使用 JDK 25 toolchain 编译生产代码，且生产 Java 源码语法、可用 API 和 classfile 级别均为 Java 25 / major 69。构建期 `org.jetbrains.intellij.platform` Gradle 插件 MUST 使用正式版 `2.17.0`，其官方源码更新 build 262 起的 Java 25 映射；该 Gradle 插件是构建工具，不是生产运行时依赖。项目 `javaVersion` 配置 MUST 驱动 JDK/toolchain 与编译 `options.release` 使用同一 Java 25 基线；实际 javac 参数 MUST 限定为 `--release 25`。该 Java 25 升级已由用户于 2026-10-24 明确批准。不得通过只调整检查 metadata、保留 Java 21 产物、静音或跳过 `verifyPluginProjectConfiguration` 来绕过平台要求。

#### Scenario: 使用 JDK 25 编译 IU 263 插件
- **WHEN** 在 JDK 25 toolchain 下构建生产插件
- **THEN** `javaVersion` 驱动的编译器与编译 `options.release` 均为 Java 25
- **AND** 构建使用正式 IPGP `2.17.0`，且未将其加入生产运行时依赖
- **AND** 实际 javac 参数使用 `--release 25`，最终生产 classfile major 为 69
- **AND** IU `263.7031.23` 的 `verifyPluginProjectConfiguration` 通过且无 Java source/target compatibility 冲突警告
- **AND** 从实际编译任务和最终包验证 toolchain、release 与 classfile，不以声明值代替产物证据
- **AND** 不静音或跳过平台配置检查，不保留 Java 21 产物或低版本兼容实现

#### Scenario: IPGP 平台与 since-build Java 映射不兼容
- **WHEN** IPGP 2.16.0 的平台配置检查将 `since-build=263` 映射为低于 Java 25 的 target 级别，导致 Java 25 产物检查失败
- **THEN** 升级保持未验收并保留检查失败证据
- **AND** 不降低已批准的 Java 25 源码/API/字节码基线，也不静音或绕过检查
- **AND** 仅在找到符合官方插件约束的可验证配置后继续验收；否则明确报告阻塞

### Requirement: 自身卸载前释放平台图标缓存引用
插件 SHALL 在自身动态卸载前通过公开平台 API 同步使禁用图标缓存失效，防止该缓存继续通过图标资源加载器强持有 Doc View 的 ClassLoader。仅匹配 Doc View plugin ID，不为其他插件卸载清理全局图标缓存；不得依赖自身卸载后的回调、异步延时、Internal API、反射或关闭无障碍功能。

#### Scenario: 禁用自身插件
- **WHEN** IDEA 向 Doc View 发送自身 `beforePluginUnload` 通知
- **THEN** 在返回并进入正常 teardown 前完成图标缓存失效
- **AND** 不改变主题、图标资源、源文件或持久化设置
- **AND** 通过 targeted 生命周期测试验证回调注册和同步失效；这不等于最终包的动态热卸载成功

#### Scenario: 卸载其他插件
- **WHEN** 收到另一个 plugin ID 的卸载通知
- **THEN** Doc View 不清理平台图标缓存，也不创建延迟任务

#### Scenario: 独立 JNI 引用仍然存在
- **WHEN** 图标引用已释放，但原生 Accessibility 或其他根仍保留 Doc View 类加载器
- **THEN** 对应包的热卸载结果保持失败，并保留准确引用链及环境
- **AND** 不将图标清理或 `require-restart=true` 扩展为已修复平台根；安装、更新和卸载的生命周期按单独的重启契约验收

### Requirement: 目录释放时断开树的插件引用
目录面板 SHALL 在 dispose 返回前，通过公开 API 断开其目录树到已释放模型和面板祖先的引用，并关闭自身树搜索弹层。清理在 EDT 同步完成，不以迟到回调越过插件卸载边界，不在持有后台任务状态锁时等待 EDT。正常使用时的目录名称、完整签名、业务说明和搜索匹配范围 SHALL 保持不变；不得依赖 Internal key、反射或关闭无障碍来清理平台持有的引用。

#### Scenario: 树渲染器仍被平台保留
- **WHEN** 平台暂时保留目录树渲染器，而所属 Doc View 面板已经 dispose
- **THEN** 被保留的树不再通过自身 model 或父容器引用 Doc View 面板/模型，搜索功能的平台注册回调不持有插件定义的 formatter
- **AND** 正常运行中的搜索仍覆盖相同节点搜索文本；重复 dispose 安全，不重新启动后台任务

#### Scenario: 其他 JNI 根仍存在
- **WHEN** 新渲染器引用链已经断开，但无障碍节点或其他根仍保留插件类加载器
- **THEN** 完整热卸载仍记录为未通过，保存新一轮实际根链，不将局部 Java 引用清理写成平台 JNI 修复
- **AND** 该热卸载失败作为诊断历史保留；不再用它替代已批准的安装、更新、卸载重启契约

#### Scenario: 永久销毁目录树时释放鼠标路径缓存
- **WHEN** Doc View 永久销毁自身创建且不再复用的目录树
- **THEN** 在 EDT 使用公开 Component API 移除该树的全部 MouseListener，断开平台鼠标监听器缓存的旧 TreePath
- **AND** 正常在用目录的菜单、选择和展开不变，不修改其他组件或全局监听器，也不通过私有类型筛选或伪造鼠标释放事件清理
- **AND** 监听器数组为空不等于平台 JNI 根已释放；本次发布按重启契约验收，不把这项清理描述为热卸载通过


#### Scenario: 树 UI 的展开监听器仍保留旧鼠标路径
- **WHEN** 目录树永久销毁，而平台渲染器仍暂时持有该树，UI delegate 安装的展开监听器关联旧节点路径
- **THEN** dispose 在 EDT 返回前通过公开 Swing 生命周期解除该 UI delegate 及其监听器归属，使树不再通过该路径持有插件节点
- **AND** 不修改正常运行中的目录交互、不反射清空平台字段；重复 dispose 安全
- **AND** 不凭 dispose 单测推断平台类加载器已回收；已知动态热卸载失败保留为历史诊断


### Requirement: 已关闭项目不得通过 Doc View 工具窗口条目保留插件
Doc View SHALL 在原生 UI 入口确定关闭本项目、但 project dispose 尚未开始时，通过公开 Tool Window 生命周期同步移除自己的窗口条目；初始化窗口时即绑定其生命周期，不以用户先展开窗口为前提。清理 MUST 在 EDT 完成，只作用于匹配的 project 和 Doc View tool window，监听连接随窗口释放，不修改平台私有字段或 JNI 根。

#### Scenario: 原生 UI 项目正常关闭
- **WHEN** 本项目从原生 UI 通过关闭检查并在 EDT 发出 `projectClosing`
- **THEN** Doc View 在项目开始 dispose 前移除其 Tool Window 条目并释放连接
- **AND** 尚未展开和已使用的窗口都遵循相同清理，其他项目的窗口保持可用
- **AND** 项目再次打开时由平台正常注册一个 Doc View 窗口

#### Scenario: 尚可取消的项目关闭阶段
- **WHEN** 项目只进入 `projectClosingBeforeSave`，随后关闭被取消
- **THEN** Doc View 不提前移除窗口、取消有效文档工作或丢弃用户草稿

#### Scenario: 窗口已随卸载释放
- **WHEN** Doc View 窗口已经释放，或收到其他项目的关闭通知
- **THEN** 已释放监听连接不再执行清理，其他项目通知不移除当前窗口
- **AND** 不在已释放项目上创建新的 UI 或延迟任务

#### Scenario: 原生平台仍保留其他引用链
- **WHEN** 工具窗口条目已清理，但新包卸载后类加载器仍有其他根
- **THEN** 对应热卸载场景保留失败和实际根链；不得将本地清理视为官方 JNI 修复
- **AND** 安装、更新和卸载按另一个公开重启契约处理，不用该局部清理替代其验证

### Requirement: 插件安装、更新和卸载声明需要 IDEA 重启
插件 descriptor SHALL 在根 `<idea-plugin>` 声明 `require-restart="true"`，要求 IDEA 对插件安装、更新和卸载采用需要 IDE 重启的生命周期。该声明选择平台公开支持的操作策略，不声称解决动态热卸载时的 ClassLoader/JNI 根。历史热卸载失败必须继续保留；原生提示和重启后状态读取与自动 descriptor 检查分别报告。

#### Scenario: descriptor 声明生命周期要求
- **WHEN** 自动测试读取插件 descriptor 或构建检查最终插件包
- **THEN** 根元素名为 `idea-plugin` 且 `require-restart` 精确为 `true`
- **AND** 不能只在文档或 CHANGELOG 中描述重启，却遗漏打包 descriptor 属性

#### Scenario: 通过 IDEA 安装、更新或卸载
- **WHEN** 用户在 IDEA 中安装、更新或卸载 Doc View
- **THEN** 平台要求按重启流程应用该变更
- **AND** 之后按 IDEA 正常重启流程确认插件状态；动态热卸载不作为本次发布的操作承诺
- **AND** 未运行的原生确认须标为未验证，不能由 descriptor 测试推断通过

#### Scenario: 单独禁用或启用插件
- **WHEN** 用户单独切换 Doc View 的启用状态
- **THEN** `require-restart` 的文档范围不被推断为该操作必定要求重启
- **AND** 该操作只有在隔离 IDEA 中实际确认提示和重启后状态后才能标为通过；否则记录为未验证

#### Scenario: 保留旧包动态热卸载失败
- **WHEN** 历史候选包在关闭界面或使用 Export 后仍被平台强引用
- **THEN** 对应 artifact 的失败和根链继续留在验证记录，不重标为热卸载修复
- **AND** 依 2026-11-09 的用户决定，若功能验收完成而原生重启提示无法验证，可在发布说明中披露为未验证项并在发布后收集反馈
