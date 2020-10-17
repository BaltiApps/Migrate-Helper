package balti.migrate.helper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.CallsPacketKotlin
import balti.migrate.helper.restoreSelectorActivity.utils.GetterType
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_CALLS_GET_TRY_CATCH

class GetCallsPackets(jobCode: Int,
                      metadataHolderPath: String,
                      context: Context,
                      progressBar: ProgressBar,
                      waitingText: TextView): ParentGetter(jobCode, metadataHolderPath, context,
        progressBar, waitingText, R.string.getting_calls, GetterType.GETTER_TYPE_CALLS) {

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