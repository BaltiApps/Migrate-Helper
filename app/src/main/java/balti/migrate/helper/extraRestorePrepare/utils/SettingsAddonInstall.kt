package balti.migrate.helper.extraRestorePrepare.utils

import android.content.Context
import balti.migrate.helper.utilities.CommonToolsKotlin
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SettingsAddonInstall(val context: Context) {
    fun start(): String {
        val commonTools = CommonToolsKotlin(context)
        val path = commonTools.unpackAssetToInternal("addonSettings.apk", "addonSettings.apk")
        val error = StringBuffer("")

        Runtime.getRuntime().exec("su").let {
            val writer = BufferedWriter(OutputStreamWriter(it.outputStream))
            val errorStream = BufferedReader(InputStreamReader(it.errorStream))

            writer.write("pm install $path\n")
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