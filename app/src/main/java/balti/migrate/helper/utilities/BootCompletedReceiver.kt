package balti.migrate.helper.utilities

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import balti.migrate.helper.simpleActivities.MainActivityKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IS_DISABLED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_TEMPORARY_DISABLE
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean

class BootCompletedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent == null || context == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED){

            if (!getPrefBoolean(PREF_TEMPORARY_DISABLE, false)){

                val packageManager = context.packageManager
                val componentName = ComponentName(context.packageName, MainActivityKotlin::class.java.name)
                if (!getPrefBoolean(PREF_IS_DISABLED, false)) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(Intent(context, StupidStartupServiceKotlin::class.java))
                    } else {
                        context.startService(Intent(context, StupidStartupServiceKotlin::class.java))
                    }
                    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                } else {
                    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                }
            }
        }
    }
}