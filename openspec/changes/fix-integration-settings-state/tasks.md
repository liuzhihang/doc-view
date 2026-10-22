# Tasks

## 1. 设置状态 RED 回归

- [x] 1.1 新增 IntelliJ project fixture 设置测试并保存/恢复三平台原始 state；覆盖 `null` 与空输入、空/部分配置 Apply、URL 尾斜线规范化后未修改，运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.IntegrationSettingsLifecycleTest' --rerun-tasks`，确认失败发生在对应旧行为断言而非 fixture 初始化。
- [x] 1.2 在同一测试类覆盖 YApi 空 ID 清除、正 long 边界、非数字/零/负数/溢出拒绝、失败时 URL/ID/token 全部不变及 Reset 清空 null ID；用同一 targeted 命令确认旧实现在这些断言处出现 RED。

## 2. 表单状态与 YApi 校验

- [x] 2.1 修正 ShowDoc、YApi、YuQue 的空值比较和 URL 规范化，使 `isModified()` 与 Apply 语义一致；重跑 targeted contract test，确认空配置、Apply 后 clean 和 URL 规范化用例通过。
- [x] 2.2 修正 YApi `isModified()` 对 ID 草稿的比较、空 ID 到 `null` 的映射、Apply 前完整校验和 Reset 全字段回填；重跑 targeted contract test，确认无效/溢出值不改动已保存字段且失败后表单草稿保留。
- [x] 2.3 在同组变更中更新 `CHANGELOG.md` 的 Fixed 项；核对描述只涵盖实际完成的集成设置行为，运行 `git diff --check`。

## 3. 平台设置路由与注册

- [x] 3.1 将 YuQue 缺配置导航改为 YuQue configurable，并使 `plugin.xml` 的 YApi ID 与 `YApiSettingsConfigurable.getId()` 一致；运行 descriptor/configurable ID 自动检查及 targeted contract test，确认二者相同。
- [x] 3.2.a 在隔离 IDEA profile 打开 YuQue 设置页，确认站点地址与 API 地址标签对应正确；不发起远程请求。原生核对只证明页面与标签显示，不证明上传配置有效。
- [x] 3.2.b 在原生 YApi 设置页提交无效项目 ID，确认 IDEA 拒绝保存并显示 `Cannot Save Settings`；按 Revert 后空白草稿恢复。
- [x] 3.2.c 在 IU 262 隔离 profile 的 `UserController` 方法光标处通过 Find Action 触发 `Upload To YuQue`；确认实际导航至 YuQue Settings、页面未配置且 Apply 禁用。源码确认 `checkSettings()` 失败时返回 `false`，上传 wrapper 在检查通过前不调用 `doUpload()`。仅验证本地缺配置导航；未验证真实账号、远端请求或上传结果。证据见 `fix-integration-settings-state/verification.md` 的 Round 46。

## 4. 集成验证

- [x] 4.1 运行 `./gradlew test --rerun-tasks` 并检查测试 XML/HTML 报告，确认既有测试和新增设置回归均被发现且无失败、错误或未解释跳过。
- [x] 4.2 运行 `./gradlew verifyPluginProjectConfiguration buildPlugin verifyPlugin`，读取目标平台 Plugin Verifier 报告并确认 descriptor 变更可打包、当前声明矩阵无新增问题。
- [x] 4.3 运行 `openspec validate fix-integration-settings-state --strict` 与 `git diff --check`；确认仅修改本 change 授权的表单/导航/descriptor、测试、CHANGELOG 与 artifacts，不提交、发布、sync 或 archive。
