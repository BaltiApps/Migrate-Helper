package balti.migrate.helper.restoreSelectorActivity.utils

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.WIFI_FILE_NAME
import balti.migrate.helper.utilities.ViewOperations
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class RootCopyTask(private val jobCode: Int, private val tempDir: String,
                   private val context: Context): AsyncTask<Any, Any, Any>() {

    private val onReadComplete by lazy { context as OnReadComplete }
    private val commonTools by lazy { CommonToolsKotlin(context) }
    private val vOp by lazy { ViewOperations(context) }
    private val errors by lazy { ArrayList<String>(0) }
    private var scriptOutput = ""
    private var suProcess: Process? = null

    private var SU_TASK_PID = -999

    private var masterSuProcess: Process? = null

    private lateinit var busyboxBinaryPath: String

    override fun onPreExecute() {
        super.onPreExecute()

        Build.SUPPORTED_ABIS[0].run {
            busyboxBinaryPath = if (this == "armeabi-v7a" || this == "arm64-v8a")
                commonTools.unpackAssetToInternal("busybox", "busybox")
            else if (this == "x86" || this == "x86_64")
                commonTools.unpackAssetToInternal("busybox-x86", "busybox");
            else ""
        }
    }

    override fun doInBackground(vararg params: Any?): Any {

        try {
            if (busyboxBinaryPath == "") {
                errors.add(vOp.getStringFromRes(R.string.no_busybox))
                return 1
            }

            val scripFile = commonTools.unpackAssetToInternal("suScript.sh", "suScript.sh")

            suProcess = Runtime.getRuntime().exec("su")
            suProcess?.let {

                masterSuProcess = it

                BufferedWriter(OutputStreamWriter(it.outputStream)).run {

                    write("chmod +x $scripFile\n")
                    write("sh $scripFile ${context.packageName} $METADATA_HOLDER_DIR $MIGRATE_CACHE $BACKUP_NAME_SETTINGS $WIFI_FILE_NAME\n")
                    write("exit\n")
                    flush()

                }

                val outputReader = BufferedReader(InputStreamReader(it.inputStream))
                var line: String?
                while (true) {

                    if (RestoreSelectorKotlin.cancelLoading)
                    {
                        cancelTask()
                        return 0
                    }

                    line = outputReader.readLine()
                    if (line == null) break
                    else {
                        line = line.trim()

                        if (line.startsWith("--- PID:"))
                            commonTools.tryIt {
                                line?.run {
                                    SU_TASK_PID = substring(lastIndexOf(" ") + 1).toInt()
                                }
                            }
                        else {
                            scriptOutput += "$line\n"
                        }

                        if (line == "--- END_OF_COPY ---") break
                    }
                }
                scriptOutput = scriptOutput.trim()

                val errorReader = BufferedReader(InputStreamReader(it.errorStream))
                line = null
                do {
                    errorReader.readLine().run {
                        this?.trim()?.let { s -> if (s != "") errors.add(s) }
                        line = this
                    }
                } while (line != null)
            }

            return 0
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add(e.message.toString())

            return 1
        }
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        vOp.doSomething {
            if (errors.size == 0)
                onReadComplete.onComplete(jobCode, true, scriptOutput)
            else onReadComplete.onComplete(jobCode, false, errors)
        }
    }

    fun cancelTask(){
        suProcess?.let { commonTools.cancelTask(it, SU_TASK_PID) }
    }
}