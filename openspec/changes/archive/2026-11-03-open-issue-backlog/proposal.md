# Proposal

## Why

公开仓库仍有 #89、#90、#91、#96、#97、#101 六项未关闭反馈。用户要求支持得了的修复，确实不能支持的说明原因关闭，并先分析、实现验证、最后归档。本轮在与远端 develop/master 一致的 7b2e0a8 独立副本工作，保留原工作区所有未提交改动。

## What Changes

- 接口和实体字段的 `@since`/`@version` 进入默认文档与模板变量（#89）。
- Spring 路由数组及 `path` 别名读取第一个映射；复验既有模板持久化修复（#90）。
- 用原截图的 Dubbo 多参数场景复验 `@param`，修复确认存在的解析缺陷（#91）。
- Markdown 隐藏集合、Map 的合成中间节点，保留真实字段和 JSON 语义（#96）。
- 独立实体编辑器复制当前编辑值为 Markdown 表格（#97）。
- 已关联 sources 的依赖字段从源码读取注释；无源码正常降级（#101）。
- 本轮六项均有可支持路径，不能把缺少源码这一输入限制等同于整项不可支持；修复未合入/发布前不关闭为完成。

## Capabilities

### New Capabilities
- `java-documentation-metadata`: Java 文档标签、参数描述与依赖源码注释的输出。
- `spring-route-selection`: Spring 多路径注解与别名的单文档路径选择。
- `body-markdown-tables`: 对象字段 Markdown 层级与独立实体复制行为。

### Modified Capabilities
无。既有 markdown-generation-contract 是基础 fixture 的验证基线，不扩大其历史承诺。

## Impact

影响 Java PSI 工具、DocView/模板数据、默认 Velocity 模板、实体编辑器、中文/英文文案与定向测试。保持 Java 21、IU 2026.1、现有生产依赖与 extension 不变。不自动下载依赖源码，不调用外部上传服务，不合并或发布。通过 PSI RED/GREEN、全量测试、构建、Verifier 和受影响 UI 烟测记录证据。
