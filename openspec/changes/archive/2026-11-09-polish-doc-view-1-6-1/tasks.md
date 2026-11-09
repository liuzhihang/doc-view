# Tasks

## 1. 注释与设置

- [x] 1.1 新增标题/正文 PSI fixture，记录 RED；实现类/方法首行、Unicode上限、classDesc模板路径，定向测试转GREEN。
- [x] 1.2 实现生成规则三个设置、持久化/Apply/Reset/Cancel/非法输入原子校验及缓存指纹；设置与指纹回归通过，更新指南。

## 2. 模板与预览

- [x] 2.1 先写同面板预览和高亮回归，记录RED；实现同面板渲染、Markdown/Velocity/变量表达式高亮与生命周期，定向测试GREEN并更新模板指南。
- [x] 2.2 修复JSON fenced code渲染高亮，验证原Markdown复制导出不变；补齐预览选择与全部导出PSI-free回归。
- [x] 2.3 弱化预览边框、灰色说明、GitHub Star及清理图标；相关布局/动作测试与实际界面检查通过。

## 3. 发布内容与整体验证

- [x] 3.1 精简README，更新1.6.1版本、changelog和欢迎页，校验文档链接与版本内容。
- [x] 3.2 实际执行全量test、verifyPluginProjectConfiguration、buildPlugin、两build verifyPlugin，记录测试计数、发现及最终ZIP SHA/descriptor/依赖审计。
- [x] 3.3 在隔离IDE/profile完成新变化的原生深浅主题及保存取消、重复预览切换、选择读访问验收，记录未覆盖1.6.0矩阵。
- [x] 3.4 完成diff/secret安全审计、OpenSpec strict validate、发布记录和commit，确认可审查交付。

## Workflow follow-up

- 状态：主 specs 已同步，本 change 已归档；其他活动 changes 保持不动。
- 状态：develop→master 已完成（#117/#119→#118），v1.6.1 tag 与 GitHub Release 已公开并下载校验；Marketplace 实际上传成功，update 1215689 待审核上架，下载内部文件匹配。详见 docs/release-1.6.1.md。

发布结果跟踪位置：归档后的 verification.md 与 docs/release-1.6.1.md；合并/tag/Release/Marketplace 回读已逐项记录；人工审核上架仍等待平台，不冒充已完成。
