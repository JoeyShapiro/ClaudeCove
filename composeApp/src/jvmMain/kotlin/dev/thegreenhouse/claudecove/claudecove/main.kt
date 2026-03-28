package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val scope = rememberCoroutineScope()
    val processManager = remember {
        //{"request_id":"dxv71vev7ef","type":"control_request","request":{"subtype":"generate_session_title","description":"add a setting to change the path of the capture","persist":false}}
        //{"request_id":"dxv71vev7ef","type":"control_request","request":{"subtype":"generate_session_title","description":"based on the contents of this directory. what type of project does this look like","persist":false}}
        ProcessManager(scope).also {
            it.start(
                "/Users/oniichan/.local/bin/claude",
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