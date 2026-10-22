# Tasks

## 1. 索引 renderer 与回归覆盖

- [x] 1.1 将项目索引改为按模块、类和方法纵向分组，并在每个方法项保留完整 canonical identity 与 SHA-256 隐藏元数据；测试多模块、重载、相对文档/源码链接、确定性顺序和现有稳定文件名。Round 28 `ProjectDocumentExporterTest` 14/14、项目文档 Action 2/2 通过。

## 2. 用户指南

- [x] 2.1 更新项目文档用户指南，描述分组索引、方法入口和隐藏身份元数据；核对文字与生成器输出一致，目标文件无尾随空白。

## 3. 集成验收

- [x] 3.1 运行 OpenSpec strict validation 与完整自动门禁。Round 29 为 37 个测试套件、185 项测试通过，IU 261.22158.277 Plugin Verifier 为 Compatible。
- [x] 3.2 在 IU 262 Markdown split preview 检查分组索引及一条方法的文档/源码导航。索引无哈希宽列；实际打开 `User2Controller-postTest-...md` 并看到 `/user2/postTest`、`POST`，Source 链接打开 `User2Controller.java`。
- [x] 3.3 在 IU 262 原生验证 `UserFacade#queryUserDateAccount` 的两个重载入口分别打开独立文档页，且请求参数与目标签名对应；证据见 `verification.md`。这只覆盖一个重载组，不代表多模块索引导航已验。
- [x] 3.4 在 IU 261.22158.277 原生检查 `billing-api` 与 `catalog-api` 的索引分组及页面导航：4 个方法项分别打开对应文档页，并在两个模块中验证 Source 导航；详情与证据见 `verification.md`。这不替代 `doc-export-workspace` 的 IU 262 task 4.3.d。

## Workflow follow-up

- 完成 3.4 的多模块原生检查后，再由维护者归档本 change。
