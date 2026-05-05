package dev.thegreenhouse.claudecove.claudecove

import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

// Use on a property to emit a specific JSON key, bypassing snake_case conversion.
// Works like @SerialName but is detectable by SerialOrSnakeCase at runtime.
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class JsonName(val value: String)

// Converts camelCase property names to snake_case JSON keys.
// Properties annotated with @JsonName use that value verbatim instead.
@OptIn(ExperimentalSerializationApi::class)
object SerialOrSnakeCase : JsonNamingStrategy {
    override fun serialNameForJson(descriptor: SerialDescriptor, elementIndex: Int, serialName: String): String {
        val jsonName = descriptor.getElementAnnotations(elementIndex)
            .filterIsInstance<JsonName>()
            .firstOrNull()
        if (jsonName != null) return jsonName.value

        return buildString {
            var allUpperSoFar = true
            var lastUpperIndex = -1
            for (i in serialName.indices) {
                val c = serialName[i]
                if (c.isUpperCase()) {
                    if (!allUpperSoFar) {
                        if (isNotEmpty()) append('_')
                        lastUpperIndex = i
                        allUpperSoFar = true
                    }
                    append(c.lowercaseChar())
                } else {
                    if (allUpperSoFar && lastUpperIndex >= 0 && i - lastUpperIndex > 1) {
                        insert(length - 1, '_')
                    }
                    allUpperSoFar = false
                    append(c)
                }
            }
        }
    }
}

class Claude {
    // Sealed class hierarchy for all your response types
    @Serializable(with = EventSerializer::class)
    sealed class Event

    // Custom serializer that pre-parses `type` (and optionally `subtype`)
    object EventSerializer : KSerializer<Event> {
        @OptIn(ExperimentalSerializationApi::class)
        val jsonConfiguration = Json {
            namingStrategy = SerialOrSnakeCase
            ignoreUnknownKeys = true   // don't crash if JSON has extra fields
            isLenient = true           // allows unquoted keys, trailing commas
        }

        override val descriptor = buildClassSerialDescriptor("Event")

        override fun deserialize(decoder: Decoder): Event {
            val json = (decoder as JsonDecoder).decodeJsonElement().jsonObject

            val type = json["type"]?.jsonPrimitive?.content

            return when (type) {
                "control_request" -> {
                    val subtype = json["request"]?.jsonObject?.get("subtype")?.jsonPrimitive?.content
                    when (subtype) {
                        "hook_callback" -> jsonConfiguration.decodeFromJsonElement<RequestControl<RequestHookCallback>>(json)
                        "can_use_tool" -> jsonConfiguration.decodeFromJsonElement<RequestControl<RequestTool>>(json)
                        else -> throw SerializationException("Unknown event type: $type / $subtype")

                    }
                }
                "control_response" -> {
                    val subtype = json["response"]?.jsonObject?.get("subtype")?.jsonPrimitive?.content
                    when (subtype) {
                        "success" -> jsonConfiguration.decodeFromJsonElement<ResponseControl<ControlResponseResponseResponse>>(json)
                        else -> throw SerializationException("Unknown event type: $type / $subtype")
                    }
                }
                "system" -> {
                    val subtype = json["subtype"]?.jsonPrimitive.content
                    when (subtype) {
                        "init" -> jsonConfiguration.decodeFromJsonElement<ResponseSystem>(json)
                        else -> throw SerializationException("Unknown event type: $type / $subtype")
                    }
                }
                "result"                                        -> jsonConfiguration.decodeFromJsonElement<ResponseResult>(json)
                else                                            -> throw SerializationException("Unknown event type: $type")
            }
        }

        override fun serialize(encoder: Encoder, value: Event) {
            throw UnsupportedOperationException("Serialization not needed")
        }
    }

    // {"request_id":"dxv71vev7ef","type":"control_request",
    // "request":{"subtype":"generate_session_title","description":"","persist":false}}
    @Serializable
    data class ControlRequest (val requestId: String, val type: String, val request: RequestTitle) {
        companion object {
            fun title(description: String) = ControlRequest(
                requestId = UUID.randomUUID().toString(),
                type = "control_request",
                request = RequestTitle(
                    subtype = "generate_session_title",
                    description = description,
                    persist = false,
                )
            )
        }
    }

    @Serializable
    data class RequestTitle (val subtype: String, val description: String, val persist: Boolean = false)

    @Serializable
    data class RequestTool(
        val subtype: String? = null,
        val toolName: String,
        val input: ToolInput,
        val permissionSuggestions: List<PermissionSuggestion>? = null,
        val toolUseId: String,
    )

    @Serializable
    data class ResponseTool(
        val behavior: String,
        val message: String? = null,
        val interrupt: Boolean? = null,
        @JsonName("updatedInput")
        val updatedInput: ToolInput? = null,
        @JsonName("updatedPermissions")
        val updatedPermissions: List<PermissionSuggestion>? = null,
        @JsonName("toolUseID")
        val toolUseID: String,
    )

    @Serializable
    data class PermissionSuggestion(
        val type: String,
        val mode: String,
        val destination: String,
    )

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
        val permissionDenials: List<RequestTool>,
        val fastModeState: String,
        val uuid: String,
    ) : Event()

    @Serializable
    data class ResponseSystem (
        val type: String,
        val subtype: String,
        val cwd: String,
        val sessionId: String,
        // ... other cool stuff i dont need now
    ) : Event()

    @Serializable
    data class ResponseControl<T>(
        val type: String,
        val response: ControlResponseResponse<T>,
    ) : Event() {
        companion object {
            fun newContinue(requestId: String) = ResponseControl(
                type = "control_response",
                response = ControlResponseResponse(
                    subtype = "success",
                    requestId = requestId,
                    response = ControlResponseResponseResponse(
                        `continue` = true,
                    )
                )
            )

            fun newAccept(requestId: String, request: RequestTool) = ResponseControl(
                type = "control_response",
                response = ControlResponseResponse(
                    subtype = "success",
                    requestId = requestId,
                    response = ResponseTool(
                        behavior = "allow",
                        updatedInput = request.input,
                        updatedPermissions = listOf(),
                        toolUseID = request.toolUseId
                    )
                )
            )

            fun newDecline(requestId: String, request: RequestTool) = ResponseControl(
                type = "control_response",
                response = ControlResponseResponse(
                    subtype = "success",
                    requestId = requestId,
                    response = ResponseTool(
                        behavior = "deny",
                        message = "The user doesn't want to proceed with this tool use. The tool use was rejected (eg. if it was a file edit, the new_string was NOT written to the file). STOP what you are doing and wait for the user to tell you how to proceed.",
                        interrupt = true,
                        toolUseID = request.toolUseId,
                    )
                )
            )

            fun newAcceptAll(requestId: String, request: RequestTool) = ResponseControl(
                type = "control_response",
                response = ControlResponseResponse(
                    subtype = "success",
                    requestId = requestId,
                    response = ResponseTool(
                        behavior = "allow",
                        updatedInput = request.input,
                        updatedPermissions = request.permissionSuggestions,
                        toolUseID = request.toolUseId
                    )
                )
            )
        }

    }

    @Serializable
    data class ControlResponseResponse<T>(
        val subtype: String,
        val requestId: String,
        val response: T,
    )

    @Serializable
    data class ControlResponseResponseResponse(
        val title: String? = null,
        val `continue`: Boolean? = null,
    )

    @Serializable
    data class RequestControl<T> (
        val type: String,
        val requestId: String,
        val request: T,
    ) : Event()

    @Serializable
    data class RequestHookCallback (
        val subtype: String,
        val callbackId: String,
        val input: AgentInput,
        val toolUseId: String,
    )

    @Serializable
    data class AgentInput(
        val sessionId: String,
        val transcriptPath: String,
        val cwd: String,
        val permissionMode: String,
        val hookEventName: String,
        val toolName: String,
        val toolInput: ToolInput,
        val toolUseId: String,
    )

    @Serializable
    data class ToolInput(
        val filePath: String,
        val content: String? = null,
        val oldString: String? = null,
        val newString: String? = null,
        val replaceAll: Boolean? = null,
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
