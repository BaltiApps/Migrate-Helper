package balti.migrate.helper.utilities

import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import balti.migrate.helper.AppInstance
import balti.migrate.helper.AppInstance.Companion.sharedPrefs
import balti.migrate.helper.R
import balti.migrate.helper.simpleActivities.MainActivityKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.CHANNEL_UNINSTALLING
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REBOOT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REMOVE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_REMOUNT_ALL_TO_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.UNINSTALL_START_ID
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_RECEIVER_PACKAGE_NAME
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class UninstallServiceKotlin: Service() {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            commonTools.makeNotificationChannel(CHANNEL_UNINSTALLING, CHANNEL_UNINSTALLING, NotificationManager.IMPORTANCE_MIN)
        }

        val uninstallNotif = NotificationCompat.Builder(this, CHANNEL_UNINSTALLING)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentText(getString(R.string.finishing))
                .build()

        startForeground(UNINSTALL_START_ID, uninstallNotif)

        if (intent != null) {

            val doReboot = intent.getBooleanExtra(EXTRA_DO_REBOOT, false)
            val doUninstall = intent.getBooleanExtra(EXTRA_DO_UNINSTALL, true)
            val doRemoveCache = intent.getBooleanExtra(EXTRA_DO_REMOVE_CACHE, true)

            try {
                AppInstance.notificationManager.cancelAll()
                finishTasks(doUninstall, doReboot, doRemoveCache)
            }
            catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            }

        }
        else stopSelf()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun finishTasks(doUninstall: Boolean, doReboot: Boolean, doRemoveCache: Boolean){

        if (doUninstall || doReboot || doRemoveCache) {

            val sourceDir = applicationInfo.sourceDir.let {
                it.substring(0, it.lastIndexOf('/'))
            }

            val fullProcess = Runtime.getRuntime().exec("su")
            BufferedWriter(OutputStreamWriter(fullProcess.outputStream)).run {

                if (doUninstall || doRemoveCache) {
                    write("mount -o rw,remount /data\n")

                    if (doRemoveCache) {
                        write("rm -rf $MIGRATE_CACHE\n")
                        write("rm -rf $METADATA_HOLDER_DIR\n")
                        write("rm -rf /data/data/*.tar.gz\n")
                    }

                    if (doUninstall) {

                        ADDON_SETTINGS_RECEIVER_PACKAGE_NAME.let {
                            if (commonTools.isPackageInstalled(it)) write("pm uninstall $it\n")
                        }
                        ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME.let {
                            if (commonTools.isPackageInstalled(it)) write("pm uninstall $it\n")
                        }

                        if (sourceDir.startsWith("/system")) {
                            disableApp()
                            write("mount -o rw,remount /system\n")
                            write("mount -o rw,remount /system/app/MigrateHelper\n")
                            write("rm -rf ${applicationInfo.dataDir} $sourceDir\n")

                            // go advanced if not removed

                            write("if [[ -e $sourceDir ]]; then\n")
                            write("    cat /proc/mounts | grep system | while read -r line || [[ -n \"\$line\" ]]; do\n")
                            write("        mp=\"\$(echo \$line | cut -d ' ' -f2)\"\n")
                            write("        md=\"\$(echo \$line | cut -d ' ' -f1)\"\n")
                            write("        if [[ \$mp == \"/system\" || \$mp == \"/\" ]]; then\n")
                            write("            mount -o rw,remount \$md \$mp\n")
                            write("        fi\n")
                            write("    done\n")
                            write("    rm -rf $sourceDir\n")
                            write("fi\n")

                            // mount all as rw if app not removed

                            if (sharedPrefs.getBoolean(PREF_REMOUNT_ALL_TO_UNINSTALL, false)) {

                                write("if [[ -e $sourceDir ]]; then\n")
                                write("    cat /proc/mounts | while read -r line || [[ -n \"\$line\" ]]; do\n")
                                write("        mp=\"\$(echo \$line | cut -d ' ' -f2)\"\n")
                                write("        md=\"\$(echo \$line | cut -d ' ' -f1)\"\n")
                                write("        mount -o rw,remount \$md \$mp\n")
                                write("    done\n")
                                write("    rm -rf $sourceDir\n")
                                write("fi\n")
                            }

                        } else write("pm uninstall $packageName\n")

                        write("rm -rf /sdcard/Android/data/$packageName/helper\n")
                    }
                }

                if (doReboot) write("reboot\n")

                write("exit\n")

                stopService(Intent(this@UninstallServiceKotlin, StupidStartupServiceKotlin::class.java))

                commonTools.doBackgroundTask({
                    flush()
                    fullProcess.waitFor()
                }, {
                    stopSelf()
                })
            }
        }
        else stopSelf()
    }


    private fun disableApp() {

        val editor = AppInstance.sharedPrefs.edit()
        editor.putBoolean(CommonToolsKotlin.PREF_IS_DISABLED, true)
        editor.commit()

        val packageManager = packageManager
        val componentName = ComponentName(this, MainActivityKotlin::class.java)
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }
}