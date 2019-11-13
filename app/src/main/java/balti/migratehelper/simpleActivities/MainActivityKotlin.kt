package balti.migratehelper.simpleActivities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.postJobs.PostJobsActivity
import balti.migratehelper.preferences.MainPreferencesActivity
import balti.migratehelper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_RESTORE_DATA
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_START_POST_JOBS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_POST_JOBS_ON_FINISH
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.LAST_SUPPORTED_ANDROID_API
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_ANDROID_VERSION_WARNING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IS_POST_JOBS_NEEDED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_USE_WATCHER
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_FILEPATH
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_HEAD
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.TG_LINK
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.last_log_report.view.*
import java.io.File

class MainActivityKotlin: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val main by lazy { AppInstance.sharedPrefs }
    private val editor by lazy { main.edit() }

    private val progressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@MainActivityKotlin, ProgressShowActivity::class.java)
                        .apply {
                            intent?.let {
                                this.putExtras(it)
                                this.action = it.action
                            }
                        }
                )
                commonTools.tryIt { commonTools.LBM?.unregisterReceiver(this) }
                finish()
            }
        }
    }

    private val endOnDisable by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) = finish()
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cpuAbi = Build.SUPPORTED_ABIS[0]

        if (cpuAbi == "armeabi-v7a" || cpuAbi == "arm64-v8a" || cpuAbi == "x86" || cpuAbi == "x86_64") {
            if (Build.VERSION.SDK_INT > LAST_SUPPORTED_ANDROID_API && !main.getBoolean(PREF_ANDROID_VERSION_WARNING, false)) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.too_fast)
                        .setMessage(R.string.too_fast_desc)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(R.string.dont_show_again) { _, _ ->
                            editor.putBoolean(PREF_ANDROID_VERSION_WARNING, true)
                            editor.commit()
                        }
                        .show()
            }
        }
        else {

            restoreButton.visibility = View.GONE

            AlertDialog.Builder(this)
                    .setTitle(R.string.unsupported_device)
                    .setMessage(getString(R.string.cpu_arch_is) + "\n" + cpuAbi + "\n\n" + getString(R.string.currently_supported_cpu))
                    .setNegativeButton(R.string.close) { _, _ ->
                        finish()
                    }
                    .setPositiveButton(R.string.contact) { _, _ ->
                        commonTools.openWebLink(TG_LINK)
                    }
                    .setNeutralButton(R.string.use_email_instead) {_, _ ->
                        val email = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("help.baltiapps@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Unsupported device")
                            putExtra(Intent.EXTRA_TEXT, commonTools.deviceSpecifications)
                        }
                        try {
                            startActivity(Intent.createChooser(email, getString(R.string.select_telegram)))
                        } catch (e: Exception) {
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setCancelable(false)
                    .show()
        }

        restoreButton.setOnClickListener {
            startActivity(Intent(this, RestoreSelectorKotlin::class.java))
        }

        uninstall_from_system.setOnClickListener {

            AppInstance.sharedPrefs.edit().putBoolean(PREF_IS_POST_JOBS_NEEDED, true).commit()

            val uIntent = Intent(this, PostJobsActivity::class.java)
                    .putExtra(EXTRA_POST_JOBS_ON_FINISH, false)

            if (commonTools.areWeDefaultSmsApp()) {
                AlertDialog.Builder(this)
                        .setMessage(R.string.default_sms_app_must_be_changed)
                        .setPositiveButton(R.string.proceed) {_, _ ->
                            startActivity(uIntent)
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
            else startActivity(uIntent)
        }

        last_logs_textView.apply {
            paintFlags = Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener { showLog() }
        }

        preferences_textView.apply {
            paintFlags = Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener { startActivity(Intent(this@MainActivityKotlin, MainPreferencesActivity::class.java)) }
        }

        watcher_disclaimer.apply {
            paintFlags = Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                AlertDialog.Builder(this@MainActivityKotlin)
                        .setMessage(R.string.watcher_extended_desc)
                        .setNeutralButton(R.string.close, null)
                        .show()
            }
        }

        close_button.setOnClickListener { finish() }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))
        commonTools.LBM?.registerReceiver(endOnDisable, IntentFilter(ACTION_END_ALL))

        commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_RESTORE_DATA))

        intent?.run {
            if (getBooleanExtra(EXTRA_DO_START_POST_JOBS, false))
                startActivity(Intent(this@MainActivityKotlin, PostJobsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        watcher_disclaimer.visibility = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P
                && main.getBoolean(PREF_USE_WATCHER, true))
            View.VISIBLE
        else View.GONE
    }

    private fun showLog(){
        val lView = View.inflate(this, R.layout.last_log_report, null)

        val ad = AlertDialog.Builder(this, R.style.DarkAlert)
                .setTitle(R.string.lastLog)
                .setIcon(R.drawable.ic_log)
                .setView(lView)
                .setNegativeButton(R.string.close, null)
                .create()

        lView.view_progress_log.setOnClickListener {
            val f = File(externalCacheDir, FILE_PROGRESSLOG)
            if (f.exists())
                startActivity(
                        Intent(this, SimpleLogViewer::class.java)                          /*kotlin*/
                                .putExtra(SIMPLE_LOG_VIEWER_HEAD, getString(R.string.progressLog))
                                .putExtra(SIMPLE_LOG_VIEWER_FILEPATH, f.absolutePath)
                )
            else Toast.makeText(this, R.string.progress_log_does_not_exist, Toast.LENGTH_SHORT).show()
        }

        lView.view_error_log.setOnClickListener {
            val f = File(externalCacheDir, FILE_ERRORLOG)
            if (f.exists())
                startActivity(
                        Intent(this, SimpleLogViewer::class.java)                          /*kotlin*/
                                .putExtra(SIMPLE_LOG_VIEWER_HEAD, getString(R.string.errorLog))
                                .putExtra(SIMPLE_LOG_VIEWER_FILEPATH, f.absolutePath)
                )
            else Toast.makeText(this, R.string.error_log_does_not_exist, Toast.LENGTH_SHORT).show()
        }

        lView.report_logs.setOnClickListener {
            commonTools.reportLogs(false)
            ad.dismiss()
        }

        ad.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(endOnDisable) }
    }

}