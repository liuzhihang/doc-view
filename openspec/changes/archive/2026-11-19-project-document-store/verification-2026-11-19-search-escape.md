# 搜索卡片 Escape 导航续验

候选版本 `1.7.2-SNAPSHOT`。

- 新增用例先在原实现上复现：搜索页按钮取得焦点后无 Esc 返回动作。
- 第一份修复包 `bda74bbeb76c62868dac46fa06102bce18eefbb0f4791fe104cc486de6d333eb` 在组件测试中通过。IDEA IU-263.7307.200 的首轮原生操作未使“更多结果”按钮离开焦点；后续正控确认按进程投递的 Esc 对搜索框也不生效，此轮不能判定产品是否通过。保留操作证据 `build/search-escape-native-20261119/evidence/first-candidate-result.json`。
- 第二份修复包 `229dd11893abfd17539e0ac74e1cfbbc288f1ea4b84561347399c24d0001e7a6` 将 Esc 直接绑定在搜索框、结果列表、返回按钮、“更多结果”和“打开 Markdown”上。17 项定向测试执行，0 失败、0 错误、0 跳过，检查搜索控件返回目录后清空结果，且目录树不会继承搜索动作。Java 25 构建和平台项目配置检查通过。全量测试共执行 576 项，0 失败、0 错误、0 跳过。Plugin Verifier 在 IU-263.7031.23 和 IU-263.7307.200 均报告兼容，仍有既有的实验 API 提示。自动化证据位于 `build/search-escape-final-20261119/`。
- 在隔离 IDEA IU-263.7307.200 的中文界面，确认前台为隔离实例后发送原生 Esc 按键。搜索框、已取得焦点的“更多结果”按钮和已取得焦点的“打开 Markdown”按钮均返回目录，原生可访问性读回确认焦点回到接口树。后两项见 `build/search-escape-native-20261119/evidence/v2-more-{before,after}.json` 和 `v2-open-{before,after}.json`。返回箭头与结果列表的 Esc 另有组件测试覆盖。汇总 `build/search-escape-native-20261119/evidence/final-result.json` 记录了隔离实例正常退出，以及退出前后 147 个虚构项目源码及生成文档的 SHA-256 一致。

继续区分目录树的按键速搜与已生成文档的字段检索，无新增按钮。现有 10,000 条接口的温查询样本 p95 为 17.606–18.445 ms（不含界面绘制与输入防抖），基于[既有测量](verification-2026-11-19-search-paging.md)，这次没有修改查询性能路径。
