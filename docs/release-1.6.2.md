# Doc View 1.6.2 发布记录

状态：实现及最终门禁完成，GitHub发布待执行；Marketplace未上传，也不在本轮授权范围内。

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

完整TDD、原生分阶段证据与边界见[OpenSpec验证记录](../openspec/changes/archive/2026-11-09-compact-settings-panel-actions/verification.md)。冻结336个构建/测试输入，发布commit与工件绑定结果在发布回读后补记。

用户已取消截图交付。最终深色像素对比、最终原生框架切换与外部浏览器点击未确认；自动框架/模板回归通过。未重跑完整历史原生矩阵、真实平台上传和OS凭据失败；未来IDE build、热卸载仍不作承诺。

## 发布与回滚

GitHub workflow API返回0个工作流，仓库无.github/workflows；不会调用publishPlugin或主动上传Marketplace。GitHub commit/tag/Release/下载SHA/CI回读待执行后填写。回滚使用已发布1.6.1包并重启IDE。
