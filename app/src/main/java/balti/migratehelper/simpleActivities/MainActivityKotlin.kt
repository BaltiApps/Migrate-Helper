package balti.migratehelper.simpleActivities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_RESTORE_DATA
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_ANDROID_VERSION_WARNING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_TEMPORARY_DISABLE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_FILEPATH
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_HEAD
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.TG_LINK
import balti.migratehelper.utilities.StupidStartupServiceKotlin
import balti.migratehelper.utilities.UninstallServiceKotlin
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cpuAbi = Build.SUPPORTED_ABIS[0]

        if (cpuAbi == "armeabi-v7a" || cpuAbi == "arm64-v8a" || cpuAbi == "x86" || cpuAbi == "x86_64") {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && !main.getBoolean(PREF_ANDROID_VERSION_WARNING, false)) {
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

            AlertDialog.Builder(this)
                    .setTitle(R.string.sure)
                    .setMessage(R.string.howToRestore)
                    .setPositiveButton(R.string.goAhead) { _, _ ->

                        val uninstallIntent = Intent(this, UninstallServiceKotlin::class.java)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(uninstallIntent)
                        else startService(uninstallIntent)

                        finishAffinity()
                    }
                    .setNegativeButton(getString(android.R.string.cancel), null)
                    .show()
        }

        temporary_disable.setOnClickListener {

            AlertDialog.Builder(this)
                    .setMessage(R.string.temporary_disable_desc)
                    .setPositiveButton(android.R.string.ok) { _, _ ->

                        stopService(Intent(this, StupidStartupServiceKotlin::class.java))
                        editor.putBoolean(PREF_TEMPORARY_DISABLE, true)
                        editor.commit()
                        sendBroadcast(Intent(ACTION_END_ALL))

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

        }

        last_logs_textView.apply {
            paintFlags = Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener { showLog() }
        }

        close_button.setOnClickListener { finish() }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))
        commonTools.LBM?.registerReceiver(endOnDisable, IntentFilter(ACTION_END_ALL))

        commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_RESTORE_DATA))
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
        commonTools.LBM?.unregisterReceiver(progressReceiver)
        commonTools.LBM?.unregisterReceiver(endOnDisable)
    }

}