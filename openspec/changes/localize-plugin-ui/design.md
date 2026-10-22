# Design

## Context

见 proposal 的 Why。当前 DynamicBundle 只有中文 base 文件；英文 IDE 使用 ROOT bundle。设置的部分分类名在 Java/XML 写死英文，欢迎 HTML 写死中文；上传入口以“语雀”作为分派值。原生平台为 IU262，最低验证平台为 IU261；只用现有 Java 与公开平台能力。

## Goals / Non-Goals

目标为主要操作界面在英语和简体中文 IDE 下语言一致，并保持设置状态、生成结果、凭据与路由不变。非目标为其它语言、翻译用户文本/源码/平台响应、在线文档全译、运行时语言切换或新增设置。IDE 修改语言后重启生效。

## Decisions

- base `DocViewBundle.properties` 使用英语，`DocViewBundle_zh_CN.properties` 保存完整简中 UI 翻译，通过现有 DynamicBundle 解析。英语在平台中解析为 ROOT，不能只增加 `_en` 文件。未翻译 locale 使用 base 英语，不注册 internal languageBundle EP。遵循[平台随包翻译与查找顺序](https://plugins.jetbrains.com/docs/intellij/providing-translations.html)。
- 生成规则帮助使用独立“固定说明 / 字段 / 直接参数”消息段组合，不再匹配中文标点。问号仍使用 ContextHelpLabel；短摘要不变成常驻长段落。
- Java 表单、action 状态、通知、目录空态和弹窗抽取可见文案；XML actions 使用平台 `action.<id>.text/description`、groups 使用 `group.<id>.text/description` 约定及可读英文 fallback；configurables 使用平台 bundle/key 属性，禁止把 `%key` 当通用 XML 插值。用真实 ActionManager presentation 验证资源解析。保留所有 action/configurable IDs、图标与线程生命周期。
- 现有 GUI Designer `.form` 生成字节码会用 JVM 默认 locale 的 `ResourceBundle.getBundle`，不能代表 IDEA UI locale。保留表单布局，在构造器完成 UI 初始化后用 `DocViewBundle.message` 明确设置可见文案；必要时为标签增加绑定字段，不改全局 Locale、不递归按文本猜测翻译。用系统 locale 与 UI locale 不同的回归覆盖此边界。
- 英文原生900×700窗口发现模板顶栏摘要省略、FlowLayout固定示例说明换行后被裁切。模板顶栏改为可换行摘要与独立动作行，预览按钮与示例提示分行；保留三工作区与编辑器主要空间，用窄viewport布局和原生复验保护可读性。英文分类名使用 `Template and Output`，避免标题助记符对 `&` 的解析差异，不新增标点转换层。
- 用户后续原生反馈指出页头两枚大按钮和大块留白不合理：模板及其他设置页的指南改用原生问号图标，悬浮显示简短用途和打开 GitHub 的链接，靠近对应说明/分区，不再用大按钮或常驻文字链接；恢复默认模板移入模板编辑工作区，与变量入口同一操作行，不占页头、不出现在预览或输出工作区。使用可容纳两种语言的布局，窄窗口操作不换行裁切；仍恢复两个模板的草稿且仅 Apply 后保存，不新增确认或状态语义。
- 上传平台用稳定标识分派，显示 label 通过 bundle 提供；本轮不引入旧显示名别名。菜单与弹窗选择返回标识而非翻译后字符串。
- 欢迎/更新 HTML 提供英文 base 和简中随包资源，保留 CSS、主题变量、内部 action ID、CSP 和外链白名单；根据平台 locale 选择正文。错误/状态使用消息资源，不加载远端正文。
- 原生指南点击发现地址错误指向 `develop`；按维护者指定，设置指南、欢迎/更新页指南目录及完整变更记录统一指向 GitHub `master`，中英正文与精确外链白名单同步。分支修正不改变文章路径、默认浏览器行为或草稿状态；远端正文未发布导致的 404 单独记录，不为消除 404 擅自推送。
- `template.spring.init`、`template.dubbo.init` 等用于默认生成内容的资源不翻译；默认/用户模板、生成 Markdown/JSON、业务节点名、注解标识、URL、平台身份保持。动态插值内容转义规则不变。
- 根任务负责同一工作树的集成测试；三个实现单元分别负责核心消息、Java/XML界面和欢迎页，不并发修改消息文件。测试先取得旧实现的语言覆盖失败，再做最小替换。

## Risks / Trade-offs

- [本地化改变业务路由或生成 golden] → 用稳定平台 ID 测试及原有全部输出 golden 验证；新旧默认模板逐字比较。
- [遗漏硬编码/消息参数或单引号] → 全量 key 对齐、参数占位符/MessageFormat 测试、可见文本扫描及 UI 状态专项；不翻译 debug/源码文本。
- [英语更长造成裁切] → 原生窄窗口和五组问号阅读检查；深浅主题使用原生颜色，保留滚动和短灰字。
- [语言切换误影响用户环境] → 只在隔离 profile 与 doc-demo 验收，备份 locale/窗口/项目设置并恢复普通 IDEA。

## Migration Plan

不迁移持久化状态。安装新包并按 IDEA 语言重启即可；回退插件版本恢复旧呈现。先执行资源/路由/欢迎专项，再执行全量 test、buildPlugin 和 IU261 Verifier；原生英文和简中检查单独记录，不把自动回归等同于原生全矩阵通过。
