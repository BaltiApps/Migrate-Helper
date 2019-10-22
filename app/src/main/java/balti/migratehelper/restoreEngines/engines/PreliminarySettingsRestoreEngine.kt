package balti.migratehelper.restoreEngines.engines

import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.restoreEngines.ParentRestoreClass
import balti.migratehelper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_DPI_READ
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_GENERIC_SETTINGS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ADB
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_DPI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FONT_SCALE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_KEYBOARD
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class PreliminarySettingsRestoreEngine(private val jobcode: Int,
                                       private val settingsPacket: SettingsPacketKotlin): ParentRestoreClass("") {

    private val errors by lazy { ArrayList<String>(0) }
    private val suProcess by lazy { Runtime.getRuntime().exec("su") }
    private val suWriter by lazy { BufferedWriter(OutputStreamWriter(suProcess.outputStream)) }

    private fun restoreAdb(){
        settingsPacket.adbItem?.let {
            if (it.isSelected) {
                resetBroadcast(true, engineContext.getString(R.string.restoring_adb),
                        EXTRA_PROGRESS_TYPE_ADB)

                suWriter.run {
                    write("settings put global adb_enabled ${it.adbState}\n")
                    flush()
                }
                Thread.sleep(DUMMY_WAIT_TIME)
            }
        }
    }

    private fun restoreFontScale(){
        settingsPacket.fontScaleItem?.let {
            if (it.isSelected) {
                resetBroadcast(true, engineContext.getString(R.string.restoring_font_scale),
                        EXTRA_PROGRESS_TYPE_FONT_SCALE)

                suWriter.run {
                    write("settings put system font_scale ${it.fontScale}\n")
                    flush()
                }
                Thread.sleep(DUMMY_WAIT_TIME)
            }
        }
    }

    private fun readDPI(){
        settingsPacket.dpiItem?.let {
            if (it.isSelected) {
                resetBroadcast(true, engineContext.getString(R.string.processing_dpi),
                        EXTRA_PROGRESS_TYPE_DPI)

                try {
                    it.dpiText?.run {
                        val lines = this.split("\\n")
                        var oDensity = 0
                        var pDensity = 0
                        for (line in lines) {
                            line.trim().let { l ->
                                if (l.startsWith("Physical density:")) {
                                    pDensity = Integer.parseInt(l.substring(l.lastIndexOf(' ')).trim())
                                } else if (l.startsWith("Override density:")) {
                                    oDensity = Integer.parseInt(l.substring(l.lastIndexOf(' ')).trim())
                                }
                            }
                        }
                        AppInstance.DPIint = when {
                            oDensity > 0 -> oDensity
                            pDensity > 0 -> pDensity
                            else -> null
                        }
                    }

                    Thread.sleep(DUMMY_WAIT_TIME)
                }
                catch (e: Exception){
                    e.printStackTrace()
                    errors.add("$ERROR_DPI_READ: ${e.message}")
                }
            }
        }
    }

    private fun readKeyboard(){
        settingsPacket.keyboardItem?.let {
            if (it.isSelected) {
                resetBroadcast(true, engineContext.getString(R.string.processing_keyboard),
                        EXTRA_PROGRESS_TYPE_KEYBOARD)
                AppInstance.keyBoardText = it.keyboardText
                Thread.sleep(DUMMY_WAIT_TIME)
            }
        }
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            settingsPacket.refreshInternalPackets()
            if (settingsPacket.internalPackets.isNotEmpty()) {
                restoreAdb()
                restoreFontScale()
                readDPI()
                readKeyboard()
            }
            suWriter.write("exit\n")
            suWriter.flush()
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("${ERROR_GENERIC_SETTINGS}: ${e.message}")
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