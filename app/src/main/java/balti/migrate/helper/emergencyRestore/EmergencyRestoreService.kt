package balti.migrate.helper.emergencyRestore

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.CHANNEL_EMERGENCY_RESTORE_RUNNING
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_ONGOING_EM
import balti.module.baltitoolbox.functions.Misc
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class EmergencyRestoreService: Service() {
    companion object {
        lateinit var emergencyServiceContext: Context
    }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        emergencyServiceContext = this

        val loadingNotification = NotificationCompat.Builder(this, CHANNEL_EMERGENCY_RESTORE_RUNNING)
                .setContentTitle(getString(R.string.loading))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Misc.makeNotificationChannel(CHANNEL_EMERGENCY_RESTORE_RUNNING, CHANNEL_EMERGENCY_RESTORE_RUNNING, NotificationManager.IMPORTANCE_LOW)
        }

        startForeground(NOTIFICATION_ID_ONGOING_EM, loadingNotification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startRestore()
        return super.onStartCommand(intent, flags, startId)
    }

    fun startRestore(){
        MainScope().launch {
            EmergencyAppInstall().executeWithResult()
            stopSelf()
        }
    }
}