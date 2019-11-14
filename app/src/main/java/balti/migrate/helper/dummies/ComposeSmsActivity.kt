package balti.migrate.helper.dummies

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.helper.R

class ComposeSmsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, R.string.please_change_sms_app, Toast.LENGTH_LONG).show()
        finish()
    }
}