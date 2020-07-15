package balti.migrate.helper.restoreEngines.engines

import balti.migrate.helper.R
import balti.migrate.helper.restoreEngines.ParentRestoreClass
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_ADB
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_DPI
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_FONT_SCALE
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_KEYBOARD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_GENERIC_SETTINGS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_SETTINGS_SCRIPT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRAS_MARKER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_RESTORE_SETTINGS_SCRIPT
import balti.module.baltitoolbox.functions.FileHandlers.getInternalFile
import balti.module.baltitoolbox.functions.Misc.tryIt
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class SettingsRestoreEngine(private val jobcode: Int,
                            private val settingsPacket: SettingsPacketKotlin): ParentRestoreClass("") {

    private val suProcess by lazy { Runtime.getRuntime().exec("su") }
    private val scriptFile by lazy { getInternalFile(FILE_RESTORE_SETTINGS_SCRIPT) }
    private var PID = -999

    private val allErrors by lazy { ArrayList<String>(0) }

    private fun restorePacket(){
        settingsPacket.let {
            var dpiValue: Int = -1
            var adbValue: Int = -1
            var keyboardText = ""
            var fontScale: Double = -1.0

            for (ip in it.internalPackets){
                if (ip.isSelected)
                    when (ip.settingsType) {
                        SETTINGS_TYPE_DPI -> dpiValue = ip.value as Int
                        SETTINGS_TYPE_ADB -> adbValue = ip.value as Int
                        SETTINGS_TYPE_KEYBOARD -> keyboardText = ip.value as String
                        SETTINGS_TYPE_FONT_SCALE -> fontScale = ip.value as Double
                    }
            }

            if (!RestoreServiceKotlin.cancelAll) BufferedWriter(FileWriter(scriptFile)).run {

                fun writeNext(line: String) {
                    write("${line}\n")
                }

                writeNext("#!sbin/sh\n")
                writeNext("echo \" \"")
                writeNext("sleep 1s")
                writeNext("echo \"--- RESTORE PID: $$\"")
                writeNext("echo \" \"")

                if (dpiValue > 0) writeNext("wm density $dpiValue")
                if (adbValue >= 0) writeNext("settings put global adb_enabled $adbValue")
                if (keyboardText != "") {
                    writeNext("ime enable $keyboardText")
                    writeNext("ime set $keyboardText")
                }
                if (fontScale > 0) writeNext("settings put system font_scale $fontScale")

                writeNext("echo \" \"")
                writeNext("echo \"--- DONE! ---\"")

                close()
            }

            scriptFile.setExecutable(true)

            if (!RestoreServiceKotlin.cancelAll) suProcess.run {
                val writer = outputStream.bufferedWriter()
                val reader = inputStream.bufferedReader()
                val errorReader = errorStream.bufferedReader()

                writer.write("sh ${scriptFile.absolutePath}\n")
                writer.write("exit\n")
                writer.flush()

                while (true) {
                    if (RestoreServiceKotlin.cancelAll) commonTools.cancelTask(suProcess, PID)
                    val line: String? = reader.readLine()
                    if (line == null || line == "--- DONE! ---") {
                        break
                    }
                    else if (line.startsWith("--- RESTORE PID:")) {
                        tryIt {
                            PID = line.substring(line.lastIndexOf(" ") + 1).trim().toInt()
                        }
                    }
                    else broadcastProgress("", line, false)
                }

                while (true) {
                    val line: String? = errorReader.readLine()
                    if (line == null) break
                    else {
                        allErrors.add("$ERROR_SETTINGS_SCRIPT: $line")
                    }
                }

            }
        }
    }

    override fun postExecuteFunction() {
        if (allErrors.size == 0)
            File("${settingsPacket.settingsFile.absolutePath}.$EXTRAS_MARKER").createNewFile()    // mark for cleaning
        onRestoreComplete.onRestoreComplete(jobcode, allErrors.size == 0, allErrors)
    }

    override fun doInBackground(vararg params: Any?): Any {

        resetBroadcast(true, engineContext.getString(R.string.restoring_settings))

        try {
            restorePacket()
        }
        catch (e: Exception){
            e.printStackTrace()
            allErrors.add("${ERROR_GENERIC_SETTINGS}: ${e.message}")
        }

        return 0
    }

}