# Java MCP 探针

此工具仅做路线图第 12 项的隔离互操作实验，不是 Doc View 的正式 MCP 工具，也不会进入插件生产包。它不启动服务器、读取项目或改变 IDEA 设置。

先准备已安装的 IDEA 与已有 Java 21 JDK；不需要下载新的依赖。对字节码要求更高的前向 IDE，显式 `--java-home` 指定该 IDE 已有 JBR；核心插件的 Java 21 基线不变。输出目录必须是仓库外尚不存在的目录。

```sh
python3 tools/mcp-java-poc/run.py --help
python3 tools/mcp-java-poc/run.py --ide-home '/path/to/idea-home' --output-dir /tmp/doc-view-mcp-probe-261
```

macOS 可直接传 `.app` 或其 `Contents`。可选 `--java-home` 指定已有 JDK。每次运行选择新输出目录，避免覆盖旧证据。

`report.json` 记录精确 IDE/MCP/JDK 信息、源码与 jar hash、编译/运行结果和各阶段输出；`compile.log`、`run.log` 保留诊断。样例只有虚构字符串和整数，不使用真实账号、源码或凭据。

探针以 `-parameters` 编译 Java 方法，并通过平台的反射工具发现/schema 和 `McpTool.call` 调用；它使用平台实现类做诊断，不承诺这些实现类可成为生产 API。原始 Java String、平台 Text 文本返回和普通 Java DTO 分别记录，平台 Text 成功不能证明原始 String 或任意 DTO 可序列化。

真实 IDEA 扩展注册、MCP `tools/list`/`tools/call`、取消、项目结束与禁用仍是独立门禁。独立 JVM 全绿不能宣布 IDEA MCP 集成成功。实际结果见 [OpenSpec 验证记录](../../openspec/changes/mcp-java-feasibility/verification.md)。

## 隔离测试插件包

`build_test_extension.py` 会把一个只返回虚构 Markdown 的 Java `McpToolset` 编译成独立插件 ZIP，并在输出目录创建空 config/system/log/plugins 目录及 `idea.properties`。它需要显式 IDE 路径，读取该 IDE 内现成的 `mcpserver.jar`；默认选择已有 Java 21，前向版本可通过 `--java-home` 指向 IDE 自带 JBR。编译目标固定为 Java 21。脚本不安装插件、不启动 IDE、不注册服务、不调用 MCP，也不更改仓库主 Gradle、生产 descriptor、依赖或平台基线。

```sh
python3 tools/mcp-java-poc/build_test_extension.py \
  --ide-home '/path/to/IntelliJ IDEA.app/Contents' \
  --output-dir /tmp/doc-view-mcp-extension-build
```

输出包含 `doc-view-mcp-java-poc.zip`、`build-report.json`、`compile.log` 和空隔离 profile。两次独立构建的 ZIP hash 相同才说明制品打包可复现；它不证明安装、扩展发现或 MCP 协议调用。未来原生测试必须使用该 profile 的 `idea.properties` 与相同目标 build，普通 IDEA profile 保持不变。

## 真实 IDE 协议验收步骤与授权边界

以下是后续验收步骤，不表示已安装或启用服务。只在隔离 profile 中安装此 ZIP，并只打开虚构临时项目；不要复用普通 IDEA profile 或自动写入现有 MCP 客户端配置。

1. 在隔离 IDEA 的 **Settings | Tools | MCP Server** 中启用服务。IDE 会显示权限同意对话框，说明第三方客户端可访问 IDE 打开的项目。由用户本人审阅并决定是否点击 **Enable**；拒绝或无法确认时停止，不使用内部 API、配置覆盖或其他方式绕过该确认。随后 Apply。
2. 保持 **Brave Mode** 关闭。在 **Exposed Tools** 中只开放 `doc_view_java_poc_describe_demo`，禁用其他工具。使用 **Copy HTTP Stream Config** 取得该隔离实例的连接配置，交给本机临时 MCP 客户端；不要猜测 URL 或自动配置其他客户端。
3. 客户端连接后运行 `tools/list`，核对工具名以及 `query`（string）、`limit`（integer）参数；用 `{"query":"fictional-api","limit":2}` 调用，预期文本为 `# fictional-api\nlimit=2`。再用 `limit=0` 检查错误输入。输入始终使用虚构值。
4. 当前 echo 立即返回，不能证明中途取消。取消验收需另有一个有界、可观察且协作响应取消的测试调用，再由 MCP 客户端在请求进行中发起取消；不要把断开连接或关闭 IDE 当作请求取消测试。协议规定取消是尽力协作，已结束或不可取消的请求可能忽略通知，详见 [MCP Cancellation](https://modelcontextprotocol.io/specification/2025-11-25/basic/utilities/cancellation)。
5. 分别验证正常关闭客户端、关闭虚构项目、关闭 MCP Server：确认客户端会话/活动连接清理；关闭项目不等于关闭应用级 MCP Server；最后在隔离实例设置中关闭 MCP Server 并 Apply，确认状态为 Disabled 且客户端无法再连接。若需验证禁用测试插件，则单独禁用并按 IDE 提示重启，再确认 POC 工具不再出现。

启用服务的同意范围涉及 IDE 打开的项目，因此“隔离 profile + 仅虚构项目”是前置条件，不能代替用户同意。终端命令和运行配置可能有逐请求确认；不要为测试关闭该确认。参见 [JetBrains MCP Server 文档](https://www.jetbrains.com/help/idea/mcp-server.html)。
