package balti.migratehelper.restoreEngines.engines

import android.os.Build
import balti.migratehelper.R
import balti.migratehelper.restoreEngines.ParentRestoreClass
import balti.migratehelper.restoreSelectorActivity.containers.WifiPacketKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_WIFI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WIFI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.WIFI_FILE_NAME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.WIFI_FILE_PATH
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class WifiRestoreEngine(private val jobcode: Int,
                        private val wifiPacket: WifiPacketKotlin): ParentRestoreClass(EXTRA_PROGRESS_TYPE_WIFI) {

    private val errors by lazy { ArrayList<String>(0) }
    private val suProcess by lazy { Runtime.getRuntime().exec("su") }
    private val suWriter by lazy { BufferedWriter(OutputStreamWriter(suProcess.outputStream)) }

    private fun restoreWifi(){
        wifiPacket.wifiFile.let {

            if (it.name != WIFI_FILE_NAME || !wifiPacket.isSelected ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            resetBroadcast(true, engineContext.getString(R.string.restoring_wifi),
                    EXTRA_PROGRESS_TYPE_WIFI)
            suWriter.run {
                write("if [[ -e $WIFI_FILE_PATH ]]; then\n")
                write("    cp $WIFI_FILE_PATH $WIFI_FILE_PATH.bak\n")
                write("fi\n")
                write("cp ${it.absolutePath} $WIFI_FILE_PATH\n")
                write("exit\n")
                flush()
            }
            Thread.sleep(DUMMY_WAIT_TIME)
        }
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            restoreWifi()
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("${ERROR_WIFI}: ${e.message}")
        }

        BufferedReader(InputStreamReader(suProcess.errorStream)).readLines().forEach {
            errors.add("$ERROR_WIFI: $it")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onRestoreComplete.onRestoreComplete(jobcode, errors.size == 0, errors)
    }
}