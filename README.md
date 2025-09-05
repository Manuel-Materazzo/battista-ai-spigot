# Battista Ai Helper

> **A Minecraft 1.21.1+ plugin that brings AI answers to your server chat!**

---

## ✨ Overview

Battista Ai Helper is a plugin for Minecraft Paper/Spigot servers that lets players ask questions and receive instant AI-powered answers directly in-game. Players can ask privately with a command, publicly in chat using a tag (like `@Helper`), or just by typing a question ending in a `?` – the plugin handles it all!

- **Fast**: Asynchronous HTTP calls, no server lag
- **Flexible**: Three activation modes (command, tag, auto-detect)
- **Safe**: Permissions, validation, error handling
- **Easy to use**: Low mantainance and configuration
- **Fully configurable**: All messages, endpoint, and behaviors are configurable

---

## 🎮 Usage

- **Ask privately:**
  ```
  /ask What should i do in this server?
  ```
  *(Private reply only to you)*


- **Ask in chat with tag:**
  ```
  @Helper Where can i get wood?
  ```
  *(Public reply visible to everyone)*


- **Auto-detect (ends with ?):**
  ```
  How do i get back to spawn?
  ````
  *(Public reply visible to everyone)*

---

## 🔒 Permissions

| Permission         | Description                          | Default |
|--------------------|--------------------------------------|---------|
| `battista.use`     | Use /ask and chat AI features        | true    |
| `battista.reload`  | Reload plugin via `/battista reload` | op      |

---

## 🛠️ Commands

| Command                | Description                       |
|------------------------|-----------------------------------|
| `/ask <question>`      | Ask AI privately                  |
| `/battista reload`     | Reload plugin configuration       |
| `/battista help`       | Show help message                 |

---

## ⚙️ Configuration

Edit everything in `config.yml`.

- **Endpoint settings:**  
  ```yaml
  endpoint:
    url: "http://localhost:8000/v2/answer"
    timeout: 30
  ```

- **Chat options:**
  ```yaml
  chat:
    tagging:
      enabled: true
      tag: "@Helper"
    auto_detect_questions: true
    response_prefix: "&7[&bAI Helper&7]&f "
  ```

- **Messages:**  
  All plugin responses are customizable.

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
        url: "http://your-ai-server.com/v2/answer"
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