package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
@Preview
fun App() {
    MaterialTheme {
        var prompt by remember { mutableStateOf("") }
        var messages by remember { mutableStateOf(listOf<Message>()) }
        val listState = rememberLazyListState()

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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                TextField(prompt,
                    label = { Text("prompt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)  // grows from 150dp, expands with content
                        .onKeyEvent { event ->
                            // KeyDown for enter is actually
                            // nativeKeyEvent=InternalKeyEvent(key=Key: Unknown keyCode: 0x0, type=Unknown)
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyUp &&
                                !event.isShiftPressed) {
                                messages = messages + Message(text = prompt, fromSelf = true)
                                prompt = ""
                                true
                            } else {
                                false
                            }
                        },
                    minLines = 5,   // always shows at least 5 lines tall
                    maxLines = 10,  // caps growth at 10 lines
                    onValueChange = { prompt = it }
                )
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
    val isMe = message.fromSelf
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isMe) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp,
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text,
                color = if (isMe) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
