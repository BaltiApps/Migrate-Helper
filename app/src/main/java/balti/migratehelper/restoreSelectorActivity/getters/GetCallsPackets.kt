package balti.migratehelper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migratehelper.restoreSelectorActivity.containers.CallsPacketKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_CALLS_GET_TRY_CATCH
import java.io.FileFilter

class GetCallsPackets(jobCode: Int,
                      metadataHolderPath: String,
                      context: Context,
                      progressBar: ProgressBar,
                      waitingText: TextView): ParentGetter(jobCode, metadataHolderPath, context, progressBar, waitingText, R.string.getting_calls) {

    override var fileFilter = FileFilter {
        it.name.endsWith(".calls.db")
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (files.isNotEmpty()) {

                val contactPackets = ArrayList<CallsPacketKotlin>(0)

                var c = 0
                files.forEach {
                    if (!RestoreSelectorKotlin.cancelLoading) {
                        contactPackets.add(CallsPacketKotlin(it, true))
                        publishProgress(++c)
                    }
                }

                if (!RestoreSelectorKotlin.cancelLoading) Thread.sleep(DUMMY_WAIT_TIME)

                return contactPackets
            }
        } catch (e: Exception) {
            errors.add("$ERROR_CALLS_GET_TRY_CATCH: ${e.message.toString()}")
        }

        return 0
    }
}