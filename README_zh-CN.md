![QuickAlias](https://raw.githubusercontent.com/Florlet/QuickAliasWiki/main/static/img/QuickAliasBanner.png)

<p align="center">
  <!-- Modrinth -->
  <a href="https://modrinth.com/project/quickalias"><img src="https://img.shields.io/modrinth/dt/jGHTlhbU?logo=modrinth&label=Modrinth&style=for-the-badge" alt="Modrinth Downloads"/></a>
  <!-- CurseForge -->
  <a href="https://www.curseforge.com/minecraft/mc-mods/quickalias"><img src="https://img.shields.io/curseforge/dt/1391158?logo=curseforge&label=CurseForge&style=for-the-badge" alt="CurseForge Downloads"/></a>
  <!-- Wiki -->
  <a href="https://quickalias.pages.dev"><img src="https://img.shields.io/badge/Documentation-Wiki-4682B4?style=for-the-badge&logo=docusaurus" alt="Wiki"/></a>
</p>

![QuickAlias-demo](https://raw.githubusercontent.com/Florlet/QuickAliasWiki/main/static/img/QuickAlias-time_set_midnight.gif)

**QuickAlias** 是一款致力于优化 Minecraft 指令交互体验的客户端模组。提供可视化别名编辑器、层级化指令菜单、动态变量解析及快捷指令面板。

本模组旨在解决繁琐的手动指令输入痛点。可构建自定义的指令层级体系，并通过原生的聊天栏自动补全功能或便捷的可视化面板进行调用。

## ✨ 核心特性

* **🎨 集成化图形编辑器**: 彻底摒弃繁琐的 JSON 配置文件编辑流程。可直接通过游戏内 GUI（聊天界面设置入口）高效地创建、编辑、重组或导入别名配置。
* **🌳 树状指令结构**: 支持构建嵌套式菜单系统。例如，定义一个根别名 `传送`，并在其下级联 `家`、`大厅` 及 `矿区` 等子选项。
* **🚀 宏指令支持**: 允许将 **多条指令** 绑定至单一别名。仅需一次触发，即可按序执行预设的一系列操作逻辑。
* **🧩 动态变量系统**:
  * **系统常量**: 支持在指令中自动解析当前坐标 (`{X}`, `{Y}`, `{Z}`)、玩家 ID (`{ID}`) 及维度信息 (`{DIM}`)。
  * **自定义参数**: 支持定义如 `/invite {player}` 的动态别名。系统将智能捕获用户输入的参数，并动态替换指令中的对应变量。
* **⌨️ 原生级集成**:
  * **自动补全**: 自定义别名将无缝集成至游戏聊天栏的智能提示系统中，提供与原版指令一致的交互体验。
  * **智能建议**: 命名为 `{id}` 的变量将自动补全在线玩家名称，`{dim}` 则自动补全维度 ID。
* **🖥️ 快捷覆盖层**: 聊天界面集成了快捷入口（`/` 图标），可唤出可视化的别名导航菜单，实现免输入的指令执行。

![QuickAlias-editor](https://raw.githubusercontent.com/Florlet/QuickAliasWiki/main/static/img/QuickAlias-player.png)

## 📖 使用指南

### 1. 创建基础别名

1. 打开聊天界面。
2. 点击 **设置 (⚙)** 图标进入 QuickAlias 配置面板。
3. 点击 `+` 按钮创建新的根别名。
4. **别名名称**: 输入触发关键词（例如：`home`）。
5. **命令**: 输入需执行的指令内容（例如：`/tp <x> <y> <z>`）。
6. 保存设置。此时即可在聊天栏通过 `/home` 调用该别名。

### 2. 使用变量（参数）

若需创建一个接收动态参数（如玩家名称）的别名：

1. 创建一个根别名，例如 `hi`。
2. 为其添加一个 **子选项** (Sub-Option)。
3. 将子选项命名为 `{id}`（花括号标识该节点为变量）。
4. 在 `{id}` 节点的命令列表中，输入：`hello {id}`。
5. **执行**: 在聊天栏输入 `/hi Steve`，系统将执行 `hello Steve`（没有“/”将作为消息发送）。

### 3. 内置占位符

以下占位符可用于任何指令配置中：

* `{X}`, `{Y}`, `{Z}` - 当前坐标值（保留一位小数）。
* `{ID}` - 当前玩家名称。
* `{DIM}` - 当前维度的注册资源名。

### 📥 安装说明

* **安装方式**: 将 `.jar` 文件放入游戏目录下的 `mods` 文件夹即可。
* **兼容性**: 本模组为纯客户端模组，在多人服务器中使用无需服务端安装。
