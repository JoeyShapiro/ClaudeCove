<img src="icons/icon-1024.png" alt="ClaudeCove logo" width="96" align="left" style="margin-right: 16px"/>

# ClaudeCove

**A proper desktop GUI for [Claude Code](https://claude.ai/code) — because you deserve windows, not a terminal.**

<br clear="left"/>

---

## 🚀 Features That Will 10x Your Developer Productivity and Disrupt the AI Interface Paradigm 🚀

- **Hyper-local AI synergy** — all inference happens via your own Claude subscription, so the synergies are truly yours
- **Blockchain-free** — zero smart contracts, maximally dumb database (SQLite)
- **Proprietary flavor-text engine** — 42 hand-crafted loading messages trained on decades of cave lore
- **Unlimited scale** — runs on any laptop, even the one your dog sat on
- **Better Than Java** - uses kotlin. all the benefits of JVM, no annoyance of Java
- **Enterprise-grade privacy focused storage** — a `.sqlite` file in the app directory
- **AI-powered AI interface** — it's an app for an AI, making it at minimum 2× more AI than your current workflow
- **Disrupting the human-computer interaction paradigm** — it has a text box

---

## Jokes Aside, Hhy...
- I wanted to try kotlin and CMP
- Claude interfaces suck
- Can't use Claude app at work
- Got to reverse engineer Claude Code (before the src leak)
- VSCode Claude WAS bad (its better now)
- Wanted to save chats
- Wanted to be able to ask questions without a folder

## What it looks like

> **TODO:** Drop your GIFs into `media/` and uncomment these lines.

<!-- ![Chatting with Claude](media/demo-chat.gif) -->
<!-- ![Managing sessions](media/demo-sessions.gif) -->
<!-- ![Permission dialogs](media/demo-permission.gif) -->
<!-- ![Thinking indicator](media/demo-thinking.gif) -->
<!-- ![Theme switcher](media/demo-themes.gif) -->

---

## Features

### Session & project management
Organize your Claude conversations the way you organize your code — by project. Every working directory gets its own folder in the sidebar. Sessions live inside them, auto-named from your first message. Switch between conversations instantly, delete the ones you don't need, and your history is always there when you come back (persisted to a local SQLite database).

### Full markdown & syntax highlighting
Claude's responses render as real markdown — headers, lists, bold, all of it. Code blocks get syntax highlighting so reading a snippet of Python or TypeScript doesn't feel like staring into the void.

### Permission prompt dialogs
When Claude wants to run a tool that needs your approval, a dialog appears in the app. Hit **Yes**, **No**, or **Yes to All** — no hunting for the right terminal line to type into.

### Thinking indicator with flavor text
While Claude is working, the app shows one of 42 loading messages themed around mining and caverns ("Dynamiting the conceptual bedrock", "Consulting the ancient rock strata", etc.). Yes, this matters. Staring at a spinner is miserable; staring at a spinner that says *"Bribing the cave troll"* is at least funny.

### Light, dark, and system themes
Three options in Settings. Warm terracotta accent color, off-white light background, very-dark-brown dark background. Looks like a real app and not an IDE config panel.

### Persistent history
All messages and session state are saved immediately to a local SQLite database. Close the app, open it again, pick up exactly where you left off. No cloud, no sync, just a file on your machine.

---

## Why a GUI instead of just using the terminal?

Claude Code already has a perfectly functional TUI. So why bother?

- **Multiple sessions, visible at once.** The sidebar shows all your projects and conversations. Switching between them is a click, not a sequence of `exit` → `cd` → `claude --resume <id>` commands you half-remember.
- **Real text rendering.** Markdown is markdown, not raw asterisks and backticks. Code blocks are highlighted. You don't have to squint.
- **Dialogs for permission prompts.** In a terminal, a permission prompt is another line of text you might miss. In ClaudeCove, it's a modal that blocks until you answer.
- **Your other apps can still exist.** A GUI sits alongside everything else on your screen. A full-screen TUI does not.
- **It feels like software.** Themes, icons, smooth scrolling, hover states — the small stuff that makes a tool pleasant to use for hours at a time.

TUIs are cool, and I really like them. But I am starting to see places where a GUI is a better option.

---

## Building & Running

**Prerequisites:** JDK 17+, Kotlin, Gradle. Claude Code CLI installed and on your PATH (or configure a custom path in Settings).

```shell
# macOS / Linux
./gradlew :composeApp:run

# Windows
.\gradlew.bat :composeApp:run
```

### Distributable packages

```shell
# macOS .dmg
./gradlew :composeApp:packageDmg

# Windows .msi
.\gradlew.bat :composeApp:packageMsi

# Linux .deb
./gradlew :composeApp:packageDeb
```

---

## Settings

Open the gear icon in the sidebar.

| Setting | Default | Description |
|---------|---------|-------------|
| Theme | System | Light, Dark, or follow the OS |
| Claude executable | system PATH | Point to a custom `claude` binary if needed |

---

## Project structure

```
composeApp/src/jvmMain/kotlin/…/claudecove/
├── App.kt            — all UI composables and screen logic
├── main.kt           — application entry point and window setup
├── ProcessManager.kt — spawns and communicates with the Claude CLI process
└── Claude.kt         — serializable data models for the Claude JSON protocol
```

Messages go in, JSON streams come out, the UI updates. That's the whole architecture.

---

Built with [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/).
