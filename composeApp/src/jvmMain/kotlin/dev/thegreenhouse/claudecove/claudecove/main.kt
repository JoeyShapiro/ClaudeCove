package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

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

    Database.connect("jdbc:sqlite:ClaudeCode.sqlite", driver = "org.sqlite.JDBC")
    transaction {
        // CREATE TABLE IF NOT EXISTS
        SchemaUtils.create(Projects, Sessions, Messages)
        // additive migrations
        // SchemaUtils.createMissingTablesAndColumns(Users, Posts)
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