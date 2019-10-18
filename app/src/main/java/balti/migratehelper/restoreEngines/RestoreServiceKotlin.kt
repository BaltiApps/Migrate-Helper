package balti.migratehelper.restoreEngines

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import balti.migratehelper.restoreSelectorActivity.containers.CallsPacketKotlin
import balti.migratehelper.restoreSelectorActivity.containers.ContactsPacketKotlin
import balti.migratehelper.restoreSelectorActivity.containers.SmsPacketKotlin
import balti.migratehelper.restoreSelectorActivity.containers.WifiPacketKotlin

class RestoreServiceKotlin: Service() {

    companion object {

        var contactsList : ArrayList<ContactsPacketKotlin>? = null
        var callsList : ArrayList<CallsPacketKotlin>? = null
        var smsList : ArrayList<SmsPacketKotlin>? = null
        var dpiText : String? = null
        var keyboardText : String? = null
        var adbState : Int? = null
        var fontScale : Double? = null
        var wifiData : WifiPacketKotlin? = null

        lateinit var serviceContext: Context
        private set

        var cancelAll = false
        private set
    }

    override fun onBind(intent: Intent?): IBinder? = null
}