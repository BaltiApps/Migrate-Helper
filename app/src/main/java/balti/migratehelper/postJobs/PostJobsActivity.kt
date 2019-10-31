package balti.migratehelper.postJobs

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.restartWatcher
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REBOOT
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_UNINSTALL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_DPI_VALUE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_POST_JOBS_ON_FINISH
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESET_SMS_APP
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_SMS_APP
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IS_POST_JOBS_NEEDED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_LAST_DPI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_TEMPORARY_DISABLE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_USE_WATCHER
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_WAS_CANCELLED
import balti.migratehelper.utilities.UninstallServiceKotlin
import kotlinx.android.synthetic.main.post_restore_jobs.*

class PostJobsActivity: AppCompatActivity() {

    companion object {
        var IS_ALIVE = false
    }

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val editor by lazy { AppInstance.sharedPrefs.edit() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_restore_jobs)

        IS_ALIVE = true

        window.setGravity(Gravity.BOTTOM)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        // this intent extra is used to mark if the activity is being called
        // after finishing restore jobs or simply for uninstalling the app.
        if (!intent.getBooleanExtra(EXTRA_POST_JOBS_ON_FINISH, true)) {
            post_jobs_reset_sms_app_desc.visibility = View.GONE
        }
        else {
            post_jobs_reset_sms_app_desc.visibility = View.VISIBLE
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
            post_jobs_reset_sms_app_android10.visibility = View.VISIBLE

        execute()

        restartWatcher(this)
    }

    private fun execute() {

        val smsAppPakageName = if (commonTools.areWeDefaultSmsApp())
            AppInstance.sharedPrefs.getString(PREF_DEFAULT_SMS_APP, "").let {
                if (it == null || it == "" || it == packageName) "com.android.messaging"
                else if (commonTools.isPackageInstalled(it)) it else "com.android.messaging"
            }
        else {
            editor.putString(PREF_DEFAULT_SMS_APP, "").commit()
            ""
        }

        if (smsAppPakageName != "") {

            // display only SMS reset layout
            post_jobs_reset_sms_app_layout.visibility = View.VISIBLE
            post_jobs_change_dpi_layout.visibility = View.GONE
            post_jobs_uninstall_layout.visibility = View.GONE

            post_jobs_default_sms_name.text = packageManager.let { it.getApplicationLabel(it.getApplicationInfo(smsAppPakageName, 0)) }

            fun android10ManualSet(){
                post_jobs_reset_sms_app_android10.setText(R.string.set_manually_desc)
                post_jobs_action_button.apply {
                    text = getString(R.string.set_manually)
                    setOnClickListener {
                        try {
                            startActivity(packageManager.getLaunchIntentForPackage(smsAppPakageName))
                        } catch (e: Exception){
                            e.printStackTrace()
                            Toast.makeText(this@PostJobsActivity, e.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // on pressing NEXT, change SMS app
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {

                if (AppInstance.sharedPrefs.getBoolean(PREF_USE_WATCHER, true)){
                    if(commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME)){

                        post_jobs_reset_sms_app_android10.setText(R.string.android_10_sms_message)

                        post_jobs_action_button.apply {
                            text = getString(R.string.next)
                            setOnClickListener {
                                restartWatcher(this@PostJobsActivity, true,
                                        if (intent.hasExtra(EXTRA_PROGRESS_TYPE)) intent
                                        else null
                                )
                                commonTools.setDefaultSms(smsAppPakageName, JOBCODE_RESET_SMS_APP)
                            }
                        }
                    }
                    else {
                        AppInstance.sharedPrefs.edit().putBoolean(PREF_USE_WATCHER, false).apply()
                        android10ManualSet()
                    }
                }
                else android10ManualSet()


            } else {
                post_jobs_action_button.apply {
                    text = getString(R.string.next)
                    setOnClickListener {
                        commonTools.setDefaultSms(smsAppPakageName, JOBCODE_RESET_SMS_APP)
                    }
                }
            }

        } else {

            // hide SMS layout and display uninstall layout
            post_jobs_reset_sms_app_layout.visibility = View.GONE
            post_jobs_uninstall_layout.visibility = View.VISIBLE

            val dpiInt = AppInstance.sharedPrefs.getInt(PREF_LAST_DPI, -1)

            if (dpiInt > 0) {

                // if DPI is present, then display DPI layout
                post_jobs_change_dpi_layout.visibility = View.VISIBLE
                post_jobs_change_dpi_value.text = "${getString(R.string.display_density)} $dpiInt"

                // if DPI checkbox is checked, check reboot checkbox and disable it
                do_change_dpi_checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) post_jobs_reboot.isChecked = true
                    post_jobs_reboot.isEnabled = !isChecked
                }

                // if DPI layout is clicked, toggle DPI checkbox state
                post_jobs_change_dpi_layout.setOnClickListener {
                    do_change_dpi_checkbox.run { this.isChecked = !this.isChecked }
                }

                // by default set DPI checked
                do_change_dpi_checkbox.isChecked = true
            }
            else {
                // no DPI data. uncheck checkbox.
                do_change_dpi_checkbox.isChecked = false
            }

            // If nothing is to be done, try to uncheck the reboot option
            // (because nothing is to be done)
            // Only uncheck reboot checkbox if DPI checkbox is unchecked
            post_jobs_do_nothing.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !do_change_dpi_checkbox.isChecked)
                    post_jobs_reboot.isChecked = false
            }

            // If restoration was cancelled, don't restore DPI
            // "Do nothing"
            if (AppInstance.sharedPrefs.getBoolean(PREF_WAS_CANCELLED, false)) {
                post_jobs_do_nothing.isChecked = true
                do_change_dpi_checkbox.isChecked = false
            }

            post_jobs_action_button.apply {
                text = getString(R.string.finish)
                setOnClickListener {

                    // send intent to uninstall service
                    val finishIntent = Intent(this@PostJobsActivity, UninstallServiceKotlin::class.java).apply {
                        if (do_change_dpi_checkbox.isChecked) putExtra(EXTRA_DPI_VALUE, dpiInt)
                        putExtra(EXTRA_DO_UNINSTALL, post_jobs_uninstall_radio.isChecked)
                        putExtra(EXTRA_DO_REBOOT, post_jobs_reboot.isChecked)
                    }

                    editor.run {

                        // disable the app if selected
                        if (post_jobs_disable_radio.isChecked) putBoolean(PREF_TEMPORARY_DISABLE, true)

                        // reset all data fed to this activity via sharedPreference
                        putString(PREF_DEFAULT_SMS_APP, "")
                        putInt(PREF_LAST_DPI, -1)
                        putBoolean(PREF_WAS_CANCELLED, false)
                        putBoolean(PREF_IS_POST_JOBS_NEEDED, false)

                        commit()
                    }

                    // start the uninstall service
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                        startForegroundService(finishIntent)
                    else startService(finishIntent)

                    // close all activities if "Do nothing" is not selected
                    if (!post_jobs_do_nothing.isChecked) {
                        commonTools.LBM?.sendBroadcast(Intent(ACTION_END_ALL))
                        finishAffinity()
                    }
                    else finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JOBCODE_RESET_SMS_APP) {
            if (resultCode != RESULT_OK) restartWatcher(this)
            execute()
        }
    }

    override fun onDestroy() {
        restartWatcher(this)
        super.onDestroy()
    }
}