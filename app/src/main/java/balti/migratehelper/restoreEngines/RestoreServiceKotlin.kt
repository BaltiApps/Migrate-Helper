package balti.migratehelper.restoreEngines

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RestoreServiceKotlin: Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}