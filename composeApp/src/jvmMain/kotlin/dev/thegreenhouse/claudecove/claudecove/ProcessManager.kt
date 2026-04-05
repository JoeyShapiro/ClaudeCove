package dev.thegreenhouse.claudecove.claudecove

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File

class ProcessManager(private val scope: CoroutineScope) {

    private lateinit var process: Process
    private lateinit var writer: BufferedWriter
    private lateinit var command: Array<out String>
    var directory: File? = null

    val stdout = MutableSharedFlow<String>()

    // TODO `newMessage to it`? or it = Message.to(newMessage)
    // TODO figure out bundling
    // TODO fix folder dialog and use a different one
    // TODO create session when starting a blank chat at start. or just go to last one
    // TODO use session id created from claude
    // TODO use streaming
    // TODO handle tool requests
    fun start(vararg command: String) {
        this.command = command
        process = ProcessBuilder(*command)
                .directory(directory ?: File(System.getProperty("java.io.tmpdir")))
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()

        writer = process.outputStream.bufferedWriter()

        // Launch stdout reader
        scope.launch(Dispatchers.IO) {
            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    scope.launch { stdout.emit(line) }
                }
            }
        }
    }

    fun sendLine(input: String) {
        writer.write(input)
        writer.newLine()
        writer.flush()
    }

    fun stop() {
        process.destroy()
    }

    fun restart() {
        this.stop()
        this.start(*this.command)
    }
}