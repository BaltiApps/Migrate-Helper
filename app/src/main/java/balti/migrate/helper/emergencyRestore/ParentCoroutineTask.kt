package balti.migrate.helper.emergencyRestore

import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

abstract class ParentCoroutineTask(): AsyncCoroutineTask() {

    abstract val suShell: Process
    val writer by lazy { BufferedWriter(OutputStreamWriter(suShell.outputStream)) }
    val reader by lazy { BufferedReader(InputStreamReader(suShell.inputStream)) }

    val END_MARKER = "----END----"

    fun writeNext(str: String){
        writer.write("$str\n")
    }

    fun flushShell(){
        writeNext("echo \"$END_MARKER\"")
        writer.flush()
    }

    fun closeShell(){
        writeNext("exit\n")
        writer.flush()
    }

    fun getOutput(): ArrayList<String> {
        val output = ArrayList<String>(0)
        Misc.iterateBufferedReader(reader, { line ->
            if (line.trim() != END_MARKER) output.add(line.trim())
            return@iterateBufferedReader line.trim() == END_MARKER
        })
        return output
    }
}