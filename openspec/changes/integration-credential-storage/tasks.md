# Tasks

## 1. 项目级凭据服务

- [x] 1.1 为 `IntegrationCredentialService` 增加包内可替换的存储 seam 和内存 fake；覆盖项目/平台隔离、ShowDoc 成对更新、替换/保留/清除操作及禁止 EDT 直接调用阻塞 API。运行 `./gradlew test --tests 'com.liuzhihang.doc.view.service.IntegrationCredentialServiceTest' --rerun-tasks`，确认失败来自行为断言而不是编译或 fixture 初始化。
- [x] 1.2 实现项目级 PasswordSafe service，ShowDoc 双凭据使用现有 Gson 编成一个安全存储值；key 不写入可见 identity metadata，service 不缓存明文。重跑 targeted test，确认 fake-store 场景通过且没有新增依赖或外部服务调用。
- [x] 1.3 用目标 IU 261 的真实 `InMemoryCredentialStore`（继承 `BaseKeePassCredentialStore`）适配 `SecretStore`，回归同平台两项目保存/读取/清除互不覆盖，并验证不同平台条目仍独立；先用旧 serviceName 实现确认 RED（A 在 B 写入后读不到原凭据），再把项目 hash 加入生成 serviceName 的 platform key 后 targeted GREEN。仅本次新键策略，不迁移或读取旧共享 serviceName 条目；原生 OS keychain 验证仍由 4.3.d 跟踪。

## 2. 设置状态与表单

- [x] 2.1 为设置序列化和表单生命周期增加回归测试，覆盖新状态无 secret、普通字段保留、密码控件、零字符输入保留、非空输入原样替换、显式清除、异步状态、Reset、Cancel 和存储失败。运行 `./gradlew test --tests 'com.liuzhihang.doc.view.contract.IntegrationCredentialLifecycleTest' --rerun-tasks`，确认失败指向契约行为而非 fixture 初始化。
- [x] 2.2 从 ShowDoc/YApi/YuQue 项目状态移除凭据字段，保持现有 storage 名称和非敏感字段；表单改用密码控件、显式清除意图、仅返回状态的异步读取及生命周期安全的回调。重跑 targeted lifecycle test，确认新序列化状态不含凭据明文。
- [x] 2.3 使用不可取消的 modal 后台任务执行 PasswordSafe 操作，成功后再提交非敏感状态；失败时保留草稿与已保存状态，并显示脱敏错误。验证失败重试、Reset/Cancel 不写入、Apply 后再 Cancel 保留已应用值。
- [x] 2.4 更新 `docs/user-guide/integrations.md` 与 `CHANGELOG.md`，说明升级后需重新填写凭据、遮蔽/清除操作及旧 XML 在正常重写前可能仍留有明文；核对链接与示例无真实凭据并运行 `git diff --check`。

## 3. 上传凭据访问

- [x] 3.1 增加三平台 fake-backed 上传测试，断言配置检查和凭据读取不直接阻塞 EDT、使用当前项目/平台身份、凭据缺失时停止请求，且错误/日志不泄露凭据。确认测试不访问网络服务。
- [x] 3.2 将上传配置检查与 PasswordSafe 解析移入现有后台任务；三平台 service 从凭据服务读取凭据并填入原请求字段，不改协议。让 `PreviewForm` 和 `PreviewRightUploadAction` 都改调后台 wrapper；验证所有上传入口均在后台读取凭据并重跑 service tests。

## 4. 集成验证

- [x] 4.1 运行 `./gradlew test --rerun-tasks`；检查测试报告，确认凭据服务、设置生命周期、上传路径和既有测试均被发现并通过，没有未解释的跳过项。
- [x] 4.2 运行 `./gradlew verifyPluginProjectConfiguration buildPlugin verifyPlugin`；检查 Plugin Verifier 对声明 IDE 矩阵的报告，确认没有新增 deprecated/internal API 使用或 descriptor/configuration 错误。
- [x] 4.3.a 在隔离 IDEA profile 中保存仅供测试的本地 ShowDoc 地址和合成凭据，重启并重新打开设置；确认地址仍保存、两项凭据显示已配置，且测试值未写入项目 XML。只核对 UI 状态与项目 XML，不读取凭据值或系统 PasswordSafe 内容，也不上传。
- [x] 4.3.b 在 ShowDoc 设置页勾选两个凭据清除框并 Apply，确认两项均显示未配置且 Apply 随后禁用；此项只证明当前会话中的清除状态。
- [x] 4.3.c 清除后正常退出并重启隔离 profile，重新打开 `doc-demo` 的 ShowDoc 设置；确认 API Key、API Token 均显示未配置且 Apply 禁用。Round 29 使用包 SHA-256 `92879354d4be39148439627e573b76579bcecfec680c98a432624a7bdee2c029`；只核对 UI，不读取系统凭据库内容，不发起真实平台请求。
- [ ] 4.3.d 在原生 IDEA 中验证 OS 凭据提示/拒绝、密码遮蔽、Reset/Cancel、存储失败后重试、项目间隔离及普通设置保留；无法交互验证的情形继续标为未验证。
- [x] 4.4 运行 `openspec validate integration-credential-storage --strict` 和 `git diff --check`；复核最终 diff 只包含本 change、相关测试及直接所需文档，无迁移/清理、外部服务调用、提交或发布。
