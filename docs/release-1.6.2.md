# Doc View 1.6.2 发布记录

状态：`v1.6.2`已发布到[GitHub Release](https://github.com/liuzhihang/doc-view/releases/tag/v1.6.2)，工件下载回读及SHA校验完成。用户随后追加 Marketplace 发布授权；现已上传 [Doc View](https://plugins.jetbrains.com/plugin/15305-doc-view) update `1216086`，2026-11-09T15:16:27Z 回读为 `approve=false / listed=false`，待审核上架。

## 本版变化

- 模板预览/编辑、变量显隐使用面板右上角原生图标工具栏，恢复默认移到更多菜单并确认；保留Apply/Reset/Cancel与变量操作焦点。
- 唯一Javadoc标题上限紧邻类注释，方法注释处说明共用；默认80、范围1–200与生成语义不变。
- GitHub入口为logo与轻量Star on GitHub链接，目标仍为既有仓库。

## 验证与工件

| 检查 | 结果 |
| --- | --- |
| 全量test | 85 suites、450 tests，0失败/错误/跳过，实际重新执行 |
| 配置/buildPlugin | 通过；Java25/major69、单JAR、352 classes |
| Verifier | IU263.7031.23和IU263.7307.200均Compatible；0 deprecated/internal/compatibility问题，16处既有experimental API |
| 原生 | 隔离IU263.7307.200；深浅主题交互、工具栏键盘焦点、恢复Apply后重开、共享上限Apply/Cancel；最终日志无ERROR/读访问断言/初始化刷新WARN |
| ZIP | Doc.View-1.6.2.zip；SHA-256 `ffb68156d517fd9a363e6d57cdfea5dbbc9eaea3bfd21bf34e0e258d5be1bcf3` |
| JAR | doc-view-1.6.2.jar；SHA-256 `110532eed542b59d3656fe399d16ca1d7971366593c3a69543f1ce9bc605e98f` |
| 描述符 | 1.6.2、since263.7031.23、无until、require-restart=true；基线和依赖不变 |

完整TDD、原生分阶段证据与边界见[OpenSpec验证记录](../openspec/changes/archive/2026-11-09-compact-settings-panel-actions/verification.md)。冻结336个构建/测试输入，候选34d792fb309535c6540bc73bc2e6c313fb83c2a0与发布commit的完整tree及逐项输入完全一致。

用户已取消截图交付。最终深色像素对比、最终原生框架切换与外部浏览器点击未确认；自动框架/模板回归通过。未重跑完整历史原生矩阵、真实平台上传和OS凭据失败；未来IDE build、热卸载仍不作承诺。

## 发布与回滚

GitHub workflow API返回0个工作流，仓库无.github/workflows，GitHub Release 不触发 Marketplace 上传。GitHub 发布完成后，用户单独追加 Marketplace 授权，沿用既有认证执行 `publishPlugin`；未改动运行代码、已验收包或 `v1.6.2` tag。

| 远端结果 | 回读事实 |
| --- | --- |
| develop | [PR#122](https://github.com/liuzhihang/doc-view/pull/122)，合并commit49620027a8850d697c118b06a62d0ed5b5daa0b4 |
| master发布提交 | [PR#123](https://github.com/liuzhihang/doc-view/pull/123)，commit`dc733ed9e3650ec4264064bf724ba9e3d5adb07c` |
| tag | `v1.6.2` annotated tag，解引用到上述master发布提交 |
| Release | 2026-11-09T13:12:24Z公开，非draft、非prerelease；ZIP771330 bytes及SHA256SUMS |
| 下载回读 | ZIP SHA与本地原生/测试工件一致，校验文件逐字节一致，GitHub资产digest一致 |
| CI | 仓库Actions workflows=0，该发布提交check-runs=0；没有把未运行CI说成通过 |
| Marketplace | update `1216086`，1.6.2、Stable、since 263.7031.23、无 until；`approve=false / listed=false`，待审核上架；下载内部文件逐项一致 |

发布结果文档后续提交不改变tag绑定的运行代码或工件。回滚使用已发布1.6.1包并重启IDE。

## Marketplace 后续提交（2026-11-09）

提交前通过当前账号资料、插件列表及 Doc View 开发者名单确认发布者身份；账号与插件更新列表均无1.6.2，未重复提交。插件详情中的 `isModificationAllowed=false` 未用于单独推断上传权限。

重新下载 GitHub Release ZIP，与本地构建及验收留存包的 SHA-256 均为 `ffb68156d517fd9a363e6d57cdfea5dbbc9eaea3bfd21bf34e0e258d5be1bcf3`。首次 Gradle 命令在签名任务解析输入时失败，`publishPlugin` 未执行；回读仍无1.6.2后，临时 init script 将发布输入明确绑定到该已验收 ZIP，并在上传前检查 SHA。随后仅执行 `publishPlugin` 成功（7s，1 actionable task / 1 executed）；未重新构建或改变工件，336个冻结输入保持一致。

Marketplace 已接收唯一 update `1216086`。[平台下载包](https://downloads.marketplace.jetbrains.com/files/15305/1216086/Doc.View-1.6.2.zip)外层经重压缩，SHA-256 为 `544a0bb0d161d14712da49858cc5979fd187753cd822ff8d28ab84a974d0d14b`；内部全部文件逐项匹配，JAR SHA-256 仍为 `110532eed542b59d3656fe399d16ca1d7971366593c3a69543f1ce9bc605e98f`，descriptor、兼容范围及更新说明一致。可下载不等于已审核上架；当前仅待 JetBrains 审核，无需再次上传。

本次后续工作仅发布与文档记录，未重跑运行时测试，也不改变上文已记录的原生验证边界。
