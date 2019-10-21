package balti.migratehelper.restoreEngines.engines

import balti.migratehelper.R
import balti.migratehelper.restoreEngines.ParentRestoreClass
import balti.migratehelper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ADB
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FONT_SCALE
import java.io.BufferedWriter

class PreliminarySettingsRestoreEngine(private val jobcode: Int,
                                       private val settingsPacket: SettingsPacketKotlin): ParentRestoreClass("") {
    override fun postExecuteFunction() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun doInBackground(vararg params: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val errors by lazy { ArrayList<String>(0) }
    private var suProcess: Process? = null
    private var suWriter: BufferedWriter? = null

    private fun restoreAdb(){
        if (settingsPacket.adbItem == null) return
        else {
            resetBroadcast(true, engineContext.getString(R.string.restoring_adb),
                    EXTRA_PROGRESS_TYPE_ADB)

            suWriter?.run {
                write("settings put global adb_enabled ${settingsPacket.adbItem?.adbState}\n")
                flush()
            }
            Thread.sleep(DUMMY_WAIT_TIME)
        }
    }

    private fun restoreFontScale(){
        if (settingsPacket.fontScaleItem == null) return
        else {
            resetBroadcast(true, engineContext.getString(R.string.restoring_font_scale),
                    EXTRA_PROGRESS_TYPE_FONT_SCALE)

            suWriter?.run {
                write("settings put system font_scale ${settingsPacket.fontScaleItem?.fontScale}\n")
                flush()
            }
            Thread.sleep(DUMMY_WAIT_TIME)
        }
    }

}