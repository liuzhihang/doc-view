# collection-independent-rendering Specification

## Purpose
保证文档生成与 YApi 集合处理不依赖 IDE 提供 Commons Collections，并保持既有空值和输出语义。
## Requirements
### Requirement: 集合处理不依赖 IDE 的 Commons Collections
插件 SHALL 在 IDE 不提供 Commons Collections 时仍能执行文档和 YApi 的集合判空处理，且不改变既有 Markdown、模板变量及 payload 内容。

#### Scenario: null 和空集合
- **WHEN** 文档参数或 YApi 参数、Header 集合为 null 或空列表
- **THEN** 系统 SHALL 保留既有空文本或空列表输出，不因集合判空抛出异常

#### Scenario: 非空集合
- **WHEN** 生成包含参数和返回字段的文档或构造 YApi payload
- **THEN** 系统 SHALL 保持既有字段、顺序和嵌套结构，不加载 Commons Collections

#### Scenario: 未涉及的边界
- **WHEN** 用户使用自定义模板或遇到无效 PSI、网络失败
- **THEN** 系统 SHALL 保持现有模板变量和失败处理，不新增外部请求或输出敏感数据
