# Spec Delta

## Purpose

规定 Doc View 项目级 DTO schema 缓存的用户可见清理行为，并使该操作与生成文档文件及目录刷新保持清楚边界。

## ADDED Requirements

### Requirement: 清理动作只清除当前项目的 DTO schema 缓存

工具窗口 toolbar 与目录操作中的 Clear Cache SHALL 清除触发该动作的项目所持有的 DTO schema 缓存；该操作不得生成或刷新文档目录。

#### Scenario: 工具窗口 toolbar 清理
- **WHEN** 当前项目 DTO schema 缓存中有条目，用户触发 toolbar 的 Clear Cache
- **THEN** 当前项目的缓存条目被清除
- **AND** 不启动接口扫描或文档生成

#### Scenario: 目录操作清理
- **WHEN** 当前项目 DTO schema 缓存中有条目，且目录树上下文可用，用户触发目录中的 Clear Cache
- **THEN** 当前项目的缓存条目被清除
- **AND** 目录节点、选择和展开状态不因清理动作而刷新或重建

### Requirement: 清理动作保留项目文件和 Scratch 文档

任一 Clear Cache 动作 SHALL 保留项目文件和已有 Scratch/Extensions 文档的内容、有效性及打开状态；动作不得关闭这些文档的编辑器。

#### Scenario: 工具窗口清理时保留手写 Scratch 内容
- **WHEN** 当前项目有一份已打开且包含手写内容的合成 Scratch 文档，用户触发 toolbar 的 Clear Cache
- **THEN** 该文档仍有效、字节内容不变且编辑器仍打开
- **AND** 无关项目文件内容不变

#### Scenario: 目录清理时保留所选文档
- **WHEN** 目录选中一个 `DocViewNode`，对应的 Scratch 文档已打开且包含手写内容，用户触发目录中的 Clear Cache
- **THEN** 所选 Scratch 文档仍有效、字节内容不变且编辑器仍打开
- **AND** 无关项目文件内容不变

#### Scenario: 空选择仍清理缓存
- **WHEN** 目录树上下文可用但没有选中节点，用户触发目录中的 Clear Cache
- **THEN** 当前项目 DTO schema 缓存仍被清除
- **AND** 任何项目或 Scratch 文件均不变

### Requirement: 两处清理入口说明相同操作范围

toolbar 与目录操作中的清理入口 SHALL 使用一致的显示名称，并明确说明它清除当前项目的 Doc View 缓存而保留已生成文档。

#### Scenario: 查看清理入口说明
- **WHEN** 用户查看 toolbar 或目录中的 Clear Cache 操作
- **THEN** 两处入口的名称与操作范围说明一致
- **AND** 说明不将清理缓存描述为刷新目录或删除文档
