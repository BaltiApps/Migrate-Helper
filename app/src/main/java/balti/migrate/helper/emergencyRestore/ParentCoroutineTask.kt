package balti.migrate.helper.emergencyRestore

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.helper.emergencyRestore.EmergencyRestoreService.Companion.emergencyServiceContext
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_EM_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_EM_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_LOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_SUBTASK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_TITLE
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
    val errorReader by lazy { BufferedReader(InputStreamReader(suShell.errorStream)) }

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

    fun getAllErrors(): ArrayList<String> {
        val errors = ArrayList<String>(0)
        Misc.iterateBufferedReader(errorReader, { line ->
            errors.add(line.trim())
            return@iterateBufferedReader false
        })
        return errors
    }

    fun sendProgress(title: String, subtask: String, log: String, progress: Int = -1){
        LocalBroadcastManager.getInstance(emergencyServiceContext).sendBroadcast(
                Intent(ACTION_EM_PROGRESS).apply {
                    putExtra(EXTRA_EM_TITLE, title)
                    putExtra(EXTRA_EM_SUBTASK, subtask)
                    putExtra(EXTRA_EM_LOG, log)
                    putExtra(EXTRA_EM_PROGRESS, progress)
                }
        )
    }

    fun sendErrors(logs: ArrayList<String>){
        LocalBroadcastManager.getInstance(emergencyServiceContext).sendBroadcast(
                Intent(ACTION_EM_ERRORS).apply {
                    putStringArrayListExtra(EXTRA_EM_ERRORS, logs)
                }
        )
    }
}