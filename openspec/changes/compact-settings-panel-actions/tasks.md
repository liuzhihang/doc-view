# Tasks

## 1. 模板紧凑操作

- [ ] 1.1 先写工具栏/恢复确认取消 contract 测试并记录 RED；实现原生图标操作栏、更多菜单、焦点及 tooltip，定向测试转 GREEN，更新模板指南。
- [ ] 1.2 保留并更新预览/变量插入/恢复/Apply/Reset/关闭生命周期测试，验证重复点击与草稿状态。

## 2. 规则和 GitHub 入口

- [ ] 2.1 先写共享上限关联布局与链接样式测试并记录 RED；调整上限位置和 Star on GitHub 链接，设置/持久化回归通过，更新设置指南与 Unreleased。

## 3. 整体验证与交付

- [ ] 3.1 实际运行全量 test、verifyPluginProjectConfiguration、buildPlugin、两 build verifyPlugin，记录测试计数/包 SHA/兼容结果并确认版本仍1.6.1。
- [ ] 3.2 最终本地包隔离 IDEA 原生验证深浅主题、键盘、重复切换、插入、恢复确认取消、Apply/Reset/关闭；保存并审阅真实模板/Generation Rules/Star截图，记录本机路径和可用 Library ID或明确阻塞。
- [ ] 3.3 完成文档链接、diff/敏感内容、OpenSpec strict 校验与验证记录，确认没有发布/推送/tag/Release/合并/部署动作。

## Workflow follow-up

- 仅在本次实现与验证完成后同步并归档 compact-settings-panel-actions；其他 changes 不变。
- 保持未发布、未推送状态，交付实际截图与精确边界；不提升版本。
