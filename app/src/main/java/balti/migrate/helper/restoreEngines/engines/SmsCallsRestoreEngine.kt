package balti.migrate.helper.restoreEngines.engines

import android.content.*
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.helper.R
import balti.migrate.helper.restoreEngines.ParentRestoreClass
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreEngines.utils.OnRestoreComplete
import balti.migrate.helper.restoreSelectorActivity.containers.CallsPacketKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.SmsPacketKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_GENERIC_SMS_CALLS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRAS_MARKER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SMS_CALLS_RESTORE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_ALL_ERRORS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_FILE_NAMES
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_FINISHED_TITLE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_IS_CANCELLED
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_START_RESTORE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_PROGRESS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_PROPERLY_RESTORED_FILES
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_RESTORE_FINISHED
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE_CALLS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE_SMS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_RESTORING_FILE_NAME
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_EXTRA_TITLE
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_CLASS
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME
import java.io.File

class SmsCallsRestoreEngine(private val jobcode: Int,
                            private val smsPackets: ArrayList<SmsPacketKotlin>,
                            private val callsPackets: ArrayList<CallsPacketKotlin>): ParentRestoreClass(EXTRA_PROGRESS_TYPE_SMS) {


    private val errors by lazy { ArrayList<String>(0) }
    private val LBM by lazy { LocalBroadcastManager.getInstance(engineContext) }

    private var handler : Handler? = null
    private var runnable: Runnable? = null

    private val completeFileNames by lazy { ArrayList<String>(0) }

    private var lastType = 0

    private var exitWait = false
    private var alreadyFinished = false

    private val addonResultsReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {

                    val progress = it.getIntExtra(ADDON_SMS_CALLS_EXTRA_PROGRESS, -1)
                    val title = it.getStringExtra(ADDON_SMS_CALLS_EXTRA_TITLE) ?: engineContext.getString(R.string.restoring_smsCalls_by_addon)
                    val type = it.getIntExtra(ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE, 0)
                    var fileName = it.getStringExtra(ADDON_SMS_CALLS_EXTRA_RESTORING_FILE_NAME) ?: ""

                    if (it.getBooleanExtra(ADDON_SMS_CALLS_EXTRA_RESTORE_FINISHED, false)){
                        it.getStringArrayListExtra(ADDON_SMS_CALLS_EXTRA_ALL_ERRORS)?.let { e ->
                            errors.addAll(e)
                        }
                        if (it.getBooleanExtra(ADDON_SMS_CALLS_EXTRA_IS_CANCELLED, false)) {
                            broadcastProgress("", engineContext.getString(R.string.smsCalls_cancelled), false)
                        }
                        it.getStringArrayListExtra(ADDON_SMS_CALLS_EXTRA_PROPERLY_RESTORED_FILES)?.let { fileNames ->
                            completeFileNames.addAll(fileNames)
                        }
                        it.getStringExtra(ADDON_SMS_CALLS_EXTRA_FINISHED_TITLE)?.let { t ->
                            broadcastProgress("", t, false)
                        }
                        exitWait = true
                    }
                    else {
                        if (type != lastType){
                            lastType = type
                            fileName = ""
                            when (lastType){
                                ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE_SMS -> resetBroadcast(false, title, EXTRA_PROGRESS_TYPE_SMS)
                                ADDON_SMS_CALLS_EXTRA_RESTORE_TYPE_CALLS -> resetBroadcast(false, title, EXTRA_PROGRESS_TYPE_CALLS)
                            }
                        }
                    }
                    broadcastProgress(fileName, "", true, progress)
                }
            }
        }
    }

    private fun restoreData() {

        val fileNames = ArrayList<String>(0)
        for (p in smsPackets) if (p.isSelected) fileNames.add(p.smsDBFile.name)
        for (p in callsPackets) if (p.isSelected) fileNames.add(p.callDBFile.name)

        engineContext.startActivity(Intent().apply {
            component = ComponentName(ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME, ADDON_SMS_CALLS_RECEIVER_CLASS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ADDON_SMS_CALLS_EXTRA_OPERATION_TYPE, ADDON_SMS_CALLS_EXTRA_OPERATION_START_RESTORE)
            putStringArrayListExtra(ADDON_SMS_CALLS_EXTRA_FILE_NAMES, fileNames)
        })


        runnable = Runnable {
            handler?.run {
                if (exitWait || RestoreServiceKotlin.cancelAll) {
                    removeCallbacks(runnable)
                    finishJob()
                }
                else postDelayed(runnable, 500)
            }
        }

        handler = Handler(Looper.getMainLooper()).apply {
            post(runnable)
        }
    }

    init {
        customPreExecuteFunction = {
            LBM.registerReceiver(addonResultsReceiver, IntentFilter(ACTION_ADDON_SMS_CALLS_RESTORE))
        }
    }

    private fun finishJob(){
        if (!alreadyFinished) {

            alreadyFinished = true
            commonTools.tryIt { LBM.unregisterReceiver(addonResultsReceiver) }

            completeFileNames.forEach {
                File(METADATA_HOLDER_DIR, "$it.$EXTRAS_MARKER").createNewFile()    // mark for cleaning
            }

            (engineContext as OnRestoreComplete).run {
                onRestoreComplete(jobcode, errors.size == 0, errors)
            }
        }
    }

    override fun postExecuteFunction() {}

    override fun doInBackground(vararg params: Any?): Any {

        alreadyFinished = false
        resetBroadcast(false, engineContext.getString(R.string.init_smsCalls_addon))

        try { restoreData() }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("${ERROR_GENERIC_SMS_CALLS}: ${e.message}")
            finishJob()
        }

        return 0
    }
}