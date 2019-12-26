package balti.migrate.helper.postJobs

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.helper.AppInstance
import balti.migrate.helper.AppInstance.Companion.sharedPrefs
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REBOOT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REMOVE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DPI_VALUE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_POST_JOBS_ON_FINISH
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESET_SMS_APP
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_SMS_APP
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IS_POST_JOBS_NEEDED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IS_WIFI_RESTORED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_LAST_DPI
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_REMOUNT_ALL_TO_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_TEMPORARY_DISABLE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_USE_WATCHER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_WAS_CANCELLED
import balti.migrate.helper.utilities.StupidStartupServiceKotlin
import balti.migrate.helper.utilities.UninstallServiceKotlin
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.restartWatcher
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
            pj_resetSmsAppDesc.visibility = View.GONE
        }
        else {
            pj_resetSmsAppDesc.visibility = View.VISIBLE
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
            pj_resetSmsAppAndroid10.visibility = View.VISIBLE

        packagePath.text = applicationInfo.sourceDir.let {
            it.substring(0, it.lastIndexOf('/'))
        }

        pj_remountAll.apply {
            isChecked = sharedPrefs.getBoolean(PREF_REMOUNT_ALL_TO_UNINSTALL, false)
            setOnCheckedChangeListener { _, isChecked ->
                sharedPrefs.edit().putBoolean(PREF_REMOUNT_ALL_TO_UNINSTALL, isChecked).commit()
            }
        }

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
            pj_resetSmsAppLayout.visibility = View.VISIBLE
            pj_changeDpiLayout.visibility = View.GONE
            pj_uninstallLayout.visibility = View.GONE

            pj_defaultSmsName.text = packageManager.let { it.getApplicationLabel(it.getApplicationInfo(smsAppPakageName, 0)) }

            fun android10ManualSet(){
                pj_resetSmsAppAndroid10.setText(R.string.set_manually_desc)
                pj_actionButton.apply {
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

                        pj_resetSmsAppAndroid10.setText(R.string.android_10_sms_message)

                        pj_actionButton.apply {
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
                    else android10ManualSet()
                }
                else android10ManualSet()


            } else {
                pj_actionButton.apply {
                    text = getString(R.string.next)
                    setOnClickListener {
                        commonTools.setDefaultSms(smsAppPakageName, JOBCODE_RESET_SMS_APP)
                    }
                }
            }

        } else {

            // hide SMS layout and display uninstall layout
            pj_resetSmsAppLayout.visibility = View.GONE
            pj_uninstallLayout.visibility = View.VISIBLE

            val dpiInt = AppInstance.sharedPrefs.getInt(PREF_LAST_DPI, -1)

            // check and disable reboot option if wifi was restored
            if (isWifiRestored()) {
                pj_reboot.isChecked = true
                pj_reboot.isEnabled = false
                pj_wifiRestoredRebootLabel.visibility = View.VISIBLE
            }
            else pj_wifiRestoredRebootLabel.visibility = View.GONE

            if (dpiInt > 0) {

                // if DPI is present, then display DPI layout
                pj_changeDpiLayout.visibility = View.VISIBLE
                pj_changeDpiValue.text = "${getString(R.string.display_density)} $dpiInt"

                // if DPI checkbox is checked, check reboot checkbox and disable it
                pj_doChangeDpiCheckbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) pj_reboot.isChecked = true
                    pj_reboot.isEnabled = if (isWifiRestored()) false else !isChecked
                }

                // if DPI layout is clicked, toggle DPI checkbox state
                pj_changeDpiLayout.setOnClickListener {
                    pj_doChangeDpiCheckbox.run { this.isChecked = !this.isChecked }
                }

                // by default set DPI checked
                pj_doChangeDpiCheckbox.isChecked = true
            }
            else {
                // no DPI data. uncheck checkbox.
                pj_doChangeDpiCheckbox.isChecked = false
            }

            // If nothing is to be done, try to uncheck the reboot option
            // (because nothing is to be done)
            // Only uncheck reboot checkbox if DPI checkbox is unchecked
            pj_doNothing.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !pj_doChangeDpiCheckbox.isChecked && !isWifiRestored())
                    pj_reboot.isChecked = false
            }

            // If restoration was cancelled, don't restore DPI
            // "Do nothing"
            if (AppInstance.sharedPrefs.getBoolean(PREF_WAS_CANCELLED, false)) {
                pj_doNothing.isChecked = true
                pj_doChangeDpiCheckbox.isChecked = false
            }

            pj_actionButton.apply {
                text = getString(R.string.finish)
                setOnClickListener {

                    // clear startup notification, if still running
                    stopService(Intent(this@PostJobsActivity, StupidStartupServiceKotlin::class.java))

                    // send intent to uninstall service
                    val finishIntent = Intent(this@PostJobsActivity, UninstallServiceKotlin::class.java).apply {
                        if (pj_doChangeDpiCheckbox.isChecked) putExtra(EXTRA_DPI_VALUE, dpiInt)
                        putExtra(EXTRA_DO_UNINSTALL, pj_uninstallRadio.isChecked)
                        putExtra(EXTRA_DO_REBOOT, pj_reboot.isChecked)
                        putExtra(EXTRA_DO_REMOVE_CACHE, pj_uninstallRadio.isChecked && pj_deleteCache.isChecked)
                    }

                    // start the uninstall service
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                        startForegroundService(finishIntent)
                    else startService(finishIntent)

                    // close all activities if "Do nothing" is not selected
                    if (!pj_doNothing.isChecked) {

                        editor.run {

                            // disable the app if selected
                            if (pj_disableRadio.isChecked) putBoolean(PREF_TEMPORARY_DISABLE, true)

                            // reset all data fed to this activity via sharedPreference
                            putString(PREF_DEFAULT_SMS_APP, "")
                            putInt(PREF_LAST_DPI, -1)
                            putBoolean(PREF_WAS_CANCELLED, false)
                            putBoolean(PREF_IS_POST_JOBS_NEEDED, false)
                            putBoolean(PREF_IS_WIFI_RESTORED, false)

                            commit()
                        }
                    }
                    finishAffinity()
                    commonTools.LBM?.sendBroadcast(Intent(ACTION_END_ALL))
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

    private fun isWifiRestored(): Boolean{
        return AppInstance.sharedPrefs.getBoolean(PREF_IS_WIFI_RESTORED, false)
    }

    override fun onDestroy() {
        restartWatcher(this)
        IS_ALIVE = false
        super.onDestroy()
    }
}