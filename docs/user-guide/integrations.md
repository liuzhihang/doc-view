# 平台集成

Doc View 可将接口文档发送到 YApi、ShowDoc 或语雀。平台配置是可选项：生成、预览、复制和导出都不需要这些账号，只在上传时填写即可。

ShowDoc 和语雀接收 Spring/Dubbo 模板生成的 Markdown。YApi 的路径、参数和 Schema 由接口数据构造，描述正文使用固定 Markdown；修改自定义模板不会改变 YApi 上传内容。

返回[用户指南](index.md)，另见[设置说明](settings.md)、[模板教程](templates.md)和[故障排查](troubleshooting.md)。

## 配置前先确认目标

配置入口是 **Settings → Tools → Doc View → Platform Integrations → YApi Settings / ShowDoc Settings / YuQue Settings**。配置按当前项目保存，IDE 菜单译名可能不同。

可从以下位置上传：

- 源码编辑器中的 Doc View 上传动作。
- 预览工具栏的 **Upload**（当前文档）或 **Upload All**（当前预览列表）。
- 工具窗口工具栏的 **Upload**（当前目录），或目录节点右键菜单的 **Upload**（所选方法、类、模块或根节点）。

预览和目录入口不要求打开 Java 编辑器；项目索引与对应视图数据就绪后可用。选择平台后会直接开始后台上传，没有额外确认页。先核对范围、平台、项目和知识库；只展开平台列表或按 Esc 返回不会上传。

**上传会写入外部平台。** 插件没有单独的“测试连接”操作；不要用上传来试探 Token 或服务地址。首次使用时先选一个确定可以写入的小范围，并确认目标文档允许创建或更新。

## ShowDoc

在 **ShowDoc Settings** 填写：

- **ShowDoc 地址**：站点或私有部署的基址，例如 `https://docs.example.test/server`。不要在末尾再填 `/api/item/updateByApi`。
- **ApiKey**：目标项目开放 API 使用的 `api_key`。通常可在 ShowDoc 目标项目的“项目设置 → 开放 API”中查找，具体菜单名以实例版本为准。
- **ApiToken**：同一项目开放 API 设置中的 `api_token`。

插件会在地址后拼接 `/api/item/updateByApi`：

```text
填写：https://docs.example.test/server
请求：https://docs.example.test/server/api/item/updateByApi
```

Apply 会去掉地址首尾空白和末尾连续的 `/`，然后回填规范化值；`://` 会保留。该地址也用作成功通知中的浏览链接前缀。

上传内容按生成文档的目录标题、文档标题和 Markdown 正文提交。ShowDoc 的开放 API 地址格式与部署方式有关；官方[开放 API 文档](https://www.showdoc.com.cn/page/102098)应与目标实例的版本和部署路由一起核对。

历史指南或旧版自托管实例可能给出完整地址，例如 `https://docs.example.test/server/index.php?s=/api/item/updateByApi`。**不要把完整地址原样粘进当前“ShowDoc 地址”字段**：插件仍会再追加 `/api/item/updateByApi`，形成重复路径。`index.php?s=` 形式如何映射到当前实现，取决于实例路由和部署版本；本指南没有连接或验证该部署，也不承诺兼容。请先向实例管理员确认插件应填的前缀和最终请求地址，再用允许写入的测试项目核对。

> 历史格式示例可参考 [ShowDoc 项目中的旧版讨论](https://github.com/star7th/showdoc/issues/532)；这是旧部署示例，不是当前插件兼容性保证。也可对照[旧版语雀指南](https://www.yuque.com/liuzhihangs/doc-view/obcybw)，其中的字段输入方式可能与当前版本不同。

## YApi

在 **YApi Settings** 填写：

- **YApi 地址**：站点基址，例如 `https://yapi.example.test`。插件会拼接 API 路径，不要填某个 API 的完整 URL。
- **Project ID**：目标项目的数字 ID，例如 `42`。需为十进制数字；不要填项目名称或完整 URL。
- **Token**：与目标项目对应的 YApi Token。

从目标 YApi 项目页面或项目设置中确认数字 Project ID 和项目 Token；界面位置可能随 YApi 版本或自部署定制变化。官方接口文档把 `project_id` 与 `token` 列为分类菜单查询参数，写入前请确认 Token 对应目标项目并有足够权限。

上传按以下顺序处理：

1. 查询 `{地址}/api/interface/getCatMenu?project_id={Project ID}&token={Token}`。
2. 若没有与生成文档目录标题同名的分类，调用 `{地址}/api/interface/add_cat` 创建分类。
3. 调用 `{地址}/api/interface/save` 提交接口。

接口路径见 YApi [OpenAPI 文档](https://github.com/YMFE/yapi/blob/master/docs/openapi-doc.html)。成功通知会链接到 `{地址}/project/{Project ID}/interface/api/cat_{分类 ID}`。

**Project ID 可留空**，保存后表示尚未配置。填写时必须是 Long 范围内的大于零的十进制数；前导零允许，例如 `001` 保存后会回填 `1`。字母、零、负数或溢出会拒绝 Apply，并保留草稿和先前保存的 URL、ID、Token。

Apply 也会去掉地址首尾空白及末尾连续 `/`，再回填规范化地址。可以先保存空白或部分配置；上传前仍要补齐必填项。

现有 YApi 分类查询协议仍把 Token 放在查询 URL 中；本次存储改造不改变该协议。使用 HTTPS，不分享带凭据的请求地址或网络诊断记录。当前项目 XML 不再保存新凭据，旧文件的注意事项见下方“Token 与本地保存”。

## 语雀

在 **YuQue Settings** 分别填写以下信息。公有服务默认站点地址为 `https://www.yuque.com`，API 基址为 `https://www.yuque.com/api/v2`。

- **站点地址**：用于生成上传成功后的浏览链接，例如 `https://docs.example.test`。私有部署填对应站点地址。
- **API 基址**：用于查询、创建和更新文档；私有部署应填写其 API 地址。
- **用户或团队路径**：代码字段为 `login`，填路径标识而非昵称或完整 URL，例如 `team-docs`。
- **知识库路径**：填写路径标识而非显示名称，例如 `api-manual`。
- **Token**：有目标知识库写入权限的账号或管理员提供的 API Token，通过 `X-Auth-Token` 请求头发送。

目标命名空间按 `login/repos` 拼接；上面的虚构示例对应 `team-docs/api-manual`。可从目标知识库的浏览地址中核对用户/团队路径与知识库路径，然后分别填入“用户或团队路径”和“知识库路径”；不同团队或私有部署的 URL 结构请以实例为准。两种 URL 不能互换：API 基址后会追加 `/repos/{login}/{repos}/docs/...`，站点地址则用来构造成功通知链接。官方 [Yuque OpenAPI 元数据](https://github.com/yuque/openapi-metadata/blob/master/yuque.tea)列出文档查询、创建、更新路径及 `x-auth-token` 请求头。

插件根据生成文档的路径生成 slug（去掉开头的 `/`，把路径中的 `/` 换成 `_`），然后先查询该 slug：只有 HTTP 404 表示不存在并允许创建；成功返回有效文档 ID 时更新。401/403、429、服务器错误、空正文或无效数据都会停止该篇上传，不会当成不存在。成功通知链接使用站点地址、`login/repos` 和返回的 slug 组合。

输入框提供虚构地址和路径示例。平台设置页的 `?` 图标提供配置说明，气泡中的指南链接会在默认浏览器打开 GitHub 上的本文，阅读需要网络。

Apply 会去掉两个地址的首尾空白和末尾连续 `/`，并回填规范化值。语雀配置不完整时，页面会提示并打开 **YuQue Settings**；配置检查失败后上传流程立即返回，不会调用平台 API。

## Token 与本地保存

凭据使用遮蔽输入框，并保存在 IDEA PasswordSafe 中，按项目和平台隔离。地址、Project ID、用户/团队与知识库路径等非敏感字段保存在项目 XML 中。页面只显示“已保存”“未配置”或“凭据存储不可用”，不会回填已保存的凭据。

- **保留**：密码框留空且不勾“清除已保存凭据”，Apply 时保留原值。看到空框不代表没有配置，先看状态文字。
- **替换**：填写新值并 Apply。ShowDoc 的 API Key 与 API Token 可分别替换；凭据按输入保存，不自动裁剪首尾空白。
- **清除**：勾选对应“清除已保存凭据”，再 Apply。只勾选后 Cancel 不会清除。
- **撤销草稿**：Revert changes 清空本次密码草稿和清除勾选，重新读保存状态；Cancel 丢弃未应用的草稿。Apply 后再 Cancel 不撤销已经保存的值。

保存凭据时会显示平台进度界面。PasswordSafe 读写失败时，普通字段也不会提交，草稿会保留，可检查 IDE 凭据存储后重试。

YApi ID 校验在凭据写入前执行，因此无效 ID 不会顺带更改 Token。ShowDoc 的 Key 和 Token 作为一组保存，避免只写入其中一项。

**升级后需重新填写凭据。** 本版本不从旧项目 XML 读取或迁移密钥，也不主动清理用户文件。旧的 `DocViewShowDocSettings.xml`、`DocViewYApiSettings.xml`、`DocViewYuQueSettings.xml` 在 IDEA 正常重写前可能仍含旧明文；备份或版本历史也不会被插件清理。分享项目配置前检查这些旧文件，凭据若已外泄应在对应平台轮换。不要把真实 Token 放进 issue、截图、示例或日志。

## 网络请求与错误

填写可直接访问的最终 API 基址。插件不会跟随 301/302 重定向，也不会自动重试失败请求；需要跳转的部署请先向管理员确认最终地址。

- 连接与连接池等待上限为 10 秒；socket 读取超时为 30 秒。读取超时限制等待数据的时间，不是整次上传的总时长。
- YApi 查询仍会将 Token 作为一个完整查询参数编码。
- 网络失败时远端可能已经收到请求。决定是否重试前，先检查目标平台状态，避免重复写入。

只有有效响应才会报告成功：YApi 需要成功代码和分类操作所需 ID；ShowDoc 需要成功代码及页面/项目 ID；语雀创建或更新需要文档 ID 和 slug。错误通知不展示认证值、请求地址或原始响应正文。具体实例问题请由管理员排查。

取消后不会开始下一篇上传，也不会作为普通上传错误显示。已发送的同步 HTTP 请求可能要等到响应或超时才停止本地处理；取消不会撤销远端已经完成的创建或更新。

## 上传结果与批量失败

成功时 IDE 通知可打开结果页面或复制链接；失败时按文档显示错误。批量上传逐项进行，一篇失败不会回滚已完成的写入，后续文档也可能继续处理，所以结果可能是部分成功。

- **ShowDoc**：每篇文档分别提交。失败后先检查该页面是否已经创建或更新，再决定是否重试。
- **YApi**：缺少分类时会先新建分类，再保存接口；如果接口保存失败，已经创建的分类不会由插件撤销。当前插件没有先按路径或标题查询旧接口，重复操作的结果以 YApi 实例返回和显示为准。
- **语雀**：当前实现会先按 slug 查询，存在时更新，不存在时创建。重新上传同一路径的文档会指向同一个 slug；上传前确认该知识库里的同一路径文档可以被更新。

一次成功通知不代表整批成功。出现部分成功时，先按单篇通知和目标目录核对状态，再处理确认失败的文档。

## 故障排查

**提示缺少平台配置**
在当前项目的对应设置页填写并 Apply。语雀配置不完整时会打开 YuQue Settings。

**地址或路由错误**
地址字段填基址，不要重复添加插件会拼接的接口后缀。Apply 会去掉首尾空白和末尾连续斜线；私有部署的路由规则请向管理员确认。

**YApi 提示 Project ID 错误**
非空 ID 必须是 Long 范围内的大于零的十进制数；空 ID 表示未配置。无效输入会拒绝整次 Apply，并保留草稿和之前保存的值。先确认 ID 属于目标项目。

**语雀提示无权限或找不到文档**
检查 Token、`login`、`repos` 是否指向同一个有写权限的知识库，并确认 API 基址包含 `/api/v2`。第二个 URL 是 API 基址，不要填成站点地址。

**批量只有部分成功**
逐项检查通知。YApi 还需确认是否留下了已创建的分类；语雀需核对对应 slug 的文档状态。只重试确认失败且状态明确的条目。

**凭据存储不可用**
检查 IDE 的 Passwords 设置和系统凭据存储。保留草稿后重试；未保存成功时，不要把空框当成已清除。

**需要验证连接**
插件没有只读连接测试。先查阅平台 API 文档或联系实例管理员；确认目标项目和写入范围后，再执行真实上传。

平台接口参考：

- [ShowDoc 开放 API 文档](https://www.showdoc.com.cn/page/102098)（私有部署路径需核对实际版本）
- [YApi OpenAPI 文档](https://github.com/YMFE/yapi/blob/master/docs/openapi-doc.html)
- [语雀 OpenAPI 元数据：文档 API 与认证头](https://github.com/yuque/openapi-metadata/blob/master/yuque.tea)

核对边界：ShowDoc 官方开放 API 页面在本次读取中只返回动态页面外壳，未能核实其最新正文；ShowDoc 的接口后缀依据当前插件源码，私有部署的 `index.php?s=` 路由仍需按实际版本核对。上面的地址和字段说明不代表本轮已验证任何第三方实例。
