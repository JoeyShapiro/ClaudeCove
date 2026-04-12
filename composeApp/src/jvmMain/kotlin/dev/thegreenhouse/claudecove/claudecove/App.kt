package dev.thegreenhouse.claudecove.claudecove

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.window.Dialog
import claudecove.composeapp.generated.resources.Res
import claudecove.composeapp.generated.resources.chat_add
import claudecove.composeapp.generated.resources.copy
import claudecove.composeapp.generated.resources.delete
import claudecove.composeapp.generated.resources.folder_new
import claudecove.composeapp.generated.resources.gear
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun App(processManager: ProcessManager) {
    val json = Json {
        namingStrategy = SerialOrSnakeCase
        ignoreUnknownKeys = true   // don't crash if JSON has extra fields
        isLenient = true           // allows unquoted keys, trailing commas
        encodeDefaults = false     // omit empty
    }

    val settings = transaction {
        Settings.selectAll()
                .associate { it[Settings.name] to it[Settings.value] }
    }
    var config by remember { mutableStateOf(
        Configuration(
            settings["session"] ?: "",
            settings["exe"]?.let { File(it) },
            settings["theme"]
        )
    ) }

    // long process to set the current directory
    transaction {
        val session = Sessions.selectAll()
                .where { Sessions.id eq config.session }
                .map { Session.from(it) }
                .firstOrNull()
        session?.let { session ->
            session.project?.let { projectId ->
                Projects.selectAll()
                        .where { Projects.id eq projectId }
                        .map { Project.from(it) }
                        .firstOrNull()?.let { project ->
                            processManager.directory = project.directory
                            processManager.restart()
                        }
            }
        }
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (config.theme) {
        "dark"  -> true
        "light" -> false
        else    -> systemDark
    }
    val claudeLight = lightColorScheme(
        background            = Color(0xFFFAF9F5),
        surface               = Color(0xFFFFFFFF),
        surfaceVariant        = Color(0xFFEFEBE4),
        primaryContainer      = Color(0xFFF5F3EE),
        secondaryContainer    = Color(0xFFE8E4DB),
        onSecondaryContainer  = Color(0xFF2D2A26),
        primary               = Color(0xFFCC7B5A),
        onPrimary             = Color(0xFFFFFFFF),
        inversePrimary        = Color(0xFFE8A98E),
        tertiaryContainer     = Color(0xFFE4D9CF),
        onTertiaryContainer   = Color(0xFF3D2E25),
        onSurface             = Color(0xFF2D2A26),
        onSurfaceVariant      = Color(0xFF5C5852),
        onBackground          = Color(0xFF2D2A26),
    )
    val claudeDark = darkColorScheme(
        background            = Color(0xFF1A1714),
        surface               = Color(0xFF211E1A),
        surfaceVariant        = Color(0xFF2A2520),
        primaryContainer      = Color(0xFF1A1714),
        secondaryContainer    = Color(0xFF252018),
        onSecondaryContainer  = Color(0xFFE8E4DC),
        primary               = Color(0xFFCC7B5A),
        onPrimary             = Color(0xFFFFFFFF),
        inversePrimary        = Color(0xFF8C4B2E),
        tertiaryContainer     = Color(0xFF3D2E25),
        onTertiaryContainer   = Color(0xFFE8DDD4),
        onSurface             = Color(0xFFE8E4DC),
        onSurfaceVariant      = Color(0xFFADA9A3),
        onBackground          = Color(0xFFE8E4DC),
    )
    val colorScheme = if (isDark) claudeDark else claudeLight

    MaterialTheme(colorScheme = colorScheme) {
        val selectionColors = TextSelectionColors(
            handleColor = MaterialTheme.colorScheme.inversePrimary,
            backgroundColor = MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.4f)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            var input by remember { mutableStateOf("") }
            var projects by remember { mutableStateOf(listOf<Project>()) }
            var sessions by remember { mutableStateOf(listOf<Session>()) }
            var currentSession by remember { mutableStateOf(config.session) }
            var messages by remember { mutableStateOf(listOf<Message>()) }
            var thinking by remember { mutableStateOf(false) }
            var askingPermission by remember { mutableStateOf(false) }
            var askingRequest: Claude.RequestTool? by remember { mutableStateOf(null) }
            var askingRequestId by remember { mutableStateOf("") }
            var showSettings by remember { mutableStateOf(false) }
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
                            is Claude.ResponseControl<*> -> {
                                when (event.response.response) {
                                    is Claude.ControlResponseResponseResponse -> {
                                        // set the title if its populated
                                        event.response.response.title?.let { title ->
                                            transaction {
                                                Sessions.update({ Sessions.id eq currentSession }) {
                                                    it[name] = title
                                                }
                                            }
                                            sessions = transaction {
                                                Sessions.selectAll()
                                                        .map { Session.from(it) }
                                            }
                                        }
                                    }
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
                                thinking = false
                            }
                            is Claude.RequestControl<*> -> {
                                when (val request = event.request) {
                                    is Claude.RequestHookCallback -> {
                                        // auto-continue hooks
                                        val ok = Claude.ResponseControl.newContinue(event.requestId)
                                        processManager.sendLine(json.encodeToString(ok))
                                    }
                                    is Claude.RequestTool -> {
                                        // show permission prompt, then respond
                                        askingPermission = true
                                        askingRequest = request
                                        askingRequestId = event.requestId
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
                                    .weight(1f)
                                    .fillMaxWidth()
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

                        Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    painter = painterResource(Res.drawable.gear),
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                if (showSettings) {
                    SettingsDialog(
                        config = config,
                        onDismiss = { showSettings = false },
                        onExeChange = { file ->
                            config.upExe(file)
                            config = Configuration(config.session, file, config.theme)
                        },
                        onThemeChange = { theme ->
                            config.upTheme(theme)
                            config = Configuration(config.session, config.exe, theme)
                        },
                        scope = scope
                    )
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
                            ChatBubble(message, isDark)
                        }
                    }

                    ThinkingFlavorText(thinking)

                    if (askingPermission) {
                        PermissionPrompt(
                            onResponse = { answer ->
                                askingRequest?.let {
                                    val response = when (answer) {
                                        "yes" -> Claude.ResponseControl.newAccept(askingRequestId, it)
                                        "no" -> Claude.ResponseControl.newDecline(askingRequestId, it)
                                        "all" -> Claude.ResponseControl.newAcceptAll(askingRequestId, it)
                                        else -> throw Exception()
                                    }
                                    processManager.sendLine(json.encodeToString(response))

                                    askingRequestId = ""
                                    askingRequest = null
                                    askingPermission = false
                                }
                            }
                        )
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
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor       = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor     = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor             = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp)
                                    .onKeyEvent { event ->
                                        if (event.key == Key.Enter && event.type == KeyEventType.KeyUp &&
                                            !event.isShiftPressed
                                        ) {
                                            if (currentSession.isEmpty()) {
                                                val newSession = Session(name = "New Session")
                                                sessions = sessions + newSession
                                                transaction {
                                                    Sessions.insert {
                                                        it[id] = newSession.id
                                                        it[name] = newSession.name
                                                        it[project] = newSession.project
                                                        it[prompt] = newSession.prompt
                                                    }
                                                }
                                                currentSession = newSession.id
                                                config.upSession(currentSession)
                                                processManager.directory = null
                                                processManager.restart()
                                            }

                                            val prompt = Claude.Prompt.new(input)
                                            // generate a title if this is a new session
                                            if (messages.isEmpty()) {
                                                val title = Claude.ControlRequest.title(input)
                                                val data = json.encodeToString(title)
                                                processManager.sendLine(data)
                                            }

                                            val data = json.encodeToString(prompt)

                                            processManager.sendLine(data)
                                            thinking = true
                                            val newMessage = Message(
                                                session = currentSession,
                                                text = input,
                                                fromSelf = true
                                            )
                                            messages = messages + newMessage
                                            transaction {
                                                // TODO `newMessage to it`? or it = Message.to(newMessage)
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
fun PermissionPrompt(
    onResponse: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Claude is requesting permission to perform an action.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { onResponse("yes") }) {
                    Text("Yes")
                }
                OutlinedButton(onClick = { onResponse("no") }) {
                    Text("No")
                }
                Button(
                    onClick = { onResponse("all") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Yes to All")
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

class Configuration(session: String, exe: File? = null, theme: String? = null) {
    var session: String = session
    var exe: File? = exe
    var theme: String = theme ?: ""

    fun upSession(value: String) {
        transaction {
            Settings.upsert {
                it[Settings.name] = "session"
                it[Settings.value] = value
            }
        }
    }

    fun upExe(file: File?) {
        transaction {
            if (file == null) {
                Settings.deleteWhere { Settings.name eq "exe" }
            } else {
                Settings.upsert {
                    it[Settings.name] = "exe"
                    it[Settings.value] = file.absolutePath
                }
            }
        }
    }

    fun upTheme(value: String) {
        transaction {
            Settings.upsert {
                it[Settings.name] = "theme"
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
fun SettingsDialog(
    config: Configuration,
    onDismiss: () -> Unit,
    onExeChange: (File?) -> Unit,
    onThemeChange: (String) -> Unit,
    scope: CoroutineScope,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).widthIn(min = 320.dp, max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                // Theme
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                            val selected = config.theme == value
                            Card(
                                modifier = Modifier.clickable { onThemeChange(value) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                // Claude executable
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Claude Executable",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = config.exe?.absolutePath ?: "Not set — using PATH",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (config.exe != null) 1f else 0.45f),
                            modifier = Modifier.weight(1f)
                        )
                        Card(
                            modifier = Modifier.clickable {
                                scope.launch {
                                    val file = openFilePicker(title = "Select Claude Executable")
                                    if (file != null) onExeChange(file)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = "Browse",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                        if (config.exe != null) {
                            Card(
                                modifier = Modifier.clickable { onExeChange(null) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Card(
                        modifier = Modifier.clickable { onDismiss() },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message, isDarkMode: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val highlightsBuilder = remember(isDarkMode) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkMode))
    }

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
                    SelectionContainer {
                        Markdown(
                            content = message.text,
                            colors = markdownColor(
                                text = MaterialTheme.colorScheme.onPrimary
                            ),
                            components = markdownComponents(
                                codeBlock = {
                                    MarkdownHighlightedCodeBlock(
                                        content = it.content,
                                        node = it.node,
                                        highlightsBuilder = highlightsBuilder,
                                        showHeader = true,
                                    )
                                },
                                codeFence = {
                                    MarkdownHighlightedCodeFence(
                                        content = it.content,
                                        node = it.node,
                                        highlightsBuilder = highlightsBuilder,
                                        showHeader = true,
                                    )
                                },
                            )
                        )
                    }

                    Row {
                        Text(
                            text = formatRelativeTime(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(
                                    AnnotatedString(
                                        message.text
                                    )
                                )
                            },
                            modifier = Modifier
                                    .size(24.dp)
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
}

fun formatRelativeTime(timestamp: Long): String {
    // TODO use kotlin instead
    val t = Instant.ofEpochMilli(timestamp)
    val now = Instant.now()
    val duration = Duration.between(t, now)
    val seconds = duration.seconds

    return when {
        seconds < 5       -> "Just now"
        seconds < 60      -> "${seconds}s ago"
        seconds < 3_600   -> "${duration.toMinutes()}m ago"
        seconds < 86_400  -> "${duration.toHours()}h ago"
        else -> {
            val date = t.atZone(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now()
            when (val days = ChronoUnit.DAYS.between(date, today)) {
                1L   -> "Yesterday"
                in 2..6 -> "${days}d ago"
                else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        }
    }
}

@Composable
fun ThinkingFlavorText(isThinking: Boolean) {
    // i could only use a thinking string and make it empty when not thinking
    // but that would add extra work and confusion.
    // i dont need to be that efficient or have others setting it
    var flavorText by remember { mutableStateOf("") }

    LaunchedEffect(isThinking) {
        if (isThinking) {
            while (true) {
                flavorText = addFlavor()
                delay(10000L)
            }
        }
    }

    if (isThinking) {
        Text(
            text = "$flavorText...", // $agent is
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

fun addFlavor(): String {
    val flavorTexts = listOf(
        "Tunneling deeper",
        "Breaking through the rock face",
        "Digging through the seam",
        "Clearing the rubble",
        "Descending the shaft",
        "Surveying the vein",
        "Panning for an answer",
        "Reading the strata",
        "Mapping the cavern",
        "Tracing the ore",
        "Following the echo",
        "Letting dust settle",
        "Sensing the dark",
        "Shoring up the tunnel",
        "Checking the timbers",
        "Calibrating the lantern",
        "Loading the cart",
        "Finding the vein",
        "Light in the rock",
        "Patience of stone",
        "Sifting through the dark",
        "The mountain thinks",
        "Something stirs below",
        "The dark has depth",
        "The cave remembers",
        "It was here before the stone",
        "Older than the mountain",
        "The tunnel bends the wrong way",
        "Counting the walls",
        "The shaft goes further than it should",
        "More rooms than there ought to be",
        "The map doesn't match",
        "Something is considering you",
        "The rock has opinions",
        "Waiting to be understood",
        "It knows you're listening",
        "The stone holds its answer",
        "Dreaming in geological time",
        "Sedimented for millennia",
        "Older words forming",
        "The deep has been patient"
    )

    val index = flavorTexts.indices.random()
    return flavorTexts[index]
}

suspend fun openFilePicker(
    title: String = "Select a file",
): File? = withContext(Dispatchers.IO) {
    System.setProperty("apple.awt.fileDialogForDirectories", "true")
    val dialog = java.awt.FileDialog(null as java.awt.Frame?, title, java.awt.FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory
    val file = dialog.file
    System.setProperty("apple.awt.fileDialogForDirectories", "false")
    if (dir != null && file != null) File(dir, file) else null
}
