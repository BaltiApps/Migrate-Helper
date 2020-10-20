package balti.migrate.helper.emergencyRestore

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.helper.R
import balti.module.baltitoolbox.functions.Misc.serviceStart

class EmergencyRestoreProgressShow: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.restore_progress_layout)

        serviceStart(this, EmergencyRestoreService::class.java)
    }


}