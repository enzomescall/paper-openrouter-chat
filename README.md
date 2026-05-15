# OpenRouterChat

[![GitHub release](https://img.shields.io/github/v/release/enzomescall/paper-openrouter-chat?display_name=tag)](https://github.com/enzomescall/paper-openrouter-chat/releases)
[![Paper](https://img.shields.io/badge/platform-Paper-ffffff?logo=minecraft&logoColor=black)](https://papermc.io/)
[![Java 25](https://img.shields.io/badge/java-25-orange)](https://adoptium.net/)

A Paper plugin that lets players send a prompt to an OpenRouter model and receive the reply in chat. Each `/openrouter` invocation is a single, stateless request — the plugin does not keep conversation history between messages.

## Features

- Direct OpenRouter API integration
- Single-turn prompts only (no conversation memory between messages)
- No bundled secrets or API keys
- Operator command to save the API key from in-game or the server console
- Configurable model, system prompt, timeout, and chat formatting
- Permission nodes for normal usage vs management
- Optional `ops-only-chat` toggle that requires operator status on top of the use permission

## Setup

1. Download the jar from Releases
2. Put it in your server's `plugins/` folder
3. Restart the server
4. Set your OpenRouter API key using either:
   - config file: `plugins/OpenRouterChat/config.yml`
   - in-game (as op) or from the server console: `/openrouter add-key <your-key>`

## Commands

The `/openrouter <message>` form must be run by a player so the response can be delivered to them. The management subcommands (`add-key`, `clear-key`, `reload`, `status`) can be run from either an in-game player with the manage permission or the server console.

- `/openrouter <message>` — send a prompt to the configured OpenRouter model (player only)
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

`ops-only-chat` is an extra gate on top of the `openrouterchat.use` permission. When set to `true`, only server operators can run `/openrouter <message>`, even if non-op players have been granted `openrouterchat.use`. When `false` (the default), the permission node alone controls access.

## Build

```bash
mvn clean package
```

Output:

```bash
target/paper-openrouter-chat-0.1.0.jar
```

## Compatibility

- Paper `26.1.2`
- Java `25`

## Security note

This repo intentionally ships with an empty API key. Users should add their own OpenRouter key after installing the plugin.

## License

GPL-3.0
