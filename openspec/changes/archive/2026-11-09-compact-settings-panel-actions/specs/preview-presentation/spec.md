## ADDED Requirements

### Requirement: 轻量的 GitHub Star 入口
系统 SHALL 以带 GitHub logo 的轻量链接显示 Star on GitHub，保留合适的字距、命中区、键盘访问与深浅主题可读性；不使用突出的大 CTA。点击 SHALL 仅打开现有 Doc View GitHub 仓库，不自动执行 Star 或其他账户动作。

#### Scenario: 打开项目仓库
- **WHEN** 用户点击或通过键盘激活 Star on GitHub
- **THEN** 浏览器打开既有仓库链接，设置草稿与持久化值不改变
