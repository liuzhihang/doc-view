# 验证记录

日期：2026-10-18。仅隔离探针；未实现 Doc View 正式 MCP 服务或工具。

## 运行范围

新增 `tools/mcp-java-poc/` 的 Java 探针、Python 启动脚本和 README。输入为本机已有 IDEA/JDK，输出到仓库外新临时目录，不下载依赖、不连接服务器、不读取用户项目、不注册到普通 IDEA profile。核心 Java 21 基线及生产 Gradle/MCP 依赖未因本项更改。

| 环境 | 命令参数与结果 | 可以证明的边界 |
| --- | --- | --- |
| IU 261.22158.277，Java 21 | `--ide-home /Users/liuzhihang/.gradle/caches/9.5.0/transforms/232d456199c62cab0c9583ddb770291c/transformed/idea-2026.1-aarch64 --output-dir /tmp/doc-view-mcp-probe-261-text`；compile=0、run=0 | Java `String`/`int` 输入与平台 `McpToolCallResultContent.Text` 返回可通过实际平台反射生成 schema、调用工具；见下方细项。 |
| IU 262.10968.63，Java 21 | 首次 `/tmp/doc-view-mcp-probe-262-first` 编译失败：MCP class major 69，高于 Java 21 支持的 65 | 这是所选 JDK 无法读取目标 jar，不是 Java 语言无法接入 MCP。 |
| IU 262.10968.63，自带 JBR 25 | `--ide-home '/Applications/IntelliJ IDEA.app/Contents' --java-home '/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' --output-dir /tmp/doc-view-mcp-probe-262-text`；compile=0、run=1 | 编译可行；反射阶段要求真实 IDE Application/extension-point 环境，独立 JVM 未建立它，因此调用未验证。未伪造注册表或新增兼容适配。 |

每次实际命令均以 `python3 tools/mcp-java-poc/run.py` 开始。输出目录中的 `report.json` 保存 build、JDK、jar/source hash、各阶段结果；`compile.log` 与 `run.log` 保存原始错误。`--help` 已运行成功。262 诊断中的 `@TestApplication` 提示是环境缺失，不能把该版本的原始 String/DTO 标成语言不支持。

## IU 261 独立 JVM 结果

- Java 方法经 `-parameters` 编译，真实平台 schema 识别 `query: string`、`limit: integer`，两者 required；平台还加入可选 `projectPath`。
- 平台 Text 返回正常，实际 `McpTool.call` 得到 `# demo-api\nlimit=2`。
- 缺少 `limit`、传错类型和业务范围校验分别被拒绝。
- 原始 Java `String` 返回在 schema 阶段被拒绝（`Properties are expected in return type`）；普通 Java record 返回在调用序列化阶段被拒绝（`is not serializable`）。不能把 Text 路径成功推广为任意 Java DTO 自动序列化。
- 这些结果来自平台实现类的隔离诊断，未证明对应实现 API 可作为稳定生产扩展 API。

## 仍未验证的正式准入项

独立测试包和空 profile 已准备，但本记录不包含安装测试扩展或启动实际 MCP 服务的证据。因此真实扩展注册、`tools/list`、协议 `tools/call`、长任务取消、关闭项目和禁用插件后的清理均未验证；必须继续保留为独立准入门禁，不能据此开始第 13 项。

第 12 项结论：**局部互操作可行，正式 MCP 准入未完成**。第 13 项保持未开始。需要在隔离目标 IDE 内按 tasks 第 2 组进行完整验收；不修改生产平台、不引入 Kotlin 或自建协议服务器绕过该门禁。

## 包隔离

探针源码位于 `tools/`，不在 Gradle Java source set；没有修改核心 descriptor 或新增 MCP dependency。1～7 的已保存生产包 `/tmp/doc-view-reliability-20261018/Doc-View-1-7-green.zip` 早于该探针，不能单独证明后续包隔离。round21 最终生产包已另行审计，见下方“生产包隔离复核”。

## 隔离 Java 扩展包准备

新增 `tools/mcp-java-poc/build_test_extension.py`、测试用 `DocViewMcpTestToolset` 与独立 plugin descriptor。扩展只接收虚构名称和整数，并返回固定文本；descriptor 依赖 `com.intellij.mcpServer`，注册目标为所选 IDE MCP descriptor 中的动态 `mcpToolset` EP。脚本先核对 MCP plugin ID 与 EP，再以该 IDE 的现存 classpath 执行 `--release 21 -parameters` 编译并验证包内容。它创建空的 config/system/log/plugins profile；没有安装插件、启动 IDE 或调用 MCP 服务，profile 的 plugins 目录仍为空。

| 目标 API | 编译器与结果 | 测试包 SHA-256 | 空 profile |
| --- | --- | --- | --- |
| IU 261.22158.277 | Java 21.0.7；`compile_exit=0`；class major 65 | `91fa5a3cf580c6c02f7aad68c045f33db3b096a1a0f3e436768674fec6b226e8` | `/tmp/doc-view-mcp-java-extension-261-20261018-a/profile/idea.properties` |
| IU 262.10968.63 | IDE 自带 JBR `javac 25.0.4`，输出目标仍为 Java 21；`compile_exit=0`；class major 65 | `91fa5a3cf580c6c02f7aad68c045f33db3b096a1a0f3e436768674fec6b226e8` | `/tmp/doc-view-mcp-java-extension-262-20261018/profile/idea.properties` |

261 的两次独立构建分别输出到 `/tmp/doc-view-mcp-java-extension-261-20261018-a` 和 `...-b`，ZIP SHA-256 完全相同，证明相同输入下包生成可复现。262 的单独构建也得到同一 ZIP hash；它只证明对 IU 262 API 的编译成功，不证明加载或运行兼容。各报告记录精确 build、编译器、`mcpserver.jar` hash、descriptor 与包 hash。

未来原生测试应以同一隔离 profile 启动匹配的 IDE，再把此测试 ZIP 安装到该 profile；不得复用普通 IDEA profile。262 的启动 profile 参数为 `/tmp/doc-view-mcp-java-extension-262-20261018/profile/idea.properties`，例如通过 `/usr/bin/open -n -a '/Applications/IntelliJ IDEA.app' --env IDEA_PROPERTIES=<该 properties 路径> --args <临时项目路径>`。该命令尚未在本次任务中执行。

## IU 262 MCP 服务启用与授权边界

只读核对本机 IU 262.10968.63 的 `plugins/mcpserver/lib/mcpserver.jar` 及 JetBrains [MCP Server 文档](https://www.jetbrains.com/help/idea/mcp-server.html)：插件 descriptor 的 ID 为 `com.intellij.mcpServer`，提供动态 `mcpToolset` 扩展点；`McpServerSettings.MyState` 中 `enableMcpServer` 和 `enableBraveMode` 初始值均为 false，服务启动路径会检查启用状态，server 地址使用 loopback。`ConsentValidator` 在设置中勾选启用时调用同意对话框；该对话框说明第三方应用可访问 IDE 打开的项目，包括文件内容和 IDE 操作。命令执行确认逻辑适用于终端命令/运行配置，关闭 Brave Mode 时保留逐请求确认。上述为本机 class metadata 与官方文档核对，不是本次运行时观察。

后续服务验收步骤与预期 echo 结果见 [POC README](../../../tools/mcp-java-poc/README.md#真实-ide-协议验收步骤与授权边界)。必须使用空隔离 profile 和虚构项目；只由用户本人审阅并决定是否接受启用对话框，拒绝或无法确认时停止。保持 Brave Mode 关闭，只开放测试工具，并使用 MCP 设置页复制的本地传输配置，不写入现有客户端配置，也不通过内部 API 或配置覆盖绕开同意检查。当前 echo 即时返回，不能证明取消；取消需额外的有界、可观察且协作响应取消的测试调用。协议取消行为与竞态边界见 [MCP Cancellation 规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/utilities/cancellation)。

本轮仅做上述文档与本机静态核对：没有安装 POC、改 IDE 设置、启动 MCP 服务或请求 `tools/list`/`tools/call`。因此真实安装、协议调用、请求取消、项目结束和禁用清理仍未验证；tasks 2.2–2.4 不勾选，第 13 项继续保持未开始。

## 生产包隔离复核

主代理对 round21 生产包 `/Users/liuzhihang/workspace/doc-view/build/distributions/Doc View-1.3.14.zip` 做结构审计，报告 `/tmp/doc-view-reliability-20261018/round21-package-audit.json`，SHA-256 为 `01e67f753ce9fa388c0dbb607fe940ddb9a371dc8570aa6347ad27a3c2c42020`。包内唯一 JAR 未发现 PoC、测试支持类、测试框架、JUnit 或 fixture；生产 descriptor 仍为版本 1.3.14、`since-build=261`、无 `until-build`，依赖仅为 platform、Java 与 Markdown，未注册 MCP 标签或属性。此审计完成 task 3.2，不代表测试扩展已安装或运行。

## 现有 IDEA MCP 连接补充检查

此前原生 UI 不可用期间，通过现有 IDEA connector 发起过只读 `get_project_modules`。专用 `doc-demo` 不在当时打开的项目中；对 `doc-view` 的实际返回为 `doc-view.main`、`doc-view`、`doc-view.test` 三个 Java module。这只证明既有 IDEA MCP 连接可读项目元数据，不证明本轮 Java probe 已注册为工具，也不代替 Doc View extension 的 `tools/list`、调用、取消、项目关闭和卸载验收。没有通过该通道打开/修改项目、运行终端或操作账户。
