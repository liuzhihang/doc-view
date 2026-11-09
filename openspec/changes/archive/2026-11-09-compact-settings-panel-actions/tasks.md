# Tasks

## 1. 模板紧凑操作

- [x] 1.1 先写工具栏/恢复确认取消 contract 测试并记录 RED；实现原生图标操作栏、更多菜单、焦点及 tooltip，定向测试转 GREEN，更新模板指南。
- [x] 1.2 保留并更新预览/变量插入/恢复/Apply/Reset/关闭生命周期测试，验证重复点击与草稿状态。

## 2. 规则和 GitHub 入口

- [x] 2.1 先写共享上限关联布局与链接样式测试并记录 RED；调整上限位置和 Star on GitHub 链接，设置/持久化回归通过，更新设置指南与 Unreleased。

## 3. 整体验证与交付

- [x] 3.1 实际运行全量 test、verifyPluginProjectConfiguration、buildPlugin、两 build verifyPlugin，记录测试计数/包 SHA/兼容结果并确认版本为1.6.2。
- [x] 3.2 最终本地包隔离 IDEA 原生验证深浅主题、键盘、重复切换、插入、恢复确认取消、Apply/Reset/关闭；记录已验证和未验证范围。用户已取消截图交付。
- [x] 3.3 完成文档链接、diff/敏感内容、OpenSpec strict 校验与验证记录；按用户要求先同步归档本次 change，再执行 GitHub 发布。

## Workflow follow-up

- 仅在本次实现与验证完成后同步并归档 compact-settings-panel-actions；其他 changes 不变。
- 用户后续明确授权1.6.2 GitHub小修订版本与Release，取消截图要求；不上传Marketplace。

## 4. 授权的 GitHub 发布

- [x] 4.1 核实远端最新1.6.1和发布工作流，准备1.6.2版本/changelog/欢迎页/发布记录，审计最终包和冻结构建输入。
- [ ] 4.2 在实现验证及本次归档后，按develop→master流程同步、打v1.6.2 tag并创建GitHub Release；下载回读核对SHA与commit/CI，记录Marketplace未执行。

发布任务4.2须在实际成功后更新归档内记录，不提前勾选。
