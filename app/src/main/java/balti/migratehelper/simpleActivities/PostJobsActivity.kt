package balti.migratehelper.simpleActivities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_END_ALL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REBOOT
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_UNINSTALL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_DPI_VALUE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESET_SMS_APP
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_SMS_APP
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_LAST_DPI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_TEMPORARY_DISABLE
import balti.migratehelper.utilities.UninstallServiceKotlin
import kotlinx.android.synthetic.main.post_restore_jobs.*

class PostJobsActivity: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val editor by lazy { AppInstance.sharedPrefs.edit() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_restore_jobs)

        window.setGravity(Gravity.BOTTOM)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        setFinishOnTouchOutside(false)
        execute()
    }

    private fun execute() {

        val smsAppName = if (commonTools.areWeDefaultSmsApp())
            AppInstance.sharedPrefs.getString(PREF_DEFAULT_SMS_APP, "").let {
                if (it == "" || it == packageName) "com.android.messaging"
                else if (commonTools.isPackageInstalled(it)) it else "com.android.messaging"
            }
        else {
            editor.putString(PREF_DEFAULT_SMS_APP, "").commit()
            ""
        }

        if (smsAppName != "") {
            post_jobs_reset_sms_app_layout.visibility = View.VISIBLE
            post_jobs_change_dpi_layout.visibility = View.GONE
            post_jobs_uninstall_layout.visibility = View.GONE

            post_jobs_default_sms_name.text = packageManager.let { it.getApplicationLabel(it.getApplicationInfo(smsAppName, 0)) }

            post_jobs_action_button.apply {
                text = getString(R.string.next)
                setOnClickListener {
                    commonTools.setDefaultSms(smsAppName, JOBCODE_RESET_SMS_APP)
                }
            }
        } else {

            post_jobs_reset_sms_app_layout.visibility = View.GONE
            post_jobs_uninstall_layout.visibility = View.VISIBLE

            val dpiInt = AppInstance.sharedPrefs.getInt(PREF_LAST_DPI, -1)
            if (dpiInt > 0) {

                post_jobs_change_dpi_layout.visibility = View.VISIBLE

                post_jobs_change_dpi_value.text = "${getString(R.string.display_density)} $dpiInt"

                do_change_dpi_checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) post_jobs_reboot.isChecked = true
                    post_jobs_reboot.isEnabled = !isChecked
                }

                post_jobs_reset_sms_app_layout.setOnClickListener {
                    do_change_dpi_checkbox.run { this.isChecked = !this.isChecked }
                }
            }
            else do_change_dpi_checkbox.isChecked = false


            post_jobs_action_button.apply {
                text = getString(R.string.finish)
                setOnClickListener {

                    val finishIntent = Intent(this@PostJobsActivity, UninstallServiceKotlin::class.java).apply {
                        if (do_change_dpi_checkbox.isChecked) putExtra(EXTRA_DPI_VALUE, dpiInt)
                        putExtra(EXTRA_DO_UNINSTALL, post_jobs_uninstall_radio.isChecked)
                        putExtra(EXTRA_DO_REBOOT, post_jobs_reboot.isChecked)
                    }

                    if (post_jobs_disable_radio.isChecked) {
                        editor.putBoolean(PREF_TEMPORARY_DISABLE, true).commit()
                    }

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                        startForegroundService(finishIntent)
                    else startService(finishIntent)

                    commonTools.LBM?.sendBroadcast(Intent(ACTION_END_ALL))
                    finish()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == JOBCODE_RESET_SMS_APP) execute()
    }

}