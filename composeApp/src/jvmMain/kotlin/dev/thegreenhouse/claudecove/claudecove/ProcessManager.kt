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

    val stdout = MutableSharedFlow<String>()

    // TODO change cwd
    // TODO add flavor
    // TODO use streaming
    fun start(vararg command: String) {
        process = ProcessBuilder(*command)
                .directory(File("/Users/oniichan/Documents/Code/Calamari"))
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
}