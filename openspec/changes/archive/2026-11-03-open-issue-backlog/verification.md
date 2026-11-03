# 验证记录

## 基线与工作区

- 2026-11-03 重新读取 GitHub：开放 Issue 为 #89、#90、#91、#96、#97、#101；远端 develop/master 均为 `7b2e0a849fe0fe54df6df08792579a361b712428`。
- `/Users/liuzhihang/workspace/doc-view` 的 develop 有维护者未提交开发工作。本轮未修改该工作区；在独立副本 `/Users/liuzhihang/Documents/Codex/2026-11-03/task/doc-view` 的 `fix/open-issue-backlog` 分支实施。
- 基线保留 Java 21、Gradle 9.5.0、IPGP 2.16.0、IU 2026.1；不包含原工作区 Java 25/IU 263 升级。
- 先创建并验证 OpenSpec proposal/design/3 specs/tasks，达到 apply-ready 后开始测试和运行时修改。适用技能为仓库 `.codex/skills/doc-view-contract-design`、`doc-view-java-maintenance` 与 OpenSpec proposal/apply/sync/archive。

## 逐项结论

| Issue | 证据与实现 | 结论 |
| --- | --- | --- |
| #89 | 接口级 `DocView.since/version`、字段自身标签、模板条件段落/变量目录；保留正文中的标签字样；多行标签按数据元素读取并保留词间空格 | 可以支持，已实现；自定义模板仍由用户显式插入变量 |
| #90 | 数组路由、`path` 别名、常量选取第一条；既有模板生命周期修复重跑 10 项测试 | 路由已修复；设置丢失原问题已有修复，不重复改设置逻辑 |
| #91 | 原图确认为 Dubbo 多参数。原截图的普通参数场景在本轮修改前已通过；新增精确名称、多行及空标签回归，修复 `id` 误匹配 `identifier`、多行截断和多余空白 | 原始基础场景已有实现，本轮补齐可复现边界 |
| #96 | 查看两张原图与维护者评论；仅在 Markdown 投影去掉合成 element/key/value，并重算普通表前缀、修正分表标题 | 已修复；不扩大此前不支持的任意容器类型解析 |
| #97 | 实体编辑器复制当前单元格及草稿值为普通/分表 Markdown；无 PSI 快照、后台渲染、空/加载/关闭/项目释放检查 | 已实现并自动测试；原生烟测受工具环境阻碍，见下文 |
| #101 | 真实编译 JAR + sources JAR，使用 IntelliJ 公共导航读取源码字段，仅替换注释来源；覆盖继承、嵌套、泛型、注解优先级与缓存刷新 | 已实现；需 IDE 关联正确版本源码，纯字节码无法恢复 Javadoc，不自动下载源码 |

六项均存在可支持路径，没有以“实现困难”或“未验证”为由关闭。不支持的输入条件已说明，不等同于整个 Issue 不支持。本轮不修改 GitHub Issue 状态。

## 自动验证

- 未修改代码基线：`./gradlew --offline test --rerun-tasks --no-build-cache`，49 suites / 237 tests，0 failures/errors/skipped，14 tasks executed。
- 首轮新增测试 28 项：23 失败。#90、#91、#89、#97 的失败直接指向功能；#96 缺完整 JDK、#101 测试 JBR 无编译器属于夹具问题，未当作功能 RED。
- 校正夹具后：#96 的 6 项全部因合成层失败，输出与原图一致；#101 的 3 项因预期源码注释为空失败，1 项既有本地源码行为通过。
- 第一轮 GREEN 修正了标签尾部 Javadoc 格式字符。参数版本标签追加 2 项多行 RED，确认原 `joining("")` 丢失词间空格，再统一使用解析工具修复。
- 全量首轮发现默认模板旧 SHA 与复制关闭测试的同步时序假设两处失败。模板 SHA 按批准的模板变更更新，保留两个语言资源完全一致和无标签 golden；关闭测试通过公开进度监听器与 latch 固定真实后台先后顺序，不删除或放宽断言，不修改生产实现来迎合测试。
- 最终命令：`./gradlew --offline test verifyPluginProjectConfiguration buildPlugin verifyPlugin --rerun-tasks --no-build-cache`，`BUILD SUCCESSFUL in 1m 15s`，18 tasks 全部实际执行。55 suites / **267 tests，0 failures/errors/skipped**；其中本轮新增 30 tests。配置校验与打包成功，Plugin Verifier 1.411 对 **IU-261.22158.277** 给出 **Compatible**，没有 compatibility/internal/deprecated API 问题。
- ZIP/JAR 读回校验：descriptor 版本 1.4.0、since-build 261；未包含 contract 测试类、testData 或 JUnit 类。无标签基础 Markdown golden 仍通过。
- 开发候选 ZIP SHA-256：`8534643cc7b8ba75ef9c893a84b414796c49e7a811631532fd9c7fa3423b33be`。

测试过程没有新增运行时依赖、平台版本或 extension。日志中既有测试 API 弃用警告不等于插件兼容问题；以 Verifier 报告为准。

## 原生 UI 烟测的实际边界

1. 使用 `runIde --args=<workspace>/qa-project` 启动虚构实体工程，日志确认 IU-261.22158.277 及 `Loaded custom plugins: Doc View (1.4.0)`，config/system/log/plugins 均在独立 Gradle sandbox。
2. Computer Use inventory 可发现 `Main / com.jetbrains.jbr.java`，但 `getApp("com.jetbrains.jbr.java")` 返回 `Invalid app`，无法操作按钮。
3. 停止本轮 runIde 进程（exit 130）后，以相同官方 IU 261 的现有原生 app wrapper 和相同隔离 properties 重试；macOS 返回 `Launch failed / NSPOSIXErrorDomain Code=163 / Launchd job spawn failed`。
4. 没有修改日常 IDEA 配置，没有安装到用户日常 IDE，也没有通过写剪贴板/配置文件冒充 UI 操作。原生按钮可见性与人工交互仍需后续烟测；自动化实际编辑器/action 测试不被描述为原生验收。

这两次是工具绑定/应用启动失败，不是自动审批拒绝。其他构建命令因沙箱不能写 `~/.gradle` 锁文件，正常申请执行权限后成功，未发生自动审批拒绝。

## 交付和后续

- 本轮只形成独立分支与开发候选包；未合并、未发布、未发 tag、未关闭 Issue。
- 发布版本仍为当前基线 1.4.0；候选 ZIP 不代表新的正式版本。正式发布需另行选定新版本，并补原生 UI 烟测。
- 修复尚未进入远端发布分支，支持项保持打开。原工作区未提交功能不应随本轮修复一并推送。
- 原始测试 XML、构建日志和 UI 阻碍记录位于副本同级 `evidence/`；最终汇总以本文件记录为准。
- 已按用户要求执行 `openspec archive open-issue-backlog --yes --json`，归档至 `2026-11-03-open-issue-backlog`，同步 3 份规格、6 项 requirements；`openspec validate --specs --strict` 为 8 passed / 0 failed，`git diff --check` 通过。归档包含上述原生 UI 未验证限制，不代表发布验收完成。
