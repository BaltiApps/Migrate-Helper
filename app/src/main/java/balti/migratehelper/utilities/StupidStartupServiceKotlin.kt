package balti.migratehelper.utilities

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.simpleActivities.MainActivityKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.CHANNEL_INIT
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PENDING_INIT_NOTIFICATION_ID
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PENDING_INIT_REQUEST_ID
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_TEMPORARY_DISABLE

class StupidStartupServiceKotlin: Service() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val initNotification by lazy {
        NotificationCompat.Builder(this, CHANNEL_INIT).apply {
            setSmallIcon(R.drawable.ic_notification_icon)
            setContentTitle(getString(R.string.notifHeader))
            setContentText(getString(R.string.notifBody))
            setContentIntent(PendingIntent.getActivity(this@StupidStartupServiceKotlin, PENDING_INIT_REQUEST_ID,
                    Intent(this@StupidStartupServiceKotlin, MainActivityKotlin::class.java), 0))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setDefaults(Notification.DEFAULT_ALL)
        }.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (!AppInstance.sharedPrefs.getBoolean(PREF_TEMPORARY_DISABLE, false)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                commonTools.makeNotificationChannel(CHANNEL_INIT, CHANNEL_INIT, NotificationManager.IMPORTANCE_HIGH)
            }

            startForeground(PENDING_INIT_NOTIFICATION_ID, initNotification)
        }
        else stopSelf()

        return super.onStartCommand(intent, flags, startId)
    }
}