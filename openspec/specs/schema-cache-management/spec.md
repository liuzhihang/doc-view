# schema-cache-management Specification

## Purpose
规定 Doc View 在多个 IDEA 项目并存时的缓存清理范围、可见入口和操作说明，并定义清理前后对已生成文档、临时草稿、打开的编辑器、未保存内容与目录状态的保护。

## Requirements

### Requirement: 清除当前项目的可重建缓存

工具窗口 `…` 的 Clear Cache SHALL 只清除触发该动作的项目所持有的 DTO schema 缓存和文档查询的派生内存缓存，包括未选中目录节点时。它 SHALL 保留当前项目已知的快照新鲜度及其他项目的缓存；不得触发接口扫描、生成、目录刷新或删除确认。

#### Scenario: 同时打开两个项目后执行清理
- **WHEN** 两个项目已有缓存，用户从项目 A 的工具窗口 `…` 点击 Clear Cache
- **THEN** A 的可重建缓存被清除，B 的缓存和文件不变
- **AND** A 的目录节点、选择、展开状态和已知快照新鲜度不变

### Requirement: 清理动作保留项目文件和 Scratch 文档

Clear Cache SHALL 保留项目文件、已生成的 Markdown/JSON、Scratch 及 HTTP 草稿的内容、有效性和打开状态，不得将未保存编辑写入磁盘或丢弃。

#### Scenario: 清理前已有打开的文件及未保存编辑
- **WHEN** 清理项目缓存前，打开了含手写内容的 Scratch 或 HTTP 草稿，或者其中仍有未保存编辑
- **THEN** 文件的磁盘内容不变，编辑器仍打开且保留未保存编辑

### Requirement: 入口与操作说明符合现行界面

用户 SHALL 从工具窗口 `…` 找到 Clear Cache；旧目录 action ID 可继续供既有快捷键和 Find Action 使用，但 SHALL NOT 在目录上下文菜单另列。别名与现行入口 SHALL 使用一致的清理逻辑、名称、说明及图标。

#### Scenario: 核对清理入口说明
- **WHEN** 用户查看当前工具窗口入口或通过 Find Action 查找清理动作
- **THEN** 说明明确清理项目缓存并保留生成文档，不将清理说成刷新目录或删除文件
