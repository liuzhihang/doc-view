# Tasks

## 1. 核心消息和规则帮助

- [x] 1.1 为当前英语/中文混合取得资源或 UI 合约 RED；建立完整英文 base 与简中资源、相同 key 和插值覆盖，默认模板逐字保持；运行 `./gradlew test --tests '*PluginLocalizationTest' --rerun-tasks`。
- [x] 1.2 去除规则说明的中文标点分段，保留原生浅线与可换行摘要；运行 `GenerationRulesLayoutTest` 并验证两种语言规则和默认模板语义不变；在用户指南说明语言范围。

## 2. Java 与 ActionSystem 界面

- [x] 2.1 将设置、表单/参数弹窗、目录/预览状态、通知与菜单可见硬编码提取为消息；XML/Configurable 显示名跟随语言且 ID 不变；增加 UI/消息键回归并运行对应专项。
- [x] 2.2 将上传选项的显示名与稳定平台身份分离，测试两种语言选择同一服务及取消无副作用；保留上传/导出 payload 和业务逻辑。

## 3. 欢迎与更新页

- [x] 3.1 提供英语/简中随包 HTML、本地化状态/错误及按钮，保持链接/安全边界和宽度样式；`WelcomeHelpTest` / `WelcomeLifecycleTest` 验证 locale 内容、占位符、导航及 CSP。

## 3a. 原生反馈：模板辅助操作

- [x] 3.2 将模板及其他设置页的指南改为原生问号（悬浮用途/指南链接），恢复按钮移入模板编辑操作行，与变量入口同组；先取得布局/组件类型回归失败，再核对中英窄窗口和恢复草稿状态。

## 4. 集成与原生验收

- [x] 4.1 执行 `./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks` 并记录测试数量、Verifier、包 SHA；运行 OpenSpec strict 与 diff 范围检查。
- [x] 4.2a 在最终e0b6包的英语/简中900×700 IDEA核对常规、规则、模板、集成总览及三平台设置；模板/规则完成深浅主题四组截图，Compact保持开启；中文临时草稿恢复、预览和Cancel后重开通过，项目XML未变。
- [x] 4.2b e0b6已核对中英主要界面及内部导航；本轮同一af61包补齐类/方法右键Upload、Preview Upload及Esc返回的中英×Light/Dark×Compact开/关八组人工验收，均正常。此前工具收起菜单不再作为未验结论，人工/工具证据分别记录于verification。真实上传未执行，全键盘仍按用户要求跳过。
- [x] 4.2c 实际问号气泡阅读/分行由维护者人工验收完成；用户进一步确认“简中和英语均已验收”。不外推未提供的逐主题/Compact矩阵。
- [x] 4.2d 维护者确认气泡覆盖简中/英语，并手动点击三类指南；工具读到settings.md，维护者补充templates.md及integrations.md实际地址。工具核对往返草稿保持、Cancel后重开清除草稿且Apply禁用。检查发现旧包误用develop，修复与新包复验单独见5，不把404正文计作可用。
- [x] 4.3 恢复隔离 profile/项目设置及普通 IDEA，记录实际验证、语言覆盖和发布剩余清单，核对 HEAD/index/无关工作保持；不提交、不发布、不归档。

## 5. 原生反馈：公开文档分支

- [x] 5.1 先取得设置帮助基址及中英欢迎/更新页文档分支的2项RED，统一master并专项通过；完整门禁48 suites/228 tests零失败、18 tasks实际执行，IU261 Verifier Compatible，新包SHA见verification。
- [x] 5.2 af61修复包在doc-demo原生点击首页/更新页指南均打开master目录，首页完整历史打开master/CHANGELOG；更新页进入模板设置、Apply禁用且无草稿。初次收尾尚未重做三处设置气泡点击，后续af61续验已由维护者点验并确认master；远端正文此前404，不能把目标正确计作正文可用。隔离配置和普通IDEA已恢复，HEAD/index及doc-demo保持。

## Workflow follow-up

- 本地验证和审阅完成后，按维护者后续明确指令决定提交、同步/归档和发布；未验项不因归档而消失。
