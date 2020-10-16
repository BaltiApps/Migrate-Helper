package balti.migrate.helper

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import balti.migrate.helper.restoreSelectorActivity.containers.*
import balti.module.baltitoolbox.ToolboxHQ

class AppInstance: Application() {

    companion object{
        lateinit var appContext: Context
        lateinit var notificationManager: NotificationManager

        val failedAppInstalls by lazy { ArrayList<AppPacketsKotlin>(0) }
        val appPackets by lazy { ArrayList<AppPacketsKotlin>(0) }
        val contactDataPackets by lazy { ArrayList<ContactsPacketKotlin>(0) }
        val smsDataPackets by lazy { ArrayList<SmsPacketKotlin>(0) }
        val callsDataPackets by lazy { ArrayList<CallsPacketKotlin>(0) }
        var settingsPacket: SettingsPacketKotlin? = null
        var wifiPacket: WifiPacketKotlin? = null

        var notificationFixGlobal = true

        var isFinished = false
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        ToolboxHQ.init(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
}