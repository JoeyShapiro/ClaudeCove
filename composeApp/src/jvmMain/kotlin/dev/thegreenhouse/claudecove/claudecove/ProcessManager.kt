package dev.thegreenhouse.claudecove.claudecove

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.BufferedWriter

class ProcessManager(private val scope: CoroutineScope) {

    private lateinit var process: Process
    private lateinit var writer: BufferedWriter

    val stdout = MutableSharedFlow<String>()

    fun start(vararg command: String) {
        process = ProcessBuilder(*command)
            .redirectErrorStream(true) // merge stderr into stdout
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