package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val scope = rememberCoroutineScope()
    // TODO let the program run anyway. but dont start claude
    val exe = Claude.findClaude()
            .getOrThrow()

    val processManager = remember {
        ProcessManager(scope).also {
            it.start(
                exe.absolutePath,
                "--output-format",
                "stream-json",
                "--verbose",
                "--input-format",
                "stream-json",
                "--max-thinking-tokens",
                "31999",
                "--model",
                "default",
                "--permission-prompt-tool",
                "stdio",
                "--mcp-config",
                "{\"mcpServers\":{\"claude-vscode\":{\"type\":\"sdk\",\"name\":\"claude-vscode\"}}}",
                "--setting-sources",
                "user,project,local",
                "--permission-mode",
                "default",
                "--include-partial-messages",
                "--debug",
                "--debug-to-stderr",
                "--enable-auth-status",
                "--no-chrome",
                "--replay-user-messages"
            )
        }
    }

    Window(
        onCloseRequest = {
            processManager.stop()
            exitApplication()
        },
        title = "Claude Cove",
    ) {
        App(processManager)
    }
}