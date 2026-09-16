# 验证记录

- 旧实现：`./gradlew test --tests '*CollectionDependencyContractTest'`，3 tests / 1 failure；失败为 DocViewData 编译类仍引用 Commons Collections，其余空值测试通过。
- 修复后：`./gradlew test buildPlugin verifyPluginProjectConfiguration` 成功，16 tests / 0 failures / 0 errors / 0 skipped，含既有 Spring Markdown golden。
- ZIP：`build/distributions/Doc View-1.3.14.zip`，descriptor 版本 1.3.14，since-build 261。仅包含插件 JAR，无测试依赖；所有生产 class 均无 `org/apache/commons/collections/` 引用。
- SHA-256：`4441e427eb4dd1becca3e99539cdb3e2e7234845eeec513a23440edccf30dafb`。
- 增量 instrumentation 输出 form 绑定类不存在提示；最终 JAR 的 PreviewForm/ParamDocEditorForm 均存在生成的 setupUI 方法。未做 GUI 安装烟测，不能据此声称 UI 已验证。
- 直接使用 build `262.8665.337` 作为 Gradle IDE 版本解析失败；JetBrains releases API 确认对应发行版本为 `2026.2.0.1`，改用发行版本进行验证。
- 自动验证完成时未运行真实 YApi 上传或用户业务工程；Git/Marketplace 发布结果以实际发布记录为准。
- 发布收口同步集合处理规范并归档本 change，手工安装与真实上传验证仍未覆盖。

- 正式基线：`./gradlew verifyPlugin` 成功；IU 261.22158.277 verdict 为 Compatible。
- 前向验证：`./gradlew verifyPlugin -PpluginVerifierIdeVersions=2026.1,2026.2.0.1` 成功；IU 261.22158.277 和用户精确 build IU 262.8665.337 均为 Compatible，无 compatibility/internal/deprecated 问题报告。Verifier 输出部分 IDE layout classPath 文件不存在的警告，最终 verdict 均为 Compatible；仍不能替代 UI 烟测。
- `openspec validate remove-commons-collections --strict` 和 `git diff --check` 通过。
