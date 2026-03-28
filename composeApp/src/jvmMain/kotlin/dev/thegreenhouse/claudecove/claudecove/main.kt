package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Claude Cove",
    ) {
        App()
    }
}