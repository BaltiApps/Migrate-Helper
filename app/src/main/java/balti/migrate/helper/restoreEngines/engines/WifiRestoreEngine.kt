package balti.migrate.helper.restoreEngines.engines

import android.os.Build
import balti.migrate.helper.R
import balti.migrate.helper.restoreEngines.ParentRestoreClass
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.WifiPacketKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_WIFI
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRAS_MARKER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WIFI
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IS_WIFI_RESTORED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.WIFI_FILE_NAME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.WIFI_FILE_PATH
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import java.io.*

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
                write("    cat ${it.absolutePath} > $WIFI_FILE_PATH\n")
                write("else\n")
                write("    cp -a ${it.absolutePath} $WIFI_FILE_PATH\n")
                write("    chmod 600 $WIFI_FILE_PATH\n")
                write("    chown system:system $WIFI_FILE_PATH\n")
                write("    restorecon -f $WIFI_FILE_PATH 2>/dev/null\n")
                write("fi\n")
                write("rm $WIFI_FILE_PATH.encrypted-checksum 2>/dev/null\n")
                write("exit\n")
                flush()
            }
            Thread.sleep(DUMMY_WAIT_TIME)
        }
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (!RestoreServiceKotlin.cancelAll && wifiPacket.isSelected) restoreWifi()
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
        if (errors.size == 0)
            File("${wifiPacket.wifiFile.absolutePath}.$EXTRAS_MARKER").createNewFile()    // mark for cleaning

        putPrefBoolean(PREF_IS_WIFI_RESTORED, errors.isEmpty())

        onRestoreComplete.onRestoreComplete(jobcode, errors.size == 0, errors)
    }
}