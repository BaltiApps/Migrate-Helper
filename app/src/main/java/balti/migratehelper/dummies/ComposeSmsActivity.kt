package balti.migratehelper.dummies

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import balti.migratehelper.R

class ComposeSmsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, R.string.please_change_sms_app, Toast.LENGTH_LONG).show()
        finish()
    }
}