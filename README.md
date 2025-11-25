![QuickAlias](https://media.forgecdn.net/attachments/1403/433/quickaliasbanner-png.png)

<p align="center">
  <!-- Modrinth -->
  <a href="https://modrinth.com/project/quickalias"><img src="https://img.shields.io/modrinth/dt/jGHTlhbU?logo=modrinth&label=Modrinth&style=for-the-badge" alt="Modrinth Downloads"/></a>
  <!-- CurseForge -->
  <a href="https://www.curseforge.com/minecraft/mc-mods/quickalias"><img src="https://img.shields.io/curseforge/dt/1391158?logo=curseforge&label=CurseForge&style=for-the-badge" alt="CurseForge Downloads"/></a>
  <!-- Wiki -->
  <a href="https://quickalias.pages.dev"><img src="https://img.shields.io/badge/Documentation-Wiki-4682B4?style=for-the-badge&logo=docusaurus" alt="Wiki"/></a>
</p>

<p align="center">
  English ·
  <a href="./README_zh-CN.md">简体中文</a>
</p>

**QuickAlias** is a client-side utility modification designed to optimize command interaction efficiency within
Minecraft. It features a comprehensive visual alias editor, hierarchical command menus, dynamic variable parsing, and a
quick access command panel.

This mod aims to resolve the tediousness associated with manual entry of repetitive command strings. Users can construct
customized command hierarchies and access them seamlessly via standard chat auto-completion or the convenient visual
overlay panel.

## ✨ Key Features

* **🎨 Integrated Visual Editor**: Eliminates the need for manual JSON configuration. Efficiently create, edit,
  reorganize, or **import** alias configurations directly through the in-game GUI (accessible via the chat interface
  settings).
* **🌳 Hierarchical Command Structures**: Supports the creation of nested menu systems. For example, define a root alias
  `Warp`, and cascade sub-options such as `Home`, `Hub`, and `Mining` under it.
* **🚀 Macro Functionality**: Allows binding **multiple commands** to a single alias identifier. A single trigger can
  execute a predefined sequence of operations in order.
* **🧩 Dynamic Variable System**:
    * **System Constants**: Automatically resolves contextual values including current coordinates (`{X}`, `{Y}`,
      `{Z}`), player ID (`{ID}`), and dimension info (`{DIM}`).
    * **Custom Arguments**: Supports defining dynamic aliases such as `/invite {player}`. The system intelligently
      captures user input and dynamically replaces the corresponding variables in the command.
* **⌨️ Native Integration**:
    * **Auto-Completion**: Custom aliases seamlessly integrate into the game's chat suggestion system, providing an
      interaction experience consistent with vanilla commands.
    * **Smart Suggestions**: Variables named `{id}` automatically prompt online player names, while `{dim}` suggests
      dimension IDs.
* **🖥️ Overlay Navigation**: The chat interface integrates a shortcut entry (the `/` icon), which summons a visual alias
  navigation menu, enabling command execution without typing.

## 📖 Usage Guide

### 1. Creating a Basic Alias

1. Open the Chat interface.
2. Click the **Settings (⚙)** icon to enter the QuickAlias configuration panel.
3. Click the `+` button to create a new root alias.
4. **Alias Name**: Enter the trigger keyword (e.g., `home`).
5. **Command**: Enter the command content to be executed (e.g., `/tp <x> <y> <z>`).
6. Save the settings. You can now call this alias via `/home` in the chat bar.

### 2. Using Variables (Arguments)

To create an alias that accepts dynamic arguments (such as a player name):

1. Create a root alias, for example, `hi`.
2. Add a **Sub-Option** to it.
3. Name the sub-option `{id}` (Curly braces indicate this node is a variable).
4. In the `{id}` node's command list, enter: `hello {id}`.
5. **Execution**: Enter `/hi Steve` in the chat bar, and the system will execute `hello Steve` (text without a slash "/"
   will be sent as a chat message).

### 3. Built-in Placeholders

The following placeholders can be used in any command configuration:

* `{X}`, `{Y}`, `{Z}` - Current coordinate values (retains one decimal place).
* `{ID}` - Current player name.
* `{DIM}` - Registered resource name of the current dimension.

### 📥 Installation

* **Installation**: Simply place the `.jar` file into the `mods` folder in your game directory.
* **Compatibility**: This is a client-side only mod; it functions on multiplayer servers without requiring server-side
  installation.
