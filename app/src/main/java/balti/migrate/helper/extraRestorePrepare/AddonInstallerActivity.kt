package balti.migrate.helper.extraRestorePrepare

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import balti.migrate.helper.R
import balti.migrate.helper.extraRestorePrepare.utils.SmsCallsAddonInstall
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_ADDON_DO_ABORT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_INSTALL_SMS_CALLS_ADDON
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SMS_CALLS_ADDON_FILES
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SMS_CALLS_ADDON_OK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_ADDON_END
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_ADDON_INSTALL_SMS_CALLS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME
import balti.module.baltitoolbox.functions.Misc.isPackageInstalled
import balti.module.baltitoolbox.functions.Misc.showErrorDialog
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.install_addons.*

class AddonInstallerActivity: Activity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    private var SMS_CALLS_ADDON_OK = false
    private var DO_ABORT = false
    private var doInstallSmsCalls = false
    private val smsCallsFilePaths by lazy { ArrayList<String>(0) }

    private fun toggleSmsCallsProcessing() {
        addon_smsCalls_ok.visibility = View.GONE
        addon_smsCalls_not_ok.visibility = View.GONE
        addon_smsCalls_progressBar.visibility = View.VISIBLE
    }

    private fun toggleSmsCallsOk() {
        SMS_CALLS_ADDON_OK = true
        addon_smsCalls_ok.visibility = View.VISIBLE
        addon_smsCalls_not_ok.visibility = View.GONE
        addon_smsCalls_progressBar.visibility = View.GONE
        doFallThroughJob(JOBCODE_ADDON_END)
    }

    private fun toggleSmsCallsNotOk() {
        SMS_CALLS_ADDON_OK = false
        addon_smsCalls_ok.visibility = View.GONE
        addon_smsCalls_not_ok.visibility = View.VISIBLE
        addon_smsCalls_progressBar.visibility = View.GONE
        doFallThroughJob(JOBCODE_ADDON_END)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinishOnTouchOutside(false)

        doInstallSmsCalls = intent.getBooleanExtra(EXTRA_DO_INSTALL_SMS_CALLS_ADDON, false)
        tryIt { smsCallsFilePaths.addAll(intent.getStringArrayListExtra(EXTRA_SMS_CALLS_ADDON_FILES)) }

        setData()

        if (!doInstallSmsCalls) {
            Toast.makeText(this, R.string.no_addon_to_install, Toast.LENGTH_SHORT).show()
            finish()
        }
        else {

            setContentView(R.layout.install_addons)

            if (isAddonDialogNeeded()) {

                if (doInstallSmsCalls && !isPackageInstalled(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME))
                    addon_layout_smsCalls.visibility = View.VISIBLE
                else addon_layout_smsCalls.visibility = View.GONE

                addon_abort_button.setOnClickListener {
                    DO_ABORT = true
                    finishThis()
                }

                addon_install_button.setOnClickListener {
                    doFallThroughJob(JOBCODE_ADDON_INSTALL_SMS_CALLS)
                }
            }
            else {
                master_addon_installer_layout.visibility = View.GONE
                doFallThroughJob(JOBCODE_ADDON_INSTALL_SMS_CALLS)
            }
        }

    }

    private fun isAddonDialogNeeded(): Boolean {
        return (doInstallSmsCalls && !isPackageInstalled(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME))
    }

    private fun installSmsCalls() {

        toggleSmsCallsProcessing()

        var error = ""

        commonTools.doBackgroundTask({
            error = SmsCallsAddonInstall(this, smsCallsFilePaths,
                    !isPackageInstalled(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME))
                    .installAddon()
        }, {
            if (error != "") {

                // install failed
                showErrorDialog(error, getString(R.string.error), this, onCloseClick = {
                    toggleSmsCallsNotOk()
                })
            } else {
                toggleSmsCallsOk()     // install success.
            }
        })
    }

    private fun setData(){

        setResult(
                if((SMS_CALLS_ADDON_OK || !doInstallSmsCalls) && !DO_ABORT)
                    RESULT_OK else RESULT_CANCELED,
                Intent().apply {
                    putExtra(EXTRA_SMS_CALLS_ADDON_OK, SMS_CALLS_ADDON_OK)
                    putExtra(EXTRA_ADDON_DO_ABORT, DO_ABORT)
                }
        )
    }

    private fun finishThis(){
        setData()
        finish()
    }

    private fun doFallThroughJob(jobCode: Int) {

        var fallThrough = false

        fun doJob(jCode: Int, func: () -> Unit, doJob: Boolean) {

            if (fallThrough || jobCode == jCode) {

                fallThrough = if (doJob) {
                    func()
                    false
                }
                else true

            }
        }

        doJob(JOBCODE_ADDON_INSTALL_SMS_CALLS, { installSmsCalls() }, doInstallSmsCalls)
        doJob(JOBCODE_ADDON_END, { finishThis() }, true)
    }

    override fun onBackPressed() {
        Toast.makeText(this, R.string.press_abort_to_cancel, Toast.LENGTH_SHORT).show()
    }
}