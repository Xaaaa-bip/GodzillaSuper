# GSL5 — Godzilla Super Loader 5

##BUG反馈群



加微信好友要这个人拉你进群
<img width="888" height="1131" alt="2e6ac2fc81fbe39d96b94d547f08e5c9" src="https://github.com/user-attachments/assets/dfbf9b57-0f83-4fef-8614-92fc1e88c1ca" />


## 目录

- [总体说明](#总体说明)
- [源码说明](#源码说明)
- [更新日志](#更新日志)
- [核心特性](#核心特性)
- [快速开始](#快速开始)
- [使用指南](#使用指南)
- [MCP 服务（AI 操控）](#mcp-服务ai-操控)
- [配置文件](#配置文件)
- [编译与构建](#编译与构建)
- [常见问题](#常见问题)
- [免责声明](#免责声明)

---

## 总体说明

基于 Godzilla 深度二次开发的 **WebShell 管理与红队后渗透平台**。在保留 Godzilla 全部能力的基础上，扩展了 **MCP 服务（AI 驱动）**、**MCP 插件工具（全内存加载）**、**Shell 一键分享（`gsl5://` 链接）**、**团队协作（多数据源）**、**NetCore 动态载荷** 等能力，并提供完整的操作审计。

> ⚠️ 本工具仅供**授权的安全测试与研究**使用，严禁用于任何非法用途。使用本工具产生的任何后果由使用者自行承担。详见文末[免责声明](#免责声明)。

### 主要功能

| 功能 | 说明 |
|------|------|
| 多平台 WebShell 管理 | JSP / ASPX / PHP / ASP / ASP.NET Core：文件管理、命令执行、数据库操作、批量测试、导入导出 |
| Shell 一键分享（CV 分享） | 选中 Shell 按 `Ctrl+C` 即复制 `gsl5://` 分享链接（URL / 密码 / 密钥 / 分组等 24 项配置全打包）；对方 `Ctrl+V` 或「目标 → 导入链接」一键导入，自动挂原分组 |
| MCP 服务（AI 操控） | 内置 HTTP/SSE 服务，Claude Desktop / Claude Code / Codex 一键接入；Shell 管理、命令执行、文件操作等 70+ 工具，Bearer Token 鉴权 |
| MCP 插件工具 | `@McpTool` 注解机制，插件方法自动注册为 MCP 工具：提权（TH_TOOLS）、凭据抓取（Mimikatz）、杀软识别、OA 信息等 10 个工具，全部内存加载不落盘 |
| 存活扫描 | 菜单「攻击 → 存活扫描」批量检测分组内 Shell 存活状态；另有批量连接测试 |
| 客户端证书认证 | Shell 支持双向 TLS：客户端证书路径 + 证书密码，适配要求客户端证书的目标 |
| 团队协作与审计 | 单机 SQLite / UNC 远程 SQLite / PostgreSQL 三数据源，全量操作审计 |
| NetCore 动态载荷 | ASP.NET Core Middleware 载荷 + AES Base64 加密，文件 / 命令 / SQL / 插件加载 |
| 检查更新 | 启动静默检查 GitHub Release，新版弹窗提示 |

### 主要修改（相对原版 Godzilla）

**新增**：
- 全局 MCP 服务与 AI 操控（3.1.0）+ MCP 插件工具（3.1.5）
- Shell 一键分享 / 导入（`gsl5://` 链接，Ctrl+C / Ctrl+V）
- NetCore 动态载荷（3.1.0）
- 团队多数据源协作（单机 / UNC / PostgreSQL）
- MCP Bearer Token 鉴权 + CLI 自动写配置 + Linux headless 支持（3.1.3）
- 检查更新（3.1.4）

**修复**：
- PHP 混淆乱码、DisplayName 中文乱码（PHP/JSP/C# 连带修复，3.1.5）
- 杀软识别名称乱码（3.1.5）
- PHP 免杀模板 0KB（3.1.1）
- 命令回显、Shell 加载遮罩竞态（3.1.2）

完整明细见 [更新日志](#更新日志)。

## 源码说明

仓库只维护 **src 源码**：`src` 是全部功能的唯一源码来源，修改后重新打包即得到可运行的 `gsl5.jar`。

```
gsl/
├── src/                     # 全部源码
│   ├── core/                # 核心框架
│   │   ├── ApplicationContext.java     # 全局上下文（RELEASE_TAG、godMode 等）
│   │   ├── Db.java                     # SQLite 持久化层
│   │   ├── MigrateDb.java              # SQLite → PostgreSQL 迁移
│   │   ├── OperationAuditLog.java      # 操作审计
│   │   ├── annotation/                 # MCP 工具注解（McpTool / McpParam）
│   │   ├── shell/                      # ShellEntity 等
│   │   ├── shellprocessor/             # ASP/PHP/JSP/C# 编码与变形处理器
│   │   ├── c2profile/                  # C2 配置模板
│   │   └── ui/                         # MainActivity、StartupModeDialog、ShellFileManager 等
│   ├── shells/
│   │   ├── channel/          # 请求通道（HTTP 直连 / C2 Profile）
│   │   ├── payloads/         # 各平台动态载荷源码（java / csharp / netcore）
│   │   ├── cryptions/        # 流量加密器（JavaAes / csharpAes / phpXor / aspXor / netcore）
│   │   └── plugins/          # 插件（java / csharp / netcore / asp / php / generic + assets 目标端模块）
│   ├── util/                 # HTTP 客户端、IP 库、工具函数
│   ├── data/                 # 内置资源（av.json、qqwry.ipdb）
│   └── META-INF/             # 打包清单（Main-Class: core.Gsl5Main）
```

| 目录 | 作用 |
|------|------|
| `src/core/` | 应用核心：全局上下文（版本号）、SQLite 持久化（Db / MigrateDb）、操作审计、`@McpTool`/`@McpParam` 注解机制（MCP 插件工具）、Shell 实体、编码与变形处理器、C2 模板、全部界面 |
| `src/shells/payloads/` | 各语言动态载荷源码（Java / C# / NetCore），生成 WebShell 时编译打包进载荷 |
| `src/shells/cryptions/` | 流量加密器源码（JAVA_AES_BASE64 / CSHARP_AES_BASE64 / PHP_XOR / ASP_XOR / NETCORE_AES_BASE64 等） |
| `src/shells/plugins/` | 插件工具（TH_TOOLS / Mimikatz / OaTools / Useradd / ShellAvscan / McpService 等）；按目标语言分子目录，`generic/` 为应用级（MCP 服务），`assets/` 为目标端模块源码 |
| `src/shells/channel/` | 请求通道实现（HTTP 直连 / C2 Profile 通道） |
| `src/util/` | 通用工具（HTTP 客户端、纯真 IP 库、工具函数） |
| `src/data/` | 内置资源（`av.json` 杀软指纹、`qqwry.ipdb` IP 地理位置库） |

> 预编译 `gsl5.jar` 不纳入仓库（见 [Release 下载](#快速开始)）；`native/` JNI 源码与相关编译脚本仅本地开发环境持有，不随仓库分发。
> 运行/构建还需外部依赖（`lib/`：ASM、PostgreSQL 驱动等；`bin/`：okhttp、kotlin-stdlib 等），未纳入本仓库。
> 修改源码后重新打包见 [编译与构建](#编译与构建)。

---

## 更新日志

### 3.1.7（2026-08-21）
- **C2 流量伪装容器链（新）**：新增 `pngidat` / `pdfstream` / `gifdata` 三个加密链，请求保持裸密文，响应伪装为结构合法的 PNG（密文藏 IDAT 图像数据区）/ PDF（FlateDecode 流 + 正确 xref 偏移）/ GIF（LZW 光栅）文件
- 随机图像尺寸、随机分块、随机陪衬 chunk（gAMA/tEXt），每个数据包形态不同
- 新增 `pngC2` / `pdfC2` / `gifC2` 三个 profile 模板（jar 内嵌，profile 目录缺失时启动自动生成）
- **MCP 免弹窗**：MCP 模式下所有 UI 弹窗转日志输出，异常堆栈直接返回给 AI 判断
- 修复 `shell_create` 指定 c2Profile 失败、`shell_add` 的 c2Profile 未持久化
- MCP CLI 桌面环境不再强制 headless（shell 操作可用），无显示环境仍自动 headless
- 修复 GUI 报错弹窗布局（还原 699x333 固定尺寸与父窗口居中）

- 修复发包后长连接不关闭：OkHttp 连接池 keep-alive 挂起（客户端最长 5 分钟）→ 每次请求完成立即断开 TCP

### 3.1.6（2026-08-19）
- **主界面中文口字修复**：内嵌 CJK 字体兜底，无中文字体环境（Linux/精简 JRE）自动回退，不再显示方框
- **JSP/JSPX 超级混淆解析错误修复**：十进制转义生成非法 XML 字符引用导致 JSP 无法解析，改为按码点输出
- 十六进制转义按 Unicode 码点输出（原按 UTF-8 字节 hex，CJK 字符错码点）
- **MCP 生成免弹窗**：`shell_create` 程序化选择处理器，不再弹出选择对话框；`obfuscation` 参数支持 6 变体（superObfuscation / superUnicode / superHex / superXml / superRandom / superDouble）
- 移除 RASP 绕过模块（不保证绕过，不再内置）
- 编码 Auto 自动探测（BOM/UTF-16/重音启发 + 方向回推）+ 文件查看乱码修复
- spring 内存马空 body 阻断正常请求空白页修复

### 3.1.5（2026-08-10）
- 修复 PHP 混淆乱码：模板生成/输出统一 UTF-8 编码（PhpConstInclude / PhpConstEval / PhpNone / Generate）
- 修复 DisplayName 中文乱码：PHP 全版本常量文件包含（include_once）、PHP 全版本常量解密绕过（eval）
- 连带修复 JSP/C# 处理器乱码：JSP/JSPX 超级混淆、无
- **MCP 插件工具**：`@McpTool` 注解机制，插件方法声明即被 MCP 扫描导入为工具（tools/list 自动列出、按 Shell 载荷类型分发）
  - `plugin_NewCmd_exec` 命令执行 · `plugin_NewCmd_info1` 获取信息1（Pillager 凭据收集：Chrome/IE/Wifi/WinSCP/Navicat/QQ）· `plugin_NewCmd_info2` 获取信息2（hunter/SharpHunter 内网信息：系统/进程/网络/AD）
  - `plugin_OaTools_proxy` OA 信息提取（8 目标）· `plugin_Useradd_adduser` 添加 Windows 用户 · `plugin_ClassLoader_run` 自定义 class 加载执行
  - `plugin_Mimikatz_run` 凭据提取 · `plugin_TH_TOOLS_exec` 提权框架（6 个 Potato）· `plugin_EfsPotato_run` EfsPotato 提权 · `plugin_ShellAvscanPlugin_scan` 杀软识别
- **全部内存加载**：info1/info2/Mimikatz 均 PE→shellcode 内存执行，无文件落盘（免杀软查杀）
- **TH_TOOLS 无 GUI 化**：大文件分片上传替代 GUI 组件，MCP 下完整支持提权链
- **shell_info 补全**：URL/主机名/用户/IP/OS/Webshell 路径/Shell 类型/AES Key + 系统关键信息
- 修复杀软识别名称乱码（isMessyCode 误判导致 GBK 破坏性转换）

### 3.1.4（2026-07-24）
- 菜单「赞助」改为「更新」，新增「检查更新」
- 点击检查 GitHub 最新 Release，不是最新版提示跳转下载
- 启动时自动静默检查，有新版才弹窗

### 3.1.3（2026-07-24）
- **MCP Bearer Token 鉴权**：`/sse` `/message` `/health` `/config` 全路由鉴权，非本机绑定无 Token 拒绝启动
- **CLI 自动写配置**：`java -jar gsl5.jar mcp` 启动后自动写入 Claude Code + Codex 配置（含 Authorization header）
- **CLI 显式 Token**：命令行直接打印完整 Bearer Token，无需去文件里找
- **Linux headless 修复**：无 GUI 环境不再报 Font NPE

### 3.1.2（2026-07-17）
- 命令回显修复、Shell 加载遮罩竞态修复
- JNI 自动加载、Win x64 DLL 内置

### 3.1.1（2026-07-17）
- PHP 免杀模板 0KB 修复

### 3.1.0（2026-07-15）
- NetCore 动态载荷 + 全局 MCP Claude/Codex 配置

---




## 核心特性

### 1. 双运行模式
- **GUI 模式（默认）**：图形化操作界面，启动时可选择数据源（见下表）。
- **MCP 无头模式**：`java -jar gsl5.jar mcp [port] [bindHost]`，内置 HTTP/SSE 服务，供 **Claude Desktop / Claude Code / Codex** 等 AI 助手远程调用全部功能。默认 **绑定 `0.0.0.0:9123`（全网卡）**，可指定网卡 IP / `127.0.0.1`。

### 2. 数据源与团队协作
启动时由 `StartupModeDialog` 选择数据源：

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| **单机模式** | 本地 SQLite `data.db` | 个人使用 |
| **团队-远程 SQLite** | UNC 共享路径（如 `\\host\share\data.db`） | 小团队、内网共享 |
| **团队-PostgreSQL** | 远程 PG，多人共享 Shell 列表 + 操作审计 | 多人协作 |

- `core/Db.java`：SQLite 持久化层（`shell` / `shellEnv` / `plugin` / `seting` / `shellGroup` 五张表）
- `core/MigrateDb.java`：SQLite → PostgreSQL 一键迁移工具（`java -cp gsl5.jar;lib/postgresql-*.jar core.MigrateDb [host] [port] [db] [user] [pass]`）

### 3. Shell 全功能管理
集中管理 **JSP / ASPX / PHP / ASP / ASP.NET Core** 多类 WebShell：

- **文件管理**（`ShellFileManager`）：浏览目录、上传/下载、删除/复制/移动、改属性、大文件分片续传、从 URL 远程下载到目标
- **命令执行**（`NewCmd`）：Windows 走 `cmd /c`、Linux 走 `bash`，支持交互终端
- **数据库工具**：MySQL / Oracle / SQL Server / PostgreSQL / SQLite，执行 SQL、保存常用连接
- **批量连接测试、搜索、克隆、导入导出**（`gsl5://import?data=...` 链接共享配置）
- **NetCore 载荷**（`NetCoreDynamicPayload` + `NETCORE_AES_BASE64`）：面向 ASP.NET Core Middleware 的完整动态载荷（文件 / 命令 / SQL / 插件加载）

### 4. Shell 一键分享与批量检测
- **CV 分享（`gsl5://` 链接）**：主界面选中 Shell 按 `Ctrl+C`，URL / 密码 / 密钥 / 载荷 / 加密器 / 编码 / 请求头 / 代理 / 超时 / 备注 / C2Profile / 大文件参数 / 客户端证书 / 所属分组共 24 项配置打包（gzip + Base64URL）后自动复制到剪贴板
- **CV 导入**：收到链接后按 `Ctrl+V` 自动检测剪贴板并提示导入；也可菜单「目标 → 导入链接」手动粘贴。导入自动生成新 ID 避免与原库冲突、自动挂回原分组，支持一次导入多条
- **存活扫描**：菜单「攻击 → 存活扫描」批量检测当前分组内 Shell 存活状态；另有批量连接测试、搜索、克隆
- **客户端证书认证**：Shell 支持双向 TLS（客户端证书路径 + 证书密码），适配要求客户端证书的目标
- MCP 模式同样支持：`shell_export` / `shell_import` / `shell_batch_test`

### 5. 插件集

| 插件 | 平台 | 功能 |
|------|------|------|
| `TH_TOOLS` | Java / C# | Potato 系列提权（EfsPotato / BadPotato / GodPotato / SweetPotato）、Shellcode 注入、自定义 Payload |
| `Mimikatz` | 通用 | 凭据抓取 |
| `Useradd` | Java / C# / 通用 | 添加账号 |
| `OaTools` | Java / C# | 金蝶 / 致远 / 泛微 / 用友 / Weblogic / vCenter 等专项代理 |
| `ShellAvscan` | 通用 | 目标安全软件探测 |
| `McpService` | 通用（应用级） | 内置 MCP HTTP/SSE 服务；全局菜单入口，支持 Claude / Codex 一键写配置 |
| NetCore 插件组 | .NET Core | RealCmd / PortScan / Zip / HttpProxy / EasySocks / EvalCode / ExecuteAssembly / ShellcodeLoader |

### 6. 现代化 UI 与审计
- FlatLaf 主题、壁纸管理器、透明度调节
- `OperationAuditLog` 全量操作审计：记录**谁、何时、做了什么**，团队模式下可经 `oplog_query` 查询

---

## 快速开始

### 0. 环境要求
- **Java**：JDK / JRE 8 或以上（开发机已验证 `1.8.0_431`）

### 2. 启动 GUI
```bash
java -jar bin/gsl5.jar
```
启动后选择数据源（单机 SQLite / 远程 SQLite / PostgreSQL），进入主界面。

### 3. 启动 MCP 无头模式（AI 操控）
```bash
java -jar bin/gsl5.jar mcp                 # 默认 0.0.0.0:9123
java -jar bin/gsl5.jar mcp 9999            # 自定义端口（仍绑 0.0.0.0）
java -jar bin/gsl5.jar mcp 9999 127.0.0.1  # 端口 + 绑定地址
java -jar bin/gsl5.jar mcp 0.0.0.0:9123    # host:port 写法
java -jar bin/gsl5.jar mcp 192.168.1.10:9123
```
然后在 Claude / Codex 的 MCP 配置中添加（详见 [MCP 服务](#mcp-服务ai-操控)）。

### 4. 下载预编译 Release
- **3.1.7（最新）**：C2 流量伪装容器链（PNG/PDF/GIF）+ MCP 免弹窗修复 + 报错弹窗布局修复 — [Release](https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/3.1.7) · [jar](https://github.com/Xaaaa-bip/GodzillaSuper/releases/download/3.1.7/gsl5.jar)
- **3.1.6**：口字修复 + 超级混淆解析错误修复 + MCP 生成免弹窗（obfuscation 6 变体）+ 移除 RASP — [Release](https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/3.1.6) · [jar](https://github.com/Xaaaa-bip/GodzillaSuper/releases/download/3.1.6/gsl5.jar)
- **3.1.5**：PHP 混淆乱码修复 + MCP 插件工具（`@McpTool` 10 个工具，全部内存加载）+ shell_info 补全— [Release](https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/3.1.5) · [jar](https://github.com/Xaaaa-bip/GodzillaSuper/releases/download/3.1.5/gsl5.jar)
- **3.1.4**：检查更新 + 菜单优化 — [Release](https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/3.1.4) · [jar](https://github.com/Xaaaa-bip/GodzillaSuper/releases/download/3.1.4/gsl5.jar)
- **3.1.3**：MCP Token 鉴权 + CLI 自动写配置 + Linux headless 修复 — [Release](https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/3.1.3) · [jar](https://github.com/Xaaaa-bip/GodzillaSuper/releases/download/3.1.3/gsl5.jar)
- **3.1.2**：命令回显修复 + JNI 自动加载 — [Release](https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/3.1.2) · [jar](https://github.com/Xaaaa-bip/GodzillaSuper/releases/download/3.1.2/gsl5.jar)
- **3.1.1**：PHP 免杀 0KB 修复 — [Release](https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/3.1.1) · [jar](https://github.com/Xaaaa-bip/GodzillaSuper/releases/download/3.1.1/gsl5.jar)
- **3.1.0**：NetCore + 全局 MCP — [Release](https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/3.1.0) · [jar](https://github.com/Xaaaa-bip/GodzillaSuper/releases/download/3.1.0/gsl5.jar)

---

## 使用指南

> 以下为 **GUI 模式**的日常操作流程。AI / MCP 操控见下一节 [MCP 服务](#mcp-服务ai-操控)。

### 添加与连接 Shell

主界面左侧为 **Shell 分组树**，右侧为操作区。菜单 / 右键分组 → **添加 Shell**，填写配置（字段对应 `data.db` 的 `shell` 表）：

| 字段 | 说明 |
|------|------|
| URL | WebShell 地址 |
| 密码 / 密钥（secretKey） | 通信密钥 |
| Payload | `JavaDynamicPayload` / `CSharpPayload` / `PhpPayload` / `NetCoreDynamicPayload` 等，需与目标服务端匹配 |
| 加密（cryption） | `JAVA_AES_BASE64` / `JAVA_C2` / `PHP_XOR` / `CSHARP_AES_BASE64` / `NETCORE_AES_BASE64` 等 |
| 编码（encoding） | 目标控制台编码。MCP 连接时自动从 DB 带入；为空/`auto` 时用 `chcp`/`locale` 自动检测并写回（Windows 多为 GBK，Linux 多为 UTF-8）；也可 `shell_detect_encoding` |
| 请求头 / 左右标志（reqLeft / reqRight） | 自定义请求体包裹方式 |
| 代理 / 超时 | `proxyType/Host/Port`、`connTimeout`、`readTimeout` |
| 备注 / 笔记 | `remark`、`note` |

保存后双击或右键 **连接**；支持 **测试连接**、**批量测试**、**搜索**、**克隆**、**导入 / 导出**（`gsl5://import?data=...` 链接共享配置）。

### 主要操作标签

选中已连接的 Shell 后，右侧切换标签：

- **NewCmd** —— 命令执行。Windows 走 `cmd /c`、Linux 走 `bash`。
- **ShellFileManager** —— 文件管理：浏览、上传 / 下载、删除、复制、移动、改属性、大文件分片续传、从 URL 远程下载到目标。
- **数据库** —— 配置目标库（MySQL / Oracle / SQL Server / PostgreSQL / SQLite）→ 执行 SQL、管理连接配置。
- **插件** —— 右键 Shell → 插件 → 选择功能模块。

### Shell 分享（gsl5:// 链接）

1. **导出**：主界面选中 Shell（可多选）→ 按 `Ctrl+C` → 分享链接自动复制到剪贴板，提示「链接已复制，可发送给他人通过 Ctrl+V 导入」
2. **分享**：把链接发给同事（微信 / QQ / 聊天工具均可）
3. **导入**：对方打开 GSL5 后按 `Ctrl+V`（自动检测剪贴板并提示），或菜单「目标 → 导入链接」粘贴 → 确认即一键入库

- 链接内含 Shell 完整配置（URL、密码、密钥、载荷、加密器、编码、请求头、代理、超时、备注、C2Profile、大文件参数、客户端证书、所属分组，共 24 项），gzip + Base64URL 编码
- 导入自动生成新 ID（不与原库冲突）、自动挂回原分组，支持一次导入多条 Shell
- MCP 模式同样支持：`shell_export` / `shell_import`（自动回连测试）
- ⚠️ 链接内含密码与密钥，请仅分享给可信人员

### 提权与后渗透插件

- **TH_TOOLS** —— Potato 系列提权（EfsPotato / BadPotato / GodPotato / SweetPotato）、Shellcode 注入、自定义 Payload
- **Mimikatz** 凭据抓取 ｜ **Useradd** 添加账号 ｜ **ShellAvscan** 探测目标安全软件
- **OaTools** —— 金蝶 / 致远 / 泛微 / 用友 / Weblogic / vCenter 等专项代理

### NetCore 载荷（ASP.NET Core）

面向 **ASP.NET Core Middleware** 的完整动态载荷（不是传统 aspx）：

| 类型 | 名称 |
|------|------|
| Payload | `NetCoreDynamicPayload` |
| Cryption | `NETCORE_AES_BASE64` |
| 入口 | ASP.NET Core Middleware（`.cs`） |

**能力**（`payload_core.dll` / 类名 `LY`）：会话 `test`/`close`、基础信息、文件管理、命令执行、SQL（目标进程需已加载对应 DbProviderFactory）、插件 `include` 加载。

**插件**（`src/shells/plugins/netcore/`）：RealCmd / SuperTerminal / PortScan / Zip / HttpProxy / EasySocksProxy / InlineExecuteAssembly / EvalCode / ShellcodeLoader（Windows）。未移植强依赖 System.Web / 域提权的 MemoryShell、Potato 等。

**使用步骤**：
1. 生成：Payload=`NetCoreDynamicPayload`，加密器=`NETCORE_AES_BASE64`
2. 将生成的 Middleware 放入 ASP.NET Core 项目：`app.UseMiddleware<GslCoreShellMiddleware>();`
3. 添加 Shell，密码密钥一致后连接
4. 打开 Shell 后插件页可见 NetCore 功能

源码说明见 `src/shells/cryptions/netcore/README.md`。

### 团队协作（PostgreSQL 模式）

启动选 "团队-PostgreSQL" → 多人共享同一 Shell 列表 → 所有操作自动写入审计日志（MCP 可经 `oplog_query` 查询；GUI 经操作审计面板查看）。从单机迁到团队库可用 `MigrateDb` 一键迁移。

---

## MCP 服务（AI 操控）

### 入口与绑定
- **GUI**：菜单 **配置 → MCP 服务**（若无配置菜单则 fallback 到插件菜单）。面板约 780×520，支持绑定地址 / 端口 / 启动 / 停止。
- **CLI**：`java -jar gsl5.jar mcp [port] [bindHost]`，也支持 `host:port` 写法。
- **默认绑定**：`0.0.0.0:9123`（全网卡）；可改为 `127.0.0.1` 或指定网卡 IP。
- 启动后日志类似：`[MCP] 服务已启动 bind=0.0.0.0:9123`，并提供可访问 URL 列表。

### 一键写入 Claude / Codex 配置
GUI 面板第二行：
| 按钮 | 写入目标 |
|------|----------|
| **写入全部配置** | Claude Code + Claude Desktop + Codex |
| **写入 Claude** | `~/.claude/mcp.json`、项目 `mcp.json` / `.mcp.json`、Claude Desktop `claude_desktop_config.json` |
| **写入 Codex** | `~/.codex/config.toml` → `[mcp_servers.gsl5]` |

也可经 MCP 工具 `mcp_config`：`client=claude|codex|all`，`write=true`，可选 `host` / `outputPath`。

### Claude 配置示例
```json
{
  "mcpServers": {
    "gsl5": {
      "type": "sse",
      "url": "http://127.0.0.1:9123/sse"
    }
  }
}
```

### Codex 配置示例
```toml
[mcp_servers.gsl5]
type = "sse"
url = "http://127.0.0.1:9123/sse"
```

### Encoding 自检
连接 / 执行时自动处理远程控制台编码：
1. 显式 `encoding` 参数 → 强制使用
2. `auto` 或 DB 为空 → 探测（`chcp` / `locale`）并写回 DB
3. DB 已有值 → 使用 DB 值，并做冷连接校验
4. 缓存命中不重复探测（`auto` / `shell_detect_encoding`）

可用工具 `shell_detect_encoding` 主动探测并写回。

### 可用工具（以源码 `McpService.java` 为准）

| 分类 | 工具 | 说明 |
|------|------|------|
| **Shell 管理** | `shell_list` | 列出所有 Shell（可按分组过滤） |
| | `shell_get` | 获取单个 Shell 完整配置 |
| | `shell_add` / `shell_edit` / `shell_delete` | 增 / 改 / 删 Shell |
| | `shell_clone` | 克隆 Shell |
| | `shell_backup` | 备份 Shell 列表 |
| **查询 / 批量** | `shell_count` | 按分组统计 |
| | `shell_search` | 关键词搜索 |
| | `shell_batch_test` | 批量测试连接 |
| **远程探测** | `shell_info` | 远程系统信息（OS / 用户 / 目录） |
| | `shell_test` | 测试单条连接 |
| | `shell_detect_encoding` | 探测远程控制台编码并写回 DB |
| | `process_list` | 列出进程 |
| | `net_info` | 网络连接信息 |
| **命令执行** | `shell_exec` | 执行系统命令（`encoding` 支持 GBK/UTF-8/`auto`） |
| **文件操作** | `file_list` | 列目录 |
| | `file_read` | 读文件（自动判断文本 / 二进制） |
| | `file_search` / `file_roots` | 按名搜索 / 列文件系统根目录 |
| | `file_upload_local` / `file_download_local` | 本机路径直传 / 直存（不经 AI 中转） |
| | `file_delete` / `file_copy` / `file_move` | 删 / 复制 / 移动 |
| | `file_mkdir` / `file_attr` | 建目录 / 设属性（权限 / 时间戳） |
| | `file_remote_down` | 从 URL 远程下载到目标 |
| **数据库** | `db_exec` | 执行 SQL |
| | `db_list_types` / `db_configs` | 列支持的库类型 / 管理连接配置 |
| **Payload / 生成** | `payload_list` | 列出所有载荷与加密器（含 NetCore） |
| | `shell_create` | 生成 Shell 文件（可指定 `cryption`、`genFile`、`c2Profile`、`obfuscation`） |
| | `c2profile_list` / `c2profile_get` | 列出 / 获取 C2 配置 |
| **环境** | `shell_env` | 读取 / 设置 Shell 环境变量 |
| **导入导出** | `shell_export` / `shell_import` | 导出 / 导入（`gsl5://` 链接） |
| **设置** | `settings_get` / `settings_set` | 读 / 改应用设置 |
| **配置** | `config_read` / `config_write` | 读 / 写 `config.yaml` |
| **MCP 管理** | `mcp_status` | 服务状态（绑定地址 / 可访问 URL） |
| | `mcp_config` | 生成 / 写入 Claude 或 Codex 配置（`client`/`write`/`host`） |
| **审计** | `oplog_query` | 查询团队操作日志 |

> `shell_create` 的 `obfuscation` 参数默认 `default`（不启用增强）；传其它值会写入 `godMode` 设置，启用增强传输模式。C2 模板可通过 `c2profile_list` 查询，指定 `c2Profile` 参数可避免弹 UI 选择框。

### 典型工作流
```
1. 启动 GSL5 MCP 服务（GUI 面板或 mcp 无头）
2. 写入 Claude / Codex 配置并连接 SSE
3. shell_list / shell_detect_encoding → 查看 Shell 并校正编码
4. shell_exec          → 在目标执行命令
5. file_list/file_read → 浏览、读取文件
6. db_exec             → 查询数据库
7. oplog_query         → 回溯操作审计
```

---

## 配置文件

运行时读取/写入的文件（位于运行目录，非源码仓库内容）：

| 文件 | 说明 |
|------|------|
| `data.db` | SQLite 数据库，存储 Shell 配置 / 插件 / 环境变量 / 设置 / 分组 |
| `config.yaml` | 运行配置（MCP 端口、认证、团队 PG 连接等） |

`config.yaml` 结构示例（**请替换为你的真实连接信息**）：
```yaml
server:
  port: 9123              # MCP HTTP 服务端口
  auth:
    enable: false         # 是否启用 MCP 认证
    password: ""
mcp:
  enable: true            # 是否启动 MCP 服务
database:                 # 团队 PostgreSQL（仅团队模式使用）
  host: 127.0.0.1
  port: 5432
  database: gsl5
  username: gsl5
  password: <your-password>
```

---

## 编译与构建

### 源码与 Release 对应（tag）

每个 Release 对应一个 tag（`3.1.5` / `3.1.4` / `3.1.3` …），tag 指向该版本发布时的源码。要打包某个版本的 jar，先切到对应 tag：

```bash
git checkout 3.1.5    # 切到 3.1.5 发布源码，按下文步骤编译打包
git checkout main     # 回到最新开发源码
```

> ⚠️ 源码默认 **GBK** 编码（`McpService.java` 为 UTF-8）。编辑 GBK 文件时避免用会重编码为 UTF-8 的工具，纯 ASCII 改动建议用 `sed`。

```bash
# 1) 编译核心源码（GBK）
javac -encoding GBK -cp "lib/*;bin/*" -d out/production/gsl5 \
  src/core/**/*.java src/shells/**/*.java src/util/**/*.java

# 2) 编译 MCP 服务（UTF-8，依赖已编译的核心）
javac -encoding UTF-8 -cp "lib/*;bin/*;out/production/gsl5" \
  -d out/production/gsl5 src/shells/plugins/generic/McpService.java

# 3) 打包
cd out/production/gsl5 && jar cf gsl5.jar *
```

**更新已有 jar 中的单个 class**（免重新打包）：
```bash
jar uf out/artifacts/gsl5_jar/gsl5.jar shells/plugins/generic/McpService.class
```

构建产物输出到 `out/production/gsl5/`（class）与 `out/artifacts/gsl5_jar/gsl5.jar`（jar）。`lib/`（ASM、PostgreSQL 等）与 `bin/`（okhttp、kotlin-stdlib 等）依赖需自行准备。

---

## 常见问题

**Q：MCP 连接失败？**
A：检查绑定地址与端口（默认 `0.0.0.0:9123`）是否被占用、防火墙是否放行；确认 Claude / Codex 配置的 URL 与实际可访问地址一致。本机客户端可用 `127.0.0.1`，跨机需写网卡 IP 并用 `mcp_config`/`写入配置` 生成对应 URL。

**Q：Shell 连接超时？**
A：检查目标 URL 是否可达，Payload / 加密方式是否与目标服务端匹配。

**Q：中文乱码？**
A：目标输出默认 GBK（Windows）/ UTF-8（Linux），可在 Shell 配置中设置 `encoding`，或用 MCP `shell_detect_encoding` / `encoding=auto` 自动探测写回。

**Q：NetCore 载荷怎么用？**
A：生成时选 `NetCoreDynamicPayload` + `NETCORE_AES_BASE64`，把 Middleware `.cs` 挂进 ASP.NET Core 管道（`UseMiddleware`），不是上传 aspx。纯文件上传默认不能当 aspx 用。

**Q：团队模式连不上数据库？**
A：PostgreSQL 需允许远程连接（`pg_hba.conf`），确认用户名密码正确；先用 `StartupModeDialog` 的 Test Connection 验证。

**Q：怎么把 Shell 配置分享给同事？**
A：选中 Shell 按 `Ctrl+C`，把复制的 `gsl5://` 链接发给对方；对方按 `Ctrl+V` 或菜单「目标 → 导入链接」即可一键导入。链接含密码密钥，仅发给可信人员。

---

## 免责声明

本工具仅供**安全研究和授权测试**使用，严禁用于任何非法用途。使用本工具产生的任何后果由使用者自行承担。所有操作会被审计日志完整记录。
