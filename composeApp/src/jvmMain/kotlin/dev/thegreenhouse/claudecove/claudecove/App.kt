package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File
import kotlin.collections.plus

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
            var projects by remember { mutableStateOf(listOf<Project>()) }
            var messages by remember { mutableStateOf(listOf<Message>()) }
            var sessions by remember { mutableStateOf(listOf<Session>()) }
            var currentSession by remember { mutableStateOf("") }
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()

            // Collect stdout
            LaunchedEffect(Unit) {
                processManager.stdout.collect { line ->
                    try {
                        val response = json.decodeFromString<Claude.Response>(line)
                        messages = messages + Message(text = response.result, fromSelf = false)
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

            // Sidebar + main content row
            Row(
                modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .safeContentPadding()
                        .fillMaxSize()
            ) {
                Card(
                    modifier = Modifier
                            .widthIn(min = 220.dp, max = 280.dp)
                            .fillMaxHeight()
                            .padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Conversations",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                        .weight(1f).fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 12.dp, vertical = 14.dp)
                            )
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        val folder = openFilePicker(title = "Select Working Directory")
                                        if (folder != null) {
                                            projects = projects + Project(
                                                id = UUID.randomUUID().toString(),
                                                name = folder.name,
                                                directory = folder
                                            )
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    "Add",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = 0.7f
                                    )
                                )
                            }
                            TextButton(
                                onClick = {
                                    // save current messages
                                    sessions = sessions.map { session ->
                                        if (session.id == currentSession) {
                                            session.copy(messages = messages)  // new object via copy()
                                        } else {
                                            session
                                        }
                                    }

                                    // go to newly added session
                                    val session = Session(name = "New Session")
                                    sessions = sessions + session
                                    currentSession = session.id
                                    messages = session.messages

                                    // start the new process
                                    processManager.directory = null
                                    processManager.restart()
                                }
                            ) {
                                Text(
                                    "Chat",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = 0.7f
                                    )
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))

                        LazyColumn(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sessions) { session ->
                                if (session.project == null) {
                                    SessionItem(
                                        name = session.name,
                                        selected = session.id == currentSession,
                                        onSessionClick = {
                                            // save current state
                                            sessions = sessions.map { mapSession ->
                                                if (mapSession.id == currentSession) {
                                                    mapSession.copy(messages = messages)  // new object via copy()
                                                } else {
                                                    mapSession
                                                }
                                            }

                                            // go to other session
                                            currentSession = session.id
                                            messages = session.messages

                                            // start the new process
                                            processManager.directory = null
                                            processManager.restart()
                                        }
                                    )
                                }
                            }
                            items(projects) { project ->
                                ProjectSidebarItem(
                                    project = project,
                                    selected = sessions.any {
                                        session -> session.id == currentSession &&
                                            session.project == project.id
                                                            },
                                    onCreateSession = {
                                        // save current messages
                                        sessions = sessions.map { mapSession ->
                                            if (mapSession.id == currentSession) {
                                                mapSession.copy(messages = messages)  // new object via copy()
                                            } else {
                                                mapSession
                                            }
                                        }

                                        // go to newly added session
                                        val newSession = Session(name = "New Session", project = project.id)
                                        sessions = sessions + newSession
                                        currentSession = newSession.id
                                        messages = newSession.messages

                                        // start the new process
                                        processManager.directory = project.directory
                                        processManager.restart()
                                    }
                                ) {
                                    sessions.forEach { session ->
                                        if (session.project == project.id) {
                                            SessionItem(
                                                name = session.name,
                                                selected = session.id == currentSession,
                                                onSessionClick = {
                                                    // save current state
                                                    sessions = sessions.map { mapSession ->
                                                        if (mapSession.id == currentSession) {
                                                            mapSession.copy(messages = messages)  // new object via copy()
                                                        } else {
                                                            mapSession
                                                        }
                                                    }

                                                    // go to other session
                                                    currentSession = session.id
                                                    messages = session.messages

                                                    // start the new process
                                                    processManager.directory = project.directory
                                                    processManager.restart()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = false
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
                                    .heightIn(min = 150.dp)
                                    .onKeyEvent { event ->
                                        if (event.key == Key.Enter && event.type == KeyEventType.KeyUp &&
                                            !event.isShiftPressed
                                        ) {
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
                            minLines = 5,
                            maxLines = 10,
                            onValueChange = { input = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectSidebarItem(
    project: Project,
    selected: Boolean,
    onCreateSession: (Project) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 0f else -90f)

    Column {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) MaterialTheme.colorScheme.tertiaryContainer
                                 else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer
                               else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "▾",
                    fontSize = 18.sp,
                    modifier = Modifier
                            .rotate(chevronRotation)
                            .alpha(0.6f)
                )
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        onCreateSession(project)
                    }
                ) {
                    Text(
                        "Chat",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                            alpha = 0.7f
                        )
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

data class Project(
    val id: String,
    val name: String,
    val directory: File,
)

// TODO serialize
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val project: String? = null, // it is better to have the parent. then its 1:many. which is easier to db-ize
    val prompt: String = "",
    val messages: List<Message> = listOf(),
)

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromSelf: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun SessionItem(
    name: String,
    selected: Boolean,
    onSessionClick: () -> Unit,
) {
    Card(
        modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSessionClick()
                },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50)
                        )
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

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

suspend fun openFilePicker(
    title: String = "Select a file",
    filter: FileNameExtensionFilter? = null
): File? = withContext(Dispatchers.IO) {
    var result: File? = null
    val chooser = JFileChooser().apply {
        dialogTitle = title
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
        filter?.let { fileFilter = it }
    }

    val returnCode = chooser.showOpenDialog(null)
    if (returnCode == JFileChooser.APPROVE_OPTION) {
        result = chooser.selectedFile
    }

    result
}
