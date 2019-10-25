package balti.migratehelper.restoreEngines.engines

import balti.migratehelper.R
import balti.migratehelper.restoreEngines.ParentRestoreClass
import balti.migratehelper.restoreEngines.RestoreServiceKotlin
import balti.migratehelper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migratehelper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_ADB
import balti.migratehelper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_FONT_SCALE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_GENERIC_SETTINGS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ADB
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FONT_SCALE
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class PreliminarySettingsRestoreEngine(private val jobcode: Int,
                                       private val settingsPacket: SettingsPacketKotlin): ParentRestoreClass("") {

    private val errors by lazy { ArrayList<String>(0) }
    private val suProcess by lazy { Runtime.getRuntime().exec("su") }
    private val suWriter by lazy { BufferedWriter(OutputStreamWriter(suProcess.outputStream)) }
    private fun flushToSu(cmd: String, ignoreCancel: Boolean = false){
        if (RestoreServiceKotlin.cancelAll || ignoreCancel){
            suWriter.write("$cmd\n")
            suWriter.flush()
        }
    }

    private fun restorePacket(packet: SettingsPacketKotlin.SettingsItem){
        packet.let {
            if (it.isSelected) {
                when(it.settingsType) {
                    SETTINGS_TYPE_ADB -> resetBroadcast(true, engineContext.getString(R.string.restoring_adb), EXTRA_PROGRESS_TYPE_ADB)
                    SETTINGS_TYPE_FONT_SCALE ->
                        resetBroadcast(true, engineContext.getString(R.string.restoring_font_scale), EXTRA_PROGRESS_TYPE_FONT_SCALE)
                }

                it.commandsToRestore.forEach { cmd ->
                    flushToSu(cmd)
                }
                Thread.sleep(DUMMY_WAIT_TIME)
            }
        }
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            settingsPacket.internalPackets.forEach {
                if (!RestoreServiceKotlin.cancelAll) {
                    if (it.settingsType in arrayOf(SETTINGS_TYPE_ADB, SETTINGS_TYPE_FONT_SCALE))
                        restorePacket(it)
                }
            }
            flushToSu("exit", true)
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
        onRestoreComplete.onRestoreComplete(jobcode, errors.size == 0, errors)
    }

}