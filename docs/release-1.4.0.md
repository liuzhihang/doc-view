# 1.4.0 发布评估（草稿）

日期：2026-10-22

状态：最终候选包已构建，并已在官方 IU 261.22158.277 中进行部分原生 smoke；全量验收仍有明确缺口。维护者已授权提交、创建 PR、合并、打 Release tag、创建 GitHub Release 和发布 Marketplace；截至本记录，这些 Git 操作与发布操作尚未执行。

本文记录发布候选的可追踪证据和未完成验收，不表示所有 OpenSpec change 已完成或归档。完整功能说明见 [`CHANGELOG.md`](../CHANGELOG.md) 的 1.4.0 条目；用户指南入口见 [`docs/user-guide/index.md`](user-guide/index.md)。

## 包与自动门禁

| 项目 | 结果 |
| --- | --- |
| 插件包 | `build/distributions/Doc View-1.4.0.zip` |
| ZIP SHA-256 | `20544d74a46bf0eef31142fd5fc6e2b0b1e2e944a0993e159297089a26ac9f1a` |
| 包清单 | 309 个条目；仅含 `Doc View/lib/doc-view-1.4.0.jar`；审计字段 `forbidden=[]` |
| 自动测试 | 49 suites、237 tests；0 failures、0 errors、0 skipped |
| Gradle 门禁 | 18 个 actionable tasks 均执行；构建成功 |
| Plugin Verifier | `IU-261.22158.277`：`Compatible` |
| 插件兼容基线 | `since-build=261`；该结果只覆盖上述 IU build，不代表所有后续 IDE 版本均已验证 |

机器可读清单：`/tmp/doc-view-release-20261022/final-artifact.json`。完整门禁日志：`/tmp/doc-view-release-20261022/final-validation.log`。日志仍有 `SpringDocViewServiceImpl` 使用或覆盖 deprecated API 的编译提示，以及测试源码的 deprecated API 提示；Verifier 没有报告 compatibility problem。该提示应作为维护跟踪项，不应写成“零弃用警告”。

## 原生验收范围与未完成项

最终包已在官方 IU 261.22158.277 和专用 `/Users/liuzhihang/workspace/doc-demo` 中启动。最终 smoke 记录位于 `/tmp/doc-view-release-20261022/`：IDE 日志确认加载 1.4.0 且 JCEF sandbox enabled；升级通知显示 1.4.0；参数编辑器真实打开并显示 `UserReqVo` 的 4 个字段；Help → Doc View Welcome 显示随包 HTML、1.4.0 版本信息和英文宽排版（截图 `140-welcome.png`）。参数编辑器字段写回没有完成，因此这不是 Schema 原生写回通过。此前 candidate235 中实际打开 Clear Cache 目录菜单后 popup 关闭，树中 5 个类节点仍在；这只记录该旧候选的部分动作和结果，不能替代最终包完整菜单任务验收。旧候选的结果不能自动视作最终 ZIP 的全量原生验收。

截至本草稿，以下是明确尚未完成的验收边界。它们表示“缺少完整验证证据”，不表示已观察到运行时缺陷：

| 范围 | 尚缺证据 | 记录 |
| --- | --- | --- |
| Preview / JCEF | HTML panel 中空结果、模板失败/恢复、取消和快速切换的可见状态替换组合；重复打开/关闭后的资源基线。已观察到若干关闭路径清空 panel/editor 字段，但这不证明对象已 GC 或长期资源稳定。 | [`fix-preview-lifecycle`](../openspec/changes/fix-preview-lifecycle/verification.md)、[`doc-generation-performance`](../openspec/changes/doc-generation-performance/verification.md) |
| OpenAPI Schema 写回 | 自动测试覆盖 PSI 写回与批次预检；最终包中可打开参数编辑器并读取 4 个字段，但没有完成真实编辑、确认、源文件读回及保留其他 Schema 属性的完整原生验收。 | [`openapi3-schema-required-mode`](../openspec/changes/openapi3-schema-required-mode/verification.md) |
| PasswordSafe 异常路径 | OS 授权拒绝和后端存储失败后的设置错误提示/重试尚未验证；项目隔离、正常保存/重启读回等已有单独原生证据。 | [`integration-credential-storage`](../openspec/changes/integration-credential-storage/verification.md) |
| 取消与性能 | 最终包完整 Spring/Dubbo 自然耗时分布、可见取消终态及无部分结果的组合证据尚不完整；受控断点暂停不作为自然耗时数据。 | [`doc-generation-performance`](../openspec/changes/doc-generation-performance/verification.md) |
| Clear Cache | candidate235 中打开了目录右键菜单并触发 Clear Cache，随后 popup 收起且 5 个类节点仍在；这不是最终 1.4.0 包的完整目录菜单/选中上下文验收。 | [`clear-cache-safety`](../openspec/changes/clear-cache-safety/verification.md) |

最终包的 Welcome HTML 已确认可见；这不覆盖 API Preview 的 JCEF 特殊状态与关闭后资源矩阵。所有原生范围仍按对应包 hash 区分，不可用旧包截图或自动测试替代未执行的最终包交互。

### 发布阻断与验证缺口

- **兼容性：** 当前可见门禁没有已确认的硬兼容性阻断；Plugin Verifier 对一个明确的 IU 261 build 给出 `Compatible`。更高 IDE build 未验证，属于兼容范围限制，不应扩大表述。
- **安全：** 当前证据没有确认的凭据泄露或 PasswordSafe 缺陷。OS 拒绝和写入失败重试是未覆盖的安全路径验证项，不能标记通过；也不能据此声称完成了全面安全审计。
- **发布流程：** [`发布检查清单`](release-checklist.md)要求活动 OpenSpec tasks 完成；本次仍有 tasks 未勾选，因此不能把 checklist 描述为全通过。维护者已明确授权继续提交和发布；按此授权继续时，保留以上验证边界，不归档仍未完成的 change。
- **已排除：** 按维护者要求，本轮不进行 YApi、ShowDoc、语雀真实上传；这表示未执行，不表示平台端到端通过。
- **独立 PoC：** `tools/mcp-java-poc/` 是开发可行性探针，不是 1.4.0 生产功能，不打入生产包；它的真实 IDE MCP 注册/协议验证另行跟踪，不自动构成该版本阻断。正式 MCP 服务也不作为 1.4.0 已交付能力。

## 版本与发布操作

1. 最终包原生烟测结束后，核对 [`CHANGELOG.md`](../CHANGELOG.md)、README、插件描述和在线用户指南与 1.4.0 实际交付一致；帮助链接须在目标分支公开可访问后复验。
2. 核查 Git 工作区与 diff，只纳入本次已授权的产品变更和发布文档；记录最终发布 commit。
3. 按项目策略从 `develop` 创建 PR 并合并到 `master`，确认 tag 指向的 commit 与发布版本一致。
4. 创建与 tag/包版本相同的 GitHub Release，再按授权发布 Marketplace。
5. 发布后核对 GitHub tag、Release、Marketplace 版本与包 hash，并按清单完成安装版核心 smoke test。

升级时用户需在新版设置中重新填写平台凭据；旧 XML 凭据不会导入或自动删除。回退可安装此前版本（当前前一版本为 1.3.14），但新版模板/设置和凭据状态不承诺由旧版兼容接管；用户需按旧版功能重新配置。
