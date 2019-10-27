package balti.migratehelper.simpleActivities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.restoreEngines.engines.AppRestoreEngine
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_POST_JOBS_STARTED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_ABORT
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_POST_JOBS_STARTED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_APP_RESTORE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_MAKING_SCRIPTS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ADB
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FINISHED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FONT_SCALE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_KEYBOARD
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WIFI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_SUBTASK
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_TASKLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_TITLE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IS_POST_JOBS_NEEDED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.TIMEOUT_WAITING_TO_KILL
import balti.migratehelper.utilities.IconTools
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

    private var wasPostJobStarted = false

    private fun setImageIcon(intent: Intent, type: String){

        if (type == EXTRA_PROGRESS_APP_RESTORE){
            try {
                if (lastIcon != AppRestoreEngine.ICON_STRING) {
                    AppRestoreEngine.ICON_STRING.run {
                        if (this.contains(".icon", true)){
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

                    else -> R.drawable.ic_waiting
                })
    }

    private fun handleProgress(intent: Intent?){

        if (intent != null){

            lastIntent.putExtras(intent)

            intent.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, -1).let {
                if (it != -1) {
                    progressBar.progress = it
                    progressBar.isIndeterminate = false
                    progressPercent.text = "$it%"
                } else {
                    progressBar.isIndeterminate = true
                    progressPercent.text = "<-->"
                }
            }

            if (intent.hasExtra(EXTRA_TITLE)) {
                intent.getStringExtra(EXTRA_TITLE).trim().run {
                    if (this != lastTitle && this != ""){
                        progressTask.text = this
                        progressLogTextView.append("\n$this\n")
                        lastTitle = this
                    }
                }
            }

            if (intent.hasExtra(EXTRA_SUBTASK))
                subTask.text = intent.getStringExtra(EXTRA_SUBTASK)

            if (intent.hasExtra(EXTRA_TASKLOG)){
                intent.getStringExtra(EXTRA_TASKLOG).run {
                    if (this != lastLog && this != ""){
                        progressLogTextView.append("$this\n")
                        lastLog = this
                    }
                }
            }

            if (intent.hasExtra(EXTRA_PROGRESS_TYPE)){

                val type = intent.getStringExtra(EXTRA_PROGRESS_TYPE)

                if (type == EXTRA_PROGRESS_TYPE_FINISHED) {

                    commonTools.tryIt { abortDialog?.dismiss() }
                    commonTools.tryIt { forceStopDialog?.dismiss() }

                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    closeWarning.visibility = View.GONE

                    if (intent.hasExtra(EXTRA_ERRORS))
                        errors.addAll(intent.getStringArrayListExtra(EXTRA_ERRORS))

                    if (errors.size > 0) {
                        reportLogButton.visibility = View.VISIBLE
                        errorLogTextView.visibility = View.VISIBLE
                        errors.forEach {
                            errorLogTextView.append("$it\n")
                        }
                    }

                    if (errors.size != 0 || intent.getBooleanExtra(EXTRA_IS_CANCELLED, false)) {
                        progressTask.setTextColor(resources.getColor(R.color.error_color))
                        subTask.setTextColor(resources.getColor(R.color.error_color))
                    }

                    intent.getLongExtra(EXTRA_TOTAL_TIME, -1).let {
                        if (it != -1L) subTask.text = calendarDifference(it)
                    }

                    progressAbortButton.visibility = View.GONE

                    progressActionButton.apply {
                        text = getString(R.string.finalize)
                        setOnClickListener {
                            startActivity(Intent(this@ProgressShowActivity, PostJobsActivity::class.java))
                        }
                    }
                }

                setImageIcon(intent, type)
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

    private val onPostJobsStart by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                wasPostJobStarted = true
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

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))
        commonTools.LBM?.registerReceiver(onPostJobsStart, IntentFilter(ACTION_POST_JOBS_STARTED))

        if (intent.extras != null) handleProgress(intent)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(EXTRA_POST_JOBS_STARTED, wasPostJobStarted)
        outState?.putAll(lastIntent.extras)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState?.run {
            if (getBoolean(EXTRA_POST_JOBS_STARTED, false)){
                if (AppInstance.sharedPrefs.getBoolean(PREF_IS_POST_JOBS_NEEDED, false))
                    startActivity(Intent(this@ProgressShowActivity, PostJobsActivity::class.java))
            }
            if (this.getString(EXTRA_PROGRESS_TYPE) != null)
                handleProgress(Intent().putExtras(this))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(onPostJobsStart) }
        commonTools.tryIt { abortDialog?.dismiss() }
        commonTools.tryIt { forceStopDialog?.dismiss() }
    }
}