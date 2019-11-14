package balti.migrate.helper.dummies

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import balti.migrate.helper.R

class SmsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, R.string.new_sms_received, Toast.LENGTH_LONG).show()
    }
}