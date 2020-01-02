package balti.migrate.helper.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SETTINGS_BROADCAST
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SETTINGS_SU
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.EXTRA_ADDON_TYPE
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.EXTRA_ADDON_TYPE_SETTINGS
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_SU_GRANTED

class AddonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.run {
            getStringExtra(EXTRA_ADDON_TYPE)?.let { type ->
                context?.let { c ->

                    LocalBroadcastManager.getInstance(c).sendBroadcast(when (type) {


                        EXTRA_ADDON_TYPE_SETTINGS -> {
                            if (this.hasExtra(ADDON_SETTINGS_EXTRA_SU_GRANTED)) Intent(ACTION_ADDON_SETTINGS_SU)
                            else Intent(ACTION_ADDON_SETTINGS_BROADCAST)
                        }

                        else -> Intent()

                    }
                            .putExtras(this))
                }
            }
        }
    }
}