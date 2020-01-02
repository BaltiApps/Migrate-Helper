package balti.migrate.helper.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SETTINGS_BROADCAST
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.EXTRA_ADDON_TYPE
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.EXTRA_ADDON_TYPE_SETTINGS

class AddonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(DEBUG_TAG, "received from addon")
        intent?.run {
            getStringExtra(EXTRA_ADDON_TYPE)?.let { type ->
                context?.let { c ->
                    LocalBroadcastManager.getInstance(c).let { lbm ->
                        lbm.sendBroadcast(when (type) {
                            EXTRA_ADDON_TYPE_SETTINGS -> {
                                Log.d(DEBUG_TAG, "sending internal")
                                Intent(ACTION_ADDON_SETTINGS_BROADCAST)
                            }
                            else -> Intent()
                        }.putExtras(this))
                    }
                }
            }
        }
    }
}