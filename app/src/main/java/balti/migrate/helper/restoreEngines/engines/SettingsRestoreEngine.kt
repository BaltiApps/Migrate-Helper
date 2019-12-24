package balti.migrate.helper.restoreEngines.engines

import balti.migrate.helper.R
import balti.migrate.helper.restoreEngines.ParentRestoreClass
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_ADB
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_DPI
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_FONT_SCALE
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_KEYBOARD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DIR_REVERT_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME_LONGER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_GENERIC_SETTINGS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRAS_MARKER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ADB
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FONT_SCALE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_KEYBOARD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_REVERT_HEAD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_LAST_DPI
import balti.migrate.helper.utilities.constants.SettingsFields
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SettingsRestoreEngine(private val jobcode: Int,
                            private val settingsPacket: SettingsPacketKotlin): ParentRestoreClass("") {

    private val errors by lazy { ArrayList<String>(0) }
    private val suProcess by lazy { Runtime.getRuntime().exec("su") }
    private val suWriter by lazy { BufferedWriter(OutputStreamWriter(suProcess.outputStream)) }
    private val timeStamp by lazy { SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().time)}

    private fun flushToSu(cmd: String, ignoreCancel: Boolean = false){
        if (!RestoreServiceKotlin.cancelAll || ignoreCancel){
            suWriter.write("$cmd\n")
            suWriter.flush()
        }
    }

    private fun backupSettings() {

        val file = File(DIR_REVERT_DIR, "${FILE_REVERT_HEAD}$timeStamp")

        flushToSu("mkdir -p $DIR_REVERT_DIR")
        flushToSu("touch ${file.absolutePath}")

        flushToSu("echo \"{\" > ${file.absolutePath}")
        flushToSu("adbVal=\"$(settings get global adb_enabled)\"")
        flushToSu("echo \"   \\\"${SettingsFields.JSON_FIELD_ADB_TEXT}\\\": \$adbVal,\" >> ${file.absolutePath}")
        flushToSu("echo \"   \\\"${SettingsFields.JSON_FIELD_DPI_TEXT}\\\": \\\"\$(wm density)\\\",\" >> ${file.absolutePath}")
        flushToSu("fontVal=\"$(settings get system font_scale)\"")
        flushToSu("echo \"   \\\"${SettingsFields.JSON_FIELD_FONT_SCALE}\\\": \$fontVal\" >> ${file.absolutePath}")
        flushToSu("echo \"}\" >> ${file.absolutePath}")

    }

    private fun restorePacket(packet: SettingsPacketKotlin.SettingsItem){
        packet.let {
            if (it.isSelected) {
                when(it.settingsType) {
                    SETTINGS_TYPE_ADB -> resetBroadcast(true, engineContext.getString(R.string.restoring_adb), EXTRA_PROGRESS_TYPE_ADB)
                    SETTINGS_TYPE_FONT_SCALE ->
                        resetBroadcast(true, engineContext.getString(R.string.restoring_font_scale), EXTRA_PROGRESS_TYPE_FONT_SCALE)
                    SETTINGS_TYPE_KEYBOARD ->
                        resetBroadcast(true, engineContext.getString(R.string.restoring_keyboard), EXTRA_PROGRESS_TYPE_KEYBOARD)
                    SETTINGS_TYPE_DPI -> commonTools.tryIt { sharedPreferences.edit().putInt(PREF_LAST_DPI, it.value as Int).commit() }
                }

                if (it.settingsType in arrayOf(SETTINGS_TYPE_ADB, SETTINGS_TYPE_FONT_SCALE, SETTINGS_TYPE_KEYBOARD)) {
                    it.commandsToRestore.forEach { cmd ->
                        flushToSu(cmd)
                    }
                    Thread.sleep(DUMMY_WAIT_TIME_LONGER)
                }
            }
        }
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            backupSettings()

            settingsPacket.internalPackets.forEach {
                if (!RestoreServiceKotlin.cancelAll) {
                    if (it.isSelected) restorePacket(it)
                }
            }
            flushToSu("exit", true)
            suProcess.waitFor()
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("${ERROR_GENERIC_SETTINGS}: ${e.message}")
        }

        BufferedReader(InputStreamReader(suProcess.inputStream)).readLines().forEach {
            broadcastProgress("", it, false)
        }

        BufferedReader(InputStreamReader(suProcess.errorStream)).readLines().forEach {
            errors.add("$ERROR_GENERIC_SETTINGS: $it")
        }

        return 0
    }

    override fun postExecuteFunction() {
        if (errors.size == 0) File("${settingsPacket.settingsFile.absolutePath}.$EXTRAS_MARKER").createNewFile()    // mark for cleaning
        onRestoreComplete.onRestoreComplete(jobcode, errors.size == 0, errors)
    }

}