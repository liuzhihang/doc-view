# Spec Delta

## Purpose

定义用户从接口目录打开单个方法 Markdown 时的响应、输出和生命周期行为，避免方法级 PSI 解析与模板渲染阻塞 IDEA 事件线程。

## ADDED Requirements

### Requirement: 方法文档生成不得阻塞界面
从接口目录打开单个方法文档时，文档构建 MUST 在可取消的后台工作中完成；生成期间 IDE 事件线程 MUST 能继续处理界面输入。

#### Scenario: 用户打开一个方法文档
- **WHEN** 用户双击或按 Enter 打开有效方法节点
- **THEN** 生成在后台开始，界面线程不等待 PSI 解析或 Markdown 渲染完成
- **AND** 用户可继续与 IDEA 界面交互

### Requirement: 成功生成后打开完整方法文档
方法文档 MUST 仅在单方法 `DocView` 和 Markdown 均完整生成后创建并打开 Scratch，并 MUST 保持既有输出内容和路径语义。

#### Scenario: 方法文档成功生成
- **WHEN** 有效方法的生成任务完整成功且所属项目仍打开
- **THEN** 系统在既有方法文档路径创建一个 Scratch，写入与当前模板一致的完整 Markdown 并打开该文件

### Requirement: 取消或失效任务不得迟到发布
用户取消生成、项目关闭或源 PSI 在生成前失效时，系统 MUST 丢弃结果，不得创建、写入或打开 Scratch。

#### Scenario: 生成完成前取消或关闭项目
- **WHEN** 取消请求或项目关闭先于后台结果的界面交付发生
- **THEN** 不创建或打开由该请求产生的 Scratch，且取消不显示生成失败通知

#### Scenario: 方法 PSI 已失效
- **WHEN** 后台任务读取到无效的类或方法 PSI
- **THEN** 任务不生成 Markdown，也不创建或打开 Scratch

### Requirement: 生成与 Scratch 写入失败需反馈
文档构建或 Scratch 写入失败时，系统 MUST 通过 Doc View 错误通知反馈失败，并 MUST NOT 将未完成文档作为成功预览打开。

#### Scenario: 构建或写入抛出错误
- **WHEN** 文档构建或 Scratch 创建/写入失败
- **THEN** 用户收到 Doc View 错误通知
- **AND** 系统不把该请求报告为成功打开
