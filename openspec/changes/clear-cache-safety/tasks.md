# Tasks

## 1. 实际 action RED fixture

- [x] 1.1 增加 `ClearCacheActionTest`：用真实项目级 `DtoSchemaCacheService` 写入可读回的 `Body` entry，创建带手写内容的 Extensions/Scratch 文件并打开 editor，添加一份无关项目文件；调用实际 `WindowClearAction` 后断言缓存已清、Scratch 文件有效且字节及打开状态不变、项目文件不变。使用当前实现运行 `./gradlew test --tests '*ClearCacheActionTest' --rerun-tasks` 并保存 XML RED 证据。
- [x] 1.2 在同一 fixture 中调用实际 `CatalogClearAction`，覆盖选中 `DocViewNode` 与无选择两种上下文；确认无选择仍清缓存，选中文档内容及 editor 不变。用相同 targeted 命令核对旧实现的预期失败和空选择行为。

## 2. 缓存清理实现

- [x] 2.1 在 RED 证据确认后，让两个 action 仅清理动作所属项目的 `DtoSchemaCacheService`，保留项目/Extensions 文件、编辑器与当前目录树状态，不触发刷新、扫描、生成或确认框；用 `ClearCacheActionTest` targeted GREEN 验证。
- [x] 2.2 统一 `plugin.xml` 中 toolbar 与目录 action 的名称、描述，明确只清理当前项目 Doc View cache 且保留生成文档；解析 descriptor 或检查 action presentation 验证两处一致。
- [x] 2.3 确认 `CustomFileUtils.delete` 不再有调用者后移除该死方法及专用 imports；用 `rg` 确认没有遗留调用，并重新运行 targeted action 测试。

## 3. 集成与原生验收

- [x] 3.1 由主代理串行运行 `./gradlew test --rerun-tasks` 与 `./gradlew verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks`；检查相关 XML 报告和 plugin verifier 结果，无失败、错误、未解释跳过或 descriptor 问题。
- [ ] 3.2 在已批准的隔离 IU `263.7307.200` 中分别从工具窗口 `…` 与目录菜单执行 Clear Cache（旧 IU 2026.1 记录保留为历史）；确认显示说明一致、没有确认框或目录刷新，选中/未选中时缓存清理可用，并确认已打开的手写 Scratch 文档与普通项目文件完整。记录 IDE build、实际观察和未覆盖项；未完成前不勾选。

2026-10-23 后续入口调整：`http-client-generation-and-ui` 已将可见清缓存入口收进工具窗口原生 `…`，目录不再显示项目级清缓存。上述 3.2 保留为旧版未完成记录；当前包须按 [HTTP/UI 验证矩阵 U03](../http-client-generation-and-ui/design.md#8-验收矩阵与证据层次) 从新入口补验，包含 A/B 项目、HTTP 草稿、手写 Scratch、导出文件及未保存编辑保护。按钮移动和单项目自动回归不代表本项原生验收完成。

2026-11-08 在新版 `…` 入口补得 A/B DTO 缓存隔离、132/133 个文件 hash 保持及同时未保存 HTTP/Markdown 保护；设置窗口标题分别显示正确项目。观察器没有直接验证文档查询派生缓存的 A/B 清除与保留，旧入口 3.2 不据此勾选。详见[当前原生记录](../http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md)。

同日后续在同一 d094 候选补齐 A/B 副本派生索引缓存双向隔离、152 个文件哈希保护及不同设置值读回，HTTP/UI 5.4 已完成。只比较公开查询条目身份，未读取私有搜索投影；旧入口 3.2 仍保留历史未执行状态。详见[后续原生记录](../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。
