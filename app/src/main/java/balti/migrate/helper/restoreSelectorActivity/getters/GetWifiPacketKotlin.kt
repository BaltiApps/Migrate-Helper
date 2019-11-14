package balti.migrate.helper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.WifiPacketKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_WIFI_GET_TRY_CATCH
import java.io.FileFilter

class GetWifiPacketKotlin(jobCode: Int,
                          metadataHolderPath: String,
                          context: Context,
                          progressBar: ProgressBar,
                          waitingText: TextView): ParentGetter(jobCode, metadataHolderPath, context, progressBar, waitingText, R.string.getting_wifi) {

    override var fileFilter = FileFilter {
        it.name == CommonToolsKotlin.WIFI_FILE_NAME
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (files.isNotEmpty()) {
                if (!RestoreSelectorKotlin.cancelLoading) {
                    Thread.sleep(DUMMY_WAIT_TIME)
                    return WifiPacketKotlin(files[0], true)
                }
            }
        }
        catch (e: Exception){
            errors.add("$ERROR_WIFI_GET_TRY_CATCH: ${e.message.toString()}")
        }

        return 0
    }
}