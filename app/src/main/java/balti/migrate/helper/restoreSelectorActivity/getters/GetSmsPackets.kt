package balti.migrate.helper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.SmsPacketKotlin
import balti.migrate.helper.restoreSelectorActivity.utils.GetterType
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_SMS_GET_TRY_CATCH

class GetSmsPackets(jobCode: Int,
                    metadataHolderPath: String,
                    context: Context,
                    progressBar: ProgressBar,
                    waitingText: TextView): ParentGetter(jobCode, metadataHolderPath, context,
        progressBar, waitingText, R.string.getting_sms, GetterType.GETTER_TYPE_SMS) {

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (files.isNotEmpty()) {

                val smsPackets = ArrayList<SmsPacketKotlin>(0)

                var c = 0
                files.forEach {
                    if (!RestoreSelectorKotlin.cancelLoading) {
                        smsPackets.add(SmsPacketKotlin(it, true))
                        publishProgress(++c)
                    }
                }

                if (!RestoreSelectorKotlin.cancelLoading) Thread.sleep(DUMMY_WAIT_TIME)

                return smsPackets
            }
        }
        catch (e: Exception){
            errors.add("$ERROR_SMS_GET_TRY_CATCH: ${e.message.toString()}")
        }

        return 0
    }
}