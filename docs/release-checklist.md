# Release Checklist

本文档用于发布 Doc View 前的检查。不同变更类型需要不同验证强度；不要把未运行的命令描述为已通过。

## 发布前准备

- 确认变更说明、contract 和任务清单已完成，或明确本次不需要。
- 确认 `AGENTS.md` 和相关 docs 已随行为变化更新。
- 确认 `CHANGELOG.md` 包含用户可见变化。
- 确认 `gradle.properties` 中 `pluginVersion` 正确。
- 确认 Marketplace 描述、截图或说明不夸大能力。
- 确认没有 token、测试账号、生产地址或私密日志进入仓库。

## Diff 范围检查

```bash
git diff --name-only
```

检查：

- 是否只修改了预期文件。
- 是否误改 `src/main/resources/META-INF/plugin.xml`。
- 是否误改 Gradle 依赖或平台版本。
- 是否误改 icons、form、message bundle 或模板。
- 是否有生成文件、IDE 缓存、临时文件进入 diff。

## 常规验证

根据变更类型选择：

```bash
./gradlew test
./gradlew buildPlugin
./gradlew verifyPlugin
```

文档-only 变更可以跳过 Gradle 命令，但必须说明原因，并至少完成：

```bash
git diff --name-only
```

## 手动 IDE 验证

涉及 runtime 行为时运行：

```bash
./gradlew runIde
```

手动检查：

- Spring Controller 方法右键 `Doc View`。
- Dubbo Service 方法右键 `Doc View`。
- line marker 显示和隐藏设置。
- tool window 打开、刷新、展开、折叠、清理缓存。
- preview 复制、导出、上传入口。
- 参数编辑写回注释或注解。
- settings、template settings、YApi、ShowDoc、YuQue settings 保存和取消。
- 项目关闭、切换、无编辑器、Dumb Mode 等边界状态。

## 平台集成验证

上传相关变更需要：

- 使用非生产 YApi/ShowDoc/YuQue 环境。
- 验证配置缺失、认证失败、网络失败和成功上传。
- 检查请求 payload 不包含多余敏感信息。
- 检查错误通知可读。
- 避免重复创建远端文档。

## 兼容性验证

涉及 IntelliJ API、`plugin.xml` 或平台版本时：

- 运行 `./gradlew verifyPlugin`。
- 检查 since/until build 策略。
- 检查 JetBrains API 废弃告警。
- 在目标 IDE 版本中运行核心手动流程。
- 记录无法覆盖的 IDE 版本风险。

## 打包和发布

发布前：

- 运行 `./gradlew buildPlugin`。
- 检查生成的插件 zip。
- 确认 changelog 渲染正确。
- 确认 Marketplace token 仅通过安全环境变量或本地 Gradle 参数提供，不写入仓库文件。
- 准备回滚版本或撤回方案。

Marketplace token 可以用以下任一方式提供：

```bash
export ORG_GRADLE_PROJECT_intellijPlatformPublishingToken='YOUR_TOKEN'
./gradlew publishPlugin
```

```bash
./gradlew publishPlugin -PintellijPlatformPublishingToken='YOUR_TOKEN'
```

默认发布 channel 来自 `pluginPublishChannels`，多个 channel 用逗号分隔，例如：

```bash
./gradlew publishPlugin -PpluginPublishChannels=beta
```

发布命令：

```bash
./gradlew publishPlugin
```

只有维护者明确要求并准备好凭据时才执行发布。

## 发布后检查

- Marketplace 页面版本正确。
- 插件可安装。
- 更新说明显示正常。
- 核心功能在安装版中可用。
- 记录用户反馈和回滚风险。

## 回滚

如需回滚：

- 确认问题版本和影响范围。
- 回滚代码或发布修复版本。
- 保留可复现步骤和日志。
- 更新 changelog 或 release notes。
