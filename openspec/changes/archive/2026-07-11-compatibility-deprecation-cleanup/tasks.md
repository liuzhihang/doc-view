## 1. RED 门禁与行为基线

- [x] 1.1 在 `build.gradle` 将 `DEPRECATED_API_USAGES` 加入 Plugin Verifier failure level，并运行 `./gradlew verifyPlugin --rerun-tasks --console=plain`，确认当前 7 处 usage 使任务按预期失败。
- [x] 1.2 新增真实 PSI/index 契约测试，覆盖 Spring 两种 Controller、Feign、Dubbo 别名和同短名噪声；保持原始 module scope 参数并将跨 module 隔离纳入 `runIde`，运行 `./gradlew test --tests '*AnnotationIndexCompatibilityTest' --rerun-tasks --console=plain`。
- [x] 1.3 新增 `DocViewService` 重复名称契约测试，覆盖同名首项、五字母加全局索引后缀、非重复项和单方法路径；运行 `./gradlew test --tests '*DocViewServiceDuplicateNameCompatibilityTest' --rerun-tasks --console=plain`。

## 2. 最小 Java API 替换

- [x] 2.1 将 Spring、Feign、Dubbo 的注解索引调用替换为 `getAnnotations(...)`，合并前复制可变集合，并重新运行 `AnnotationIndexCompatibilityTest`。
- [x] 2.2 将 tool window 数据提供迁移为 `uiDataSnapshot(DataSink)` 且保留父类 snapshot，将树搜索迁移到 `TreeUIHelper`；运行 `./gradlew compileJava --rerun-tasks --console=plain`。
- [x] 2.3 将重复名称随机字符生成替换为 `RandomStringUtils.secure().nextAlphabetic(5)`，并重新运行 `DocViewServiceDuplicateNameCompatibilityTest`。

## 3. 文档与长期规范

- [x] 3.1 更新 `docs/intellij-compatibility.md`，记录 7 处 deprecated usage 已清理、Verifier 失败门禁、正式矩阵与前向验证边界。
- [x] 3.2 更新 `CHANGELOG.md` 的 Unreleased 兼容性说明，不宣称扩大 IDE 支持范围或改变用户行为。
- [x] 3.3 将 `intellij-api-compatibility` delta spec 同步到 `openspec/specs/**`，但不归档 change。

## 4. 完整验证与手工烟测

- [x] 4.1 运行 `./gradlew test buildPlugin verifyPluginProjectConfiguration verifyPlugin --rerun-tasks --console=plain`，确认测试、打包、项目配置和 IU 2026.1 Verifier 全部通过。
- [x] 4.2 检查 Plugin Verifier 报告无 deprecated、compatibility 或 internal API 问题，并确认产物未打入 JUnit/test framework 等 test-only 依赖。
- [x] 4.3 运行 `./gradlew verifyPlugin -PpluginVerifierIdeVersions=2026.1,262.8665.176 --rerun-tasks --console=plain`，记录精确前向 build 结果且不修改正式矩阵。
- [x] 4.4 在 `runIde` 多模块样例中验证 Spring/Feign/Dubbo 扫描隔离，以及 tool window 刷新、展开/折叠、目录右键、导出/上传数据上下文和键盘速度搜索；记录 IDE build、已覆盖项与未覆盖风险。
- [x] 4.5 运行 `openspec validate compatibility-deprecation-cleanup --strict`、`git diff --check` 和范围/敏感信息检查，记录插件 ZIP 的 SHA-256。

## 验证记录（2026-07-11）

- 全量 Gradle 验证通过：13 个测试零失败，插件完成打包、项目配置检查，IU `261.22158.277` Verifier verdict 为 Compatible。
- 一次性前向验证通过：IU `261.22158.277` 与 `262.8665.176` verdict 均为 Compatible；仓库正式矩阵仍只配置 `2026.1`。
- Plugin Verifier 未生成 deprecated、compatibility、internal 或 override-only 问题；插件 JAR 未包含 JUnit、Hamcrest、IntelliJ test framework、契约测试或 test support 类。
- `openspec validate --all --strict --no-interactive` 为 8 passed、0 failed；工作区 staged/unstaged `git diff --check` 通过，当前 change 文件未发现高风险凭据模式。
- `build/distributions/Doc View-1.3.12.zip` SHA-256：`8ac00c7e5a20544e23eb9f9f68e91823265b748df7c8993b2e7e81f5374259f2`。
- `runIde` 手工烟测使用 IU `261.22158.277`、Doc View `1.3.12` 和 `annotations`/`api-a`/`api-b` 三 module 样例：`api-a`、`api-b` 各自仅显示本 module 的 Spring、Feign、Dubbo 类和方法，注解 stub module 不显示；Refresh、根节点 Expand/Collapse、类节点右键菜单、toolbar Export 进入目录选择器后取消均通过，四个自定义 `DataKey` 的 action 消费路径可用。
- 父类 Quick Actions 弹窗保留 Refresh、Export、Expand、Collapse、Clear Cache、Settings；速度搜索将选择移动到折叠的 `BetaOnlyClient`，搜索前、中、后均未显示 `betaFeign` 子节点；Notifications 中没有 Doc View 错误，最近一次启动日志没有插件相关 linkage、UI data、索引、EDT、网络或文件异常。
- 手工烟测未执行实际 Markdown 写入、真实平台上传、Method Open、Http Client、Clear、Dumb Mode 竞态或 IU 2026.2 UI；Upload 仅验证目录右键入口，`WINDOW_ROOT_NODE`/`WINDOW_TOOLBAR` 则由同数据上下文的 toolbar Export 路径覆盖。临时 app wrapper 产生的 Ultimate plugin 禁用和 Station socket 警告与 Doc View 无关。
