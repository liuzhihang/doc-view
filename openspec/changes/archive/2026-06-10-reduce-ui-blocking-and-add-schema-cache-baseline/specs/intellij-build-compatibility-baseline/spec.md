## ADDED Requirements

### Requirement: 性能基线发布验证

项目 release preparation SHALL 在发布插件前，针对受支持的 IntelliJ IDEA 兼容范围验证性能基线。

#### Scenario: 准备发布版本

- **WHEN** 性能基线实现已准备发布
- **THEN** 维护者在创建 Marketplace artifact 前更新插件版本元数据和 release notes 或 changelog entry

#### Scenario: 运行自动化发布验证

- **WHEN** 维护者准备该基线的插件产物
- **THEN** 运行 `./gradlew test`、`./gradlew buildPlugin` 和 `./gradlew verifyPlugin`，或明确记录跳过原因

#### Scenario: 运行手动 IDE 验证

- **WHEN** 维护者在 `./gradlew runIde` 中验证插件
- **THEN** 验证启动时不再出现已报告的启动错误，并验证 preview generation、refresh、cache clear、export、upload preparation、recursive DTO handling 和 Markdown rendering responsiveness

#### Scenario: 请求 Marketplace 发布

- **WHEN** 维护者发布插件到 JetBrains Marketplace
- **THEN** 只有在自动化验证、手动 IDE smoke testing、版本元数据更新完成，并且维护者明确确认且所需凭据可用后，才执行发布

### Requirement: 启动兼容性错误已修复

Doc View SHALL 避免在 IntelliJ startup 阶段因不安全的 service 请求或重型工作触发启动时报错。

#### Scenario: Startup activity 在受支持 IDEA 上运行

- **WHEN** `DocViewStartupNotification` 或任意 Doc View startup hook 在受支持 IntelliJ IDEA 版本上运行
- **THEN** 它不会在 class initialization 期间触发不安全的 service access
- **AND** 它不会启动重型 PSI scanning、DTO parsing、Markdown rendering、export、upload 或 cache prewarming

#### Scenario: Startup notification 保持稳定

- **WHEN** startup notification 满足展示条件
- **THEN** 现有 notification title、message、actions 和 trigger conditions 保持稳定，仅调整兼容性安全的 service access timing
