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
- [x] 3.2 按实际发布入口验证项目缓存安全：以工具窗口 `… → Clear Cache` 取代旧工具栏和目录菜单的原生矩阵。IU `263.7307.200` 的同一 d094 候选验证 A/B 双向缓存隔离和 Scratch、HTTP 草稿、导出文件及未保存编辑保护，详见 [U03](../../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)；空选择可执行及无刷新由 action 自动回归覆盖。旧入口的原生场景被替代，从未验收为通过。

2026-10-23 后续入口调整：`http-client-generation-and-ui` 已将可见清缓存入口收进工具窗口原生 `…`，目录不再显示项目级清缓存。旧版目录菜单原生矩阵未完成；现行入口按 [HTTP/UI 验证矩阵 U03](../../http-client-generation-and-ui/design.md#8-验收矩阵与证据层次) 从新入口补验，包含 A/B 项目、HTTP 草稿、手写 Scratch、导出文件及未保存编辑保护。按钮移动和单项目自动回归本身不构成验收。

2026-11-08 在新版 `…` 入口补得 A/B DTO 缓存隔离、132/133 个文件 hash 保持及同时未保存 HTTP/Markdown 保护；设置窗口标题分别显示正确项目。观察器没有直接验证文档查询派生缓存的 A/B 清除与保留，此时没有关闭旧入口矩阵。详见[当前原生记录](../../http-client-generation-and-ui/verification-2026-11-08-http-cache-search.md)。

同日后续在同一 d094 候选补齐 A/B 副本派生索引缓存双向隔离、152 个文件哈希保护及不同设置值读回，HTTP/UI 5.4 已完成。只比较公开查询条目身份，未读取私有搜索投影；旧版目录菜单从未在这一目标版本通过，现行能力以实测的新入口结案。详见[后续原生记录](../../http-client-generation-and-ui/verification-2026-11-08-remaining-native.md)。
