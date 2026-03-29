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
                *Claude.args
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