# Tasks

## 1. 隔离 Java 探针

- [x] 1.1 新增 Java 工具与启动脚本/README：通过显式 IDE 路径、Java 21 编译、`-parameters`、独立输出目录执行；运行 `python3 tools/mcp-java-poc/run.py --help`，核对不修改核心 Gradle/descriptor、不下载依赖。
- [x] 1.2 在 IU 261.22158.277 对应目录运行 `python3 tools/mcp-java-poc/run.py --ide-home <261-home> --output-dir <temp-261>`；记录精确 build、真实工具 schema 与 String/int 成功调用、缺参数/类型错误负例、普通 Java DTO 边界，保存报告并在 README 解释各阶段结果。
- [x] 1.3 对本机 IU 262.10968.63 单独运行同一命令并保存前向结果；缺 JDK/组件则标未验证，不更改生产平台或追加语言适配；执行 `git diff --check`。

## 2. 隔离包与真实 IDE 服务

- [x] 2.1 准备只处理虚构字符串的 Java `McpToolset` 测试扩展、显式 IDE/JDK 构建脚本及空隔离 profile；以 `--release 21 -parameters` 编译并验证 descriptor/包内容。对 IU 261.22158.277 与 262.10968.63 分别构建，重复构建 hash 一致；不安装、不改普通 profile、不启动服务。
- [ ] 2.2 在指定隔离实例安装并记录实际插件身份/加载日志；确认普通 profile 未改变。未完成前不得以包构建结果替代安装验收。
- [ ] 2.3 通过隔离实例本机 MCP 服务执行 `tools/list`、正常和非法 `tools/call`；验证工具可发现、参数名/结果正确，并记录请求结果而非仅反射调用。
- [ ] 2.4 在隔离实例验证长任务取消、项目结束、测试扩展/MCP 禁用后不可调用及无资源残留；记录执行步骤和实际结果，无法操作环境时保持未完成。

## 3. 准入结论

- [x] 3.1 更新 verification 与路线图，将编译、schema/调用、真实服务、取消/生命周期分开；必要项未验则第 13 项不开始；运行 `openspec validate mcp-java-feasibility --strict` 和 `git diff --check`。
- [x] 3.2 复核生产包未包含 PoC、生产 descriptor/依赖/平台没有因本项改变，记录文件范围和运行命令；不提交、发布、sync 或 archive。round21 production package audit 已保存。
