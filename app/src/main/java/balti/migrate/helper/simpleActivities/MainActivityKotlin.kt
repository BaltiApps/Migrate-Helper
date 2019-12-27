package balti.migrate.helper.simpleActivities

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import balti.migrate.helper.AppInstance
import balti.migrate.helper.R
import balti.migrate.helper.postJobs.PostJobsActivity
import balti.migrate.helper.preferences.MainPreferencesActivity
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.revert.RevertSettingsActivity
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_RESTORE_DATA
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DIR_TWRP_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_START_POST_JOBS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_POST_JOBS_ON_FINISH
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.LAST_SUPPORTED_ANDROID_API
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_ANDROID_VERSION_WARNING
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IS_POST_JOBS_NEEDED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_USE_WATCHER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_FILEPATH
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_HEAD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.TG_LINK
import balti.migrate.helper.utilities.ToolsNoContext
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.last_log_report.view.*
import java.io.File

class MainActivityKotlin: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val main by lazy { AppInstance.sharedPrefs }
    private val editor by lazy { main.edit() }

    private val fCode1 = 11
    private val fCode2 = 21

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

        revert_system_settings.setOnClickListener {
            startActivity(Intent(this, RevertSettingsActivity::class.java))
        }

        twrp_uninstall_textView.apply {
            paintFlags = Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {

                val layoutView = View.inflate(this@MainActivityKotlin, R.layout.twrp_unpack_layout, null)

                val ad = AlertDialog.Builder(this@MainActivityKotlin)
                        .setView(layoutView)
                        .setNegativeButton(R.string.close, null)
                        .create()

                val zipHelper = layoutView.findViewById<Button>(R.id.zip_helper_only)
                val zipHelperCache = layoutView.findViewById<Button>(R.id.zip_helper_cache)

                zipHelper.setOnClickListener {
                    extractTWRPUninstall(fCode1)
                    commonTools.tryIt { ad.dismiss() }
                }

                zipHelperCache.setOnClickListener {
                    extractTWRPUninstall(fCode2)
                    commonTools.tryIt { ad.dismiss() }
                }

                ad.show()
            }
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
        watcher_disclaimer.setText(
                if (commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME)) R.string.watcher_installed_disclaimer
                else R.string.watcher_disclaimer
        )
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

    private fun extractTWRPUninstall(fCode: Int) {

        if (isFilePermissionGranted()) {
            val fileName: String = if (fCode == fCode1)
                "twrp_helper_uninstall.zip"
            else "twrp_helper+cache_uninstall.zip"

            fileName.let {
                commonTools.unpackAssetToInternal(it, it, false)
                File(commonTools.workingDir, it).run {
                    if (this.exists()) {
                        ToolsNoContext.moveFile(this, File(DIR_TWRP_UNINSTALL)).run {
                            if (this == "")
                                Toast.makeText(this@MainActivityKotlin, R.string.extracted_under, Toast.LENGTH_SHORT).show()
                            else Toast.makeText(this@MainActivityKotlin, this, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        else requestFilePermission(fCode)
    }

    private fun isFilePermissionGranted() =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestFilePermission(fCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), fCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == fCode1 || requestCode == fCode2) {
            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                extractTWRPUninstall(requestCode)
            else Toast.makeText(this, R.string.storage_perm_needed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(endOnDisable) }
    }

}