# 最终候选文档库原生生命周期

使用 IDEA IU 263.7307.200、独立配置与虚构 project-a。与[首屏导航验收](verification-2026-11-19-search-first-page.md)使用相同插件 ZIP `2f0284a418459a7e6e102500ec526d6b37fe68e0d4b75cae04664de40337575a`，安装 JAR SHA-256 `942422587aa51c79c69d9321375b279f66cd1765e07041ceea68d506984056cb`，逐字节核对 ZIP 内 JAR 与安装位置。下列原生场景沿用已经完成 578 项自动测试和双 build Verifier 的包；归档后的重新打包与审核结果另列。

| 场景 | 观察结果 |
| --- | --- |
| 已有文档更新后提交前取消 | 在同一包先提交有效基线，再只修改既有 `findUser` 的 Javadoc。暂停时只有同路径 Markdown 已替换，旧 `index.json` 字节和快照不变。IDEA 原生进度任务显示正在停止；放行后显示部分发布路径且无残余暂存。 |
| 部分发布后的搜索与拒读 | 旧索引仍返回 `findUser` 的旧摘要；尝试打开已替换的 Markdown 时，UI 明确提示文件与索引不一致。 |
| 明确重试 | 同一接口保留页面身份，正常重新生成并提交快照，界面显示本会话内最新。62 个接口、125 个索引引用匹配，无暂存残余。 |
| 已提交后的迟到取消 | 检查磁盘确认新快照和 125 个引用均已写入，暂存为零；IDEA 显示正在停止后放行任务。UI 最终仍显示导出成功、本会话内最新；所有磁盘内容与暂停时一致，没有回滚。 |
| 落盘搜索、分页和 Markdown | 选中并打开 `findUser` 对应的真实 Markdown，读回本次的 Javadoc 标记和路径；清空搜索后首屏 20/62，再取到 40/62。 |
| 尚无输出与首次生成 | 移开隔离样例的既有生成目录后启动，窗口显示尚未生成且打开窗口没有创建输出根。实际点击生成，界面显示 62 个接口、126 个输出、本会话内最新；逐一校验索引引用。 |
| 关闭和重开 | 退出完成后再次打开同一虚构项目，窗口显示 62 个接口、新鲜度未知；读回新旧 JSON 和 Markdown 摘要相同。 |
| 外部手改的覆盖保护 | 在虚构 Markdown 增加一行再触发生成；IDEA 报告目标未修改，实际手改字节和索引保留，其余文档均不变。恢复所备份字节后重新校验 125 个引用全部匹配。 |

只在发布线程用本地 JDI 控制两处边界的时序；取消来自真实 IDEA UI，不是调试器注入。首次外部源码修改未立即被 VFS 读取，空更新不计作通过；执行原生“从磁盘全部重新加载”后才开始有效案例。原始记录在本机 `build/openspec-native-closeout-20261119/` 下的 `{partial,late}-jdi.log`、`{partial-result,retry,late-result,fresh,reopened,external-conflict,restored}.json`。结束前恢复虚构源码及外部改写字节，三个隔离进程都已退出。未启用 MCP；外部客户端、插件卸载及模板/JCEF 等由各自 change 验证。

## 归档后兼容性复查

归档移动后修复了文档、格式样例测试，以及 MCP 测试直接读取磁盘 schema 的路径。曾尝试同步更新打包 MCP 合同中的 `diskSchemaSource`；虽然类与协议定义没有变化，但旧发行清单要求合同原始字节指纹，真实客户端安装检查返回 `INCOMPATIBLE`，子 JVM 启动事务也因指纹校验失败。该包已撤回。最终保留 v1 合同原字节，通过仓库文档链接指向实际归档，并断言 MCP 资源内的 12 个磁盘定义与归档 JSON Schema 完全一致。恢复后重新打包的 ZIP 与上述原生已验包逐字节相同（相同 SHA-256）；失败记录与对照见 `build/openspec-native-closeout-20261119/fingerprint-red/`、`archive-package-diff.json`、`final-package-proof.json`。

最终全量复验：100 套件、580 项测试全部成功（0 失败、0 错误、0 跳过），含两项真实 Codex 受控发行源与子 JVM 验收；`buildPlugin` 和 `verifyPluginProjectConfiguration` 通过。同一 SHA 的既有 Plugin Verifier 对 IU 263.7031.23 和 263.7307.200 均为 Compatible，各有 16 条既有 experimental API 报告。OpenSpec 全库严格校验 32 项通过。
