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

    // TODO ping when done
    // TODO add buddy
    // TODO use streaming
    // TODO support plugins
    // TODO stat folder
    // TODO open here
    // TODO custom icon
    // TODO dont allow switching if thinking
    fun start(vararg command: String): Boolean {
        val cwd = this.directory ?: File(System.getProperty("java.io.tmpdir"))
        if (!cwd.exists()) return false

        this.command = command
        process = ProcessBuilder(*command)
                .directory(cwd)
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
        return true
    }

    fun sendLine(input: String) {
        writer.write(input)
        writer.newLine()
        writer.flush()
    }

    fun stop() {
        process.destroy()
    }

    fun restart(): Boolean {
        this.stop()
        return this.start(*this.command)
    }

    fun resume(session: String): Boolean {
        this.stop()
        return this.start(*this.command, "--resume", session)
    }
}