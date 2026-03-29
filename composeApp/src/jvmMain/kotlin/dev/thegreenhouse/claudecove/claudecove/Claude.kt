package dev.thegreenhouse.claudecove.claudecove

import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

class Claude {
    // {"request_id":"dxv71vev7ef","type":"control_request",
    // "request":{"subtype":"generate_session_title","description":"","persist":false}}
    @Serializable
    data class ControlRequest (val requestId: String, val type: String, val request: Request)

    @Serializable
    data class Request (val subtype: String, val description: String, val persist: Boolean = false)

    // {"type":"user","uuid":"cc919660-fa64-4ca6-9e0c-473def3b9436","session_id":"","parent_tool_use_id":null,
// "message":{"role":"user","content":[{"type":"text","text":""}]}}
    @Serializable
    data class Prompt (val type: String, val uuid: String, val sessionId: String, val parentToolUseId: String?, val message: Message) {
        companion object {
            fun new(text: String) = Prompt(
                type = "user",
                uuid = UUID.randomUUID().toString(),
                sessionId = "",
                parentToolUseId = null,
                message = Message(
                    role = "user",
                    content = listOf(Content(
                        type = "text",
                        text = text
                    ))
                ),
            )
        }
    }

    @Serializable
    data class Message (val role: String, val content: List<Content>)

    @Serializable
    data class Content (val type: String, val text: String)

    @Serializable
    data class Response (
        val type: String,
        val subtype: String,
        val isError: Boolean,
        val durationMs: Int,
        val durationApiMs: Int,
        val numTurns: Int,
        val result: String,
        val stopReason: String,
        val sessionId: String,
        val totalCostUsd: Float,
        // ... more stuff
        val permissionDenials: List<String>,
        val fastModeState: String,
        val uuid: String,
    )

    companion object {
        fun findClaude(): Result<File> {
            // TODO get proper list of locations and add setting for custom
            val home = File(System.getProperty("user.home"))
            val file = if (System.getProperty("os.name").lowercase().contains("windows")) {
                File(System.getenv("APPDATA"), "config.ini")
            } else {
                home.resolve(".local/bin/claude")
            }

            if (!file.exists()) {
                return Result.failure(FileNotFoundException("File not found: ${file.absolutePath}"))
            }

            return Result.success(file)
        }
    }
}
