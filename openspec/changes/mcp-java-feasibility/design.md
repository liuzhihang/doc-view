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
