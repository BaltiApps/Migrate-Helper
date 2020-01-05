package balti.migrate.helper.extraRestorePrepare.utils

import android.content.Context
import balti.migrate.helper.utilities.CommonToolsKotlin
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

abstract class AddonInstall(open val context: Context, private val apkAsset: String) {

    private val customCommands by lazy { ArrayList<String>(0) }

    protected fun setCustomCommands(commands: ArrayList<String>){
        customCommands.clear()
        customCommands.addAll(commands)
    }

    protected fun start(): String {
        val commonTools = CommonToolsKotlin(context)
        val path = commonTools.unpackAssetToInternal(apkAsset, apkAsset)
        val error = StringBuffer("")

        Runtime.getRuntime().exec("su").let {
            val writer = BufferedWriter(OutputStreamWriter(it.outputStream))
            val errorStream = BufferedReader(InputStreamReader(it.errorStream))

            writer.write("pm install $path\n")
            customCommands.forEach { cmd -> writer.write("$cmd\n") }
            writer.write("exit\n")
            writer.flush()

            while (true) {
                val line: String? = errorStream.readLine()
                if (line == null) break
                else {
                    error.append("$line\n")
                }
            }
        }

        return error.toString().trim()
    }
}