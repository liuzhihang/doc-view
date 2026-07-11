# 发布检查清单

本文是 Doc View 发布验证矩阵的唯一文档来源。不要把未运行、0 测试、缓存命中或部分命令描述为完整通过。

## 1. 范围与安全

- 活动 OpenSpec changes 已完成 tasks；长期 requirement 已按需要 sync/archive。
- `git status` 和 diff 只包含计划范围，无生成物、IDE 缓存或临时文件。
- 用户可见变化已写入 `CHANGELOG.md`，版本与 `gradle.properties` 一致。
- 仓库和 artifacts 不含 token、cookie、账号、私有 endpoint 或生产请求体。
- Marketplace/平台 token 只通过安全环境或本地参数提供。
- 明确 release 风险、回滚 commit/version 和未覆盖场景。

## 2. 自动验证矩阵

| 变更类型 | 最小验证 |
| --- | --- |
| 文档/skill/OpenSpec-only | 文件/链接存在性、OpenSpec validate/status、`git diff --check`、diff 范围 |
| Java/PSI/DTO/Markdown | 针对性失败/通过测试、`./gradlew test` |
| Gradle/descriptor/resource/打包 | `./gradlew buildPlugin` |
| IntelliJ API/平台/extension | `./gradlew verifyPluginProjectConfiguration`、`buildPlugin`、`verifyPlugin` |
| UI/生命周期/写回 | 上述自动验证 + `runIde` 手工烟测 |
| 上传集成 | 自动测试或 payload 检查 + 非生产 endpoint 手工验证 |

常用命令：

```bash
./gradlew test
./gradlew buildPlugin
./gradlew verifyPlugin
```

若 `src/test` 没有真实测试或 Gradle 允许 0 tests，`test` 成功只能证明测试源码/相关任务未失败，不能证明行为 contract。

## 3. Plugin Verifier

- `pluginVerifierIdeVersions` 覆盖计划声明支持的 IDE 版本。
- 报告无 compatibility problem 和新增 internal API。
- deprecated API 已修复或在独立 change/风险说明中跟踪。
- `pluginSinceBuild`、Java 字节码和目标平台一致。
- `pluginUntilBuild` 为空时明确说明未来版本尚未自动获得验证。

## 4. `runIde` 手工烟测

- Spring Controller、Feign、Dubbo 的发现与文档生成。
- line marker、右键 action、tool window 打开/刷新/展开/选择。
- preview、复制、导出和参数编辑写回。
- settings、template、YApi、ShowDoc、YuQue 页面保存/取消。
- 无 editor、Dumb Mode、文件失效、项目关闭/重开。
- 大 Controller、递归 DTO 或批量操作期间 UI 可响应并可取消。

记录使用的 IDE build、样例工程、实际结果和未覆盖项。

## 5. 平台集成

- 使用 mock 或非生产服务，不在仓库保存真实地址/凭据。
- 验证缺少配置、认证失败、超时、网络错误、服务端错误和成功响应。
- 日志与通知不输出 token 或完整敏感 payload。
- 重试不会重复创建远端文档；部分失败有明确反馈。

## 6. 打包与发布

- 检查 `build/distributions` zip 中 descriptor 版本、since/until build 和 bundled dependencies。
- 确认测试框架/JUnit 等 test-only 依赖未打入插件包。
- 检查 changelog HTML 与 Marketplace 描述不夸大能力或兼容范围。
- 只有维护者明确授权且凭据已准备时才执行 `./gradlew publishPlugin`。
- 面向正式发布的代码按仓库策略从 `develop` 合入 `master`，确认 tag 指向发布 commit 并推送。

## 7. 发布后与回滚

- Marketplace 版本、兼容范围、更新说明和安装包正确。
- GitHub Release/tag 与 Marketplace 版本一致，产物 SHA 可追踪。
- 安装版完成核心 smoke test。
- 记录已知问题、监控反馈和回滚条件。

回滚时先确认问题版本和影响范围，再撤回/替换发布或发布修复版本；保留复现信息，不在紧急修复中夹带无关重构。

## 凭据事件

如果历史中发现疑似真实 token：立即脱敏当前树且不回显原值；外部撤销/轮换、权限与使用记录检查作为单独授权动作尽快执行，两者互不替代。Git 历史重写不能替代轮换，且必须单独批准、协调所有分支/tag/fork 后执行。
