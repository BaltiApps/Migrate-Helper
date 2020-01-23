package balti.migrate.helper.extraRestorePrepare.utils

import android.Manifest
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SMS_CALLS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_CALLS_GRANTED
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_MASTER_JOBCODE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_SMS_GRANTED
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_CLASS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME

class CommunicatorAddon(val context: Context) {

    private val commonTools by lazy { CommonToolsKotlin(context) }
    private val onPermissionAsked by lazy { context as OnPermissionAsked }
    private var lastRequestCode = 0

    private val GAP = 500L

    private fun smsCallsSend(bundle: Bundle, masterJobCode: Int) {
        context.startActivity(
            Intent().apply {
                component = ComponentName(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME, ADDON_SMS_CALLS_RECEIVER_CLASS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ADDON_SMS_CALLS_EXTRA_MASTER_JOBCODE, masterJobCode)
                putExtras(bundle)
            }
        )
    }

    private val smsCallsAddonReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) { intent?.run {
                commonTools.tryIt {

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

        Thread.sleep(GAP)
        lastRequestCode = requestCode
        commonTools.LBM?.registerReceiver(smsCallsAddonReceiver, IntentFilter(ACTION_ADDON_SMS_CALLS_PERMISSION))

        if (commonTools.getDefaultSmsApp() == ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME) sendResult(requestCode, true)
        else {
            smsCallsSend(Bundle().apply {
                putString(ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE, ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION)
            }, requestCode)
        }
    }

    fun grantWriteCallLogToSmsCallsAddon(requestCode: Int) {

        Thread.sleep(GAP)
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
            smsCallsSend(Bundle().apply {
                putString(ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE, ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION)
            }, requestCode)
        }
    }

    private fun sendResult(requestCode: Int, result: Boolean){
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(smsCallsAddonReceiver) }
        onPermissionAsked.onPermissionAsked(requestCode, result)
    }

}