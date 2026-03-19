# OpenRouterChat

[![GitHub release](https://img.shields.io/github/v/release/enzomescall/paper-openrouter-chat?display_name=tag)](https://github.com/enzomescall/paper-openrouter-chat/releases)
[![Paper](https://img.shields.io/badge/platform-Paper-ffffff?logo=minecraft&logoColor=black)](https://papermc.io/)
[![Java 21](https://img.shields.io/badge/java-21-orange)](https://adoptium.net/)

A Paper plugin that lets players chat with an OpenRouter model directly from inside the game.

## Features

- Direct OpenRouter API integration
- No bundled secrets or API keys
- In-game operator command to save the API key
- Configurable model, prompt, timeout, and chat formatting
- Permission nodes for normal usage vs management

## Setup

1. Download the jar from Releases
2. Put it in your server's `plugins/` folder
3. Restart the server
4. Set your OpenRouter API key using either:
   - config file: `plugins/OpenRouterChat/config.yml`
   - in-game as op: `/openrouter add-key <your-key>`

## Commands

- `/openrouter <message>` — send a prompt to the configured OpenRouter model
- `/openrouter add-key <api-key>` — save the OpenRouter API key
- `/openrouter clear-key` — remove the stored API key
- `/openrouter reload` — reload config
- `/openrouter status` — view current status

Aliases:
- `/orc`
- `/llm`

## Permissions

- `openrouterchat.use`
  - Allows using `/openrouter <message>`
  - Default: `op`
- `openrouterchat.manage`
  - Allows `/openrouter add-key`, `clear-key`, `reload`, and `status`
  - Default: `op`

## Config

```yaml
api-key: ""
model: "openai/gpt-4.1-mini"
site-url: ""
site-name: "Paper OpenRouter Chat"
system-prompt: "You are a helpful Minecraft server assistant. Keep answers concise and useful for in-game chat."
max-tokens: 300
temperature: 0.7
timeout-seconds: 45
response-prefix: "&b[OpenRouter] &f"
error-prefix: "&c[OpenRouter] &7"
thinking-message: "&e[OpenRouter] &7&oThinking..."
ops-only-chat: false
```

## Build

```bash
mvn clean package
```

Output:

```bash
target/paper-openrouter-chat-0.1.0.jar
```

## Compatibility

- Paper `1.21.4`
- Java `21`

## Security note

This repo intentionally ships with an empty API key. Users should add their own OpenRouter key after installing the plugin.

## License

GPL-3.0
