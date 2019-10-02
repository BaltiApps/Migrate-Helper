package balti.migratehelper

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.FILE_MAIN_PREF
import java.io.File

class AppInstance: Application() {

    companion object{
        lateinit var appContext: Context
        lateinit var sharedPrefs: SharedPreferences
        lateinit var notificationManager: NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        sharedPrefs = getSharedPreferences(FILE_MAIN_PREF, Context.MODE_PRIVATE)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        File(externalCacheDir.absolutePath).mkdirs()
    }
}