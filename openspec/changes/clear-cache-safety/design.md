# Design

## Context

`DtoSchemaCacheService` 已按 `Project` 提供 DTO schema 缓存及 `clear()`。`WindowClearAction` 调用该服务后还会删除 Extensions root 下的 `Doc View` 路径；`CatalogClearAction` 在清缓存后会删除所选 `DocViewNode` 的文档路径。`CustomFileUtils.delete` 目前只被这两个动作调用，并会关闭匹配的编辑器再删除文件。

## Goals / Non-Goals

**Goals:**

- 两个入口都只调用当前项目的 `DtoSchemaCacheService.clear()`。
- 目录动作有树上下文时，即使没有选中节点也照常清缓存。
- 保留全部项目文件与 Scratch/Extensions 文档，不创建、修改、删除文件或关闭编辑器。
- 清理动作不触发目录扫描、刷新、文档生成或确认对话框。
- 让两个入口的显示名称和描述准确表达相同范围。

**Non-Goals:**

- 改变 cache key、epoch、深拷贝、失效或项目生命周期策略。
- 把清理动作改造成 Refresh、Scratch 管理器或项目文件清理工具。
- 清除 Markdown、模板、预览、集成凭据或其他项目/应用状态。
- 扩展到文件级缓存、全 IDE cache 或其它插件的清理动作。

## Decisions

### 复用现有项目级缓存服务

动作只调用 `DtoSchemaCacheService.getInstance(project).clear()`。保留 Project scope 和服务的既有 `clear()` 行为；不新增全局状态、refresh coordinator 或清理服务。toolbar 动作仍要求 Project context。目录动作要求 Project 与树 context，但将清缓存与当前选择解耦；无选中节点时清除缓存，不执行节点文件操作。

### 删除 Scratch 删除副作用

移除两个 action 对 `CustomFileUtils.delete` 的调用。确认 helper 无其它调用者后，一并移除 `CustomFileUtils.delete` 及其专用 import；保留 `CustomFileUtils.openMd()`、Scratch 路径生成和打开文档的代码。清缓存不应通过虚拟文件查找来间接创建或删除 Scratch 路径。

### 将 action contract 测试放在项目级 IDEA fixture

测试使用轻量 IntelliJ Java fixture 创建普通项目文件与 Extensions/Scratch 合成文件，写入手动内容并打开编辑器。先通过真实 `DtoSchemaCacheService` 插入可读回的 `Body` 缓存条目，再构造携带真实 Project 的 `AnActionEvent` 调用两个实际 action。Catalog 用测试 `DocViewNode` 返回目标 Scratch 路径，并用 `SimpleTree` 上下文覆盖“选中节点”和“无选择”两种情况。每次清理后读取缓存、虚拟文件字节与 `FileEditorManager.isFileOpen`；teardown 负责关闭测试编辑器和删除 fixture 文件。

该测试不启动真实项目、网络或 Gradle 自定义沙箱；后续由主代理串行运行 targeted RED，确认旧 action 对 Scratch 文件/编辑器的破坏后再 apply。原生验收仍需在专用项目中查看两处入口的文字并验证打开的手写文档保持不变。

### 统一 action 呈现

两处 descriptor 使用 `Clear Cache`，描述为“清除当前项目的 Doc View schema cache，并保留生成文档”。英文措辞沿用现有 descriptor 语言；不新建本地化框架。含义不得写成“Refresh”或“Delete Documents”。

## Risks / Trade-offs

- [轻量 fixture 的 Extensions root/editor 行为可能与完整 IDE 不同] → 自动测试断言文件字节与 editor open state；另保留 IU 2026.1 原生验收项。
- [目录 action 上下文缺少 SimpleTree 时缓存不清] → 保留其现有无效上下文 no-op；验证真实 toolbar/catalog context，明确空选择与缺失上下文是两种情况。
- [缓存清除后用户期待目录立刻重建] → action 描述限定为缓存清除，目录不自动刷新；用户可使用现有 Refresh 动作按需重建。

## Migration Plan

无需迁移持久化数据或用户文件。升级后两处 Clear Cache 只清当前项目 DTO schema 内存缓存；用户可手动刷新目录，生成文档继续保留。若新版本回滚，旧版本动作可能恢复删除副作用，因此无需增加运行时兼容开关或状态迁移。
