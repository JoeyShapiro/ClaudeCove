package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun appDataDir(appName: String): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> File(home, "Library/Application Support/$appName")
        os.contains("win") -> File(System.getenv("APPDATA") ?: "$home/AppData/Roaming", appName)
        else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", appName)
    }
}

fun main() = application {
    val scope = rememberCoroutineScope()
    // TODO let the program run anyway. but dont start claude
    val exe = Claude.findClaude()
            .getOrThrow()

    val appDataDir = appDataDir("Claude Cove")
    appDataDir.mkdirs()
    Database.connect("jdbc:sqlite:${File(appDataDir, "ClaudeCode.sqlite").absolutePath}", driver = "org.sqlite.JDBC")
    transaction {
        // CREATE TABLE IF NOT EXISTS
        SchemaUtils.create(Projects, Sessions, Messages, Settings)
        // additive migrations
        SchemaUtils.createMissingTablesAndColumns(Sessions)
    }

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