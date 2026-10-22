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
- [ ] 3.2 在专用 IU 2026.1 项目中分别点 toolbar 与目录 Clear Cache；确认显示说明一致、没有确认框或目录刷新，选中/未选中时缓存清理可用，并确认已打开的手写 Scratch 文档与普通项目文件完整。记录 IDE build、实际观察和未覆盖项；未完成前不勾选。
