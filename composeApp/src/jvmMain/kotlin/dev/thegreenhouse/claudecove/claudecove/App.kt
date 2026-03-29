package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun App(processManager: ProcessManager) {
    val json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
        ignoreUnknownKeys = true   // don't crash if JSON has extra fields
        isLenient = true           // allows unquoted keys, trailing commas
        encodeDefaults = true      // include fields that equal their default value
    }

    val selectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.inversePrimary,
        backgroundColor = MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.4f)
    )

    MaterialTheme {
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            var input by remember { mutableStateOf("") }
            var messages by remember { mutableStateOf(listOf<Message>()) }
            val listState = rememberLazyListState()

            // Collect stdout
            LaunchedEffect(Unit) {
                processManager.stdout.collect { line ->
                    try {
                        val result = json.decodeFromString<Claude.Result>(line)
                        messages = messages + Message(text = result.result, fromSelf = false)
                    } catch (e: SerializationException) {
                        println(e.localizedMessage)
                        println(line)
                        println("------------------------------")
                    }
                }
            }

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }

            Column(
                modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .safeContentPadding()
                        .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = false  // set true if you want newest at bottom like most chat apps
                ) {
                    items(items = messages) { message ->
                        ChatBubble(message)
                    }
                }
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        input,
                        label = { Text("prompt") },
                        modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp)  // grows from 150dp, expands with content
                                .onKeyEvent { event ->
                                    // KeyDown for enter is actually
                                    // nativeKeyEvent=InternalKeyEvent(key=Key: Unknown keyCode: 0x0, type=Unknown)
                                    if (event.key == Key.Enter && event.type == KeyEventType.KeyUp &&
                                        !event.isShiftPressed
                                    ) {
                                        // {"type":"user","uuid":"cc919660-fa64-4ca6-9e0c-473def3b9436","session_id":"","parent_tool_use_id":null, "message":{"role":"user","content":[{"type":"text","text":""}]}}
                                        val prompt = Claude.Prompt.new(input)
                                        val data = json.encodeToString(prompt)

                                        processManager.sendLine(data)
                                        messages = messages + Message(
                                            text = input,
                                            fromSelf = true
                                        )
                                        input = ""
                                        true
                                    } else {
                                        false
                                    }
                                },
                        minLines = 5,   // always shows at least 5 lines tall
                        maxLines = 10,  // caps growth at 10 lines
                        onValueChange = { input = it }
                    )
                }
            }
        }
    }
}

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromSelf: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatBubble(message: Message) {
    val clipboardManager = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = maxWidth * 0.75f

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.fromSelf) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                        .widthIn(max = bubbleMaxWidth)
                        .hoverable(interactionSource)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    // TODO use markdown and only render as markdown when streaming is done
                    SelectionContainer {
                        Text(
                            text = message.text,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    TextButton(
                        onClick = {
                            clipboardManager.setText(
                                AnnotatedString(
                                    message.text
                                )
                            )
                        },
                        modifier = Modifier
                                .align(Alignment.End)
                                .alpha(if (isHovered) 1f else 0f)
                    ) {
                        Text(
                            "Copy",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(
                                alpha = 0.7f
                            )
                        )
                    }
                }
            }
        }
    }
}
