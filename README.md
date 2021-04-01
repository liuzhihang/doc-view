Doc View
=======

[![JetBrains Plugins](https://img.shields.io/jetbrains/plugin/v/15305-doc-view.svg)](https://plugins.jetbrains.com/plugin/15305-doc-view)
[![Version](http://phpstorm.espend.de/badge/15305/version)](https://plugins.jetbrains.com/plugin/15305-doc-view/versions)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15305-doc-view.svg)](https://plugins.jetbrains.com/plugin/15305-doc-view)
[![License](https://img.shields.io/badge/license-MIT-red.svg)](https://github.com/liuzhihang/toolkit/blob/master/LICENSE)

特征
----

- 生成接口文档
    - 生成请求返回参数列表
    - 生成请求返回参数示例
- 支持 Spring Controller
- 支持 Dubbo 接口
- 支持 `validation` 相关注解
- 复制 Markdown 文本剪贴板
- 导出 Markdown 文件
- 支持自定义 Markdown 模版
- 批量导出
- 支持界面编辑文档, 注释, 同时会保存回代码注释中
- 支持在编辑实体界面, 将实体复制为 Json 字符串

**注：因 API 问题，新插件仅支持 2020.1 和 2020.2 版本 。**


待办
----

- [ ] 批量生成（当前仅支持单个类或者方法）
- [ ] 面板支持查看 Markdown 源文本
- [ ] 支持从 Swagger 注解获取字段相关信息
- [ ] 支持 Setting 设置
- [ ] 支持上传到 YApi
- [ ] 支持 ToolWindow 查看

演示
----

![1111-l7NaTW](https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/1111-l7NaTW.gif)

[更多截图演示](https://github.com/liuzhihang/doc-view/discussions/17)

安装
----

- **在线安装:**
    - `File` -> `Setting` -> `Plugins` -> 搜索 `Doc View`

- **手动安装:**
    - [下载插件](https://github.com/liuzhihang/doc-view/releases) -> `File` -> `Setting` -> `Plugins`
      -> `Install Plugin from Disk...`

使用
----

- 右键菜单选择 `Doc View`

更新
----

## [v1.0.9](https://github.com/liuzhihang/doc-view/releases/tag/v1.0.9) (2021-04-01)

1. 支持在右键菜单选择 Doc Editor 直接编辑文档
    1. 编辑接口文档名称
    2. 编辑接口描述
    3. 编辑字段是否必填
    4. 编辑字段注释说明
    5. 点击确定, 会回写到源文件的注释中
2. 支持在 Entity 中通过邮件菜单选择Doc Editor 编辑字段信息
    1. 编辑字段是否必填
    2. 编辑字段注释说明
    3. 点击确定, 会回写到源文件的注释中
    4. 支持将 Entity 复制为 Json 字符串
    5. 复制 Json 字符串时, 支持 Entity 中包含对象的转换
3. 从 Doc View 预览界面直接跳转到编辑界面
4. 一些设置说明

[查看更多历史更新记录](./doc/ChangeNotes.md)

关于我
----

欢迎关注公众号：『 程序员小航 』

![wechat-vxgNsq](https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/wechat-vxgNsq.png)

欢迎加我微信进群, 讨论技术或者对插件提出建议. (关注公众号 -> 找到我)

小伙伴们
----

感谢以下小伙伴的参与:

[lvgo](https://github.com/lvgocc)



其他插件
----

&emsp;Toolkit: [https://github.com/liuzhihang/toolkit](https://github.com/liuzhihang/toolkit)


&emsp;copy-as-json: [https://github.com/liuzhihang/copy-as-json](https://github.com/liuzhihang/copy-as-json)

本工具使用 JetBrains IDEA 进行开发
----
<a href="https://www.jetbrains.com/?from=Toolkit"><img src="https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/jetbrains-logo-MrNwcp.png" width="20%" height="20%"></a><a href="https://www.jetbrains.com/?from=Toolkit"><img src="https://cdn.jsdelivr.net/gh/liuzhihang/oss/pic/article/idea-logo-XpnqgG.png" width="20%" height="20%"> </a>


<script defer src="https://plugins.jetbrains.com/assets/scripts/mp-widget.js"></script>