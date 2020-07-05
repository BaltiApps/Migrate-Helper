package balti.migrate.helper.extraRestorePrepare.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import balti.migrate.helper.AppInstance
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_SMS_APP
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.TIMEOUT_ADDON_DELAY
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SMS_CALLS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_CALLS_GRANTED
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_MASTER_JOBCODE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_SMS_GRANTED
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME
import balti.module.baltitoolbox.functions.Misc.tryIt

class CommunicatorAddon(val context: Context) {

    private val commonTools by lazy { CommonToolsKotlin(context) }
    private val onPermissionAsked by lazy { context as OnPermissionAsked }
    private var lastRequestCode = 0

    private fun smsCallsSend(operation: String, masterJobCode: Int, bundle: Bundle = Bundle()) {

        Thread.sleep(TIMEOUT_ADDON_DELAY)
        context.startActivity(AddonSmsCallsConstants.getSmsCallsIntent(bundle, operation, masterJobCode))
    }

    private val smsCallsAddonReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) { intent?.run {
                tryIt {

                    val requestCode = getIntExtra(ADDON_SMS_CALLS_EXTRA_MASTER_JOBCODE, 0).let {
                        if (it != 0) it
                        else lastRequestCode
                    }

                    val result = when (getStringExtra(ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE)){
                        ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION -> getBooleanExtra(ADDON_SMS_CALLS_EXTRA_SMS_GRANTED, false)
                        ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION -> getBooleanExtra(ADDON_SMS_CALLS_EXTRA_CALLS_GRANTED, false)
                        else -> false
                    }

                    sendResult(requestCode, result)
                }
            }}
        }
    }

    fun setSmsCallsAddonAsDefaultSmsApp(requestCode: Int) {

        lastRequestCode = requestCode
        commonTools.LBM?.registerReceiver(smsCallsAddonReceiver, IntentFilter(ACTION_ADDON_SMS_CALLS_PERMISSION))

        if (commonTools.isAddonDefaultSmsApp()) sendResult(requestCode, true)
        else {
            AppInstance.sharedPrefs.edit().putString(PREF_DEFAULT_SMS_APP, commonTools.getDefaultSmsApp()).apply()
            smsCallsSend(ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION , requestCode)
        }
    }

    fun grantWriteCallLogToSmsCallsAddon(requestCode: Int) {

        lastRequestCode = requestCode
        commonTools.LBM?.registerReceiver(smsCallsAddonReceiver, IntentFilter(ACTION_ADDON_SMS_CALLS_PERMISSION))
        var isGranted = false

        context.packageManager.getPackageInfo(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME, PackageManager.GET_PERMISSIONS).let { pi ->
            pi.requestedPermissions?.run {
                for (i in indices){
                    if (this[i] == Manifest.permission.WRITE_CALL_LOG) {
                        isGranted = pi.requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
                        break
                    }
                }
            }
        }

        if (isGranted) sendResult(requestCode, true)
        else {
            smsCallsSend(ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION, requestCode)
        }
    }

    private fun sendResult(requestCode: Int, result: Boolean){
        tryIt { commonTools.LBM?.unregisterReceiver(smsCallsAddonReceiver) }
        onPermissionAsked.onPermissionAsked(requestCode, result)
    }

}