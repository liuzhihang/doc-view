## Purpose

Doc View keeps UI-facing PSI scanning, DTO schema parsing, Markdown rendering, export preparation, and upload preparation responsive by moving heavy work off the EDT and bounding recursive schema generation.

## Requirements

### Requirement: 启动阶段避免全项目扫描

Doc View SHALL NOT 在插件启动期间扫描全项目、构建全部 endpoint DTO、预渲染 Markdown 或预热 DTO schema cache。

#### Scenario: 项目打开

- **WHEN** IntelliJ 打开一个安装了 Doc View 的项目
- **THEN** Doc View 只执行轻量 startup work，例如读取 settings 和检查 notification eligibility
- **AND** 在用户触发相关 action 前，不运行 Spring、Dubbo、DTO schema、Markdown、export 或 upload scan

#### Scenario: Tool window 创建

- **WHEN** Doc View tool window content 被创建
- **THEN** catalog discovery 或 endpoint scan 被延迟到显式 refresh/open workflow，或运行在可取消的后台任务中
- **AND** EDT 仅负责创建和更新 UI component

### Requirement: 重型扫描和渲染工作运行在 EDT 外

Doc View SHALL 在 EDT 外执行重型 PSI scanning、DTO body/schema construction、example generation、batch export preparation、upload payload preparation 和 Markdown rendering。

#### Scenario: 打开 Preview

- **WHEN** 用户为 Spring、Feign 风格、Dubbo、class 或 method target 打开 preview
- **THEN** Doc View 在后台 read action 或等效 IntelliJ background workflow 下构建 `DocView` 数据
- **AND** EDT 只显示 loading/progress 状态并应用最终选中的结果

#### Scenario: 请求批量导出或上传

- **WHEN** 用户从 tool window 或 preview action 导出或上传多个 API
- **THEN** Doc View 在可取消的后台任务中执行 scan、DTO construction、Markdown rendering 和 payload preparation
- **AND** cancellation 或 project disposal 会阻止 stale result 像成功任务一样更新 UI 或发送通知

#### Scenario: Action update 被调用

- **WHEN** IntelliJ 调用 Doc View action `update`
- **THEN** action update logic 保持轻量，并且 SHALL NOT 执行递归 DTO parsing、Markdown rendering、network access、export writes 或 whole-project scanning

### Requirement: DTO schema cache 具备明确生命周期和失效策略

Doc View SHALL 提供 project-scoped DTO schema cache，用于复用派生 schema 数据，并明确 key、生命周期和失效规则。

#### Scenario: 复用缓存 Schema

- **WHEN** 同一 project、framework、class 或 method signature、schema role、相关 settings、template version 和 PSI modification stamp 请求同一个 DTO schema
- **THEN** Doc View 可以复用缓存的派生 schema 数据，而不是重复执行等价 PSI traversal

#### Scenario: PSI 或 Settings 变化

- **WHEN** source PSI、excluded parameter settings、required annotation settings、template settings、framework detection settings 或其他影响 schema 的配置发生变化
- **THEN** Doc View 失效受影响的 cache entry，或保守清理 project schema cache 后再提供新结果

#### Scenario: 用户清理 Tool Window 缓存

- **WHEN** 用户触发 Doc View tool window/catalog context 中可见的 clear-cache action
- **THEN** Doc View 清理当前 project 的用户可感知 catalog/cache state 和 DTO schema cache entry

#### Scenario: 项目关闭

- **WHEN** project 被 dispose
- **THEN** Doc View 释放 schema cache entry，并且不保留长期 PSI element 引用

### Requirement: DTO 递归解析有边界

Doc View SHALL 保护 DTO request 和 response body parsing，避免 recursive type cycle 和过深 nesting depth。

#### Scenario: DTO 包含直接循环

- **WHEN** DTO field expansion 遇到当前 expansion path 中已经存在的 type
- **THEN** Doc View 停止展开该 recursive child path
- **AND** scan 可以完成，不发生 stack overflow、infinite loop 或 IDE freeze

#### Scenario: DTO 超过最大 Schema 深度

- **WHEN** DTO expansion 达到配置或文档化的 maximum schema depth
- **THEN** Doc View 保留当前 field metadata，但不继续展开更深 children
- **AND** Markdown generation、export 和 upload preparation 使用有界输出继续执行

#### Scenario: DTO 包含嵌套 Collections 和 Maps

- **WHEN** DTO expansion 遍历 nested arrays、collections、maps 和 generic substitutions
- **THEN** cycle detection 和 depth counting 应用于有效 element/value type，而不仅是 immediate field type text

### Requirement: Markdown 渲染延迟且稳定

Doc View SHALL 在 rendered Markdown 未被立即需要时避免 eager Markdown rendering，同时在相同 `DocView` 和 template 输入下保持生成 Markdown contract 稳定。

#### Scenario: Preview 数据加载完成

- **WHEN** 后台解析为包含多个 `DocView` entry 的 preview 完成
- **THEN** Doc View 可以将 Markdown rendering 延迟到用户选择或请求具体 entry 时
- **AND** 选中 entry 的渲染结果与等价输入下 eager rendering path 产生的 Markdown 文本一致

#### Scenario: Template 或 Schema 数据变化

- **WHEN** template settings、schema-affecting settings 或 source PSI 在 Markdown memoized 后发生变化
- **THEN** Doc View 在 copy、export、upload 或 preview display 使用前失效 stale rendered Markdown

### Requirement: 性能验证已文档化

Doc View SHALL 文档化 startup、EDT responsiveness、background scanning、cache invalidation、recursion limits、lazy rendering、export/upload preparation 和 cancellation 的人工验证步骤。

#### Scenario: 维护者查看性能指南

- **WHEN** 维护者在本变更后打开 `docs/performance-guide.md`
- **THEN** 指南描述 background-task boundary、schema cache key 和 invalidation strategy、recursion/depth protections、lazy rendering expectations，以及 manual IDE validation steps
