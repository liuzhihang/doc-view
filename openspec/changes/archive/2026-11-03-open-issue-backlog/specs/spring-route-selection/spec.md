# Spring Route Selection

## Purpose

定义 Spring 类级与方法级路由注解到单篇接口文档地址的确定性选择规则，使多路径、常量及 path 别名仍能生成有效示例地址，并使没有可解析路由时的降级行为可预测。

## ADDED Requirements

### Requirement: 选择第一条声明路径
系统 SHALL 对类级与方法级映射分别选择显式 value 或 path 的第一条路径，并按现有前后斜杠规则拼接，不为同一方法自动复制文档。

#### Scenario: 类与方法的数组路径
- **WHEN** 类声明 `@RequestMapping({"/aa/aa", "/bb/bb"})`，方法声明多个 value 路径
- **THEN** 文档地址包含类与方法各自第一条路径

#### Scenario: path 别名与常量
- **WHEN** 类或方法使用 path 属性、单字符串或可解析字符串常量
- **THEN** 路径解析得到对应字符串并按相同规则规范化

#### Scenario: 空数组与不可解析值
- **WHEN** 注解无路径、空数组或第一项无法解析
- **THEN** 使用可用别名或空路径降级，不抛异常、不将源码表达式当作URL
