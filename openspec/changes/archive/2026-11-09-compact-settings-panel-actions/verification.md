# 验证记录 — compact-settings-panel-actions / 1.6.2

## 范围与授权

最初仅未发布UI调整；用户后续明确取消截图并要求归档、发布小版本和GitHub Release。GitHub发布阶段目标仅 liuzhihang/doc-view，Marketplace当时排除；GitHub发布后用户单独追加Marketplace授权，执行结果见文末。GitHub API已返回0个Actions工作流，master/develop也没有.github/workflows。没有更改生产语言、依赖、平台基线或生成语义。

开始于干净分支codex/doc-view-1.6.1-polish，HEAD81870ed。已保留并合入远端master的2ab2c4c文档更新（README历史演示与已完成changes归档），这些是维护者既有变更；本轮只同步归档compact-settings-panel-actions，没有自行批量归档其他change。旧develop工作区和另一个release worktree未修改。

## TDD与修正

- 新CompactSettingsActionsTest在旧实现实际RED：3项失败，缺少紧凑工具栏及共享上限仍位于分组外。
- 首轮编译缺Component import；随后测试工具栏定位从不存在的client property改为平台组件实现的ActionToolbar接口。
- 定向5类22项实际GREEN。后续受影响2类13项再次GREEN，未跳过测试。
- 首轮完整Verifier拦下2处updateActionsImmediately过时API；改为公开updateActionsAsync后通过。
- 原生发现尚未addNotify的工具栏主动刷新被忽略WARN：去掉初次主动刷新，只异步更新可见工具栏。无原生窗口的布局fixture显式初始化动作；原尺寸和窄窗口断言保留。
- 原生Tab/Space显示变量后焦点回设置搜索框，先复现再修复重挂载焦点。最终1.6.2中显示/隐藏后焦点均在变量按钮，随后Tab/Space可打开更多菜单。

## 最终自动验证

所有最终命令使用JAVA_HOME指向本机官方IDEA JBR25，offline、rerun-tasks、no-build-cache、no-parallel；未用旧缓存替代新结果。

| 项目 | 实际结果 |
| --- | --- |
| test + verifyPluginProjectConfiguration + buildPlugin + verifyPlugin | SUCCESS，18 tasks executed，1m59s |
| 全量测试 | 85 suites / 450 tests / 0 failures / 0 errors / 0 skipped |
| IU263.7031.23 | Compatible，16处既有experimental API，无deprecated/internal/compatibility问题 |
| IU263.7307.200 | SUCCESS，12 tasks executed，1m19s；Compatible，同样16处既有experimental API |
| 打包 | 单JAR，352个class，major69；version1.6.2，since263.7031.23，无until，require-restart=true；无测试依赖 |
| ZIP SHA-256 | `ffb68156d517fd9a363e6d57cdfea5dbbc9eaea3bfd21bf34e0e258d5be1bcf3` |
| JAR SHA-256 | `110532eed542b59d3656fe399d16ca1d7971366593c3a69543f1ce9bc605e98f` |

最终ZIP已和原生安装包逐字节SHA比对；两次Verifier构建未改变ZIP。descriptor中change-notes为1.6.2两条实际UI更新，欢迎/更新页中英摘要已同步。冻结336个构建/测试输入，发布提交后逐项比对。

命令：

```sh
./gradlew test verifyPluginProjectConfiguration buildPlugin verifyPlugin --offline --rerun-tasks --no-build-cache --no-parallel --console=plain
./gradlew verifyPlugin -PpluginVerifierIdeVersions= -I /tmp/doc-view-1.6.1-verify-local.gradle --offline --no-configuration-cache --rerun-tasks --no-build-cache --no-parallel --console=plain
```

第二命令的init script仅指定本机官方IDEA路径和独立报告目录，不进入仓库或生产包。

## 原生验证

隔离官方IDEA IU263.7307.200，独立config/system/plugins/log，虚构sample工程；最终加载1.6.2，正常退出。最终日志ERROR=0、read-action断言=0、初始化刷新WARN=0。

| 场景 | 覆盖与边界 |
| --- | --- |
| 紧凑工具栏与可访问名称 | 原生确认三个图标动作、更多菜单仅恢复默认、tooltip/action名称；浅色布局在迭代中实际目视核对 |
| Tab/Space键盘 | 迭代中确认键盘预览；最终包逐步确认变量显示/隐藏、按钮焦点保留、更多菜单与恢复确认 |
| 同面板预览与插入 | 迭代中重复切换、JSON颜色和预览态插入已目视验证；最终包深色主题预览完成，插入完整变量后返回源码 |
| 恢复默认 | 迭代中取消保持预览/草稿，确认返回源码；最终包确认→Apply/OK→重开为默认模板；最终自动回归同时覆盖两框架与取消 |
| Reset与关闭 | 迭代中预览态Reset返回已保存自定义草稿，后续未保存修改Cancel重开保持保存值；最终自动回归覆盖；最终深色插入草稿通过Cancel丢弃 |
| 共享标题上限 | 最终包AX顺序：Class comment→唯一80输入→Class name；Method comment提示共用。Apply79，草稿81取消，重开79，再恢复80保存 |
| 深浅主题 | 最终包原生Islands Light与Islands Darcula交互检查；用户取消截图后不再获取像素，未重新做深色颜色对比或导出图片 |
| GitHub入口 | 轻量ActionLink+Github图标+Star on GitHub；浅色曾目视、深色原生AX有明确链接名称；未实际点击外部浏览器，目标沿用既有bundle仓库URL |
| 框架切换 | 自动回归通过；本轮最终原生切换未取得可靠观察结果，不声明原生通过 |

截图保存接口及系统截图应用曾受阻，用户随后明确取消截图交付；没有截图路径或Library ID，也不再等待截图授权。未运行完整历史原生功能矩阵、真实三方上传、OS凭据失败、未来IDE build或热卸载；这些不视为通过。既有experimental API仍需后续跟踪。

## 证据与发布

本机证据目录为`doc-view-compact-ui-evidence/`（本次任务根目录下），包含release-1.6.2-gates.log、release-1.6.2-local-verifier.log、release-test-summary.json、release-package-audit.json、release-frozen-inputs.json、最终测试XML和Verifier报告。原始profile与日志不公开。

实现与验证完成后按用户授权先同步并归档；GitHub发布任务在归档时保持待执行；现已完成PR#122（develop）和#123（master）、v1.6.2 tag与正式Release。下载ZIP及SHA256SUMS均匹配，GitHub资产digest也一致；release commit为dc733ed9e3650ec4264064bf724ba9e3d5adb07c，与候选34d792f完整tree及336输入相同。该提交check-runs数量为0，不冒称CI运行通过。GitHub发布完成时Marketplace尚未上传；后续授权提交结果见下节。渠道结果见[1.6.2发布记录](../../../../docs/release-1.6.2.md)。

## Marketplace 后续授权与回读

用户在获知“Marketplace仍未上传”后明确授权发布。使用既有认证确认当前账号资料、Doc View 插件归属与开发者名单，上传前账号/插件版本列表均无1.6.2。GitHub重新下载包、本地构建包与验收留存包 SHA 完全相同。

首次命令因 Gradle 签名任务读取被排除构建任务的 provider 失败，`publishPlugin` 未执行，远端回读仍无1.6.2。随后将发布输入明确绑定已验收 ZIP，上传前验证 SHA；`publishPlugin` 实际执行成功（7s，1 task executed），没有重建、改动冻结336输入或 tag。

2026-11-09T15:16:27Z 回读唯一 update `1216086`，版本1.6.2、Stable、since263.7031.23、无until，`approve=false / listed=false`，待JetBrains审核。平台外层ZIP重压缩后SHA为 `544a0bb0d161d14712da49858cc5979fd187753cd822ff8d28ab84a974d0d14b`，内部全部文件与GitHub原包逐项一致；JAR SHA仍为 `110532eed542b59d3656fe399d16ca1d7971366593c3a69543f1ce9bc605e98f`，更新说明与descriptor一致。

本机证据位于原 evidence 目录的 `marketplace-1.6.2-*`，只保存脱敏结果；凭据未输出或新增保存。本次仅发布及文档记录，未重跑runtime测试，不扩大原生验证声明。上传成功不等于上架，无需重复提交。
