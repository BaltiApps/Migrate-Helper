package balti.migrate.helper.extraRestorePrepare.utils

import android.Manifest
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOB_RESULT_DENIED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOB_RESULT_OK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOB_RESULT_TIMEOUT
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SMS_CALLS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_CALLS_GRANTED
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_SMS_GRANTED
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_CLASS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME

class CommunicatorAddon(val context: Context) {

    private val commonTools by lazy { CommonToolsKotlin(context) }
    private var isSmsCallsAddonDefaultSmsApp: Boolean? = null
    private var isSmsCallsAddonGrantedCallLogs: Boolean? = null

    private fun smsCallsSend(bundle: Bundle) {
        Intent().apply {
            component = ComponentName(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME, ADDON_SMS_CALLS_RECEIVER_CLASS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtras(bundle)
        }
    }

    private val smsCallsAddonReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) { intent?.run {
                commonTools.tryIt {
                    when (getStringExtra(ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE)){
                        ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION ->
                            isSmsCallsAddonDefaultSmsApp = getBooleanExtra(ADDON_SMS_CALLS_EXTRA_SMS_GRANTED, false)
                        ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION ->
                            isSmsCallsAddonGrantedCallLogs = getBooleanExtra(ADDON_SMS_CALLS_EXTRA_CALLS_GRANTED, false)
                    }
                }
            }}
        }
    }

    private val INTERVAL = 200L

    fun setSmsCallsAddonAsDefaultSmsApp(): Int {

        commonTools.LBM?.registerReceiver(smsCallsAddonReceiver, IntentFilter(ACTION_ADDON_SMS_CALLS_PERMISSION))
        isSmsCallsAddonDefaultSmsApp = null

        if (commonTools.getDefaultSmsApp() == ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME) isSmsCallsAddonDefaultSmsApp = true
        else {
            smsCallsSend(Bundle().apply {
                putString(ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE, ADDON_SMS_CALLS_EXTRA_OPERATION_GET_SMS_PERMISSION)
            })

            while (isSmsCallsAddonDefaultSmsApp == null) {
                Thread.sleep(INTERVAL)
            }
        }

        commonTools.LBM?.unregisterReceiver(smsCallsAddonReceiver)

        return when (isSmsCallsAddonDefaultSmsApp) {
            true -> JOB_RESULT_OK
            false -> JOB_RESULT_DENIED
            else -> JOB_RESULT_TIMEOUT
        }
    }

    fun grantWriteCallLogToSmsCallsAddon(): Int {

        commonTools.LBM?.registerReceiver(smsCallsAddonReceiver, IntentFilter(ACTION_ADDON_SMS_CALLS_PERMISSION))
        var isGranted = false
        isSmsCallsAddonGrantedCallLogs = null

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

        if (isGranted) isSmsCallsAddonGrantedCallLogs = true
        else {
            smsCallsSend(Bundle().apply {
                putString(ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE, ADDON_SMS_CALLS_EXTRA_OPERATION_GET_CALLS_PERMISSION)
            })

            while (isSmsCallsAddonGrantedCallLogs == null) {
                Thread.sleep(INTERVAL)
            }
        }

        commonTools.LBM?.unregisterReceiver(smsCallsAddonReceiver)

        return when (isSmsCallsAddonGrantedCallLogs) {
            true -> JOB_RESULT_OK
            false -> JOB_RESULT_DENIED
            else -> JOB_RESULT_TIMEOUT
        }
    }

}