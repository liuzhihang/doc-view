# Spec Delta

## Purpose

定义从 Java PSI 构建 `DocView` 和 Markdown 时可复现的性能测量、协作式取消与输出一致性边界，使性能优化基于真实工作负载与数据，而不是预设收益。

## ADDED Requirements

### Requirement: 性能优化前必须记录可比较基线
性能优化 MUST 在固定 Java PSI 工作负载上分别记录 `DocView` 构建、Markdown 渲染和目录扫描的耗时分布，并记录 IDE/JBR、工作负载规模和重复次数；生成与扫描指标不得合并，未测量前不得声称性能提升。

#### Scenario: 评估整类预览性能
- **WHEN** 维护者对整类 Spring 或 Dubbo 预览路径进行性能优化
- **THEN** 基线分别记录整类 `DocView` 构建与 Markdown 渲染的测量结果
- **AND** 冷缓存与 PSI/settings/template 稳定后的热缓存结果分开报告
- **AND** 报告注明 fixture 的方法数、DTO 字段数与嵌套深度，以及 IDE/JBR、运行次数和 p50/p95

#### Scenario: 评估 Tool Window 目录刷新
- **WHEN** 维护者对模块内接口目录刷新进行性能优化
- **THEN** 基线单独记录刷新时间、module/class/method 数量和普通接口扫描设置
- **AND** 目录扫描结果不与 `DocView` 构建或 Markdown 渲染耗时合并

#### Scenario: 基线环境或工作负载不可复现
- **WHEN** 缺少固定 fixture、环境信息或测量结果
- **THEN** 该变更只能报告为待测或候选分析
- **AND** 不得报告性能已提升，也不得以未经测量的假设扩展缓存

#### Scenario: 观察多次编辑后的 schema 缓存保留
- **WHEN** 维护者评估重复 PSI、设置或模板变更后的 schema cache
- **THEN** 报告记录当前 key epoch、缓存条目数和缓存 DTO 节点规模
- **AND** 每次变更后的重新生成结果与只保留当前 epoch 的条目基线分开比较

### Requirement: 过期 schema 快照不得随版本变化持续累积
项目级 schema cache MUST 在观察到新的 PSI、生成设置或模板版本后释放旧版本快照，并 MUST 拒绝迟到任务重新写入旧版本快照。

#### Scenario: 反复修改后重新生成同一 schema
- **WHEN** 用户多次修改固定 fixture 的 PSI、生成设置或模板，并在每次变更后重新生成同一 schema
- **THEN** cache 只保留当前版本可用的 snapshot entries
- **AND** 旧版本任务迟到完成时不重新增加旧版本条目
- **AND** 当前版本生成的 `DocView` 与 Markdown 内容保持不变

### Requirement: 生成任务必须协作响应取消
整类方法构建、递归 DTO 字段解析和模块目录扫描 MUST 在可取消的工作边界检查当前任务取消状态；取消的工作 MUST 丢弃部分结果，不得发布不完整的 `DocView` 列表、Markdown 或目录树。

#### Scenario: 用户在生成或目录扫描期间取消
- **WHEN** 用户在多个接口方法、DTO 字段或模块条目仍在处理时取消当前任务
- **THEN** 工作在后续方法、字段或模块遍历边界停止
- **AND** 当前请求不发布部分构建结果或目录
- **AND** 已发布的其他预览请求状态不被该取消覆盖

#### Scenario: 取消发生在目录类节点的方法遍历中
- **WHEN** Tool Window 正在为一个候选类逐个筛选并构造方法节点，且任务在首个匹配方法的筛选或节点构造期间被取消
- **THEN** 取消异常向外传播，后续方法不得继续筛选或构造
- **AND** 当前类或目录快照中的部分节点不得发布
- **AND** 不要求被中断的 `MethodNode` 构造必须完成或保留在局部节点列表中

### Requirement: 性能优化保持文档输出一致
对于生成名称确定的相同 Java PSI、项目设置和模板，性能优化前后的生成结果 MUST 保持一致，包括 `DocView` 字段、完整 Markdown 正文和接口目录成员。

#### Scenario: 对比优化前后生成结果
- **WHEN** 不含重复业务名称的 Spring 或 Dubbo fixture 使用相同设置和模板分别运行优化前后的生成链路
- **THEN** 生成的 `DocView` 字段和 Markdown golden 完全相同
- **AND** 冷/热缓存状态不改变输出内容

#### Scenario: 对比优化前后目录成员
- **WHEN** 相同项目 PSI 和扫描设置分别运行优化前后的目录扫描
- **THEN** module/class/method 成员与可见顺序保持一致
