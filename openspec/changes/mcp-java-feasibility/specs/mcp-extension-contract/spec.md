# Spec Delta

## Purpose

定义 Java 文档工具进入 IntelliJ MCP 服务前所需的可复现输入输出与平台实验记录。该能力先用于隔离探针和准入验收，不把反射调用、入口存在或一次编译成功解释为生产插件已经提供 MCP 服务。

## ADDED Requirements

### Requirement: 实验明确绑定平台与隔离范围

PoC SHALL 记录所用 IDE 的精确 build、Java 版本、MCP 捆绑组件和探针来源；MUST 不修改普通 IDEA 配置、Doc View 生产 descriptor/依赖或真实项目文档。

#### Scenario: 在本地运行探针
- **WHEN** 操作者指定已安装的目标 IDE 与独立输出目录
- **THEN** 探针使用该 IDE 的 jar 进行 Java 编译与运行并记录结果
- **AND** 编译产物与日志位于独立输出目录，生产插件未包含探针

#### Scenario: 平台或依赖缺失
- **WHEN** 指定位置不含可用 MCP 组件或 Java 编译器
- **THEN** 报告明确失败或未验证原因
- **AND** 不自动提高插件平台基线或安装新的语言运行时

### Requirement: Java 工具参数与结果必须经过实际调用

PoC SHALL 分别记录 Java 普通方法的发现、输入 schema、具名参数绑定及输出；输入至少包含字符串与整数，并以虚构 Markdown 字符串作为受支持结果。

#### Scenario: 简单 Java 工具
- **WHEN** 通过平台工具调用链传入有效字符串与整数参数
- **THEN** 返回预期虚构 Markdown，参数名与 schema 一致
- **AND** 不是直接调用 Java 方法后伪装成平台调用证据

#### Scenario: 缺失或错误类型参数
- **WHEN** 调用缺少必填参数或给整数参数传入非法文本
- **THEN** 平台调用拒绝该输入并产生可记录错误
- **AND** 工具不返回成功正文

#### Scenario: 普通 Java DTO 边界
- **WHEN** 实验使用不含序列化生成代码的 Java DTO 作为结果
- **THEN** 记录平台实际接受或拒绝结果
- **AND** DTO 不支持时只批准 String 结果范围，不据此增加 Kotlin 生产源码或序列化兼容层

### Requirement: 真实服务和生命周期验收单独记录

生产集成准入 SHALL 要求隔离 IDE 实际发现并调用工具，且可取消、禁用与安全结束项目；独立 JVM 的 schema/反射调用不能替代此验收。

#### Scenario: 隔离服务注册和调用
- **WHEN** 测试扩展在指定 IDEA 中启用，测试客户端请求工具列表并调用虚构工具
- **THEN** 工具出现在服务工具列表并返回预期结果
- **AND** 记录精确 IDE build、扩展包身份与本机请求结果

#### Scenario: 取消与项目结束
- **WHEN** 调用被取消或所属测试项目结束
- **THEN** 任务终止且不再发送成功结果或保留项目资源

#### Scenario: 禁用扩展
- **WHEN** 在隔离配置中禁用测试扩展或 MCP 组件
- **THEN** 对应工具不可再调用且普通 Doc View 不受影响

### Requirement: 未通过准入不得宣布生产 MCP 能力

PoC SHALL 按编译、schema/调用、服务注册、取消和生命周期分别报告通过、失败、未验证；存在必要失败或未验证时 MUST 保持正式 MCP 工具实现未开始。

#### Scenario: 原生或服务环境不可用
- **WHEN** 只能完成独立 JVM 探针，无法执行真实隔离服务验证
- **THEN** 报告只声明已证实的 Java 边界，保留服务验证未完成
- **AND** 后续不依赖 MCP 的仓库文档或 Wiki 工作可继续
