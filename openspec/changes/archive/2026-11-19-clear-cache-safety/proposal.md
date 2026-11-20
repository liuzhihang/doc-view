# Proposal

## Why

`WindowClearAction` 与 `CatalogClearAction` 都标为清理缓存，但当前会删除 `Doc View` Scratch 文档或所选文档，并可能关闭用户正在编辑的文件。用户需要一个可预期的缓存操作：清除当前项目的 DTO schema 内存缓存，同时保留生成文档和手写内容。

## What Changes

- 统一工具窗口 toolbar 与目录上下文中的清理动作：只清除触发动作所属项目的 DTO schema cache。
- 保留所有 Scratch/Extensions 文件、内容和打开状态；不扫描或刷新目录，不生成文档，不弹确认框。
- 统一两处动作的显示名称与说明，明确清理范围是项目缓存。
- 移除动作不再使用的 Scratch 删除 helper（仅当确认没有其他调用点时）。

## Capabilities

### New Capabilities

- `schema-cache-management`: 规定项目级 DTO schema cache 清理动作的范围，以及它与 Scratch 文档和目录刷新的边界。

### Modified Capabilities

无。

## Impact

涉及 `WindowClearAction`、`CatalogClearAction`、`plugin.xml` 中两处 action presentation、可能不再使用的 `CustomFileUtils.delete`，以及 action contract 测试。`DtoSchemaCacheService` 的缓存结构和失效策略不变；不修改生成 Markdown、导出、上传、Scratch 文件格式或项目文件。

## 结案范围

以上记录最初设计。后续 UI 将现行入口移到工具窗口 `…`；旧目录 action ID 只为既有快捷键和 Find Action 保留，仍走同一清理逻辑。同步后的 [delta](specs/schema-cache-management/spec.md) 记录现行入口、派生文档搜索缓存和项目隔离；旧菜单的原生证据见 [验证记录](verification.md)。
