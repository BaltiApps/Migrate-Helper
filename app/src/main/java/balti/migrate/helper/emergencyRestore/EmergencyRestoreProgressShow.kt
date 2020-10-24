package balti.migrate.helper.emergencyRestore

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.helper.AppInstance.Companion.emFailedCombined
import balti.migrate.helper.R
import balti.migrate.helper.emergencyRestore.utils.RetryEmergencyMissingPackages
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_EM_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_EM_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_EMERGENCY_DATA
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_FINISHED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_LOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_SUBTASK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_TITLE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_RETRY_APP_INSTALLS
import balti.module.baltitoolbox.functions.Misc.serviceStart
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.restore_progress_layout.*

class EmergencyRestoreProgressShow: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private var forceStopDialog: AlertDialog? = null

    private fun handleProgress(intent: Intent?){
        intent?.run {
            EXTRA_EM_TITLE.let { if (hasExtra(it)) progressTask.text = getStringExtra(it) }
            EXTRA_EM_SUBTASK.let { if (hasExtra(it)) subTask.text = getStringExtra(it) }
            EXTRA_EM_LOG.let { if (hasExtra(it)) progressLogTextView.append(getStringExtra(it)) }
            getIntExtra(EXTRA_EM_PROGRESS, -1).let {
                if (it != -1) {
                    progressBar.progress = it
                    progressBar.isIndeterminate = false
                    progressPercent.text = "$it%"
                } else {
                    progressBar.isIndeterminate = true
                    progressPercent.text = "<-->"
                }
            }
            if (getBooleanExtra(EXTRA_EM_FINISHED, false)) {

                tryIt { forceStopDialog?.dismiss() }

                progressActionButton.apply {
                    text = getString(R.string.finish)
                    visibility = View.VISIBLE
                    setOnClickListener { finish() }
                }

                progressAbortButton.visibility = View.GONE

                if (errorLogTextView.text.isNotBlank()) {
                    reportLogButton.apply {
                        visibility = View.GONE
                    }
                }

                if (emFailedCombined.isNotEmpty()) retryButton.visibility = View.VISIBLE
            }
        }
    }

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handleProgress(intent)
            }
        }
    }

    private fun handleErrors(intent: Intent?){
        intent?.run {
            EXTRA_EM_ERRORS.let { if (hasExtra(it)) getStringArrayListExtra(it).forEach {
                errorLogTextView.append("$it\n")
            } }
        }
    }

    private val errorReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handleErrors(intent)
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

        progressActionButton.visibility = View.GONE
        progressAbortButton.apply {
            text = getString(R.string.force_stop)
            visibility = View.VISIBLE
            setOnClickListener {

                forceStopDialog = AlertDialog.Builder(this@EmergencyRestoreProgressShow).apply {

                    this.setTitle(getString(R.string.force_stop_alert_title))
                    setPositiveButton(R.string.kill_app) { _, _ ->
                        commonTools.forceStopSelf()
                    }
                    setNegativeButton(android.R.string.cancel, null)
                }.create()

                forceStopDialog?.show()
            }
        }
        reportLogButton.apply {
            visibility = View.GONE
            setOnClickListener { commonTools.reportLogs(true) }
        }
        retryButton.apply {
            visibility = View.GONE
            setOnClickListener {
                startActivityForResult(
                        Intent(this@EmergencyRestoreProgressShow, RetryEmergencyMissingPackages::class.java),
                        JOBCODE_RETRY_APP_INSTALLS
                )
            }
        }
        app_icon.setImageResource(R.drawable.ic_app)

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_EM_PROGRESS))
        commonTools.LBM?.registerReceiver(errorReceiver, IntentFilter(ACTION_EM_ERRORS))

        handleProgress(intent)
        handleErrors(intent)

        if (!EmergencyRestoreService.wasStarted && !intent.hasExtra(EXTRA_EM_TITLE))
            serviceStart(this, EmergencyRestoreService::class.java)
        else commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_EMERGENCY_DATA))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JOBCODE_RETRY_APP_INSTALLS)
            if (resultCode == RESULT_OK) recreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        tryIt { commonTools.LBM?.unregisterReceiver(errorReceiver) }
        tryIt { forceStopDialog?.dismiss() }
    }
}