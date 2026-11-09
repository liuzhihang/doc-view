# Design

## Context

动机见 proposal。当前核心包没有 MCP 依赖或扩展注册。实际 IU 261.22158.277 的 `plugins/mcpserver/lib/mcpserver.jar` 包含公开 `McpToolset`、`McpToolsProvider` 和 `McpTool`，descriptor 定义动态 `mcpToolset`、`mcpToolsProvider`。`McpTool.call` 在 Java 字节码中带 Continuation 参数；`ToolsetReflection_utilKt.asTools` 为平台自身的注解工具反射入口。公开入口和实现桥接的稳定性必须区分。

已核对 [261 反射发现实现](https://github.com/JetBrains/intellij-community/blob/261.22158/plugins/mcp-server/src/com/intellij/mcpserver/impl/util/toolsetReflection.util.kt)、[CallableBridge](https://github.com/JetBrains/intellij-community/blob/261.22158/plugins/mcp-server/src/com/intellij/mcpserver/impl/util/CallableBridge.kt)、[返回值转换](https://github.com/JetBrains/intellij-community/blob/261.22158/plugins/mcp-server/src/com/intellij/mcpserver/impl/ReflectionCallableMcpTool.kt) 与 [ReflectionToolsProvider](https://github.com/JetBrains/intellij-community/blob/261.22158/plugins/mcp-server/src/com/intellij/mcpserver/impl/ReflectionToolsProvider.kt)。实现支持普通与 suspend 方法，但依赖 Kotlin reflection/serializer；Java 类型是否被正确表示必须实际执行。

## Goals / Non-Goals

目标是为第 13 项提供精确、可重复的准入结论。探针不读取源码 PSI，不设计最终文档 DTO，不提供用户可见正式工具，不改账户/MCP 信任设置、不监听公网，也不新增 Kotlin 源码或依赖下载。

## Decisions

### 分离纯 Java 探针与真实 IDE 验收

先新增 `tools/mcp-java-poc/` 的 Java 源码、Python 启动脚本和 README。脚本只收集显式 `--ide-home` 下现存平台及 MCP jar、找到显式或现存 JDK，使用 `javac --release 21 -parameters` 编译到指定临时目录，再运行探针。Python 只属于开发工具，不进入生产 jar。普通插件 Gradle 不增加 MCP dependency；避免 PoC 影响其他 changes 的正式构建或使插件强依赖 MCP。

探针通过平台 `asTools` 创建真实描述符和 `McpTool.call`，不以直接调用样例方法代替。此实现包调用只用于实验，不作为正式插件依赖选择。保留输出 schema/参数名，验证字符串、整数、错误输入和 String 返回；普通 Java DTO 为单独边界实验，拒绝时不增加临时序列化适配。

真实注册须在单独 profile 中安装最小扩展并由 MCP 客户端调用 `tools/list` / `tools/call`，再验取消/项目结束/禁用；普通 IDEA 不改设置。可操作环境缺失时只记录未验证，正式第 13 项保持前置门禁。没有完成这部分时不把独立探针称为“IDE 集成成功”。

### 精确版本与输入限制

第一目标为实际已缓存的 IU 261.22158.277；其次单独运行本机 IU 262.10968.63 作为前向实验。脚本从 `product-info.json` / MCP descriptor 记录版本，不根据目录名猜测。不得为使探针通过静默改变 `pluginSinceBuild`，也不复制新版本类进旧版 classpath。

所有输入使用 `demo-api`、固定整数与虚构 Markdown。错误日志只包含这些数据；不扫描用户环境变量或读取普通 profile 的凭据。失败记录异常类型和调用阶段，避免把缺少类/运行器问题误判成 Java 语言不可行。

### 准入决策

简单参数与 String 返回可行但 Java DTO 不可行时，第 13 项仅能提出 String/JSON 文本结果；不因此违反 Java-only。若真实注册/调用/取消尚未验证，准入结论为“未完成”，可继续项目文档与 Wiki。只有证据齐全才创建正式可选集成的运行实现，并再做 Plugin Verifier；本 PoC 不提前注册生产 extension。

## Risks / Trade-offs

- [反射参数名丢失] → 明确 `-parameters` 并读实际 schema，不能用位置参数伪装成功。
- [调用桥接被误当公开稳定 API] → PoC 可调用实现用于诊断，正式代码只依赖已验证的扩展 contract。
- [独立 JVM classpath 与 IDE loader 不同] → 分开报告，真实服务门禁保留。
- [缺少原生环境] → 保留未验任务并跳过依赖第 13 项，继续独立路线。
- [依赖差异导致 261/262 结果不同] → 分别保留报告，不做兼容适配或偷偷升级核心基线。

## 探针阶段发现与范围细化

首轮 IU 261.22158.277 实验已编译成功，但 Java `String!` 返回类型进入 schema 属性解析并失败，尚未执行工具正文。官方 `schema.util.kt` 对 Kotlin 非平台 String 使用类型相等检查，对 MCP 自有结果接口另有 subtype 分支。因此保留原始 String 失败实验，并另测公开 `McpToolCallResultContent.Text` 作为明确的工具返回类型；这仍是虚构 Markdown 文本输出，不复制/修改平台实现、不添加兼容分支或 Kotlin 源码。Java DTO 作为独立实验，即使文本路径失败也应记录结果。

262.10968.63 的 MCP jar 为 class major 69（Java 25），现有 Java 21 编译器不能读取；可显式使用该 IDE 自带且已存在的 JBR javac/java 执行前向实验，探针仍 `--release 21`，不改生产 Java 21 配置或下载 JDK。若编译结果因方法签名不同失败，记录差异而不增加跨版本桥接。

## 263 隔离前向实验（2026-10-23 追加授权）

维护者要求继续用隔离 IDEA 263 验证官方取消修复，并明确本轮不改生产基线。可以从 JetBrains 官方来源下载完整 macOS ARM64 IDE 包，核对官方 checksum、签名、product-info、MCP descriptor 和实际 SDK 类；不能把 Marketplace 插件 ZIP 或源码分支声明替代分发包证据。使用新临时 profile、虚构项目、同一公开工具扩展入口及随包 JBR；测试端只暴露虚构工具，保留命令执行确认，结束后关闭服务及本轮实例。

先用既有有界协作取消探针测试正常完成、显式通知取消和重复取消；取消须有服务端 cancelled/cleanup 且无完成成功证据，仅 202、客户端断流或超时均不算通过。再分别测试在途禁用测试扩展、在途关闭 MCP 服务及项目关闭，核对任务结束、工具不可调用和重新启用后的恢复。探针或临时客户端若有自身缺陷，保留失败证据后用公开 API 最小修复，再对变动场景复验，不修改宿主 SDK 或接入内部实现。

资源验收比较相同 IDE 进程的空闲基线、任务结束和卸载后对象/线程/端口观察，检查探针 Run、continuation、provider/tool 及插件类加载器；能取得 retained-path 则进一步定位。单次 heap histogram 无实例只能证明该时点的对象观察，不能表述为任意资源均无泄漏。所有结果与 261 历史失败分开记录；263 通过也不自动改变生产最低版本，升级需按确切首个通过的 build 单独完成兼容性 contract、构建、Verifier 与原生回归。


### 263 补充资源与客户端验收

后续隔离续验复用已核对的 263.7031.23、修复探针 0.1.1 与同一测试 profile；新进程、新证据目录，不覆盖前轮失败记录。固定三轮“显式取消 → 在途禁用 → 重新启用”，在同一 PID 取得空闲、任务结束、卸载后的 Thread.print、具名对象直方图及 classloader/class-unload 证据。成功标准是每个调用恰好终止/清理、无迟到成功；每次禁用后工具最终移除、具名 Run/wrapper/service/provider/tool 无存活实例且探针类卸载；启用后工具恢复。IDE 通用线程池数量波动不等于探针泄漏，须结合运行栈和探针资源所有权判断。只有观测到残留或卸载失败时才进一步查 retained-path，不把没有 heap dump 本身视作失败；本实验始终不证明长期或全 IDE 无泄漏。

客户端互操作优先使用本机已有的官方 MCP SDK，明确其版本和来源，分别记录用户侧取消、发出的通知、原 HTTP 响应及同会话后续调用；不修改用户客户端配置，不新增生产依赖。若无可用客户端，只记录公开源码与临时探针结果，正式客户端验收保持未完成。

## 2026-10-23：Codex turn interrupt 与正式产品边界

在用户明确授权后，使用仓库外临时的 Codex `0.163.0-alpha.11.2` app-server 配置调用 263 隔离 IDEA MCP Server。直接调用三轮的 Codex turn 均成为 `interrupted`，后续同会话 echo 成功；其中 run 3 正常 `completed`，run 4/5 则在客户端进程于 23:37:41 退出时被取消，不能归因于 turn interrupt。两轮透明 wire 均没有观察到 `notifications/cancelled`：第一轮在请求派发后约 160 ms 中断，IDE 请求仍运行约 30.7 秒并 `completed` / `cleanup`；第二轮确认服务器任务已启动并等待约两秒后中断，任务仍运行约 30.4 秒并 `completed` / `cleanup`。两轮均在同一会话随后成功调用 echo。不能把 app-server turn 状态或客户端进程退出当作服务端任务已由 turn interrupt 取消。

另经授权在仓库外临时安装官方 `@modelcontextprotocol/sdk` 1.32.1，三轮 AbortSignal 取消均向同一 263 隔离服务器发送取消通知，服务端关联日志为 `cancelled` 后 `cleanup`，且每轮同会话 echo 成功。原调用返回空 HTTP 202 被 SDK 标为取消错误而非工具成功。这个结果补充证明官方 SDK 取消路径可用，但不能替代 Codex turn-interrupt 门槛。

临时 SDK 安装与 Codex app-server `-c` 进程覆盖使用用户授权的隔离证据目录；未修改全局 Codex 配置、生产插件、普通 IDEA profile 或用户日常 MCP 设置。实测使用的是隔离测试插件工具，不能表示 Doc View 正式工具已注册。

正式集成方向是在同一个 Doc View 插件中增加可选 MCP 扩展、注册到 IDEA 自带 MCP Server，并复用 UI 按钮调用的文档服务；所需 API 边界仅在插件内部。当前没有正式的 Doc View 接口搜索/详情工具，也没有未来显式生成/更新工具。独立 PoC 继续作为隔离准入探针。因 Codex turn interrupt 仍未传播到服务端，task 2.4 保持未完成，生产 IDEA 最低版本仍为 261；条件升级单独跟踪在 [proposal](../../intellij-263-baseline-upgrade/proposal.md) 和 [verification](../../intellij-263-baseline-upgrade/verification.md)，不能把 263 前向实验计为生产升级回归。
