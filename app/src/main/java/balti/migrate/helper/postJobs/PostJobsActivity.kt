package balti.migrate.helper.postJobs

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REBOOT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REMOVE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SCAN_SYSTEM_APK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_SMS_APP
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IS_WIFI_RESTORED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_REMOUNT_ALL_TO_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_TEMPORARY_DISABLE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_WAS_CANCELLED
import balti.migrate.helper.utilities.StupidStartupServiceKotlin
import balti.migrate.helper.utilities.UninstallServiceKotlin
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_RESTORE_SMS_PERMISSION
import balti.module.baltitoolbox.functions.Misc.isPackageInstalled
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefString
import kotlinx.android.synthetic.main.post_restore_jobs.*

class PostJobsActivity: Activity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    //private val editor by lazy { sharedPrefs.edit() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.PostJobsActivityTheme)
        setContentView(R.layout.post_restore_jobs)

        window.setGravity(Gravity.BOTTOM)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        packagePath.text = applicationInfo.sourceDir.let {
            it.substring(0, it.lastIndexOf('/'))
        }

        pj_remountAll.apply {
            isChecked = getPrefBoolean(PREF_REMOUNT_ALL_TO_UNINSTALL, false)
            setOnCheckedChangeListener { _, isChecked ->
                putPrefBoolean(PREF_REMOUNT_ALL_TO_UNINSTALL, isChecked, true)
            }
        }

        step1()
    }

    private fun step1() {

        val defaultSmsAppPackage = if (commonTools.isAddonDefaultSmsApp())
            getPrefString(PREF_DEFAULT_SMS_APP, "").let {
                if (it == "" || it == packageName) "com.android.messaging"
                else if (isPackageInstalled(it)) it else "com.android.messaging"
            }
        else {
            putPrefString(PREF_DEFAULT_SMS_APP, "")
            ""
        }

        if (defaultSmsAppPackage != "") {

            // display only SMS reset layout
            pj_resetSmsAppLayout.visibility = View.VISIBLE
            pj_uninstallLayout.visibility = View.GONE

            pj_defaultSmsName.text = packageManager.let {
                try {
                    it.getApplicationLabel(it.getApplicationInfo(defaultSmsAppPackage, 0))
                } catch (e: Exception) {
                    Toast.makeText(this, "Err: ${e.message.toString()}", Toast.LENGTH_SHORT).show()
                    "NULL"
                }
            }

            pj_actionButton.apply {
                text = getString(R.string.change)
                setOnClickListener {
                    try {
                        startActivity(
                                AddonSmsCallsConstants.getSmsCallsIntent(
                                        Bundle(), ADDON_SMS_CALLS_EXTRA_OPERATION_RESTORE_SMS_PERMISSION, 0)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@PostJobsActivity, e.message.toString(), Toast.LENGTH_SHORT).show()
                    }
                    step2()
                }
            }
        }
        else step2()
    }

    private fun step2() {

        pj_resetSmsAppLayout.visibility = View.GONE
        pj_uninstallLayout.visibility = View.VISIBLE

        applicationInfo.sourceDir.startsWith("/system").let {
            pj_scanSystem.isChecked = !it
            if (!it) pj_scanSystem.visibility = View.VISIBLE
        }

        // check and disable reboot option if wifi was restored
        if (isWifiRestored()) {
            pj_reboot.isChecked = true
            pj_reboot.isEnabled = false
            pj_wifiRestoredRebootLabel.visibility = View.VISIBLE
        }
        else pj_wifiRestoredRebootLabel.visibility = View.GONE

        // If nothing is to be done, try to uncheck the reboot option
        // Also uncheck deleteCache
        // (because nothing is to be done)
        // Only uncheck reboot checkbox if wifi is not restored
        pj_doNothing.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isWifiRestored())
                    pj_reboot.isChecked = false
                pj_deleteCache.isChecked = false
            }
        }

        // If restoration was cancelled, Do nothing
        if (getPrefBoolean(PREF_WAS_CANCELLED, false)) {
            pj_doNothing.isChecked = true
        }

        pj_actionButton.apply {
            text = getString(R.string.finish)
            setOnClickListener {

                // clear startup notification, if still running
                stopService(Intent(this@PostJobsActivity, StupidStartupServiceKotlin::class.java))

                val finishIntent = Intent(this@PostJobsActivity, UninstallServiceKotlin::class.java).apply {
                    putExtra(EXTRA_DO_UNINSTALL, pj_uninstallRadio.isChecked)
                    putExtra(EXTRA_DO_REBOOT, pj_reboot.isChecked)
                    putExtra(EXTRA_DO_REMOVE_CACHE, pj_deleteCache.isChecked)
                    putExtra(EXTRA_SCAN_SYSTEM_APK, pj_scanSystem.isChecked)
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                    startForegroundService(finishIntent)
                else startService(finishIntent)

                // close all activities if "Do nothing" is not selected
                if (!pj_doNothing.isChecked) {

                    // disable the app if selected
                    if (pj_disableRadio.isChecked) putPrefBoolean(PREF_TEMPORARY_DISABLE, true)

                    // reset all data fed to this activity via sharedPreference
                    putPrefString(PREF_DEFAULT_SMS_APP, "")
                    putPrefBoolean(PREF_WAS_CANCELLED, false)
                    putPrefBoolean(PREF_IS_WIFI_RESTORED, false)

                }
                finishAffinity()
                commonTools.LBM?.sendBroadcast(Intent(ACTION_END_ALL))
            }
        }
    }

    private fun isWifiRestored(): Boolean{
        return getPrefBoolean(PREF_IS_WIFI_RESTORED, false)
    }
}