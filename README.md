# Battista Ai Helper

> **A Minecraft 1.21.1+ plugin that brings AI answers to your server chat!**

---

## ✨ Overview

Battista Ai Helper is a plugin for Minecraft Paper/Spigot servers that lets players ask questions and receive instant
AI-powered answers directly in-game. Players can ask privately with a command, publicly in chat using a tag (
like `@Helper`), or just by typing a question ending in a `?` – the plugin handles it all!



- **Fast**: Asynchronous HTTP calls for zero server lag
- **Flexible**: Multiple activation modes (command, tag, auto-detect)
- **Integrated**: AI helper appears in TAB and answers interactively
- **Document-Aware**: Knowledge filtering system, with built-in document management and commands to view active knowledge sources
- **Fully Configurable**: Customizable message lengths, endpoints, behaviors, and all user-facing text
- **Safe \& Reliable**: Comprehensive permissions system, input validation, and robust error handling

---

## 🎮 Usage

- **Ask with a command:**
  ```
  /ask What should i do in this server?
  ```
  *(Private reply only to you)*


- **Ask interactively:**
  ```
  /ask
  ```
  *[AI Helper] Hey, i'm Battista, the AI Helper! feel free to ask me anything! Just send a chat message and i'll respond
  to you!*
  ```
  What should i do in this server?
  ```
  *(Private reply only to you)*


- **Ask in chat with tag:**
  ```
  @Helper Where can i get wood?
  ```
  *(Public reply visible to everyone)*


- **Auto-detect questions (ends with ?):**
  ```
  How do i get back to spawn?
  ````
  *(Public reply visible to everyone)*

---

## 🔒 Permissions

| Permission        | Description                          | Default |
|-------------------|--------------------------------------|---------|
| `battista.use`    | Use /ask and chat AI features        | true    |
| `battista.reload` | Reload plugin via `/battista reload` | op      |

---

## 🛠️ Commands

| Command            | Description                  |
|--------------------|------------------------------|
| `/ask`             | Ask AI with interactive mode |
| `/ask <question>`  | Ask AI privately             |
| `/battista reload` | Reload plugin configuration  |
| `/battista help`   | Show help message            |

---

## ⚙️ Configuration

The plugin is highly configurable through `config.yml`, allowing you to customize every aspect of the AI helper's behavior and appearance.

### Key Configuration Areas

**Backend Integration**: Configure your AI backend endpoint URL and connection settings. The plugin is designed to work optimally with the separate [Battista AI Backend application](https://github.com/Manuel-Materazzo/battista-ai-backend).

**Knowledge Filtering**: Set up source filters to target specific knowledge subsets, such as server-specific folders in your document repository. This enables contextual responses tailored to your server's needs.

**Tab Menu Integration**: Control whether the AI helper appears in the player TAB menu (requires ProtocolLib) and customize its skin and display name.

**Chat Activation Methods**: Fine-tune how players can interact with the AI:

- **Tagging**: Enable/disable chat tag detection (default: `@Helper`)
- **Auto-detection**: Configure automatic question recognition for messages ending with `?`
- **Response formatting**: Customize the chat prefix for AI responses

**Rate Limiting**: Configure character limits and timeouts to prevent abuse while maintaining responsive gameplay.

**Messages**: All player-facing text is fully customizable, including error messages, prompts, and system notifications.
They also support [pseudo-mardown](https://github.com/Manuel-Materazzo/battista-ai-spigot/blob/master/SYNTAX_HELP.md)!

The configuration file includes detailed comments explaining each setting and providing examples for optimal setup.

---

## 📦 Installation

1. **Build or Download the plugin:**

    - Maven:
      ```bash
      mvn clean package
      ```
      The jar will be in `target/`.

    - Or download from the release page

2. **Copy the jar:**
   ```bash
   cp target/aihelper-plugin-1.0.0.jar /your/server/plugins/
   ```

3. **Restart your server.**

4. **Configure the plugin:**
    - Edit `plugins/Battista-Ai-Helper/config.yml`
    - Set your AI endpoint:
      ```yaml
      endpoint:
        answer-url: "http://your-ai-server.com/v2/answer"
      ```
    - Adjust any other settings/messages as needed.

---

## 🏆 Status

**Production-ready.**  
Tested on Paper/Purpur/Spigot 1.21.1+  
Java 21+ required.  
Bug reports and improvements welcome!

---

## 📄 License

MIT

---