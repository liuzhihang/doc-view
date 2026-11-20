# Doc View 1.7.2 发布记录

GitHub Release 已公开。JetBrains Marketplace 已收到稳定通道更新 `1225101`，提交后公开列表仍显示 1.7.1，1.7.2 等待审核和上架。

## 版本与兼容性

- 最低 IntelliJ IDEA Ultimate 正式 build 为 `263.7307.200`（IDEA 2026.3），Java 源码、API 与字节码基线为 Java 25。只对该精确 build 声明本轮 Plugin Verifier 与原生覆盖；上限未固定。
- 安装、更新和卸载按 IDEA 的重启流程生效。已知的历史热卸载失败与平台 JNI 引用保留在[基线记录](../openspec/changes/archive/2026-11-20-intellij-263-baseline-upgrade/verification.md)。
- 项目文档支持即时搜索、匹配高亮和键盘分页。MCP / AI 设置会自动检查 Skill，按状态展示操作，并提供 IDEA MCP Server 设置入口。完整变化见[更新日志](../CHANGELOG.md)与[用户指南](user-guide/index.md)。
- macOS Apple Silicon 上验证了 `codex-cli 0.169.0-alpha.11.1`。本次插件继续使用兼容的 GitHub v1.7.1 Skill ZIP 与 manifest，插件兼容范围 `[1.7.1, 2.0.0)`，安装还会校验 MCP contract fingerprint。其他系统、架构及 CLI build 未验证写入。

## 最终发行包

| 项目 | 读回 |
| --- | --- |
| ZIP | `Doc View-1.7.2.zip`，951,120 bytes，SHA-256 `c372c22e1773a0ae1487f2de4d08060edb690cf9fed0c17282c12750ac0dacbc` |
| 主 JAR | SHA-256 `f8de3a0b8dbea35c5357898d30b2f738700dd585b9f507dfae9b56a2fd7e806f` |
| Descriptor | `com.liuzhihang.doc-view` / `1.7.2` / `since-build=263.7307.200` / `require-restart=true`，无 `until-build` |
| 字节码 | 418 个运行时类均是 major 69，未包含测试与探针 |
| Gradle | 99 suites / 584 tests，0 失败、错误、跳过，18 个任务实际执行，构建与配置检查通过 |
| Plugin Verifier | `IU-263.7307.200` Compatible，保留 16 项 Experimental API 与 IDE SDK 布局警告 |

Gradle 以 IDEA 的 JBR 25 作为 `org.gradle.java.home`，执行 `./gradlew --offline --no-configuration-cache test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --no-build-cache --no-parallel`，完整证据见[稳定版构建记录](../openspec/changes/archive/2026-11-20-intellij-263-baseline-upgrade/verification-2026-11-20-stable-minimum.md)。

## 原生验收与保留边界

- R2 在隔离 IDEA 通过 GET、JSON、Form 的 3 次显式回环发送，确认生成不发网。通过菜单、键盘、双项目隔离及文件保护、Markdown Copy/Export、Schema 写回和一次 Undo、XML Dubbo、项目关闭及 Java 对象回收。R3 只改包内 descriptor 的中英插件介绍，其余内容逐字相同；实际加载 R3 并复验 GET 草稿、原生 Markdown、目录刷新和完整维护菜单。见[原生验收](../openspec/changes/archive/2026-11-20-http-client-generation-and-ui/verification-2026-11-20-final-r2.md)。
- 固定 1,093 对象的 HTTP 模型完成 7 次进程首次生成与 7 次热生成。生成到编辑器打开的 p95 分别为 981 ms 和 605 ms；观察到最长 514 ms 的 IDE UI Timer 间隔。时序范围及资源终态见同一原生记录。
- 内置 MCP 已使用目标 Codex 客户端完成前后两种真实取消、会话恢复及项目生命周期。真实自然 Skill 工作流、官方包安装、项目关闭时的暂存取消分别在所记录的候选包验证。见[MCP 验收](../openspec/changes/archive/2026-11-20-doc-view-mcp-tools/verification-2026-11-20-native-acceptance.md)与[Skill 验收](../openspec/changes/archive/2026-11-20-doc-view-client-skill/tasks.md)。
- 未进行 YApi、ShowDoc、YuQue 的真实平台上传。跨应用多组合和历史 EAP 证据分别保留在实际候选记录；不据此推断其他平台、完整原生堆或长期内存行为。JUnit 编译与 Gradle 10 弃用提醒见构建记录。

## GitHub 和 Marketplace

| 项目 | 2026-11-20 发布回读 |
| --- | --- |
| GitHub `v1.7.2` | [公开 Release](https://github.com/liuzhihang/doc-view/releases/tag/v1.7.2)，tag 指向 `master` 上的 `19a9c276a5bebe2c3318f46784ebcbd874610ff5`。重新下载 `Doc.View-1.7.2.zip`，按公开 `SHA256SUMS` 校验通过，与本地发行 ZIP 字节一致。 |
| JetBrains Marketplace 1.7.2 | 官方上传 API 返回 HTTP 201，更新 `1225101`、稳定通道，`approve=false`、`listed=false`，最低 build `263.7307.200`。公开列表仍为 1.7.1。 |

[Marketplace 下载接口](https://plugins.jetbrains.com/plugin/download?updateId=1225101) 已返回该更新对应的包。Marketplace 重新封装外层 ZIP，下载 SHA-256 为 `88b3b9290e7f1796b5a3c5bdd11af25ee56c89b5eb1fe5a291e751777f3ee53d`；唯一文件条目主 JAR 与 GitHub 包的 JAR 字节完全相同。下载回读成立时，1.7.2 仍未在 Marketplace 公开列表出现。

1.7.2 只发插件 ZIP。公开下载器在两份 Skill 附件均缺失时继续查询较早稳定版本。只缺一个附件会报错。Skill 包继续从官方 v1.7.1 取得，见[发行说明](skill-release.md)。

如遇必须回退的问题，可在 [GitHub v1.7.1](https://github.com/liuzhihang/doc-view/releases/tag/v1.7.1) 或 [Marketplace 1.7.1](https://plugins.jetbrains.com/plugin/15305-doc-view/versions/stable/1219746) 取得前版。1.7.1 的 Codex 版本与兼容范围见[发布记录](release-1.7.1.md)。
