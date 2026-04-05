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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import claudecove.composeapp.generated.resources.Res
import claudecove.composeapp.generated.resources.chat_add
import claudecove.composeapp.generated.resources.copy
import claudecove.composeapp.generated.resources.delete
import claudecove.composeapp.generated.resources.folder_new
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File
import kotlin.collections.plus
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert

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

    val settings = transaction {
        Settings.selectAll()
                .associate { it[Settings.name] to it[Settings.value] }
    }
    var config by remember { mutableStateOf(
        Configuration(
            settings["session"] ?: "",
            settings["exe"]?.let { File(it) }
        )
    ) }


    MaterialTheme {
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            var input by remember { mutableStateOf("") }
            var projects by remember { mutableStateOf(listOf<Project>()) }
            var sessions by remember { mutableStateOf(listOf<Session>()) }
            var currentSession by remember { mutableStateOf(config.session) }
            var messages by remember { mutableStateOf(listOf<Message>()) }
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()

            sessions = transaction {
                Sessions.selectAll()
                        .map { Session.from(it) }
            }
            projects = transaction {
                Projects.selectAll()
                        .map { Project.from(it) }
            }
            messages = transaction {
                Messages.selectAll()
                        .where { Messages.session eq currentSession }
                        .map { Message.from(it) }
            }

            // Collect stdout
            LaunchedEffect(Unit) {
                processManager.stdout.collect { line ->
                    try {
                        val event = Json.decodeFromString<Claude.Event>(line)

                        when (event) {
                            is Claude.ResponseControl -> {
                                transaction {
                                    Sessions.update({ Sessions.id eq currentSession }) {
                                        it[name] = event.response.response.title
                                    }
                                }
                                sessions = transaction {
                                    Sessions.selectAll()
                                            .map { Session.from(it) }
                                }
                            }
                            is Claude.ResponseResult -> {
                                val newMessage = Message(
                                    session = currentSession,
                                    text = event.result,
                                    fromSelf = false
                                )
                                messages = messages + newMessage
                                transaction {
                                    Messages.insert {
                                        it[id]        = newMessage.id
                                        it[session]   = newMessage.session
                                        it[text]      = newMessage.text
                                        it[fromSelf]  = newMessage.fromSelf
                                        it[timestamp] = newMessage.timestamp
                                    }
                                }
                            }
                        }
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
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val folder = openFilePicker(title = "Select Working Directory")
                                        if (folder != null) {
                                            val newProject = Project(
                                                name = folder.name,
                                                directory = folder
                                            )
                                            projects = projects + newProject
                                            transaction {
                                                Projects.insert {
                                                    it[id] = newProject.id
                                                    it[name] = newProject.name
                                                    it[directory] = newProject.directory.absolutePath
                                                }
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.folder_new),
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = 0.7f
                                    )
                                )
                            }
                            IconButton(
                                onClick = {
                                    // go to newly added session
                                    val newSession = Session(name = "New Session")
                                    sessions = sessions + newSession
                                    transaction {
                                        Sessions.insert {
                                            it[id] = newSession.id
                                            it[name]   = newSession.name
                                            it[project]  = newSession.project
                                            it[prompt] = newSession.prompt
                                        }
                                    }

                                    currentSession = newSession.id
                                    config.upSession(currentSession)
                                    messages = transaction {
                                        Messages.selectAll()
                                                .where { Messages.session eq currentSession }
                                                .map { Message.from(it) }
                                    }

                                    // start the new process
                                    processManager.directory = null
                                    processManager.restart()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.chat_add),
                                    contentDescription = "Chat",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
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
                                // cant just query the db
                                if (session.project == null) {
                                    SessionItem(
                                        name = session.name,
                                        selected = session.id == currentSession,
                                        onSessionClick = {
                                            // go to other session
                                            currentSession = session.id
                                            config.upSession(currentSession)
                                            messages = transaction {
                                                Messages.selectAll()
                                                        .where { Messages.session eq currentSession }
                                                        .map { Message.from(it) }
                                            }

                                            // start the new process
                                            processManager.directory = null
                                            processManager.restart()
                                        },
                                        onDeleteClick = {
                                            transaction {
                                                Sessions.deleteWhere { Sessions.id eq session.id }
                                                Messages.deleteWhere { Messages.session eq session.id }
                                                sessions = transaction {
                                                    Sessions.selectAll()
                                                            .map { Session.from(it) }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            items(projects) { it ->
                                ProjectSidebarItem(
                                    project = it,
                                    selected = sessions.any {
                                        session -> session.id == currentSession &&
                                            session.project == it.id
                                                            },
                                    onCreateSession = {
                                        // go to newly added session
                                        val newSession = Session(name = "New Session", project = it.id)
                                        sessions = sessions + newSession
                                        transaction {
                                            Sessions.insert {
                                                it[id] = newSession.id
                                                it[name]   = newSession.name
                                                it[project] = newSession.project
                                                it[prompt] = newSession.prompt
                                            }
                                        }

                                        currentSession = newSession.id
                                        config.upSession(currentSession)
                                        messages = transaction {
                                            Messages.selectAll()
                                                    .where { Messages.session eq currentSession }
                                                    .map { Message.from(it) }
                                        }

                                        // start the new process
                                        processManager.directory = it.directory
                                        processManager.restart()
                                    }
                                ) {
                                    sessions.forEach { session ->
                                        if (session.project == it.id) {
                                            SessionItem(
                                                name = session.name,
                                                selected = session.id == currentSession,
                                                onSessionClick = {
                                                    // go to other session
                                                    currentSession = session.id
                                                    config.upSession(currentSession)
                                                    messages = transaction {
                                                        Messages.selectAll()
                                                                .where { Messages.session eq currentSession }
                                                                .map { Message.from(it) }
                                                    }

                                                    // start the new process
                                                    processManager.directory = it.directory
                                                    processManager.restart()
                                                },
                                                onDeleteClick = {
                                                    transaction {
                                                        Sessions.deleteWhere { Sessions.id eq session.id }
                                                        Messages.deleteWhere { Messages.session eq session.id }
                                                        sessions = transaction {
                                                            Sessions.selectAll()
                                                                    .map { Session.from(it) }
                                                        }
                                                    }
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
                                            // generate a title if this is a new session
                                            if (messages.isEmpty()) {
                                                val title = Claude.ControlRequest.title(input)
                                                val data = json.encodeToString(title)
                                                processManager.sendLine(data)
                                            }

                                            val data = json.encodeToString(prompt)

                                            processManager.sendLine(data)
                                            val newMessage = Message(
                                                session = currentSession,
                                                text = input,
                                                fromSelf = true
                                            )
                                            messages = messages + newMessage
                                            transaction {
                                                Messages.insert {
                                                    it[id]        = newMessage.id
                                                    it[session]   = newMessage.session
                                                    it[text]      = newMessage.text
                                                    it[fromSelf]  = newMessage.fromSelf
                                                    it[timestamp] = newMessage.timestamp
                                                }
                                            }

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
                IconButton(
                    onClick = {
                        onCreateSession(project)
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.chat_add),
                        contentDescription = "Chat",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
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

// maybe try out DAO. but DSL seems good enough.
// DAO just seems to do the map for me,
// and im not sure if i can trust it with joins.
// it seems meant for "lifecycle/persistence behavior"
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val directory: File,
) {
    companion object {
        fun from(row: ResultRow): Project {
            return Project(
                id = row[Projects.id],
                name = row[Projects.name],
                directory = File(row[Projects.directory])
            )
        }
    }
}

object Projects : Table("projects") {
    val id        = varchar("id", 36)
    val name      = varchar("name", 256)
    val directory = varchar("directory", 1024) // store the path as a string

    override val primaryKey = PrimaryKey(id)
}

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val project: String? = null, // it is better to have the parent. then its 1:many. which is easier to db-ize
    val prompt: String = "",
) {
    companion object {
        fun from(row: ResultRow): Session {
            return Session(
                id = row[Sessions.id],
                name = row[Sessions.name],
                project = row[Sessions.project],
                prompt = row[Sessions.prompt],
            )
        }
    }
}

object Sessions : Table("sessions") {
    val id        = varchar("id", 36)
    val name      = varchar("name", 256)
    val project   = varchar("project", 36).nullable()
    val prompt    = varchar("prompt", 1024)

    override val primaryKey = PrimaryKey(Projects.id)
}

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val session: String,
    val text: String,
    val fromSelf: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun from(row: ResultRow): Message {
            return Message(
                id = row[Messages.id],
                session = row[Messages.session],
                text = row[Messages.text],
                fromSelf = row[Messages.fromSelf],
                timestamp = row[Messages.timestamp]
            )
        }
    }
}

object Messages : Table("messages") {
    val id        = varchar("id", 36)
    val session   = varchar("session", 36)
    val text      = text("text")
    val fromSelf  = bool("from_self")
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(Projects.id)
}

class Configuration(session: String, exe: File? = null) {
    var session: String = ""
    var exe: File? = null

    fun upSession(value: String) {
        transaction {
            Settings.upsert {
                it[Settings.name] = "session"
                it[Settings.value] = value
            }
        }
    }
}

object Settings : Table("settings") {
    val name      = varchar("name", 128)
    val value     = varchar("value", 1024)

    override val primaryKey = PrimaryKey(name)
}

@Composable
fun SessionItem(
    name: String,
    selected: Boolean,
    onSessionClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Card(
        modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
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
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { onDeleteClick() },
                modifier = Modifier.alpha(if (isHovered) 1f else 0f)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.delete),
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
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

                    IconButton(
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
                        Icon(
                            painter = painterResource(Res.drawable.copy),
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(
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
