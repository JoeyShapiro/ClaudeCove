package dev.thegreenhouse.claudecove.claudecove

import kotlinx.serialization.Serializable
import java.util.UUID

class Claude {
    val loadingFlavorText = listOf(
        // Digging in / excavating
        "Tunneling deeper",
        "Breaking through the rock face",
        "Digging through the seam",
        "Clearing the rubble",
        "Descending the shaft",
        // Prospecting / searching
        "Surveying the vein",
        "Panning for an answer",
        "Reading the strata",
        "Mapping the cavern",
        "Tracing the ore",
        // Atmosphere / ambiance
        "Listening to the deep",
        "Following the echo",
        "Reading the walls",
        "Letting dust settle",
        "Sensing the dark",
        // Tools / process
        "Shoring up the tunnel",
        "Checking the timbers",
        "Calibrating the lantern",
        "Adjusting the airflow",
        "Loading the cart",
        // More poetic/abstract
        "Finding the vein",
        "Light in the rock",
        "Patience of stone",
        "Sifting through the dark",
        "The mountain thinks"
    )

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
    data class Result (
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
}
