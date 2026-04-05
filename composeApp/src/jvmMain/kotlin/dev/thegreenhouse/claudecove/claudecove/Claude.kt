package dev.thegreenhouse.claudecove.claudecove

import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

class Claude {
    // Sealed class hierarchy for all your response types
    @Serializable(with = EventSerializer::class)
    sealed class Event

    // Custom serializer that pre-parses `type` (and optionally `subtype`)
    object EventSerializer : KSerializer<Event> {
        @OptIn(ExperimentalSerializationApi::class)
        val jsonConfiguration = Json {
            namingStrategy = JsonNamingStrategy.SnakeCase
            ignoreUnknownKeys = true   // don't crash if JSON has extra fields
            isLenient = true           // allows unquoted keys, trailing commas
            encodeDefaults = true      // include fields that equal their default value
        }

        override val descriptor = buildClassSerialDescriptor("Event")

        override fun deserialize(decoder: Decoder): Event {
            val json = (decoder as JsonDecoder).decodeJsonElement().jsonObject

            val type = json["type"]?.jsonPrimitive?.content
            val subtype = json["subtype"]?.jsonPrimitive?.contentOrNull

            return when {
                type == "control_response" -> jsonConfiguration.decodeFromJsonElement<ResponseControl>(json)
                type == "result"           -> jsonConfiguration.decodeFromJsonElement<ResponseResult>(json)
                else -> throw SerializationException("Unknown event type: $type / $subtype")
            }
        }

        override fun serialize(encoder: Encoder, value: Event) {
            throw UnsupportedOperationException("Serialization not needed")
        }
    }

    // {"request_id":"dxv71vev7ef","type":"control_request",
    // "request":{"subtype":"generate_session_title","description":"","persist":false}}
    @Serializable
    data class ControlRequest (val requestId: String, val type: String, val request: Request) {
        companion object {
            fun title(description: String) = ControlRequest(
                requestId = UUID.randomUUID().toString(),
                type = "control_request",
                request = Request(
                    subtype = "generate_session_title",
                    description = description,
                    persist = false,
                )
            )
        }
    }

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
    data class ResponseResult (
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
    ) : Event()

    @Serializable
    data class ResponseControl(
        val type: String,
        val response: ControlResponseResponse,
    ) : Event()

    @Serializable
    data class ControlResponseResponse(
        val subtype: String,
        val requestId: String,
        val response: ControlResponseResponseResponse,
    )

    @Serializable
    data class ControlResponseResponseResponse(
        val title: String,
    )

    companion object {
        val args = arrayOf(
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

        fun findClaude(): Result<File> {
            // could be in winget or homebrew. at that point use should specify it
            // or maybe i could use where/which
            val home = File(System.getProperty("user.home"))
            val file = home.resolve(".local/bin/claude")

            if (!file.exists()) {
                return Result.failure(FileNotFoundException("File not found: ${file.absolutePath}"))
            }

            return Result.success(file)
        }
    }
}
