# Proposal

## Why

路线图希望把文档查询接入 IDEA 自带 MCP，但当前生产插件没有 MCP 依赖或工具注册。已安装的 IU 261.22158.277 确有 `mcpToolset` 和 `mcpToolsProvider` 扩展点；这只能证明入口存在，不能证明 Java 方法的 schema、参数反射、序列化或真实 MCP 调用可用。先用隔离 PoC 检查这些前置条件，再决定是否启动正式只读文档工具。

## What Changes

- 建立独立、Java-only 的 MCP 探针，使用明确指定 IDE 的捆绑 jar；不接入 Doc View 生产包，不修改核心平台基线或下载新运行时依赖。
- 验证带注解的 Java 普通方法、具名 String/int 参数、String 结果，以及无 Kotlin serialization 元数据的 Java DTO 边界；记录编译、schema 与实际反射调用分别成功或失败。
- 定义真实隔离 IDE 的注册、MCP 请求、取消、禁用和项目生命周期验收；缺少可操作环境时保持这些项未验证。
- 固定 IU 261.22158.277，并把本机 IU 262.10968.63 作为单独前向实验，不据一次实验升级正式支持范围。
- 只有全部必要门禁通过才进入第 13 项；失败或未验证时可继续不依赖 MCP 的 Wiki/文档工作。

## Capabilities

### New Capabilities

- `mcp-extension-contract`：Java MCP 扩展的可复现实验、输入/输出/取消边界及生产集成准入证据。

### Modified Capabilities

无。

## Impact

只新增隔离 PoC 工具源码/运行说明和 OpenSpec 验证记录。生产 `src/main`、Gradle 核心依赖、`plugin.xml` 和用户 MCP 设置保持原样。PoC 只处理虚构字符串，不扫描用户项目、访问账户或执行网络写入；真实服务验证仅允许独立本机测试实例。
