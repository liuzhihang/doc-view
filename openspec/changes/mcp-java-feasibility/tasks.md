# Tasks

## 1. 隔离 Java 探针

- [x] 1.1 新增 Java 工具与启动脚本/README：通过显式 IDE 路径、Java 21 编译、`-parameters`、独立输出目录执行；运行 `python3 tools/mcp-java-poc/run.py --help`，核对不修改核心 Gradle/descriptor、不下载依赖。
- [x] 1.2 在 IU 261.22158.277 对应目录运行 `python3 tools/mcp-java-poc/run.py --ide-home <261-home> --output-dir <temp-261>`；记录精确 build、真实工具 schema 与 String/int 成功调用、缺参数/类型错误负例、普通 Java DTO 边界，保存报告并在 README 解释各阶段结果。
- [x] 1.3 对本机 IU 262.10968.63 单独运行同一命令并保存前向结果；缺 JDK/组件则标未验证，不更改生产平台或追加语言适配；执行 `git diff --check`。

## 2. 隔离包与真实 IDE 服务

- [x] 2.1 准备只处理虚构字符串的 Java `McpToolset` 测试扩展、显式 IDE/JDK 构建脚本及空隔离 profile；以 `--release 21 -parameters` 编译并验证 descriptor/包内容。对 IU 261.22158.277 与 262.10968.63 分别构建，重复构建 hash 一致；不安装、不改普通 profile、不启动服务。
- [x] 2.2 在指定隔离实例安装并记录实际插件身份/加载日志；确认普通 profile 未改变。2026-10-23 在 IU 261.22158.277 的独立 profile 加载 0.1.0，原生 Exposed Tools 发现测试工具；普通 profile/专用项目的 58 个冻结文件未变，未写入现有客户端配置。证据范围见 verification，不把此项扩大为协议调用通过。
- [x] 2.3 通过隔离实例本机 MCP 服务执行 `tools/list`、正常和非法 `tools/call`；验证工具可发现、参数名/结果正确，并记录请求结果而非仅反射调用。2026-10-23 获维护者明确授权后，IU 261 的 Streamable HTTP 初始化、echo schema、正常调用及三种非法参数拒绝均通过；会话 DELETE 成功。后续在隔离 263 IDEA MCP Server 上通过 Codex app-server 发现并调用测试 echo 与有界延迟探针，证明 IDEA Server 与 Codex 工具调用链可连接；不代表正式查询工具已实现或取消互操作通过。证据见 verification。
- [x] 2.4 在隔离实例验证长任务取消、项目结束、测试扩展/MCP 禁用后不可调用及有限资源释放。IU 263.7031.23 / SDK 0.15.0 的修复探针 0.1.1 已通过显式取消、三次有效在途插件卸载、在途服务关闭和项目关闭；四次卸载后探针对象/类为零并记录类卸载，不扩展为全 IDE 或长期无泄漏结论。官方 Codex `0.165.0-alpha.1` 候选随后在同一精确隔离 IU build 上按 `2025-06-18` 协议完成三轮 turn-interrupt 取消、cleanup、无迟到完成和同会话 echo。Codex `0.163.0-alpha.11.2` 的两轮透明 wire 失败继续保留；IU 261.22158.277 的历史取消失败也不覆盖。本项仅关闭 PoC 及候选互操作范围；生产仍为 261，精确基线升级的构建、Verifier、原生回归单独跟踪在 [proposal](../intellij-263-baseline-upgrade/proposal.md) 和 [verification](../intellij-263-baseline-upgrade/verification.md)。

## 3. 准入结论

- [x] 3.1 更新 verification 与路线图，将编译、schema/调用、真实服务、取消/生命周期分开；必要项未验则第 13 项不开始；运行 `openspec validate mcp-java-feasibility --strict` 和 `git diff --check`。
- [x] 3.2 复核生产包未包含 PoC、生产 descriptor/依赖/平台没有因本项改变，记录文件范围和运行命令；不提交、发布、sync 或 archive。round21 production package audit 已保存。

## 4. 263 隔离前向实验（不改生产基线）

- [x] 4.1 核对官方完整 macOS ARM64 安装包 checksum/签名、product build、MCP descriptor 和实际 SDK 取消实现；准备新隔离 profile、虚构项目和有界探针，记录包身份。已验 IU 263.7031.23 官方完整包/签名/checksum、SDK 0.15.0 实际字节码；Java 21 探针仅在仓库外适配 263 API。
- [x] 4.2 原生执行正常请求、显式协议取消和重复取消；核对服务端终止/cleanup、无迟到成功及会话可继续调用。客户端只收到 202 不算通过。修复版服务端 cancelled/cleanup、无迟到 completed、同会话 echo 均已取证；重复通知为迟到取消。原 tools/call 空 202 仍是客户端互操作限制，不计为严格传输一致性通过。
- [x] 4.3 在任务运行中分别禁用测试扩展、关闭 MCP 服务和关闭项目；核对终止、不可调用及恢复，发现探针或客户端缺陷先修复再复验。0.1.0 loader 残留与事件误标保留；0.1.1 改为插件自有 project service，三个原生场景已复验，工具最终移除、服务恢复与项目关闭后路由拒绝均有证据。
- [x] 4.4 比较空闲、任务结束、卸载后对象/线程/类加载器证据及监听端口；如无法取得完整释放证据则保留缺口，不把单次 histogram 当作全量无泄漏证明。263.7031.23 / 探针 0.1.1 同一 PID 已补 10 份资源快照、四次显式取消及三次有效在途卸载（另一次为空闲样本）：取消后无具名 Run/wrapper，四次卸载后对象/类均为 0，每次 JVM 卸载 12 类，重新启用恢复工具，最终服务/端口/进程关闭。仅完成本组有限场景，不宣称全 IDE 或长期无泄漏；未发现需进一步 retained-path 定位的探针残留。
- [x] 4.5 更新版本矩阵和准入结论，关闭本轮服务/实例，复核普通 profile、生产文件与包不变；运行 strict/diff/link 检查，不自动提高最低版本或归档。已关闭服务/实例并卸载 DMG，58 个冻结文件、G4 包与生产文件未变；实际检查结果见 verification。
