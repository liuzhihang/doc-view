# Spec Delta

## Purpose

在 IDE 编辑区提供本地 HTML 欢迎与当前版本更新介绍，完整使用文档留在 GitHub；以明确入口、低密度布局和可预测的版本及资源生命周期支持用户。

2026-10-19 本修订替换此前“七章离线手册”目标。Round58 已实现本修订，旧手册验证不视为本 contract 通过；后续布局修正单独记录。

## ADDED Requirements

### Requirement: 欢迎与更新页在编辑器中可重复打开
系统 SHALL 从 Help 菜单与 Find Action 提供 Doc View 页面入口。同一项目重复打开 SHALL 聚焦既有标签；页面 SHALL 展示已安装版本、产品简介、常用操作和本版本重点变化。

#### Scenario: 手动重新打开
- **WHEN** 用户在同一项目连续执行打开动作或关闭后重新执行
- **THEN** 系统 SHALL 复用已打开标签或正常重建已关闭页面
- **AND** SHALL NOT 接管普通 Java/Markdown 文件或打开其他项目的设置

#### Scenario: 常用操作
- **WHEN** 用户点击接口目录、模板设置或平台设置入口
- **THEN** 系统 SHALL 导航到当前页面所属项目的对应视图
- **AND** SHALL NOT 自动生成、上传、Apply 或 Reset 设置
- **AND** 已存在的未保存设置草稿 SHALL 保持

### Requirement: 页面使用本地 HTML，完整文档位于 GitHub
系统 SHALL 使用随插件交付的 HTML/CSS 资源呈现欢迎和更新页，SHALL NOT 内嵌完整用户指南、模板教程、平台教程、故障排查或七章手册导航。完整文档 SHALL 在 GitHub 仓库维护。

#### Scenario: 无网络打开首页
- **WHEN** 网络不可用且运行环境满足目标 IDE 的 JCEF 要求
- **THEN** 欢迎和更新内容 SHALL 正常呈现
- **AND** 页面加载 SHALL NOT 请求远端正文、字体、脚本、图片或账号认证

#### Scenario: 在线使用指南
- **WHEN** 用户点击欢迎页或设置页的文档入口
- **THEN** 系统 SHALL 在外部浏览器打开对应 GitHub 文档或明确章节锚点
- **AND** SHALL NOT 将在线手册加载到欢迎页内或自动下载离线副本

#### Scenario: 受控导航
- **WHEN** 页面收到不属于预定本地动作或 HTTP/HTTPS 外链的目标
- **THEN** 系统 SHALL 拒绝打开目标
- **AND** SHALL NOT 读取任意本地文件、执行任意 IDE 动作或将远端页面作为欢迎正文

### Requirement: 欢迎与更新内容有清晰层级
系统 SHALL 以简短介绍、主要操作、少量入门摘要和当前版本亮点组织页面，SHALL NOT 将整份 CHANGELOG 或长教程堆入首页。

#### Scenario: 宽窄与主题
- **WHEN** 用户在深浅 IDE 主题、Compact 开/关、约 600px 窄编辑区或宽编辑区打开页面
- **THEN** 文字、按钮和焦点态 SHALL 清晰可见，区块 SHALL 有可辨留白
- **AND** 宽窗口 SHALL 利用可用编辑区扩展内容而非固定窄栏，并保留适度上限与左右留白；窄窗口 SHALL 转为单列且不因布局裁切正文或动作
- **AND** Compact SHALL NOT 通过缩小正文强行容纳更多内容

#### Scenario: 宽屏阅读区域
- **WHEN** 编辑区可用宽度约为 2560 CSS px
- **THEN** 页面外框 SHALL 扩展至约 1800 CSS px，正文区域 SHALL 不少于 1600 CSS px
- **AND** SHALL 保持现有正文字号、区块间距及长段落的阅读行宽限制
- **AND** 在约 600 CSS px 的编辑区 SHALL 无页面水平溢出

#### Scenario: 版本准确
- **WHEN** 用户查看本地更新页
- **THEN** 页面 SHALL 使用安装插件的版本身份和对应更新内容
- **AND** 正式发布构建 SHALL NOT 显示开发预览或未发布标识
- **AND** 开发构建的未发布变更 SHALL 明确标识为未发布，SHALL NOT 冒充某个已发布版本的功能
- **AND** 完整历史 SHALL 通过明确的 GitHub 外链访问

### Requirement: 首次与升级提示有明确频率
系统 SHALL 按 IDE 应用级状态记录已提示版本；首次使用 SHALL 自动打开欢迎页一次，后续升级 SHALL 仅提示一次可打开更新页的通知；相同版本重复打开项目 SHALL 不再次提示。

#### Scenario: 多项目首次启动
- **WHEN** 同一 IDE 首次加载插件且有多个项目启动
- **THEN** 系统 SHALL 只在一个有效项目自动展示欢迎页
- **AND** 各项目手动打开的页面与设置动作 SHALL 相互隔离

#### Scenario: 升级与关闭提醒
- **WHEN** 已提示过旧版本且用户没有关闭更新提醒
- **THEN** 新版本 SHALL 仅显示一次更新通知并提供本地更新页入口
- **AND** SHALL NOT 自动抢占编辑器
- **AND** 关闭提醒后 SHALL 不再出现升级通知，但手动入口仍可用

### Requirement: 浏览器随编辑器释放
系统 SHALL 保持 UI 可响应，关闭页面或项目时 SHALL 释放其拥有的浏览器、client 和监听器并忽略迟到回调；SHALL NOT 保留旧 Swing/Markdown reader 作为兼容渲染路径。

#### Scenario: 关闭与重开
- **WHEN** 用户在页面加载期间关闭 editor 或项目，再重新打开页面
- **THEN** 旧回调 SHALL NOT 写回已关闭界面或覆盖新页面
- **AND** 旧浏览器及监听器 SHALL 释放，新页面 SHALL 正常显示

#### Scenario: JCEF 不可用
- **WHEN** 当前运行环境不满足 JCEF 要求
- **THEN** 系统 SHALL 在 FileEditor 原生容器中提供简短状态说明和 GitHub 文档外链
- **AND** SHALL NOT 创建第二套手册阅读器或备用浏览器实现

#### Scenario: 索引期间打开
- **WHEN** 项目正在建立索引
- **THEN** 用户 SHALL 仍能打开欢迎与更新页
- **AND** 页面 SHALL NOT 触发全量 PSI 扫描
