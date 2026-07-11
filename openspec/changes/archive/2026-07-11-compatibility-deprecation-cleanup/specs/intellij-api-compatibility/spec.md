## ADDED Requirements

### Requirement: 配置目标不得包含 deprecated API usage
系统 SHALL 在配置的 IntelliJ IDEA 验证矩阵上不使用 Plugin Verifier 标记为 deprecated 的 API，并 SHALL 将 deprecated API usage 配置为 `verifyPlugin` 的失败条件。

#### Scenario: 目标 IDE 兼容性验证
- **WHEN** 对构建产物执行配置矩阵的 `verifyPlugin`
- **THEN** 验证通过且报告不包含 deprecated API usage
- **AND** compatibility problem 和 internal API usage 仍按发布门禁处理

#### Scenario: 后续重新引入 deprecated API
- **WHEN** 插件生产代码重新调用目标 IDE 标记为 deprecated 的 API
- **THEN** `verifyPlugin` 失败并指出对应 usage

### Requirement: 模块注解扫描行为保持稳定
系统 SHALL 使用目标平台公开的注解索引 API 查找 Spring、Feign 和 Dubbo 候选项，并 MUST 保持既有 short name、project、module scope 与 FQN 业务过滤语义。

#### Scenario: Spring Controller 扫描
- **WHEN** 当前 module 同时包含有效的 `@Controller` 和 `@RestController` 类，并包含其他包下的同短名注解噪声
- **THEN** Spring 扫描结果包含两个有效类
- **AND** 不包含错误包注解的类或其他 module 的类

#### Scenario: Feign 扫描
- **WHEN** 当前 module 包含有效的 `@FeignClient` 接口和其他包下的同短名注解噪声
- **THEN** Feign 扫描结果仅包含有效接口

#### Scenario: Dubbo 别名扫描
- **WHEN** 当前 module 使用既有 Dubbo `Service` 或 `DubboService` 注解别名声明接口
- **THEN** Dubbo 扫描结果包含所有有效接口
- **AND** 不因首个索引结果为空而遗漏后续别名结果

#### Scenario: Dumb Mode 边界
- **WHEN** 调用方在索引不可用的 Dumb Mode 中触发模块扫描
- **THEN** 系统保持平台既有索引异常语义
- **AND** 本 change 不把索引暂不可用降级为空目录

### Requirement: Tool window 数据上下文保持稳定
系统 SHALL 通过公开 `UiDataProvider` 数据快照机制提供 tool window 数据，并 SHALL 保留父类数据贡献和现有自定义 `DataKey` 对象 identity。

#### Scenario: Tool window action 读取数据
- **WHEN** tool window 的刷新、展开/折叠、目录、导出或上传 action 读取数据上下文
- **THEN** `WINDOW_PANE` 返回当前 panel
- **AND** `WINDOW_ROOT_NODE` 返回当前 root node
- **AND** `WINDOW_CATALOG_TREE` 返回当前 catalog tree
- **AND** `WINDOW_TOOLBAR` 返回当前 toolbar component

#### Scenario: 父类 Quick Action 数据
- **WHEN** IntelliJ Platform 从 panel 创建 UI data snapshot
- **THEN** 父类 `SimpleToolWindowPanel` 的数据贡献仍然存在

### Requirement: 目录树速度搜索行为保持稳定
系统 SHALL 通过公开平台 API 为目录树安装键盘速度搜索，并 MUST 保持默认节点文本匹配和不自动展开节点的既有行为。

#### Scenario: 键盘定位目录节点
- **WHEN** 用户聚焦目录树并输入唯一节点文本
- **THEN** 选择移动到匹配节点
- **AND** 搜索不会额外展开未展开节点

### Requirement: 重复文档名称格式保持稳定
系统 SHALL 在整类文档构建时保持既有重复名称处理：同名首项保留原名，后续同名项追加下划线、五个 ASCII 字母和该项在完整列表中的零基索引；非重复名称保持不变。

#### Scenario: 两个同名文档
- **WHEN** 整类构建结果的索引 0 和索引 1 具有相同原始名称 `查询用户`
- **THEN** 索引 0 的名称仍为 `查询用户`
- **AND** 索引 1 的名称匹配 `查询用户_[A-Za-z]{5}1`

#### Scenario: 非重复文档名称
- **WHEN** 整类构建结果中的名称只出现一次
- **THEN** 该名称保持不变

#### Scenario: 单方法构建
- **WHEN** 调用方只构建一个指定方法的文档
- **THEN** 系统不执行整类重复名称处理

### Requirement: 正式矩阵与前向验证分离
系统 SHALL 以仓库中固定的 `pluginVerifierIdeVersions` 作为声明验证矩阵，并 MUST NOT 因一次性预发布 IDE 验证而扩大正式支持声明。

#### Scenario: 精确 build 前向验证
- **WHEN** 维护者通过命令行属性对额外的精确 IDE build 执行 Plugin Verifier
- **THEN** 结果记录为前向兼容证据
- **AND** 仓库中的正式验证矩阵和插件兼容范围保持不变
