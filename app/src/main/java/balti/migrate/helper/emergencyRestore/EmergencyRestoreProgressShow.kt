package balti.migrate.helper.emergencyRestore

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_EM_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_EM_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_LOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_SUBTASK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_TITLE
import balti.module.baltitoolbox.functions.Misc.serviceStart
import kotlinx.android.synthetic.main.restore_progress_layout.*

class EmergencyRestoreProgressShow: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.run {
                    EXTRA_EM_TITLE.let { if (hasExtra(it)) progressTask.text = getStringExtra(it) }
                    EXTRA_EM_SUBTASK.let { if (hasExtra(it)) subTask.text = getStringExtra(it) }
                    EXTRA_EM_LOG.let { if (hasExtra(it)) progressLogTextView.append(getStringExtra(it)) }
                }
            }
        }
    }

    private val errorReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.run {
                    EXTRA_EM_ERRORS.let { if (hasExtra(it)) getStringArrayListExtra(it).forEach {
                        errorLogTextView.append("$it\n")
                    } }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.restore_progress_layout)

        progressLogTextView.apply {
            gravity = Gravity.BOTTOM
            movementMethod = ScrollingMovementMethod()
        }

        errorLogTextView.apply {
            gravity = Gravity.BOTTOM
            movementMethod = ScrollingMovementMethod()
        }

        app_icon.setImageResource(R.drawable.ic_app)

        serviceStart(this, EmergencyRestoreService::class.java)

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_EM_PROGRESS))
        commonTools.LBM?.registerReceiver(errorReceiver, IntentFilter(ACTION_EM_ERRORS))
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.LBM?.unregisterReceiver(progressReceiver)
        commonTools.LBM?.unregisterReceiver(errorReceiver)
    }
}