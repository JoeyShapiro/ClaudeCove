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

    // {"type":"result","subtype":"success","is_error":false,"duration_ms":20586,"duration_api_ms":19589,"num_turns":2,"result":"This is a **2D action/bullet-hell game** written in C, called **Calamari**.\n\n**Tech stack:**\n- **Language:** C (C99)\n- **Graphics:** Raylib\n- **Build system:** Zig (`build.zig`)\n\n**What it does:**\nThe player moves with WASD and has a unique dash mechanic — hold the left mouse button to record a spline path (Catmull-Rom curve) based on mouse movement, then release to dash along that path, damaging enemies. Enemies spawn and chase the player.\n\n**Current features:**\n- Focus/dash movement with spline path recording\n- Enemy mobs that chase the player\n- Kill count, kill streak, health/energy bars\n- Collision detection and damage system\n\nIt's a small, self-contained game in an early development stage — everything lives in a single `main.c` (~267 lines).","stop_reason":"end_turn","session_id":"3f4bf46f-b7ca-4a02-b503-849e06e210c5","total_cost_usd":0.06580615000000001,"usage":{"input_tokens":3,"cache_creation_input_tokens":4895,"cache_read_input_tokens":26279,"output_tokens":402,"server_tool_use":{"web_search_requests":0,"web_fetch_requests":0},"service_tier":"standard","cache_creation":{"ephemeral_1h_input_tokens":4895,"ephemeral_5m_input_tokens":0},"inference_geo":"","iterations":[],"speed":"standard"},"modelUsage":{"claude-sonnet-4-6":{"inputTokens":3,"outputTokens":402,"cacheReadInputTokens":26279,"cacheCreationInputTokens":4895,"webSearchRequests":0,"costUSD":0.03227895,"contextWindow":200000,"maxOutputTokens":32000},"claude-haiku-4-5-20251001":{"inputTokens":20,"outputTokens":977,"cacheReadInputTokens":45372,"cacheCreationInputTokens":19268,"webSearchRequests":0,"costUSD":0.03352720000000001,"contextWindow":200000,"maxOutputTokens":32000}},"permission_denials":[],"fast_mode_state":"off","uuid":"c200ce7b-03a0-46a0-89a9-b6fa47a556df"}
    @Serializable
    data class Result(

    )
}
