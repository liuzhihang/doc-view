# template-editing-preview Specification

## Purpose

为编辑 API 文档模板提供连续的源码编辑与渲染预览体验，使用户能够辨认 Markdown 结构和 Velocity 变量，在同一内容区域核对固定示例的实际输出，同时保持草稿、保存取消和资源生命周期可预测。

## Requirements

### Requirement: 同面板模板预览
系统 SHALL 在当前模板内容区域切换编辑与渲染预览，不跳转另一个预览 tab。切换回编辑 MUST 保留草稿、光标和已选框架；预览只消费当前草稿与固定虚构 DTO，不扫描用户源码或持久化设置。

#### Scenario: 重复切换和框架切换
- **WHEN** 用户修改 Spring 或 Dubbo 草稿并多次点击预览/编辑按钮，再切换框架
- **THEN** 当前区域显示对应草稿的渲染 Markdown，返回编辑时草稿和光标仍在，框架不会串用结果
- **AND** 迟到预览结果不能覆盖较新的请求，关闭设置后释放编辑器及渲染资源

#### Scenario: 无效模板与取消
- **WHEN** 模板渲染失败、内容为空或用户取消设置窗口
- **THEN** 显示明确状态，仍可返回编辑；失败和取消不更改持久化模板

### Requirement: 主题适配模板高亮
系统 SHALL 在模板源码中高亮 Markdown 结构、Velocity引用和指令；变量参考列表中的表达式 SHALL 与说明区分。在渲染预览中，JSON fenced code SHALL 显示可辨认的语法颜色，浅色和深色主题均可阅读。

#### Scenario: 源码与渲染
- **WHEN** 模板包含 Markdown 标题、${DocView.name}、${DocView.requestExample}、#if/#end 和带语言标识的 json fenced code
- **THEN** 编辑器中结构、引用与指令可辨认，渲染结果的 JSON 字符串/数字等语法高亮
- **AND** 高亮不改变模板文本、变量名称或生成 Markdown

#### Scenario: 预览中的编辑动作显示源码
- **WHEN** 用户在预览状态插入变量、确认恢复默认模板或 Reset
- **THEN** 内容区返回当前框架的源码，显示对应草稿或持久化文本，并使旧预览请求失效

### Requirement: 紧凑的模板面板操作
系统 SHALL 在当前模板内容区域右上角提供紧凑图标操作栏，直接提供预览/编辑与变量显隐，恢复默认作为更多菜单中的次级动作。图标 SHALL 有明确 tooltip、可访问名称、键盘焦点与激活方式，并适配深浅主题；不添加 AI 功能。

#### Scenario: 常用操作与重复切换
- **WHEN** 用户用鼠标或键盘反复切换预览/编辑、显示/隐藏变量或切换框架
- **THEN** 操作不跳出当前模板区域，现有草稿、光标及变量插入目标不变，选中/提示状态反映当前操作
- **AND** 操作栏不再显示 Restore default templates、Show template variables 或 Preview Spring 长按钮

### Requirement: 恢复默认需要确认且仅修改草稿
系统 SHALL 在选择恢复默认后说明会替换 Spring 与 Dubbo 模板草稿并请求确认；取消 MUST 保持源码、预览、光标及持久化设置不变。确认 SHALL 返回源码并恢复两套默认草稿，只有 Apply/OK 才保存；Reset 或关闭取消仍按已有设置语义处理。

#### Scenario: 取消和确认恢复
- **WHEN** 用户在自定义草稿或预览中选择恢复默认并取消
- **THEN** 不修改草稿或已保存模板，当前编辑/预览模式保持不变
- **WHEN** 用户确认恢复后 Reset 或取消设置窗口
- **THEN** 恢复已保存模板；确认后 Apply 则持久化默认模板，重开仍一致
