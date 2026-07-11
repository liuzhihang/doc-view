## 1. OpenSpec 与入口治理

- [x] 1.1 完成 Speckit skill 删除和 OpenSpec skill/config 纳管，确保 `openspec list/status` 可用
- [x] 1.2 将稳定项目上下文和 proposal/design/spec/tasks 规则写入 `openspec/config.yaml`
- [x] 1.3 重写 `AGENTS.md` 的变更范围、文档索引和 AI 工作流入口，移除一次性白名单与 Speckit 标记

## 2. Canonical docs

- [x] 2.1 创建并核对 `docs/architecture.md`、`docs/contract-design.md` 与 `docs/performance-guide.md`
- [x] 2.2 创建并核对 `docs/intellij-compatibility.md` 与 `docs/release-checklist.md`
- [x] 2.3 创建 OpenSpec 版 `docs/ai-workflow.md` 和 `docs/roadmap.md`，删除 Speckit 流程表述与遗留 `docs/contracts/**` 单次记录

## 3. Repo-local skills

- [x] 3.1 更新 contract/docs/java/release 四个 Doc View skill 的 Read First、OpenSpec 分工和验证要求
- [x] 3.2 检查所有 skill 与 `AGENTS.md` 引用的 canonical doc 均存在且无重复规则漂移

## 4. 凭据安全

- [x] 4.1 将 `src/test/http/YApiGetTest.http` 的 endpoint、YApi base URL 和 token 改为 HTTP Client 变量
- [x] 4.2 在 `.gitignore` 排除 HTTP Client 私有 environment 文件，并在工作流/发布文档记录外部轮换与历史清理边界

## 5. 验证

- [x] 5.1 验证 OpenSpec artifacts apply-ready，且 OpenSpec CLI 能解析 change
- [x] 5.2 运行 canonical doc/skill 链接、Speckit 残留和 tracked 凭据占位符检查
- [x] 5.3 运行 `git diff --check` 和 diff 范围检查，确认没有 Java/runtime/Gradle/plugin.xml 行为改动
