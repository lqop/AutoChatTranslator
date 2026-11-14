# 自动聊天翻译 (Auto Chat Translator)

A simple client-side Minecraft mod that **automatically translates incoming chat messages** and keeps the original text. Never get lost in foreign chat again!

一个简单的 Forge 客户端模组：**自动翻译收到的聊天消息，并保留原文**。支持系统消息（NPC、模组剧情对话）翻译，内置全中文游戏内配置界面。

> Currently in testing — bugs may occur. 模组仍在测试阶段，欢迎反馈。

## Features / 功能

- Auto-translates incoming chat and shows the original / 自动翻译聊天消息，保留原文
- Hover a translation to see the original, click to copy it / 悬停译文显示原文，点击复制原文
- Optional system-message translation (NPC & mod dialogue) / 可选翻译系统消息
- Multiple providers / 多翻译后端：
  - **MyMemory** — free, no API key / 免费、无需密钥
  - **Google** — public endpoint / 公共接口
  - **DeepL** — requires API key / 需要 API key
  - **OpenAI-compatible** — OpenRouter, DeepSeek, MiMo, Ollama and more (presets + connection test)
    / OpenAI 兼容接口，内置常用服务商预设和一键连接测试
- In-game config screen (YACL) with a full Chinese UI / 基于 YACL 的游戏内中文配置界面
- Translation cache, request queue, timeout and length filters / 缓存、队列、超时、长度过滤
- Config file (`config/autochattranslator-client.toml`) with Chinese comments / 带中文注释的配置文件

## Requirements / 环境要求

| | |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47.x |
| Java | 17 |
| Dependency | [YetAnotherConfigLib (YACL)](https://www.curseforge.com/minecraft/mc-mods/yet-another-config-lib) 3.6.6+ (required, client) |

## Download / 下载

- CurseForge: <https://www.curseforge.com/minecraft/mc-mods/simple-auto-translation>

## Commands / 命令

| Command | Description |
|---|---|
| `/ctrans` or `/ctrans status` | Show current status / 查看状态 |
| `/ctrans toggle` | Enable / disable / 开关翻译 |
| `/ctrans config` | Open the config screen / 打开配置界面 |
| `/ctrans test <text>` | Test translation / 测试翻译 |

## Privacy / 隐私说明

Messages to be translated are sent to the translation provider you selected. API keys are stored in plain text in your local config file — never share your config file.

被翻译的聊天内容会发送给你选择的翻译服务。API 密钥以明文保存在本地配置文件中，请勿分享。

## Building / 构建

```bash
gradle build
# output: build/libs/autochattranslator-1.20.1-<version>.jar
```

Requires Java 17. The dev environment also needs `mixin.env.remapRefMap` pointed at `build/createSrgToMcp/output.srg` to run YACL in-game (already configured in `build.gradle`).

## License / 许可证

[MIT](LICENSE)
