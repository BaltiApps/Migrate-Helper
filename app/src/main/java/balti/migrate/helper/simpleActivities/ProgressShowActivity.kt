package balti.migrate.helper.simpleActivities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import balti.migrate.helper.AppInstance
//import balti.migrate.helper.AppInstance.Companion.sharedPrefs
import balti.migrate.helper.R
import balti.migrate.helper.postJobs.PostJobsActivity
import balti.migrate.helper.restoreEngines.engines.AppRestoreEngine
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_RESTORE_DATA
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_ABORT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_START_POST_JOBS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_APP_RESTORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_MAKING_SCRIPTS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ADB
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CLEANING
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FINISHED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FONT_SCALE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_KEYBOARD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WIFI
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SUBTASK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_TASKLOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_TITLE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_TRACK_RESTORE_FINISHED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.TIMEOUT_WAITING_TO_KILL
import balti.migrate.helper.utilities.IconTools
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import kotlinx.android.synthetic.main.restore_progress_layout.*
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter

class ProgressShowActivity: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val iconTools by lazy { IconTools() }
    private val errors by lazy { ArrayList<String>(0) }

    private var lastLog = ""
    private var lastIcon = ""
    private val lastIntent by lazy { Intent() }

    private var lastTitle = ""

    private var forceStopDialog: AlertDialog? = null
    private var abortDialog: AlertDialog? = null

    private val endOnDisable by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) = finish()
        }
    }

    private fun setImageIcon(intent: Intent, type: String){

        if (type == EXTRA_PROGRESS_APP_RESTORE){
            try {
                if (lastIcon != AppRestoreEngine.ICON_STRING) {
                    AppRestoreEngine.ICON_STRING.run {
                        if (this.contains(".icon", true) || this.contains(".png", true)){
                            val iconFile = File(METADATA_HOLDER_DIR, this.trim())
                            iconTools.setIconFromFile(app_icon, iconFile)
                        }
                        else iconTools.setIconFromIconString(app_icon, this)
                        lastIcon = this
                    }
                }
            }
            catch (_: Exception){
                app_icon.setImageResource(R.drawable.ic_waiting)
            }
        }
        else if (type == EXTRA_PROGRESS_TYPE_FINISHED){

            val isCancelled = intent.getBooleanExtra(EXTRA_IS_CANCELLED, false)

            app_icon.setImageResource(
                    when {
                        isCancelled -> R.drawable.ic_cancelled_icon
                        errors.size > 0 -> R.drawable.ic_error
                        else -> R.drawable.ic_finished_icon
                    }
            )
            if (errors.size != 0 || isCancelled) app_icon.setColorFilter(
                    ContextCompat.getColor(this@ProgressShowActivity, R.color.error_color),
                    android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
        else app_icon.setImageResource(
                when (type){

                    EXTRA_PROGRESS_TYPE_SMS -> R.drawable.ic_sms_icon
                    EXTRA_PROGRESS_TYPE_CALLS -> R.drawable.ic_call_log_icon
                    EXTRA_PROGRESS_TYPE_WIFI -> R.drawable.ic_wifi_icon
                    EXTRA_PROGRESS_TYPE_ADB -> R.drawable.ic_adb_icon
                    EXTRA_PROGRESS_TYPE_FONT_SCALE -> R.drawable.ic_font_scale_icon
                    EXTRA_PROGRESS_TYPE_KEYBOARD -> R.drawable.ic_keyboard_icon

                    EXTRA_PROGRESS_MAKING_SCRIPTS -> R.drawable.ic_app_scripts_icon

                    EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL -> R.drawable.ic_canceling_icon
                    EXTRA_PROGRESS_TYPE_CLEANING -> R.drawable.ic_app_cleaner_icon

                    else -> R.drawable.ic_waiting
                })
    }

    private fun handleProgress(handlingIntent: Intent?){

        if (handlingIntent != null){

            lastIntent.putExtras(handlingIntent)

            handlingIntent.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, -1).let {
                if (it != -1) {
                    progressBar.progress = it
                    progressBar.isIndeterminate = false
                    progressPercent.text = "$it%"
                } else {
                    progressBar.isIndeterminate = true
                    progressPercent.text = "<-->"
                }
            }

            if (handlingIntent.hasExtra(EXTRA_TITLE)) {
                handlingIntent.getStringExtra(EXTRA_TITLE).trim().run {
                    if (this != lastTitle && this != ""){
                        progressTask.text = this
                        progressLogTextView.append("\n$this\n")
                        lastTitle = this
                    }
                }
            }

            if (handlingIntent.hasExtra(EXTRA_SUBTASK))
                subTask.text = handlingIntent.getStringExtra(EXTRA_SUBTASK)

            if (handlingIntent.hasExtra(EXTRA_TASKLOG)){
                handlingIntent.getStringExtra(EXTRA_TASKLOG).run {
                    if (this != lastLog && this != ""){
                        progressLogTextView.append("$this\n")
                        lastLog = this
                    }
                }
            }

            if (handlingIntent.hasExtra(EXTRA_PROGRESS_TYPE)){

                val type = handlingIntent.getStringExtra(EXTRA_PROGRESS_TYPE)

                if (type == EXTRA_PROGRESS_TYPE_FINISHED) {

                    tryIt { abortDialog?.dismiss() }
                    tryIt { forceStopDialog?.dismiss() }

                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    closeWarning.visibility = View.GONE

                    if (handlingIntent.hasExtra(EXTRA_ERRORS))
                        errors.addAll(handlingIntent.getStringArrayListExtra(EXTRA_ERRORS))

                    if (errors.size > 0) {
                        reportLogButton.visibility = View.VISIBLE
                        errorLogTextView.visibility = View.VISIBLE
                        errors.forEach {
                            errorLogTextView.append("$it\n")
                        }
                    }

                    if (errors.size != 0 || handlingIntent.getBooleanExtra(EXTRA_IS_CANCELLED, false)) {
                        progressTask.setTextColor(resources.getColor(R.color.error_color))
                        subTask.setTextColor(resources.getColor(R.color.error_color))
                    }

                    handlingIntent.getLongExtra(EXTRA_TOTAL_TIME, -1).let {
                        if (it != -1L) subTask.text = calendarDifference(it)
                    }

                    progressAbortButton.visibility = View.GONE

                    progressActionButton.apply {
                        text = getString(R.string.finalize)
                        setOnClickListener {
                            startActivity(Intent(this@ProgressShowActivity, PostJobsActivity::class.java)
                                    .putExtras(handlingIntent)
                            )
                        }
                    }
                }

                setImageIcon(handlingIntent, type)
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

    private fun calendarDifference(longDiff: Long): String {
        var longDiff = longDiff
        var diff = ""

        try {

            longDiff /= 1000

            val d = longDiff / (60 * 60 * 24)
            if (d != 0L) diff = diff + d + "days "
            longDiff %= (60 * 60 * 24)

            val h = longDiff / (60 * 60)
            if (h != 0L) diff = diff + h + "hrs "
            longDiff %= (60 * 60)

            val m = longDiff / 60
            if (m != 0L) diff = diff + m + "mins "
            longDiff %= 60

            val s = longDiff
            diff = diff + s + "secs"

        } catch (ignored: Exception) {
        }

        return diff
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.restore_progress_layout)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        progressTask.text = getString(R.string.please_wait)

        progressActionButton.apply{
            text = getString(R.string.minimize)
            setOnClickListener { finishAffinity() }
        }

        progressAbortButton.apply {

            visibility = View.VISIBLE
            setOnClickListener {
                if (this.text == getString(R.string.force_stop)) {

                    forceStopDialog = AlertDialog.Builder(this@ProgressShowActivity).apply {

                        this.setTitle(getString(R.string.force_stop_alert_title))
                        this.setMessage(getString(R.string.force_stop_alert_desc))

                        setPositiveButton(R.string.kill_app) { _, _ ->

                            Runtime.getRuntime().exec("su").apply {
                                BufferedWriter(OutputStreamWriter(this.outputStream)).run {
                                    this.write("am force-stop $packageName\n")
                                    this.write("exit\n")
                                    this.flush()
                                }
                            }

                            val handler = Handler()
                            handler.postDelayed({
                                Toast.makeText(this@ProgressShowActivity, R.string.killing_programmatically, Toast.LENGTH_SHORT).show()
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, TIMEOUT_WAITING_TO_KILL)
                        }

                        setNegativeButton(R.string.wait_to_cancel, null)

                    }.create()

                    forceStopDialog?.show()
                }
                else {

                    abortDialog = AlertDialog.Builder(this@ProgressShowActivity)
                            .setTitle(R.string.abort_title)
                            .setMessage(R.string.abort_desc)
                            .setPositiveButton(R.string.abort_anyway) { _, _ ->
                                text = getString(R.string.force_stop)
                                commonTools.LBM?.sendBroadcast(Intent(ACTION_RESTORE_ABORT))
                            }
                            .setNegativeButton(R.string.let_it_continue, null)
                            .setCancelable(false)
                            .create()

                    abortDialog?.show()
                }
            }
        }

        progressBar.max = 100

        reportLogButton.apply {
            visibility = View.GONE
            setOnClickListener { commonTools.reportLogs(true) }
        }

        progressLogTextView.apply {
            gravity = Gravity.BOTTOM
            movementMethod = ScrollingMovementMethod()
        }

        if (intent.extras != null){
            handleProgress(intent)
            Handler().postDelayed({
                if (intent.getBooleanExtra(EXTRA_DO_START_POST_JOBS, false))
                    startActivity(Intent(this, PostJobsActivity::class.java))
            }, 100)
        }

        commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_RESTORE_DATA))
        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))
        commonTools.LBM?.registerReceiver(endOnDisable, IntentFilter(ACTION_END_ALL))
    }

    override fun onResume() {
        super.onResume()
        if (getPrefBoolean(PREF_TRACK_RESTORE_FINISHED, true) && AppInstance.isFinished) {
            if (!intent.hasExtra(EXTRA_PROGRESS_TYPE) || intent.getStringExtra(EXTRA_PROGRESS_TYPE) != EXTRA_PROGRESS_TYPE_FINISHED) {
                startActivity(Intent(this, MainActivityKotlin::class.java))
                finish()
            }
        }
    }

    override fun onDestroy() {
        tryIt { intent.replaceExtras(Bundle()) }
        super.onDestroy()
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        tryIt { commonTools.LBM?.unregisterReceiver(endOnDisable) }
        tryIt { abortDialog?.dismiss() }
        tryIt { forceStopDialog?.dismiss() }
    }
}