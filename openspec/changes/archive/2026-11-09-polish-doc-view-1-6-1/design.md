# Design

## Context

见 proposal。基线为 v1.6.0 commit 67d54b7，独立 checkout/分支。类标题当前拼接全部注释并删除空格，方法固定截15个UTF-16单元，方法描述重复首行。模板已有Markdown编辑器但仅预览源码且跳tab；popup 使用 Markdown provider。选择类名已快照化，captureAllExport 仍访问 PSI getName。旧 release 原生例外只适用1.6.0，不能转为本轮通过证据。

## Goals / Non-Goals

目标是实现各 delta contract 并保持现有上传、导出、参数解析、文档库及 HTTP 功能。保持 Java-only、现有依赖及平台基线。不给旧模板写法增加迁移或双轨实现，不重构全站 UI，不批量归档旧 changes。

## Decisions

- 新增专用于类/方法的 Javadoc 普通正文解析，使用 PSI block tag 边界截取原文，再规范化注释边框和行结尾；首个非空物理行是标题，其后正文保留段落、英文空格和 inline tags。字段及参数旧入口不受影响。上限默认80、范围1–200、按code point截断。类固定首行，方法新增默认true的仅首行选项；描述来源开关默认true，作用于类和方法的普通正文fallback。
- 新增 DocView.classDesc 与 DocViewData.classDesc，Spring/Dubbo 都构建；模板变量暴露且中英默认模板仅在非空时加“类说明”。结构化文档库v1继续已有字段，不改变格式版本或旧详情读取；Markdown保留类说明，方法desc不承担类描述。固定模板示例补上类说明。相关缓存及指纹包含新设置。
- 设置草稿控件先校验上限再原子应用同页；Reset/取消遵循现有Configurable。模板编辑器保留Markdown高亮，叠加Velocity引用/指令的主题文本属性，变量列表使用表达式强调；不增加Velocity bundled plugin依赖。
- 同面板预览保留当前框架tab，用内容卡片/组件切换呈现MarkdownHtmlPanel；复用现有后台协调器的代次和dispose机制，HTML生成在后台read action，主题高亮复用bundled Markdown公开API，JSON语言的独立Java词法着色在共享预览工具中完成：平台HTML没有JSON provider时原本无颜色，使用当前EditorColorsScheme对安全转义后的JSON字符串、数字、关键字着色，不增加JSON插件依赖，不改变原始Markdown。
- 预览所需简单类名与全名均在后台加载中捕获，UI回调只取快照。目录边框改为无边框scrollpane+既有单像素splitter，不改交互结构。
- README首屏只留能力摘要、安装要求、快速开始和指南导航；完整细节链接用户指南。版本改1.6.1并更新changelog和欢迎页，保留过去记录。

## Risks / Trade-offs

- [Javadoc输出有意变化] → PSI fixture、完整golden及上传/导出回归；只修改受本contract影响的断言。
- [模板高亮平台API差异] → 本机SDK读源码/签名、精确两build Verifier及原生深浅主题检查；禁止新增internal/deprecated调用。
- [JCEF与异步生命周期] → 重复切换、迟到结果、关闭/重开自动及原生检查。
- [发布网络权限] → 初次SSH fetch拒绝已通过正式require_escalated审批解除，fetch成功；后续操作遵循正式审批，不改协议或代理规避。最新远端master仅增加1.6.0发布结果文档。

## Migration Plan

用户升级后使用新的普通Javadoc默认值；自定义模板可加入${DocView.classDesc}或显式恢复默认模板。保留1.6.0兼容平台基线。按仓库develop→master流程，确认新版本未占用后发布；回滚选择1.6.0。任何必要验收未通过均不宣称发布就绪。
