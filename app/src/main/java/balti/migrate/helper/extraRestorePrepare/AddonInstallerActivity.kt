package balti.migrate.helper.extraRestorePrepare

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import balti.migrate.helper.R
import balti.migrate.helper.extraRestorePrepare.utils.SettingsAddonInstall
import balti.migrate.helper.extraRestorePrepare.utils.SmsCallsAddonInstall
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_ADDON_DO_ABORT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_INSTALL_SETTINGS_ADDON
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_INSTALL_SMS_CALLS_ADDON
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SETTINGS_ADDON_OK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SMS_CALLS_ADDON_FILES
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SMS_CALLS_ADDON_OK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_ADDON_END
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_ADDON_INSTALL_SETTINGS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_ADDON_INSTALL_SMS_CALLS
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SETTINGS_SU
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_OPERATION_DUMMY_SU
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_SU_ERROR
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_SU_GRANTED
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_RECEIVER_CLASS
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_RECEIVER_PACKAGE_NAME
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME
import kotlinx.android.synthetic.main.install_addons.*

class AddonInstallerActivity: Activity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    private var SETTINGS_ADDON_OK = false
    private var SMS_CALLS_ADDON_OK = false
    private var DO_ABORT = false

    private var doInstallSmsCalls = false
    private var doInstallSettings = false
    private var settingsSuCancelled = false

    private var requestSuDialog: AlertDialog? = null

    private val smsCallsFilePaths by lazy { ArrayList<String>(0) }

    private val settingsSuReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.run {

                    commonTools.tryIt { requestSuDialog?.dismiss() }

                    if (!settingsSuCancelled) {

                        if (this.getBooleanExtra(ADDON_SETTINGS_EXTRA_SU_GRANTED, false)) {

                            toggleSettingsOk()

                        } else {
                            val err = intent.getStringExtra(ADDON_SETTINGS_EXTRA_SU_ERROR)

                            AlertDialog.Builder(this@AddonInstallerActivity).apply {

                                setTitle(R.string.grant_root_to_settings_addon)
                                setMessage((if (err != null) err + "\n\n" else "") + getString(R.string.grant_root_to_settings_addon))
                                setPositiveButton(R.string.abort) { _, _ ->
                                    DO_ABORT = true
                                    toggleSettingsNotOk()
                                }
                                setNegativeButton(R.string.skip_settings) { _, _ ->
                                    toggleSettingsNotOk()
                                }
                                setCancelable(false)
                            }
                                    .show()
                        }
                    }
                }
            }
        }
    }



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
        doFallThroughJob(JOBCODE_ADDON_INSTALL_SETTINGS)
    }

    private fun toggleSmsCallsNotOk() {
        SMS_CALLS_ADDON_OK = false
        addon_smsCalls_ok.visibility = View.GONE
        addon_smsCalls_not_ok.visibility = View.VISIBLE
        addon_smsCalls_progressBar.visibility = View.GONE
        doFallThroughJob(JOBCODE_ADDON_INSTALL_SETTINGS)
    }



    private fun toggleSettingsProcessing() {
        addon_settings_ok.visibility = View.GONE
        addon_settings_not_ok.visibility = View.GONE
        addon_settings_progressBar.visibility = View.VISIBLE
    }

    private fun toggleSettingsOk() {
        SETTINGS_ADDON_OK = true
        addon_settings_ok.visibility = View.VISIBLE
        addon_settings_not_ok.visibility = View.GONE
        addon_settings_progressBar.visibility = View.GONE
        doFallThroughJob(JOBCODE_ADDON_END)
    }

    private fun toggleSettingsNotOk() {
        SETTINGS_ADDON_OK = false
        addon_settings_ok.visibility = View.GONE
        addon_settings_not_ok.visibility = View.VISIBLE
        addon_settings_progressBar.visibility = View.GONE
        doFallThroughJob(JOBCODE_ADDON_END)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinishOnTouchOutside(false)

        doInstallSmsCalls = intent.getBooleanExtra(EXTRA_DO_INSTALL_SMS_CALLS_ADDON, false)
        doInstallSettings = intent.getBooleanExtra(EXTRA_DO_INSTALL_SETTINGS_ADDON, false)
        commonTools.tryIt { smsCallsFilePaths.addAll(intent.getStringArrayListExtra(EXTRA_SMS_CALLS_ADDON_FILES)) }

        setData()

        if (!doInstallSettings && !doInstallSmsCalls) {
            Toast.makeText(this, R.string.no_addon_to_install, Toast.LENGTH_SHORT).show()
            finish()
        }
        else {

            setContentView(R.layout.install_addons)

            if (isAddonDialogNeeded()) {

                if (doInstallSmsCalls && !commonTools.isPackageInstalled(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME))
                    addon_layout_smsCalls.visibility = View.VISIBLE
                else addon_layout_smsCalls.visibility = View.GONE

                if (doInstallSettings && !commonTools.isPackageInstalled(ADDON_SETTINGS_RECEIVER_PACKAGE_NAME))
                    addon_layout_settings.visibility = View.VISIBLE
                else addon_layout_settings.visibility = View.GONE

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

        commonTools.LBM?.registerReceiver(settingsSuReceiver, IntentFilter(ACTION_ADDON_SETTINGS_SU))
    }

    private fun isAddonDialogNeeded(): Boolean {
        return (doInstallSmsCalls && !commonTools.isPackageInstalled(ADDON_SETTINGS_RECEIVER_PACKAGE_NAME)) ||
                (doInstallSettings && !commonTools.isPackageInstalled(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME))
    }

    private fun installSettings() {

        fun askSu() {

            commonTools.tryIt {
                requestSuDialog = AlertDialog.Builder(this).apply {
                    setView(View.inflate(this@AddonInstallerActivity, R.layout.install_addon_settings_su_request, null))
                    setNegativeButton(android.R.string.cancel) { _, _ ->
                        settingsSuCancelled = true
                    }
                    setNeutralButton(R.string.abort) { _, _ ->
                        DO_ABORT = true
                    }
                    setCancelable(false)
                }.create().apply {
                    setOnDismissListener {
                        if (DO_ABORT || settingsSuCancelled) {
                            toggleSettingsNotOk()
                        }
                    }
                }

                requestSuDialog?.show()

                startActivity(Intent().apply {
                    component = ComponentName(ADDON_SETTINGS_RECEIVER_PACKAGE_NAME, ADDON_SETTINGS_RECEIVER_CLASS)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(ADDON_SETTINGS_EXTRA_OPERATION_DUMMY_SU, true)
                })
            }
        }

        toggleSettingsProcessing()

        // if settings addon installed, just ask for root
        if (commonTools.isPackageInstalled(ADDON_SETTINGS_RECEIVER_PACKAGE_NAME)) {

            askSu()

        } else {

            // else, start installation

            var error = ""

            commonTools.doBackgroundTask({
                error = SettingsAddonInstall(this).installAddon()
            }, {
                if (error != "") {

                    // install failed
                    commonTools.showErrorDialog(error, getString(R.string.error), true, closeFunc = {

                        // if settings addon is not present, deny all settings restore, continue. Else ask for su
                        if (!commonTools.isPackageInstalled(ADDON_SETTINGS_RECEIVER_PACKAGE_NAME)) {
                            toggleSettingsNotOk()
                        } else askSu()

                    })
                } else {
                    askSu()     // install success. ask for su permission from addon
                }
            })
        }
    }

    private fun installSmsCalls() {

        toggleSmsCallsProcessing()

        // if settings addon installed, continue to settings addon
        if (commonTools.isPackageInstalled(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME)) {
            toggleSmsCallsOk()
        }
        else {

            // else, start installation

            var error = ""

            commonTools.doBackgroundTask({
                error = SmsCallsAddonInstall(this, smsCallsFilePaths).installAddon()
            }, {
                if (error != "") {

                    // install failed
                    commonTools.showErrorDialog(error, getString(R.string.error), true, closeFunc = {

                        // if settings addon is not present, deny all settings restore, continue. Else ask for su
                        if (!commonTools.isPackageInstalled(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME)) {
                            toggleSmsCallsNotOk()
                        } else toggleSmsCallsOk()

                    })
                } else {
                    toggleSmsCallsOk()     // install success.
                }
            })

        }
    }

    private fun setData(){

        SETTINGS_ADDON_OK = SETTINGS_ADDON_OK && !settingsSuCancelled

        setResult(
                if((SETTINGS_ADDON_OK || !doInstallSettings) &&
                        (SMS_CALLS_ADDON_OK || !doInstallSmsCalls) &&
                        !DO_ABORT)
                    RESULT_OK else RESULT_CANCELED,
                Intent().apply {
                    putExtra(EXTRA_SETTINGS_ADDON_OK, SETTINGS_ADDON_OK)
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
        doJob(JOBCODE_ADDON_INSTALL_SETTINGS, { installSettings() }, doInstallSettings)
        doJob(JOBCODE_ADDON_END, { finishThis() }, true)
    }

    override fun onBackPressed() {
        Toast.makeText(this, R.string.press_abort_to_cancel, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.LBM?.unregisterReceiver(settingsSuReceiver)
    }
}