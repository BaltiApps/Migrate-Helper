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
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import balti.migrate.helper.R
import balti.migrate.helper.emergencyRestore.EmergencyRestoreProgressShow
import balti.migrate.helper.emergencyRestore.EmergencyRestoreService
import balti.migrate.helper.postJobs.PostJobsActivity
import balti.migrate.helper.preferences.MainPreferencesActivity
import balti.migrate.helper.progressShow.ProgressShowActivity
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_RESTORE_DATA
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DIR_TWRP_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_START_POST_JOBS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.INFO_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.LAST_SUPPORTED_ANDROID_API
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_ANDROID_VERSION_WARNING
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_FILEPATH
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_HEAD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.TG_DEV_LINK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.TG_LINK
import balti.migrate.helper.utilities.ToolsNoContext
import balti.module.baltitoolbox.functions.FileHandlers.unpackAsset
import balti.module.baltitoolbox.functions.Misc.openWebLink
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.last_log_report.view.*
import java.io.*

class MainActivityKotlin: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    //private val main by lazy { AppInstance.sharedPrefs }
    //private val editor by lazy { main.edit() }

    private val fCode1 = 11
    private val fCode2 = 21

    private var filePermissionAsked = false
    private var filePermissionAsked2 = false

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
                tryIt { commonTools.LBM?.unregisterReceiver(this) }
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

        File(METADATA_HOLDER_DIR).mkdirs()
        File(INFO_HOLDER_DIR).mkdirs()

        if (cpuAbi == "armeabi-v7a" || cpuAbi == "arm64-v8a" || cpuAbi == "x86" || cpuAbi == "x86_64") {
            if (Build.VERSION.SDK_INT > LAST_SUPPORTED_ANDROID_API && !getPrefBoolean(PREF_ANDROID_VERSION_WARNING, false)) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.too_fast)
                        .setMessage(R.string.too_fast_desc)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(R.string.dont_show_again) { _, _ ->
                            putPrefBoolean(PREF_ANDROID_VERSION_WARNING, true)
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
                        openWebLink(TG_LINK)
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
            if (!EmergencyRestoreService.wasStarted)
                startActivity(Intent(this, RestoreSelectorKotlin::class.java))
            else Toast.makeText(this, R.string.emergency_restore_running, Toast.LENGTH_SHORT).show()
        }

        uninstall_from_system.setOnClickListener {

            val uIntent = Intent(this, PostJobsActivity::class.java)

            if (commonTools.isAddonDefaultSmsApp()) {
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
                    tryIt { ad.dismiss() }
                }

                zipHelperCache.setOnClickListener {
                    extractTWRPUninstall(fCode2)
                    tryIt { ad.dismiss() }
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

        contact_on_telegram.apply {
            setOnClickListener {
                AlertDialog.Builder(this@MainActivityKotlin).apply {
                    setMessage(R.string.contact_desc)
                    setPositiveButton(R.string.contact_group) { _, _ ->
                        openWebLink(TG_LINK)
                    }
                    setNegativeButton(R.string.contact_developer) { _, _ ->
                        openWebLink(TG_DEV_LINK)
                    }
                    setNeutralButton(android.R.string.cancel, null)
                }
                        .show()
            }
        }

        main_menu.setOnClickListener {
            val popupMenu = PopupMenu(this, main_menu)
            popupMenu.run {
                menuInflater.inflate(R.menu.main_menu, menu)
                setOnMenuItemClickListener {
                    when(it.itemId){
                        R.id.lastLog_menu -> showLog()
                        R.id.changelog_menu -> showChangelog()
                    }
                    true
                }
                show()
            }
        }

        emergency_restore.setOnClickListener {
            if (!RestoreServiceKotlin.isBackupInitiated) {
                val startIntent = Intent(this, EmergencyRestoreProgressShow::class.java)
                if (EmergencyRestoreService.wasStarted) startActivity(startIntent)
                else {
                    AlertDialog.Builder(this)
                            .setMessage(R.string.emergency_restore_desc)
                            .setPositiveButton(R.string.proceed) { _, _ ->
                                startActivity(startIntent)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                }
            }
            else Toast.makeText(this, R.string.restore_already_running, Toast.LENGTH_SHORT).show()
        }

        close_button.setOnClickListener { finish() }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))
        commonTools.LBM?.registerReceiver(endOnDisable, IntentFilter(ACTION_END_ALL))

        commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_RESTORE_DATA))

        intent?.run {
            if (getBooleanExtra(EXTRA_DO_START_POST_JOBS, false))
                startActivity(Intent(this@MainActivityKotlin, PostJobsActivity::class.java))
        }

        filePermissionAsked = false
        filePermissionAsked2 = false
    }

    private fun showChangelog(){
        val changelog = AlertDialog.Builder(this)

        val padding = 20

        val scrollView = ScrollView(this)
        scrollView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val allVersions = TextView(this)
        allVersions.setPadding(padding, padding, padding, padding)
        allVersions.text = ""
        allVersions.textSize = 15f

        scrollView.addView(allVersions)

        /*Add increasing versions here*/

        allVersions.append("\n" + getString(R.string.version_4_0_alpha) + "\n" + getString(R.string.version_4_0_alpha_content) + "\n")
        allVersions.append("\n" + getString(R.string.version_3_1_1) + "\n" + getString(R.string.version_3_1_1_content) + "\n")
        allVersions.append("\n" + getString(R.string.version_3_1) + "\n" + getString(R.string.version_3_1_content) + "\n")

        changelog.setTitle(R.string.changelog)
                .setView(scrollView)
                .setPositiveButton(R.string.close, null)
                .show()
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
            val f = File(INFO_HOLDER_DIR, FILE_PROGRESSLOG)
            if (f.exists())
                startActivity(
                        Intent(this, SimpleLogViewer::class.java)                          /*kotlin*/
                                .putExtra(SIMPLE_LOG_VIEWER_HEAD, getString(R.string.progressLog))
                                .putExtra(SIMPLE_LOG_VIEWER_FILEPATH, f.absolutePath)
                )
            else Toast.makeText(this, R.string.progress_log_does_not_exist, Toast.LENGTH_SHORT).show()
        }

        lView.view_error_log.setOnClickListener {
            val f = File(INFO_HOLDER_DIR, FILE_ERRORLOG)
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

        val fileName: String = if (fCode == fCode1)
            "twrp_helper_uninstall.zip"
        else "twrp_helper+cache_uninstall.zip"

        fun check(error: String){
            if (error == "")
                Toast.makeText(this, R.string.extracted_under, Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }

        fun work(moveFunction: (mtdFile: File) -> Unit) {
            File(METADATA_HOLDER_DIR, fileName).run {
                unpackAsset(fileName, this)
                    if (this.exists()) {
                        moveFunction(this)
                    } else Toast.makeText(this@MainActivityKotlin, "${getString(R.string.unpack_failed)}: ${this.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }

        if (isFilePermissionGranted()) {
            work { check(ToolsNoContext.moveFile(it, File(DIR_TWRP_UNINSTALL))) }
        }
        else if (!filePermissionAsked2) requestFilePermission(fCode)
        else {
            AlertDialog.Builder(this).apply {
                setMessage(R.string.use_root_to_unpack)
                setPositiveButton(android.R.string.ok){_, _ ->

                    var errors = ""

                    work { source ->

                        commonTools.doBackgroundTask({
                            try {
                                val destination = File(DIR_TWRP_UNINSTALL, source.name).absolutePath

                                val suProcess = Runtime.getRuntime().exec("su")

                                suProcess?.let {
                                    BufferedWriter(OutputStreamWriter(it.outputStream)).run {
                                        write("mv ${source.absolutePath} ${destination}\n")
                                        write("exit\n")
                                        flush()
                                    }

                                    val errorReader = BufferedReader(InputStreamReader(it.errorStream))

                                    var line: String?
                                    do {
                                        errorReader.readLine().run {
                                            this?.trim()?.let { s -> if (s != "") errors += "$s\n" }
                                            line = this
                                        }
                                    } while (line != null)
                                }
                            }
                            catch (e: Exception) {
                                e.printStackTrace()
                                errors += e.message.toString()
                            }
                        }, {
                            check(errors)
                        })
                    }
                }
                setNegativeButton(android.R.string.cancel, null)
            }
                    .show()
        }
    }

    private fun isFilePermissionGranted() =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestFilePermission(fCode: Int) {
        if (filePermissionAsked) filePermissionAsked2 = true
        filePermissionAsked = true
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
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        tryIt { commonTools.LBM?.unregisterReceiver(endOnDisable) }
    }

}