package balti.migrate.helper.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.helper.utilities.constants.AddonReceiverConstants
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.EXTRA_ADDON_TYPE_SMS_CALLS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants

class AddonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.run {
            getStringExtra(AddonReceiverConstants.EXTRA_ADDON_TYPE)?.let { type ->
                context?.let { c ->

                    LocalBroadcastManager.getInstance(c).sendBroadcast(
                            when (type) {
                                EXTRA_ADDON_TYPE_SMS_CALLS -> {
                                    if (hasExtra(AddonSmsCallsConstants.ADDON_SMS_CALLS_EXTRA_SMS_GRANTED) || hasExtra(AddonSmsCallsConstants.ADDON_SMS_CALLS_EXTRA_CALLS_GRANTED))
                                        Intent(AddonReceiverConstants.ACTION_ADDON_SMS_CALLS_PERMISSION)
                                    else Intent(AddonReceiverConstants.ACTION_ADDON_SMS_CALLS_RESTORE)
                                }
                                else -> Intent()
                            }.putExtras(this)
                    )
                }
            }
        }
    }
}