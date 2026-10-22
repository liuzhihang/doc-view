# Doc View 用户指南

Doc View 从 Java 源码、注释和注解生成 API 文档，支持在 IDE 中查看、复制 Markdown、导出文件，以及按需上传到 YApi、ShowDoc 和语雀。本地生成不需要配置任何平台账号。

## 从这里开始

- **第一次生成文档**：按本页的安装和快速开始步骤操作。
- **调整字段、标题或导出方式**：阅读[设置说明](settings.md)。
- **修改模板或查变量**：阅读[模板教程](templates.md)。
- **连接 YApi、ShowDoc 或语雀**：阅读[平台集成](integrations.md)。
- **排查菜单、模板或导出问题**：阅读[故障排查](troubleshooting.md)。

模板设置中的 Apply、Reset 和 Cancel 各自作用不同；细节见[模板状态验证记录](../../openspec/changes/fix-template-settings-lifecycle/verification.md)。

## 安装

1. 打开 IDE 的 **Settings → Plugins → Marketplace**，搜索 `Doc View`，核对作者与[插件页面](https://plugins.jetbrains.com/plugin/15305-doc-view)，选择兼容版本安装。目标 IDE 为 IntelliJ IDEA Ultimate 2026.1 或更新版本，最低 build 为 261；Java 和 Markdown 插件应启用。
2. 离线安装可从[项目 Releases](https://github.com/liuzhihang/doc-view/releases)取得兼容的插件包，在 **Plugins → 齿轮菜单 → Install Plugin from Disk…** 选择下载包。
3. 按 IDE 提示完成加载或重启，打开 Java 项目，等待依赖解析与索引完成。

macOS 可从应用菜单进入 Settings；菜单中文名称会随 IDE 语言变化。用户项目不需要为了查看文档额外引入 Doc View 的 Java 依赖。

当前开发版本的主要 UI 跟随 IDEA 的英语或简体中文，其它语言回退英语；语言更改按 IDEA 提示重启。界面语言不会翻译源码、已有默认模板和用户模板，也不会改变生成文档内容。完整指南当前仍以中文维护，详见[界面语言](settings.md#界面语言)。

## 三步生成第一份文档

在已有 Spring Web 依赖、IDE 能解析注解的示例项目中，加入下面的虚构示例。无需启动 Web 服务，Doc View 读取源码。

```java
package example.docs;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户接口 */
@RestController
@RequestMapping("/users")
public class UserController {
    /** 查询用户
     * @param id 用户编号
     */
    @GetMapping("/detail")
    public UserResponse getUser(@RequestParam("id") String id) {
        return new UserResponse();
    }
}

class UserResponse {
    /** 用户编号 */
    public String id;
    /** 用户名称 */
    public String name;
}
```

1. **选中目标。** 将光标放在 `getUser` 方法内，右键选择 **Doc View**。若要查看这个类的多个接口，将光标放在类声明处、方法外，再执行同一动作。
2. **检查内容。** 方法入口应展示该接口；类入口应展示符合条件的方法目录。按示例源码，默认输出应包含名称“查询用户”、路径 `/users/detail`、请求方式 `GET`、请求参数 `id` 和返回字段 `id`、`name`。
3. **取得 Markdown。** 在预览工具栏选择 **Copy** 复制当前文档；选择 **Export** 并选定目录，输出当前文档文件。只有要传到外部平台时才需要配置[平台集成](integrations.md)。

HTML 预览依赖 IDE 的 Markdown 支持；如果当前环境无法显示 HTML，可直接查看 Markdown 文本。生成第一份文档无需先改模板或填写 Token。

## 支持的源码入口

下面列出常用入口。每种类型都需要 Java 插件完成解析；若识别不到，先检查依赖解析、模块范围和项目设置。

- **Spring Controller — Spring 模板**：在接口方法或类声明处执行 Doc View；IDE 必须能解析 Controller 和 Spring 映射注解。
- **Feign 接口 — Spring 模板**：接口带可解析的 `@FeignClient`，方法带插件支持的 Spring 映射注解。
- **Dubbo 接口 — Dubbo 模板**：接口、实现类注解或 Dubbo XML 关联能被识别；从接口声明或接口方法执行。
- **普通 Java 接口 — Dubbo 模板**：先在当前项目设置中开启“普通接口”，然后从支持的抽象接口方法进入。
- **普通实体类 — 不生成 API 文档**：用 **Doc Editor** 编辑字段说明或复制 JSON。

Feign 的最小入口示例（要求项目已有 OpenFeign 与 Spring Web 依赖）：

```java
package example.docs;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 用户客户端 */
@FeignClient(name = "sample-users")
public interface UserClient {
    /** 查询用户 */
    @GetMapping("/users/detail")
    String getUser(@RequestParam("id") String id);
}
```

在 `getUser` 上执行 Doc View，应走 Spring 文档生成路径。此示例仅说明识别入口，不要求真实存在名为 `sample-users` 的服务。

Dubbo 的最小入口示例（两个文件，要求项目已有 Apache Dubbo 依赖）：

```java
// UserService.java
package example.docs;

/** 用户服务 */
public interface UserService {
    /** 查询用户
     * @param id 用户编号
     */
    String getUser(String id);
}
```

```java
// UserServiceImpl.java
package example.docs;

import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class UserServiceImpl implements UserService {
    @Override
    public String getUser(String id) {
        return "sample";
    }
}
```

在 **`UserService` 接口**的 `getUser` 上执行 Doc View，应使用 Dubbo 模板。不要只在实现类方法上寻找入口。示例不要求启动注册中心，也不是侧栏自动发现全部 Dubbo 声明方式的验收；若侧栏没出现节点，先按[故障排查](troubleshooting.md)检查识别条件。

## 预览、编辑器文件、导出有什么区别

### 使用接口目录

#### 查看名称与签名

工具窗口按模块、类、方法排列。类名和方法名是主文字，业务名称以灰色次要文字显示，例如 `UsersController  用户接口`、`find  查询用户`。方法行不显示参数列表；悬浮提示保留完整签名，重载方法各有独立节点。没有业务说明时不会重复显示代码名。目录文字不会更改文档标题或导出文件名。

同一模块内的类按完整限定名排序；同名类再按源文件路径和源码位置排列。类内方法保留源码顺序，重复刷新时也不会因搜索结果的返回顺序而跳动。

#### 搜索与打开

展开目录后，直接输入代码名、签名、参数类型或业务名称即可搜索。搜索不额外展开折叠节点。方法节点按 Enter 或双击打开 Markdown；回到 Java 源码时，使用右键菜单的 **Jump to Source / Edit Source**（名称随 IDE 语言变化），或当前 keymap 中对应的原生命令。已删除的源码节点不能跳转，刷新后可更新目录。

#### 刷新、缓存与位置

- **Refresh** 重新扫描接口目录。索引未就绪时会显示等待状态；没有结果时检查注解和普通接口设置。取消或失败会保留已展示的目录，可再次刷新。
- **Clear Cache** 只清除当前项目的 DTO schema 内存缓存，保留 Scratch、打开的编辑器和项目文档；不会刷新目录或改写已有 Markdown。
- 刷新完成不会主动展开已隐藏的工具窗口。新安装默认位于左侧；用工具窗口菜单 **Move To** 可移到右侧。已有停靠位置由 IDEA 管理。

#### 导出入口

工具栏的 **Export** 菜单包含 **Export Markdown**（选择目录批量导出）和 **Export Project Docs**（生成项目内 API 文档）。工具栏、右键菜单和弹出框中的同类操作使用 IDEA 图标，悬浮提示与菜单名称说明操作。

### 各操作的输出

#### 预览与编辑

- **源码右键 Doc View**：打开当前方法或类的预览；动作会先保存 IDE 中尚未保存的文档，索引期间入口可能隐藏。
- **侧栏方法节点 → Open**：在编辑器打开 Markdown。文件位于 IDE 的 Scratch 扩展区域，不会自动进入项目 Git 仓库。
- **预览 Copy**：复制当前 Markdown，不会上传或创建项目文件。
- **Doc Editor / 保存字段说明**：编辑实体字段信息，保存时可能写回源码注释或注解；它和修改 Markdown 模板不同，保存后检查代码差异。

#### 导出

- **预览 Export**：保存当前文档到所选目录；同名文件已存在时会询问是否覆盖。
- **类/目录 Export、预览 Export All**：批量导出接口，受“合并导出”影响。确认覆盖时替换目标内容，取消则保留原字节；详见[设置说明](settings.md)。
- **Export Project Docs**：在项目 `docs/doc-view/` 生成索引和接口页面，采用固定目录、稳定身份与手改冲突保护；不受“合并导出”影响，见下节。

#### 上传

- **Upload / Doc Upload**：把文档发送到选定平台，属于外部写入。先核对目标、范围和权限。

**源码、生成文件和平台文档不会自动双向同步。** 改注释/设置后重新生成，再按需要导出或上传；手工修改生成 Markdown 不等于修改源码或全局模板。需要长期维护的生成文档请导出到明确目录并自行管理版本。

## 项目内 API 文档与 AI 检索

在 Doc View 工具窗口打开 **Export → Export Project Docs**。等待项目索引完成后，插件在后台扫描支持的 Spring、Feign、Dubbo 与已开启的普通接口，并使用当前项目模板生成 Markdown。

### 文件结构与内容

每个项目的文档放在固定目录：

```text
docs/doc-view/
├── index.md
└── apis/
    └── UserController-find-<完整身份hash>.md
```

`index.md` 按模块和类纵向分组，每个方法单独列出完整签名、框架名以及 `[Document]`、`[Source]` 短链接。每条方法项上方的 `doc-view-entry:v1` HTML 注释保存 canonical identity 与 SHA-256；IDE Markdown 预览隐藏注释，原始文件仍可供 AI 或其他工具读取。

每个接口有独立页面。同名重载、不同模块或不同源码路径由稳定身份区分，不依赖业务标题和源码行号。接口页使用当前 Spring/Dubbo 模板并附项目相对源码链接；路径或模块变化会形成新身份。导出结果是当次源码和设置的快照，代码改变后需重新导出。

### 再次导出

- 内容相同的接口不会追加或重写；有变化时替换对应受管理页面，再更新索引。
- 没有可导出接口时，索引会更新为空，旧页面仍保留。
- 插件不删除旧页面或用户编写的说明；不再列入索引的旧页面由你决定是否处理。

### 保留手写内容

**把手写说明放在独立文件。** 生成页顶部有所有权与内容校验标记。标记缺失、正文被手工修改、同名目录或路径冲突时，导出会中止并报告路径，不自动覆盖。先把要保留的内容另存或合并到独立说明，再由你处理冲突文件。删除标记不会解除冲突，只会让页面被视作普通用户文件。

### 取消与磁盘故障

暂存期间取消会清理本次临时文件并保留现有成品。开始发布后会执行有限的文件替换批次；磁盘故障可能造成部分页面已更新而索引仍旧。通知会列出已发布、失败及无法清理的路径。确认目录状态并解决原因后重试；这不是跨文件事务，也不回滚远端或其他程序的写入。

### AI 检索边界

导出文件是普通 Markdown，可以加入 Git，并由 IDE 或 AI 按普通文件读取。IDE 的 **Find in Files → Project** 可搜索接口名、方法签名或独有词；结果受当前 scope、排除规则与索引状态影响。本功能没有注册自定义 IDEA 索引、MCP 工具或实时知识库；IDE 原生检索能力取决于当前项目的索引状态。

## 找到设置与帮助

当前入口是 **Settings → Tools → Doc View**。根页设置常规行为；子页为 **Generation Rules**（生成规则）、**Template and Output**（模板与输出）和 **Platform Integrations**（平台集成），三个平台各有独立设置页。插件设置按项目保存；工具窗口齿轮也可进入设置。

**Help → Doc View Welcome** 打开可关闭、可重新打开的本地欢迎/更新编辑器页；也可在 Find Action 搜索 `Doc View Welcome`。本地页面随插件提供，离线可用，但只包含简短入门和本次开发版本的更新摘要，不含完整手册。

设置页的 `?` 图标提供简短说明，气泡里的指南链接会在默认浏览器打开 GitHub 上对应章节，阅读需要网络。模板、平台入口仍然进入当前项目设置，不会自动 Apply 或上传。

首次加载插件会自动打开首页一次；同一 IDE 的其他项目不会重复触发。升级只通知一次“查看更新”，不会自动打开首页；相同版本不再提示。首页的“新版本更新时提醒我”立即生效，作用于整个 IDE；关闭后仍可手动打开帮助。

已有[语雀文档](https://www.yuque.com/liuzhihangs/doc-view)保留为历史资料入口；旧截图、菜单和规则可能与当前代码不同。遇到问题可先查[故障排查](troubleshooting.md)，再在[项目 Issues](https://github.com/liuzhihang/doc-view/issues)提供插件版本、虚构示例、复现步骤和脱敏后的结果。安全问题按[安全报告说明](../../SECURITY.md)处理。

## 文档版本与本次覆盖范围

各指南示例使用虚构数据。历史模板专用构建的 Apply/Reset/Cancel 与重启证据见[验证记录](../../openspec/changes/fix-template-settings-lifecycle/verification.md)；相关设置评估与后续顺序见[设置评估](../settings-audit.md)及[路线图](../roadmap.md)。验证记录按实际插件包分别记录，历史结果不能代替后续构建的复验。
