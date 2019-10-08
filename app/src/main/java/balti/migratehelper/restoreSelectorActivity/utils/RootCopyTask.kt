package balti.migratehelper.restoreSelectorActivity.utils

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.OnReadComplete
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.WIFI_FILE_NAME
import balti.migratehelper.utilities.ViewOperations
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

            Runtime.getRuntime().exec("su").let {

                BufferedWriter(OutputStreamWriter(it.outputStream)).run {

                    CommonToolsKotlin.METADATA_HOLDER_DIR.let { mtd ->

                        write("#!sbin/sh\n\n")

                        write("echo \" \"\n")
                        write("pm grant ${context.packageName} android.permission.PACKAGE_USAGE_STATS\n")
                        write("pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS\n")

                        write("cp -f $tempDir/*.json $mtd 2>/dev/null\n")
                        write("cp -f $tempDir/*.vcf $mtd 2>/dev/null\n")
                        write("cp -f $tempDir/*.sms.db $mtd 2>/dev/null\n")
                        write("cp -f $tempDir/*.calls.db $mtd 2>/dev/null\n")
                        write("cp -f $tempDir/*.perm $mtd 2>/dev/null\n")
                        write("cp -f $tempDir/$BACKUP_NAME_SETTINGS $mtd 2>/dev/null\n")
                        write("cp -f $tempDir/$WIFI_FILE_NAME $mtd 2>/dev/null\n")
                        write("echo --- END_OF_COPY ---\n")
                        write("exit\n")

                        flush()
                    }
                }

                val outputReader = BufferedReader(InputStreamReader(it.inputStream))
                var line: String?
                while (true) {
                    line = outputReader.readLine()
                    if (line == null) break
                    else {
                        line = line.trim()
                        scriptOutput += "$line\n"
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
}