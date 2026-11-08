# 验证记录

初验：2026-10-18；原生续验：2026-10-23。仅隔离探针；未实现 Doc View 正式 MCP 服务或工具。

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

## 当前状态（2026-10-24）

PoC 的 task 2.4 已关闭：IU 263.7031.23 / SDK 0.15.0 探针的显式取消、在途禁用、服务关闭、项目关闭和有限资源释放有原生证据；官方 Codex `0.165.0-alpha.1` 候选在该隔离 build 上的真实 turn-interrupt 取消也已三轮通过。`mcp-java-feasibility` 共 14 项 tasks 全部完成，但本 change 尚未 sync/archive。详见文末最新候选记录与[完整取消验收说明](../../../docs/mcp-cancellation.md#候选客户端运行证据)。

旧 IU 261.22158.277 的取消失败、Codex `0.163.0-alpha.11.2` 的 turn-interrupt 失败均按版本和环境保留，不能由新候选结果覆盖。生产基线仍为 261；用户已批准精确 IU 263.7031.23 / JBR 25 升级并正在构建，构建、Verifier 和完整原生回归尚无通过结论。正式 MCP 工具仍未开始；它的 task 1.3 仍等待生产基线和文档库最终验收。

第 12 项结论更新为：**隔离 Java MCP 扩展可行，指定 Codex 候选的取消互操作通过；生产基线升级和正式 Doc View MCP 实现仍未验收**。不因 PoC 完成自动注册生产 extension、提高已发布兼容范围或归档正式工具 change。

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

2026-10-18 仅做上述文档与本机静态核对，当时未安装 POC、改 IDE 设置、启动 MCP 服务或请求 `tools/list`/`tools/call`。安装、协议及生命周期的最新状态由下方 2026-10-23 续验更新；第 13 项继续保持未开始。

## 生产包隔离复核

主代理对 round21 生产包 `/Users/liuzhihang/workspace/doc-view/build/distributions/Doc View-1.3.14.zip` 做结构审计，报告 `/tmp/doc-view-reliability-20261018/round21-package-audit.json`，SHA-256 为 `01e67f753ce9fa388c0dbb607fe940ddb9a371dc8570aa6347ad27a3c2c42020`。包内唯一 JAR 未发现 PoC、测试支持类、测试框架、JUnit 或 fixture；生产 descriptor 仍为版本 1.3.14、`since-build=261`、无 `until-build`，依赖仅为 platform、Java 与 Markdown，未注册 MCP 标签或属性。此审计完成 task 3.2，不代表测试扩展已安装或运行。

## 现有 IDEA MCP 连接补充检查

此前原生 UI 不可用期间，通过现有 IDEA connector 发起过只读 `get_project_modules`。专用 `doc-demo` 不在当时打开的项目中；对 `doc-view` 的实际返回为 `doc-view.main`、`doc-view`、`doc-view.test` 三个 Java module。这只证明既有 IDEA MCP 连接可读项目元数据，不证明本轮 Java probe 已注册为工具，也不代替 Doc View extension 的 `tools/list`、调用、取消、项目关闭和卸载验收。没有通过该通道打开/修改项目、运行终端或操作账户。


## 2026-10-23：IU 261 真实扩展加载与工具发现

本轮证据目录为 `/tmp/doc-view-next-20261023/`。使用 JetBrains 官方完整 IU 261.22158.277，构建并安装测试 ZIP 到新建的 `mcp-prep/profile/plugins`，以该 profile 的 `idea.properties` 启动；只打开本轮创建的 `mcp-fictional-project`，内容为虚构 README。未复用普通 IDEA profile、未自动配置现有 Codex/Claude Code 客户端。

- 插件身份：`org.docview.mcp.java.poc` / `Doc View MCP Java POC` / `0.1.0`。ZIP SHA-256 为 `91fa5a3cf580c6c02f7aad68c045f33db3b096a1a0f3e436768674fec6b226e8`，安装 JAR 与 ZIP 内 JAR 一致；仍只有测试 `mcpToolset`，没有正式接口搜索/详情工具。
- `mcp-prep/profile/log/idea.log` 记录精确 IU build、四个独立 profile 路径，以及 `Loaded custom plugins: Doc View MCP Java POC (0.1.0)`；观察时无 ERROR 级记录。
- 原生 **Settings | Tools | MCP Server | Exposed Tools** 显示 `DocViewMcpTestToolset` 及 `doc_view_java_poc_describe_demo`，描述为固定虚构 API 注册测试。证据 `mcp-registered-tools-ax.txt` / `mcp-registered-tools.png`。这证明真实 IDE 已发现扩展，仍不等于客户端 `tools/list`/`tools/call` 已通过。
- 服务启用时真实出现 **Enable MCP Server?** 确认框，说明第三方应用可读取打开项目、触发 IDE 动作及运行命令。此阶段停在该确认并请求维护者授权，没有通过配置绕过同意；之后维护者回复“允许”，执行情况见下一节。确认框证据为 `mcp-enable-consent-ax.txt` / `mcp-enable-consent.png`。Brave Mode 未启用。
- `mcp-install-preservation.json` 核对 `doc-demo` 与普通 261/262 profile 的 58 个已冻结文件，hash 全部一致。此结论限冻结文件，不声称普通 profile 全目录未发生任何自主运行变化。
- 生产 G4 包 SHA-256 仍为 `68852145f231627e93a3716d2fb47d6e96236dac2b634441c50872f72db510eb`；本项未更改生产 Java、Gradle 或 descriptor。

此阶段完成 **7/9**，剩余 2.3–2.4。echo 同步且即时返回，不能证明协作取消；因此另用公开 `McpToolsProvider` / `McpTool.call(..., Continuation)` 准备有界测试。授权后进度已更新为 8/9，仍不把公开入口存在等同于协议取消可用。


### 授权前取消探针准备（历史阶段）

临时目录 `mcp-cancel-draft/` 另备了只含 `doc_view_java_poc_bounded_delay` 的 Java `McpToolsProvider` 草稿，不替换已验 echo、不写入生产或仓库。使用公开 `McpTool.call` 的 Continuation、请求 Job、`DelayKt.delay` 和项目 Disposable；输入限定 5–30 秒虚构延迟，无项目则明确拒绝，不读取项目文件。准备阶段尚未安装或调用；授权后的安装和实际结果见下一节。

主代理以现有 Java 21 执行 `--release 21 -parameters -proc:none -Xlint:all` 对 261 jar 编译，无警告；独立 JVM 的四个 smoke 场景通过：唯一工具描述符、无项目拒绝、真实协程 Job 取消/恢复/回调解绑、正常完成/恢复/回调解绑。证据 `mcp-cancel-draft/root-verification.json`、`compile-root.log`、`smoke-root.log`。Project 是测试 proxy，tick 为缩短测试值：这不证明真实耗时、服务取消通知、IDE 项目关闭、插件卸载或 GC；2.4 不勾选。取消与最终成功门禁并发时允许已完成请求先返回，不能用事件日志推断对象已被回收。


本机临时协议客户端草稿保存在 `mcp-client/mcp_client.py`，要求显式 loopback URL，仅允许 echo 与 bounded-delay 两个测试工具。已修复网络尝试标记、清理后落盘和取消误用 echo 的问题；主代理复验两个模式的 `--dry-run` 均明确 `network_attempted=false`，记录 `mcp-client/root-dry-run.json`。这段是等待维护者答复时的准备记录，干跑不作为 2.3/2.4 通过证据。

## 2026-10-23：授权后协议与生命周期续验

维护者明确回复“允许”后，在上述隔离 profile 的真实确认框中点击 Enable。仅打开虚构项目，Brave Mode 保持关闭，Exposed Tools 收紧后再进行协议验收，验收时仅开放两个测试工具。通过设置页 **Copy HTTP Stream Config** 获取 `http://127.0.0.1:64343/stream`，使用本机临时客户端，不自动配置 Codex/Claude 或写入其配置文件。服务端实际协商协议为 `2025-06-18`。本节证据目录为 `/tmp/doc-view-mcp-allowed-20261023/`。

取消探针单独安装为 `org.docview.mcp.java.cancel.poc` / `Doc View MCP Java Cancel POC` / `0.1.0`，注册公开 `mcpToolsProvider`，Java 21 class major 65。ZIP SHA-256 为 `c44e089c2295b1c8b468abf1ba3aa544f3b5265e45d111ab54ddc8f1da98403a`，源码 SHA-256 为 `558f7557d7f5abf3d9ad7b12150690b1882320ca22d68e55bb35495321cf4105`；包和源码仍在仓库外，不进入生产 source set。

| 检查 | 实际结果与边界 | 证据 |
| --- | --- | --- |
| 初始化与工具发现 | initialize 200，初始化通知 202；`tools/list` 只含 echo 与 bounded-delay。echo 的 `query:string`、`limit:integer` 必填，`projectPath` 可选 | `echo-suite-recheck/suite-20261023T123314Z.json` |
| echo 调用 | 正常参数返回 `# fictional-api\nlimit=2` 且 `isError=false`；limit=0、缺少 limit、错误类型分别返回 `isError=true`；session DELETE 200 | 同上；完成 task 2.3 |
| 有界任务正常完成 | 请求 5 秒，约 5.085 秒返回 `Bounded delay completed.`；日志 run 1 有 started、completed、cleanup | `delay-normal.json`、隔离 `idea.log` |
| 协议取消（未通过） | run 2/3 在途发送同会话、同请求 ID 的 `notifications/cancelled`，均收到 202，但服务端约 5 秒后仍 completed/cleanup。第二次继续等待原请求，实际收到成功结果，`server_cancellation_proven=false` | `protocol-cancel/`、`protocol-cancel-observed/cancel-20261023T123424Z.json`；不能把客户端“通知发送 passed”当作服务端取消通过 |
| 在途关闭项目 | 启动 30 秒延迟请求后用原生 Close Project；回到 Welcome，run 4 在约 15.8 秒时记录 project_closed → cancelled → cleanup，无 completed。客户端最终 40 秒超时、DELETE 200，未获得结构化取消结果 | `project-closed-ax.txt`、`project-close-call.json`、`idea.log`；证明项目 dispose 到探针 Job 的取消路径，不证明协议取消 |
| 项目关闭后对象观察 | GC.class_histogram 中未出现探针 Run/continuation 实例；仍有 provider/tool 实例，因为插件此时仍加载 | `post-project-close-histogram.txt`；单次观察不能推出无所有资源残留，也不能把仍加载的工具实例定性为泄漏 |
| 空闲扩展禁用 | Settings 中禁用两个测试插件并 Apply；日志均显示 Successfully unloaded，classloader unload checked=false。新会话 `tools/list` 为空；两个工具调用均返回 not found / `isError=true` | `post-plugin-disable-applied.json`；欢迎页初次禁用尚未应用的记录另保留在 `post-plugin-disable.json`，不作为通过证据 |
| 卸载后对象观察 | 同一 IDE 进程再次 GC.class_histogram，未出现 `org.docview.mcp` 探针实例 | `post-plugin-unload-histogram.txt`；没有取得 classloader retained-path，也未覆盖在途卸载 |
| 服务关闭与退出 | 原生取消 Enable MCP Server 后初始化连接拒绝；退出后保存配置不再启用服务，两个 PoC plugin ID 保持禁用；本轮进程已退出且 64343 无监听，官方 DMG 已卸载 | `service-disabled-ax.txt`、`service-disabled.json`、`cleanup.json`；关闭时已无在途任务 |

首次协议 suite 将响应 `_message` 过早移除，导致四个正确工具结果被误判。修复临时客户端后重新执行完整 suite，三种负例与正常结果均通过；session DELETE 还补齐协商版本 header。最初失败报告保留在 `echo-suite/`，不能与复验混为一次成功运行。负例拒绝和项目关闭取消被平台记录为错误日志，因此不声称本次 IDE 日志无错误。

### 取消根因与下一步

对运行中的同版本 261 jar 作只读 bytecode 核对：`Protocol.onNotification` 按通知 method 查 handler，无 handler/fallback 时直接返回；`ServerSession`/`Server` 与 IDEA MCP 适配层未见 `notifications/cancelled` 到请求 Job 的注册。HTTP transport 对没有 JSON-RPC 响应的通知返回 202。工具的公开 `call(JsonObject, Continuation)` 可以取得 coroutine Job，但不暴露协议请求 ID 和 ServerSession；IntelliJ `McpCallInfo` 的 callId 另行生成。证据为本节目录下 `Protocol-bytecode.txt`、`ServerSession-bytecode.txt`、`Server-bytecode.txt`、`McpToolAdapter-bytecode.txt`、`StreamableHttpTransport-bytecode.txt`。根因判断由这条调用链和两次原生失败共同支持，范围限 **IU 261.22158.277 当前实现**，未推断其他版本均有此行为。

[协商版本的 MCP Cancellation 规范](https://modelcontextprotocol.io/specification/2025-06-18/basic/utilities/cancellation)允许可选取消，要求接收方尽可能停止与释放资源，并允许无法取消时忽略通知。因此本次结论是 **不满足本 change 的协作取消准入标准**，不直接宣称平台违反所有 MCP 实现要求。项目关闭能取消 Job，也排除了“Java Continuation 无法响应任何取消”的泛化结论。

task 2.4 保持未完成，第 13 项不启动。后续先查明平台是否提供公开修复入口；若要提高目标基线或将首版调整为严格有界同步查询，必须另行明确 contract 并批准，不能默默放宽验收。不得复制内部协议实现、自建第二个 MCP Server 或以独立 JVM smoke 替代本项。完整资源验收、在途插件/服务禁用及竞态矩阵仍需补齐。

最终核对：本轮只修改路线图及本 change 的 tasks/verification 三份文档；生产 G4 ZIP hash 不变。`doc-demo`/普通 261、262 profile 的 58 个冻结文件与既有 `.agents/` 六文件 hash 均未变，范围不扩大为全目录证明；见 `final-preservation.json`。两个活动 change 的 strict 校验及 `git diff --check` 通过，OpenSpec apply 读回本 change 为 9 total / 8 complete / 1 remaining。未改生产代码，未补跑无关 Gradle，未 sync/archive、提交、推送或发布。HTTP/UI change 仍为 29/36，其许可、菜单和原生生命周期剩余项独立保留。

## 2026-10-23：取消问题的公开 API 与官方修复核查

结论：**当前目标 IU 261.22158.277 的协议取消仍未通过；已找到上游修复及 JetBrains 集成提交，但没有证实可直接用于该基线的官方修复包或公开插件补救入口。** 本次为源码、发行元数据、分发包 bytecode 和临时客户端的离线核查，没有重新启动隔离 IDEA 或发起原生协议复验。证据目录为 `/tmp/doc-view-mcp-cancel-investigation-20261023/`，版本对比另存 `/tmp/doc-view-mcp-cancel-compare-20261023/`。

### 已核对的公开入口与版本边界

| 范围 | 实际核对结果 | 能证明的边界 |
| --- | --- | --- |
| 261.22158.277 的公开工具入口 | `McpTool.call(JsonObject, Continuation)` 可响应已有 Job 的取消；`McpCallInfo.callId` 是平台另行生成的计数，`ToolCallListener` 只观察调用前后 | 没有向工具提供 JSON-RPC request ID、ServerSession 或取消通知回调；不能在插件中仅凭公开入口把通知映射到请求 Job。见官方 [McpTool](https://github.com/JetBrains/intellij-community/blob/261.22158/plugins/mcp-server/src/com/intellij/mcpserver/McpTool.kt)、[McpCallInfo](https://github.com/JetBrains/intellij-community/blob/261.22158/plugins/mcp-server/src/com/intellij/mcpserver/McpCallInfo.kt) 与 [宿主适配](https://github.com/JetBrains/intellij-community/blob/261.22158/plugins/mcp-server/src/com/intellij/mcpserver/impl/McpServerService.kt)。 |
| 本机 262.10968.63（静态前向核对） | 工具入口未增加协议取消参数；`McpCallInfo` 增加 sessionId，但 callId 仍另行生成。SDK 的 Protocol/ServerSession 和宿主仍未见取消 handler 注册；未知通知无 fallback 时直接结束 | 没有执行本版本原生取消；新增 sessionId 不等于请求 ID，新 `mcpManagedSessionSupport` 属内部扩展且没有逐调用取消 hook。结论限所查分发包。 |
| Marketplace 最新 261 插件包 | 查询 [官方更新接口](https://plugins.jetbrains.com/api/plugins/26071/updates?size=100)，261 最新为 `261.26222.30`，要求 `since-build=261.26222`；下载包只作静态检查，未安装 | 不兼容当前 `261.22158.277` 基线；包中没有 SDK 类，不能用它推断新 IDE 的 SDK 取消行为。公开工具签名也未增加取消入口。ZIP SHA-256：`ac7e32322342d9c3015b3161d16cb19b8a38dac6f43355fcfc42c396d4dc863e`。 |

本机 261 的 SDK 类合并在 `mcpserver-frontend.jar`（SHA-256 `399bb5562e70794eea478fb5b39300b0ae022d4bb3129eb31bcbd2d157cb88eb`）；262 的 SDK 位于 `io.modelcontextprotocol.kotlin.sdk.jar`（SHA-256 `d2895239435007b2ffde6d70bc39877d90adc257240e54dcf44b83e4917aa12b`）。这些分发包没有可用的 Maven 版本元数据，因此依靠实际类行为取证，不将公开分支的 SDK 版本声明冒充本机二进制版本证明。

### 官方修复路线

MCP Kotlin SDK [PR #884](https://github.com/modelcontextprotocol/kotlin-sdk/pull/884) 于 2026-07-15 合并，[0.15.0](https://github.com/modelcontextprotocol/kotlin-sdk/releases/tag/0.15.0) 于 2026-07-28 发布。其实现注册 `CancelledNotification` handler，按协议 request ID 找到在途 Job 并取消；初始化后并发分发，避免慢请求阻塞取消通知，并处理取消后的响应和清理。已核对 [0.15.0 Protocol 源码](https://github.com/modelcontextprotocol/kotlin-sdk/blob/0.15.0/kotlin-sdk-core/src/commonMain/kotlin/io/modelcontextprotocol/kotlin/sdk/shared/Protocol.kt)，不能把更早的 RequestContext 或 stdio 异常传播改动当作这项修复。

JetBrains 已在 [IJPL-256419 对应提交](https://github.com/JetBrains/intellij-community/commit/31d56d7ca6107b991f328116f84195892df70ae1)将 SDK 从 0.12.0 升到 0.15.0，同时调整 handler 上下文、逐请求 HTTP headers 和响应流生命周期。固定提交 `d8c6714c7a3c110c56994d6b0f10814f902100e7` 的 [依赖声明](https://github.com/JetBrains/intellij-community/blob/d8c6714c7a3c110c56994d6b0f10814f902100e7/libraries/mcp-kotlin-sdk/intellij.libraries.mcp.kotlin.sdk.iml)及公开 [263 分支声明](https://github.com/JetBrains/intellij-community/blob/263/libraries/mcp-kotlin-sdk/intellij.libraries.mcp.kotlin.sdk.iml)均为 0.15.0；宿主已经使用 `currentRequestHandlerExtra()` 获取协议 request ID。这证明官方集成路线存在，不证明 263 分发包的取消验收通过，也不证明当前 261 基线得到回移修复。

不能在 Doc View 中直接覆盖宿主 SDK：官方升级本身包含宿主适配改动，替换 JAR 会跨越类加载、二进制签名和平台基线边界。本轮保留 Java-only 和 IDEA-owned MCP Server 路线，没有复制内部实现、反射安装通知 handler、自建第二个服务器或调整生产依赖。

### 已修复的问题与复验结果

临时客户端原先把取消通知收到 202 写成 `cancel_test.passed=true` / `overall=passed`，虽另有 `server_cancellation_proven=false`，汇总语义仍容易误导。本次修复 `/tmp/doc-view-next-20261023/mcp-client/mcp_client.py`，明确区分通知送达与服务端取消：原工具在通知后成功返回记 `failed`，缺少停止证据记 `unverified`，都不返回通过状态。原始网络请求、请求 ID、session header 和协商版本核对未发现错误。

- 主代理执行 `python3 -B /tmp/doc-view-mcp-cancel-investigation-20261023/test_cancel_semantics.py`：**5/5 离线回归通过**。覆盖通知 wire、成功返回后的失败判定、无结果时未验证判定、报告/退出码和 dry-run 的 `network_attempted=false`。没有打开 socket；不能代替 IDEA 原生取消复验。
- 对历史 `protocol-cancel-observed/cancel-20261023T123424Z.json` 只读重判，另存 `protocol-cancel-observed-reclassification.json`：送达 `true`、取消证明 `false`、整体 `failed`。原 JSON SHA-256 仍为 `61785052a0ad5dbd874686509be461654cb1e7000af23da45b4af6a33da6196a`，未改写历史证据。
- task 2.4 保持未勾选，进度仍 **8/9**。当前基线没有可落地的公开修复，本次没有重复相同原生失败实验；263 仅为后续隔离验证候选，需先明确该实验的平台范围，再核对官方二进制并验证通知后 Job 终止、无成功响应、cleanup、在途禁用和资源释放。该候选通过也不能自动关闭 261 的失败项或提高生产基线。

本次 `openspec validate mcp-java-feasibility --strict`、`git diff --check` 和三份文档相对链接核对通过。494 文件基线中仅本节涉及的 verification、tasks、roadmap 三份文档变化；58 个普通 profile/专用项目冻结文件、既有 `.agents/` 六文件、HEAD 与 G4 生产 ZIP 均未变，见 `final-preservation.json`。临时服务端口 64343 无监听；未安装新版 IDE/SDK、运行生产 Gradle、提交、推送、sync 或 archive。


## 2026-10-23：263 官方分发包与原生续验

本轮获准继续隔离验证，不修改生产最低版本。证据根目录为 `/tmp/doc-view-mcp-263-20261023/`；所有探针、安装包、客户端和原始日志均留在仓库外。本节取代此前“263 原生未验”的候选状态，不覆盖 261 的历史失败。

### 实际安装包与 SDK

- 官方完整包为 [IDEA 2026.3 EAP 7 ARM64](https://download.jetbrains.com/idea/idea-263.7031.23-aarch64.dmg)，实际 `product-info.json` / build 为 **IU-263.7031.23**，不是当前最新 263 版本。DMG 为 1,575,622,276 字节，SHA-256 `c173e165d6365387302694d1fd9cd0d3b02f420f4af942fcd9ed80a37e1c00d2` 与正常 TLS 获取的官方 checksum 一致；`codesign --verify --deep --strict` 成功，签名为 JetBrains / `2ZEFAR8TH3`，含 notarization ticket。初始下载片段曾误用 `-k`，后续 checksum 验证完整文件身份；不将全程传输描述为 TLS 校验通过。
- 实际 MCP 目录是 `plugins/mcpserver-plugin`，plugin ID `com.intellij.mcpServer`，版本 `263.7031.23`；仍由 IDEA 自己注册、运行 MCP Server。许可证清单确认随包 Kotlin SDK **0.15.0**；SDK JAR SHA-256 `d85501edcf60d9f9d0c56413a4e01f19913c98d025f6164f416d45e0c86fcadd`。
- 对实际 SDK 字节码确认：`CancelledNotification` handler 根据 request ID 找到在途 Job 并取消；initialized 后允许并发分发，完成时移除映射。这与上节官方 SDK 修复相符，不再只是源码分支推断。身份与字节码记录在 `sdk-audit/`。
- 使用随包 JBR `25.0.4.1+1-b635.72` 编译，探针仍 `--release 21 -parameters -proc:none`、class major 65。263 的公开 `McpToolCategory` / `McpToolDescriptor` 构造参数已改变，临时探针按目标 API 直接适配；仅提高 `since-build` 不能替代 API 适配或生产兼容性回归。

### 先修复再复验

原版 0.1.0 在 263 上已能响应协议取消。在途禁用时也停止任务，但日志误称 `project_closed`（当时项目仍打开），IDE 随后报告 classloader 仍被引用；GC 后 Run 为 0，但剩余一个 BoundedDelayTool 和两个 provider lambda，不能记为资源释放通过。

临时修复版 **0.1.1** 改用插件自有 project service 作为每次调用的 Disposable 父节点，增加一次性清理保护，把事件改为 `owner_disposed`。依据 [JetBrains Disposer 指南](https://plugins.jetbrains.com/docs/intellij/disposers.html#choosing-a-disposable-parent)，避免直接以 Project 为父节点。没有修改 IDE SDK、接入内部通知 handler 或自建服务器。六个独立 JVM smoke 场景通过；原生安装后重启隔离实例，读回实际版本、descriptor 和 JAR hash。

- 修复 ZIP SHA-256：`4ead7b83867e103dc1dbae98b0ed209c000b2a0a66a9d5a857af363833135caa`。
- 实际安装 JAR SHA-256：`8dfecb3f7491fa722cd456331a26cb83c8e0f6de003e16cb369206756bc56d12`。
- 原版失败证据保留于 `unload-original/`、`resource-audit/original-observation.md`；修复源码、descriptor、最小 diff、构建/smoke 日志位于 `fixed-probe/`。本轮未取得原版 retained-path，且原版进过 Exposed Tools 页面、修复版未进，不能声称已经证明旧 loader 引用链的唯一根因。
- 客户端增加“只观察、不主动取消/断流”的生命周期模式，固定两个测试工具与显式虚构 projectPath。11 个离线自测通过；模式 dry-run 不访问网络，不把通知 202 计为服务端取消通过。

### 原生结果（修复版 0.1.1）

隔离 config/system/log/plugins 全部在证据根目录，只打开虚构项目。MCP 使用 `127.0.0.1:64343/stream`，协议协商为 `2025-11-25`；只开放两个测试工具，保留命令确认，未自动配置现有客户端。日志时间为本地 UTC+8，run ID 在插件重新加载后重新计数。

| 检查 | 实际证据与结论 |
| --- | --- |
| 工具发现与普通请求 | 两个工具 schema 通过；echo 正常返回及 limit=0、缺参数、错误类型拒绝均通过。5 秒任务 run 1 于 22:28:09.563 started，14.641 completed，14.646 cleanup。 |
| 显式取消与重复取消 | 30 秒任务 run 2 于 22:28:14.653 started；约 1 秒后发送取消，15.669 cancelled / request_cancelled，15.670 cleanup。重复/迟到取消收到 202；无该 run 的 completed，后续同会话 echo 通过，session DELETE 200。证据 `protocol-fixed/`。 |
| 在途禁用扩展 | run 3 于 22:28:39.042 started，原生禁用/Apply 后 54.186 owner_disposed、cancelled，54.187 cleanup；客户端没有发取消通知，原调用返回工具错误，同会话 echo 正常。立即 tools/list 仍见 delay，稍后 `unload-fixed/settled-tools.json` 确认 delay 消失、echo 仍存在，记录清理竞态，不把第一份快照说成已移除。 |
| 卸载后资源 | 22:28:54.563 JVM 记录卸载 12 个探针类（含 lambda）；后续 GC 和 loader 快照均不再包含取消探针对象/类。与原版残留不同，这证明本次卸载完成；不扩大为长期或全部 IDE 无泄漏。 |
| 重新启用后在途关闭服务 | UI 重新启用插件，无需重启；30 秒任务已开始。22:33:27.905 cancelled / request_cancelled，27.907 cleanup。客户端连接终止、后续请求拒绝，64343 无监听；未发取消通知。证据 `service-stop-fixed/`。 |
| 恢复服务后在途关闭项目 | 恢复同一隔离服务后任务 started / owner_bound；原生 Close Project 令 22:33:54.902 owner_disposed、cancelled，54.903 cleanup，原调用返回工具错误。已关闭项目的 echo 路由返回错误符合预期，不记为正常 echo 通过。证据 `project-close-fixed/`。 |
| 项目关闭后资源 | GC 中 Run、探针具名 continuation wrapper、project lifetime service 为 0；provider/tool 仍加载符合插件启用状态。保存 Thread.print，未观察到探针运行栈；没有完整前后线程基线、重复启停压力测试或 retained-path，task 4.4 保留未完成。 |

显式取消有一个不能省略的客户端边界：取消通知本身返回空 **202** 符合规范；原 tools/call 也以空 **202** 结束，当前临时客户端将其分类为 TransportError。实际 SDK 的取消 HTTP 清理路径可以解释这一行为，但 [Streamable HTTP 规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports#streamable-http)没有把它明确列作 JSON-RPC 请求的普通成功响应。[取消规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/utilities/cancellation)要求尽力停止和清理，并建议不再回复取消请求。故本轮结论是“服务端取消及清理已验证、会话可继续使用”，不是“所有客户端与传输一致性已验收”。正式接入还需目标客户端互操作验收。

### 关闭、保留边界与下一步

已在原生 UI 关闭服务，确认 `enableMcpServer=false`（保存时默认 false 省略）、Brave Mode 未启用；隔离 PID 66383 已退出，64343 无监听，DMG 已卸载。IDEA Home 在关闭最后一个项目时自行创建 `~/IdeaProjects/IdeaHome`，内容仅本次生成的 `.idea`；退出后完整移动至 `cleanup/auto-created-IdeaHome` 留证，没有删除文件。58 个普通 profile/专用项目冻结文件全部未变，G4 生产 ZIP hash 仍为 `68852145f231627e93a3716d2fb47d6e96236dac2b634441c50872f72db510eb`。

本轮仅更新六份 MCP 相关文档；生产 Java、Gradle、descriptor、Java 21、`since-build=261`、既有 HTTP/UI 改动及 HEAD 未变。没有生产 Gradle/Verifier 重跑，没有提交、推送、发布、sync 或 archive。

提高最低版本是可行方向，但现在只证明 **IU 263.7031.23** 这一个实际包。正式升级需单独确定目标 build（不能用宽泛 `263` 覆盖未验证的更早包），同步 OpenSpec 兼容性 contract、构建依赖、最低版本及文档，完成构建/Verifier、现有功能原生回归和目标客户端取消互操作后，再推进正式只读搜索/详情工具。当前 261 的 task 2.4 保持失败/未完成，不因 263 前向成功自动关闭。

本轮最终检查：`openspec validate mcp-java-feasibility --strict`、`git diff --check`、六份改动文档的 58 个相对文件链接核对通过；临时客户端 11/11 离线自测通过。494 文件冻结基线仅上述六份文档变化，58 个普通 profile/专用项目文件、G4 ZIP 和 HEAD 均未变，记录见 `final-preservation.json`。任务进度 12/14，未完成项为 2.4 与 4.4。


## 2026-10-23：重复资源验收与扩展点确认

证据目录 `/tmp/doc-view-mcp-263-followup-20261023/`。沿用已核对的 IU 263.7031.23 官方分发包、独立 profile 和 0.1.1 探针；新进程 PID 70136。安装 JAR SHA-256 再次读回为 `8dfecb3f7491fa722cd456331a26cb83c8e0f6de003e16cb369206756bc56d12`。未修改探针运行代码、宿主 SDK、生产基线或客户端配置。

### 使用的确实是 IDEA 自带 MCP 扩展

维护者提供的[官方扩展点表](https://plugins.jetbrains.com/docs/intellij/intellij-community-plugins-extension-point-list.html#comintellijmcpserver)已重新核对。页面当前标明 IntelliJ Platform 2026.2.3，源码链接固定在 262.10968.63；当前 263 实验另以实际 descriptor/API 为准。

| 项目 | 当前用途 |
| --- | --- |
| `com.intellij.mcpServer` | IDEA MCP 宿主插件 ID；测试插件 descriptor 声明对此插件的依赖。 |
| `com.intellij.mcpServer.mcpToolset` | echo 的 Java 方法工具组，测试 schema 与普通参数调用。 |
| `com.intellij.mcpServer.mcpToolsProvider` | 有界取消探针显式提供 McpTool，处理其 continuation/Job 生命周期。 |
| 其余扩展点 | 本 PoC 没有使用 elicitation、项目路径/过滤定制、依赖提供者、managed session 或状态栏扩展。官方表标为 Internal 的入口没有接入。 |

IDEA 负责 MCP Server、连接配置与协议传输，Doc View 测试插件向它注册工具。公开工具入口的存在不等于旧版本拥有完整取消路径；261 的失败与 263 SDK 0.15.0 的原生成功分别保留。Doc View 生产 descriptor 仍未注册正式 MCP 查询工具。

### 四轮调用与三次有效在途卸载

四轮均执行初始化、两个工具 schema、echo 正常/三种非法参数、5 秒正常任务、30 秒任务在约 1 秒时发显式取消，以及同会话 echo/DELETE。echo 输入断言合计 **16/16**，正常任务 **4/4**；四次服务端显式取消从 started 到 cleanup 分别约 **1.005 / 1.010 / 1.006 / 1.006 秒**。各 run 恰好一次终结与 cleanup，没有取消后 completed；重复通知均为迟到通知，不计作额外活动取消。原请求仍为空 HTTP 202，临时客户端仍标 TransportError，不被汇总为工具成功。证据为 `cycle-*/protocol/`、`protocol-summary.json` 与 `server-call-summary.json`。

| 样本 | 原生操作与日志 | 卸载后读回 |
| --- | --- | --- |
| cycle 1 | run 4 于 22:50:00.227 started；22:50:09.303 owner_disposed / cancelled，09.304 cleanup | delay 不可发现且调用拒绝，echo 可调用；12 类卸载，具名对象/类为 0。 |
| cycle 2 | 22:51:32.560 先正常 completed，22:51:38.244 才禁用 | 只计为空闲卸载样本，不计在途取消；已另补 cycle 4。 |
| cycle 3 | run 3 于 22:53:00.032 started；22:53:09.854 owner_disposed / cancelled，09.855 cleanup | delay 不可发现且调用拒绝，echo 可调用；12 类卸载，具名对象/类为 0。 |
| cycle 4 | run 3 于 22:54:18.044 started；22:54:29.197 owner_disposed，29.198 cancelled / cleanup | delay 不可发现且调用拒绝，echo 可调用；12 类卸载，具名对象/类为 0。 |

每轮重新启用后的下一轮 schema/调用成功，未通过重启 IDE 掩盖重复启停问题。所有有效在途禁用发生前都先核对服务端 started/owner_bound；客户端观察模式不发送取消通知。cycle 2 的操作时机无效证据原样保留在 `assessment.json`。

### 资源结论及边界

同一 PID 取得 10 份快照：暖机空闲基线、四轮取消后/卸载后，以及最终 service-off；30 次 Thread.print、GC.class_histogram、VM.classloaders 命令均 exit 0。采样脚本先核对进程可执行路径及 config/system/log/plugins 四个隔离路径，再采样；不在活跃延迟请求期间执行 GC。

- 插件启用时 12 个探针类、7 个具名 provider/tool/service/lambda 实例稳定；每次取消后 Run、探针 continuation wrapper 与 Run lambda 为 0。
- 四次禁用后具名对象及 loader 内探针类均为 0，统一 JVM 日志共 **48 条**目标类卸载记录，每次 12 类。没有本轮 `still referenced` 警告或 ERROR。
- 线程总数在 106–157 间波动，最终服务关闭样本为 124；采样无探针工作栈，卸载后带插件 ID 的 coroutine scope 消失。源码复核无自建线程/池/Timer/活跃 Run 静态表，不能将 IDE 通用线程数变化直接归为探针泄漏。
- 这些重复证据完成 task 4.4 的有限验收。没有 heap retained-path；本轮未观察到探针残留，因此无需为证明所有 IDEA/native 资源清零而扩大分析。长期压力、任意时序及整台 IDE 无泄漏不在已通过结论内。

只读复核报告为 `resource-audit/followup-observation.md` 和 `probe-resource-review.md`；原始文件位于 `root-evidence/`、`class-unload.log`。原生 UI 最终关闭服务后 64343 无监听，PID 70136 已退出，测试取消插件保存为禁用，DMG 已卸载。关闭时没有在途请求，不冒充再次完成在途服务关闭验证。

### 官方客户端互操作记录（截至 2026-10-23 资源续验）

本机与 bundled runtime 未发现可用官方 Python/TypeScript MCP SDK；未下载新依赖。对随 IDEA 提供的官方 Kotlin SDK 0.15.0 做了固定源码和实际字节码复核，并尝试最小 Java client 构造：所查 HTTP transport factory 的 ReconnectionOptions 构造器/default wrapper 对 javac 不可直接调用，编译未通过。没有添加 Kotlin glue、反射绕过或自写官方 SDK 替身；也没有运行官方客户端网络调用。失败限于本次所选 Java client 构造路径，不推导为 Java 服务端扩展不可行。

实际协商仍是 **2025-11-25**，按该版[取消规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/utilities/cancellation)及[传输规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)评估。SDK 源码显示本地 coroutine 取消会发送通知，HTTP 202 分支不产生工具成功结果；这只是静态行为证据，不代替目标客户端实时验收。细节、固定提交和编译失败保存于 `client-sdk/`。task 2.4 及正式版本升级回归保持未完成，不能借扩展点列表或资源门槛通过自动宣布正式 MCP 交付。

以上内容是 2026-10-23 资源续验时的记录。其后本轮获准临时安装官方 TypeScript SDK 并运行 Codex app-server；以下新证据更新客户端互操作状态，但不关闭 2.4。

资源续验阶段的最终检查（历史记录，早于下节 Codex 测试）：`openspec validate mcp-java-feasibility --strict`、`git diff --check`、四份文档的 58 个相对链接及对应标题锚点核对通过。该阶段 494 文件冻结基线仅 design、tasks、verification、roadmap 四份文档变化；58 个普通 profile/专用项目文件、生产基线文件、G4 ZIP 与 HEAD 均未变，见本轮 `final-preservation.json`。未运行生产 Gradle/Verifier，也未提交、推送、发布、sync 或 archive。当前进度 **13/14**，task 2.4 保持未完成。

## 2026-10-23：Codex turn interrupt 互操作

本轮使用仓库外隔离证据 `/tmp/doc-view-mcp-upgrade-20261023/codex/` 和 `/tmp/doc-view-mcp-upgrade-20261023/client-sdk/`。目标是已授权的 IDEA 263.7031.23 隔离 MCP Server；Codex 为 `0.163.0-alpha.11.2`。本节只记录真实客户端和服务端日志，不把 SDK 字节码、HTTP 202 或客户端 turn 状态推断为取消。

Codex 直接 app-server 调用三轮均将 turn 标记为 `interrupted`，后续同会话 echo 成功；其中 run 3 正常完成，run 4/5 在 23:37:41 客户端进程退出时取消，不能归因于先前的 turn interrupt。透明 wire 第一轮见 `codex/wire-run/gate-result.json`：run 6 / request id 5 在派发后约 160 ms 发起 turn interrupt，捕获到 0 个 `notifications/cancelled`；IDE 请求返回 HTTP 200、耗时约 30.7 秒，服务器日志为 `completed` 后 `cleanup`。第二轮见 `codex/delayed-run/gate-result.json`：确认服务端任务已启动并等待约两秒后中断，仍捕获到 0 个取消通知；run 11 / request id 5 约 30.4 秒后正常 `completed` / `cleanup`。两轮同一 Codex MCP 会话中的后续 echo 均成功。不能以关闭 IDE 或客户端、普通断流、Codex turn 显示 interrupted 作为 turn-interrupt 取消通过。

补充客户端证据来自官方 `@modelcontextprotocol/sdk` 1.32.1：三轮 AbortSignal 取消均收到空 HTTP 202；IDE 日志通过会话 hash 与时间关联到 run 8–10，每个任务均出现 `cancelled` / `request_cancelled` 后 `cleanup`，每轮同会话 echo 成功。SDK 将原 tools/call 的空 202 视为取消错误而非成功。此结果证明该官方 SDK 的协议取消路径工作，不证明 Codex app-server 的 turn interrupt 会传播取消；因此不改变本节 Codex gate 失败结论。

临时 SDK 包安装与 Codex app-server 的 `-c` 进程配置覆盖均得到用户授权，文件留在上述 `/tmp` 证据目录。普通 Codex 全局配置内容未读取；后续 hash 读回与先前基线不一致，来源未知，因此不能据此声称字节未变，也未尝试恢复。实际注册到 IDEA Server 的仍是隔离探针工具，不是 Doc View 产品工具；生产插件没有在本轮注册或改动。

产品边界已确认：正式 MCP 将作为同一个 Doc View 插件内的可选扩展注册到 IDEA 自带 MCP Server，并复用 UI 按钮使用的文档服务；独立 PoC 只是隔离探针。正式接口搜索/详情和未来显式生成/更新能力均未交付。生产平台基线仍为 IU 261；IU 263.7031.23 的条件升级及其构建、Verifier、原生回归由 [proposal](../intellij-263-baseline-upgrade/proposal.md) 和 [verification](../intellij-263-baseline-upgrade/verification.md) 单独跟踪。当前仍 **13/14**，task 2.4 保持未勾选。

## 2026-10-24：Codex `0.165.0-alpha.1` 候选三轮实测通过

在仓库外临时目录使用官方 Codex `0.165.0-alpha.1` app-server 和同版本 code-mode-host，连接已授权的隔离 IU `263.7031.23` MCP Server。发行归档读回匹配官方 checksum，运行二进制版本与 SHA 已记录；MCP initialize 实际协商 `2025-06-18`。本轮只开放两个虚构探针工具，没有读取项目源码。详细来源、逐轮原始证据和 ACK 校正说明见[取消验收说明](../../../docs/mcp-cancellation.md#候选客户端运行证据)。

| 轮次 | delay request ID | turn/interrupt RPC ACK | IDEA started→cancelled→cleanup | 后续同 session echo | 取消后观察 |
| --- | ---: | ---: | --- | --- | --- |
| 1 | 5 | 102.954 ms | 154 ms / 1 ms | request 6 成功 | 31.005 秒无迟到完成 |
| 2 | 7 | 103.796 ms | 73 ms / 1 ms | request 8 成功 | 31.005 秒无迟到完成 |
| 3 | 9 | 102.763 ms | 24 ms / 0 ms | request 10 成功 | 31.005 秒无迟到完成 |

三轮都在原请求 HTTP 流关闭后发送与 delay 请求 ID 匹配的 `notifications/cancelled`；IDEA 记录对应 run `cancelled(reason=request_cancelled)` 和一次 cleanup，Codex turn 为 `interrupted`。按 transcript 中匹配的 RPC id 读回三次空 result ACK，修正 runner 摘要等待顺序导致的 `null`；未把 HTTP 202 单独作为通过证据。三个工具调用均无迟到服务端完成，同一会话的 echo 正常工作。候选门槛通过，不代表日常桌面 Codex 已升级。

旧 `0.163.0-alpha.11.2` 失败按原环境保留：run 3 服务端正常完成；run 4/5 在客户端进程退出时取消，不归因于 turn interrupt。两轮透明 wire 分别在任务启动后约 160 ms 和 2 秒发送 interrupt，均未观察到取消通知，长任务约 30 秒后正常完成并 cleanup；后续 echo 成功。0.165 候选的实际三轮证据更新候选状态，不反写旧版本结果。

普通 Codex 全局配置内容没有读取。本轮观察到的当前 hash 与先前基线不同，来源未知；不声称字节未变、不归因，也未尝试恢复。测试使用临时 app-server 进程配置覆盖；这只说明没有主动读取或恢复普通配置，不能证明该配置此前或本轮期间保持 byte-identical。

据此 PoC task 2.4 已完成；资源边界仍只覆盖已有的有限 IU 263 探针场景。生产平台仍是 IU 261。用户已批准精确 `IU-263.7031.23` / JBR 25 生产基线升级，目前正在构建；本记录没有运行 Gradle、Plugin Verifier 或生产插件原生回归，也不声称升级通过。正式 MCP 工具仍未注册或实现，见 [doc-view-mcp-tools tasks](../doc-view-mcp-tools/tasks.md)；其 task 1.3 保持未勾选，等待生产基线与文档库最终验收。
