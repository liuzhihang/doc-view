## 1. 实现

- [x] 1.1 添加依赖和 null/空列表回归测试，并通过针对性 test 记录旧实现失败。
- [x] 1.2 替换两个生产类的 CollectionUtils，升级 1.3.14 并更新 changelog/版本摘要。

## 2. 验证

- [x] 2.1 运行 ./gradlew test buildPlugin verifyPluginProjectConfiguration，检查测试数、ZIP 和版本。
- [x] 2.2 运行 ./gradlew verifyPlugin 及精确用户 build 前向验证，记录结果和手工测试剩余风险。
- [x] 2.3 运行 openspec validate remove-commons-collections --strict 和 git diff --check，记录交付证据。
