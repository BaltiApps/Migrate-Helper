package balti.migratehelper.restoreEngines

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import balti.migratehelper.AppInstance
import balti.migratehelper.AppInstance.Companion.appPackets
import balti.migratehelper.AppInstance.Companion.callsDataPackets
import balti.migratehelper.AppInstance.Companion.settingsPacket
import balti.migratehelper.AppInstance.Companion.smsDataPackets
import balti.migratehelper.AppInstance.Companion.wifiPacket
import balti.migratehelper.R
import balti.migratehelper.restoreEngines.engines.*
import balti.migratehelper.restoreEngines.utils.OnRestoreComplete
import balti.migratehelper.restoreSelectorActivity.containers.*
import balti.migratehelper.simpleActivities.ProgressShowActivity
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_RESTORE_DATA
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_ABORT
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ALL_SUPPRESSED_ERRORS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.CHANNEL_RESTORE_ABORTING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.CHANNEL_RESTORE_END
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.CHANNEL_RESTORE_RUNNING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_RESTORE_SERVICE_ERROR
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_NOTIFICATION_FIX
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FINISHED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_TASKLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_TITLE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_APP
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_CALLS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_END
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_SETTINGS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_SMS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_WIFI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_CANCELLING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_FINISHED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_ONGOING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PENDING_INTENT_REQUEST_ID
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.TIMEOUT_WAITING_TO_CANCEL_TASK
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.collections.ArrayList

class RestoreServiceKotlin: Service(), OnRestoreComplete {

    companion object {

        lateinit var serviceContext: Context
        private set

        var cancelAll = false
        private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private var isBackupInitiated = false
    private val commonTools by lazy { CommonToolsKotlin(this) }

    private var progressWriter: BufferedWriter? = null
    private var errorWriter: BufferedWriter? = null

    private var lastTitle = ""
    private var lastLog = ""
    private var lastDeterminateProgress = 0

    private val allErrors by lazy { ArrayList<String>(0) }
    private val criticalErrors by lazy { ArrayList<String>(0) }

    private var startTime = 0L
    private var endTime = 0L

    private var currentTask: ParentRestoreClass? = null

    private var notificationFix = false
    private val isSettingsNull : Boolean
        get() = if (settingsPacket == null) true else settingsPacket!!.internalPackets.isEmpty()


    private val toReturnIntent by lazy { Intent(ACTION_RESTORE_PROGRESS) }

    private val cancellingNotification by lazy {
        NotificationCompat.Builder(this, CHANNEL_RESTORE_ABORTING)
                .setContentTitle(getString(R.string.cancelling))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .build()
    }

    private fun addError(error: String, addToCriticalWithoutChecking: Boolean = true){
        allErrors.add(error)
        if (!addToCriticalWithoutChecking) {
            var isCritical = true
            for (it in ALL_SUPPRESSED_ERRORS) {
                if (error.startsWith(it)) {
                    isCritical = false
                    break
                }
            }
            if (isCritical) criticalErrors.add(error)
        }
        else criticalErrors.add(error)
    }

    private fun addError(errors: ArrayList<String>, addToCriticalWithoutChecking: Boolean = true){
        errors.forEach {
            addError(it, addToCriticalWithoutChecking)
        }
    }

    private val cancelReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                commonTools.tryIt {

                    cancelAll = true

                    commonTools.LBM?.sendBroadcast(Intent(ACTION_RESTORE_PROGRESS)
                            .apply {
                                putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL)
                                putExtra(EXTRA_TITLE, getString(R.string.cancelling))
                            }
                    )
                    AppInstance.notificationManager.notify(NOTIFICATION_ID_CANCELLING, cancellingNotification)

                    commonTools.doBackgroundTask({

                        currentTask?.let {
                            while (it.status != AsyncTask.Status.FINISHED) {
                                commonTools.tryIt { Thread.sleep(100) }
                            }
                            commonTools.tryIt { Thread.sleep(TIMEOUT_WAITING_TO_CANCEL_TASK) }
                        }

                    }, {
                        restoreFinished(getString(R.string.restoreCancelled))
                    })
                }
            }
        }
    }

    private val progressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {

                if (intent == null || !intent.hasExtra(EXTRA_PROGRESS_TYPE) || !intent.hasExtra(EXTRA_TITLE)) return

                toReturnIntent.putExtras(intent)

                intent.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, -1).run {
                    if (this != -1) lastDeterminateProgress = this
                }

                val type = intent.getStringExtra(EXTRA_PROGRESS_TYPE)
                if (type in arrayOf(EXTRA_PROGRESS_TYPE_SMS, EXTRA_PROGRESS_TYPE_CALLS))
                    return

                intent.getStringExtra(EXTRA_TITLE).trim().run {
                    if (this != lastTitle) {
                        progressWriter?.write("\n$this\n")
                        lastTitle = this
                    }
                }

                if (intent.hasExtra(EXTRA_TASKLOG)){
                    intent.getStringExtra(EXTRA_TASKLOG).run {
                        if (this != lastLog) {
                            progressWriter?.write("$this\n")
                            lastLog = this
                        }
                    }
                }

            }
        }
    }

    private val requestProgressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                toReturnIntent.action = ACTION_RESTORE_PROGRESS
                commonTools.LBM?.sendBroadcast(toReturnIntent)
            }
        }
    }

    private fun startBackup(){
        startTime = timeInMillis()

        isBackupInitiated = true
        cancelAll = false

        AppInstance.notificationManager.cancelAll()

        doFallThroughJob(JOBCODE_RESTORE_SMS)
    }

    override fun onCreate() {
        super.onCreate()

        serviceContext = this

        val loadingNotification = NotificationCompat.Builder(this, CHANNEL_RESTORE_RUNNING)
                .setContentTitle(getString(R.string.loading))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .build()

        commonTools.tryIt {
            progressWriter = BufferedWriter(FileWriter(File(externalCacheDir, FILE_PROGRESSLOG)))
            errorWriter = BufferedWriter(FileWriter(File(externalCacheDir, FILE_ERRORLOG)))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            commonTools.makeNotificationChannel(CHANNEL_RESTORE_RUNNING, CHANNEL_RESTORE_RUNNING, NotificationManager.IMPORTANCE_LOW)
            commonTools.makeNotificationChannel(CHANNEL_RESTORE_END, CHANNEL_RESTORE_END, NotificationManager.IMPORTANCE_HIGH)
            commonTools.makeNotificationChannel(CHANNEL_RESTORE_ABORTING, CHANNEL_RESTORE_ABORTING, NotificationManager.IMPORTANCE_MIN)
        }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))
        commonTools.LBM?.registerReceiver(cancelReceiver, IntentFilter(ACTION_RESTORE_ABORT))
        registerReceiver(cancelReceiver, IntentFilter(ACTION_RESTORE_ABORT))
        commonTools.LBM?.registerReceiver(requestProgressReceiver, IntentFilter(ACTION_REQUEST_RESTORE_DATA))

        startForeground(NOTIFICATION_ID_ONGOING, loadingNotification)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.run {
            try {
                if (!isBackupInitiated) {
                    notificationFix = intent.getBooleanExtra(EXTRA_NOTIFICATION_FIX, false)
                    startBackup()
                }
            }
            catch (e: Exception){
                e.printStackTrace()
                addError(e.message.toString())
                restoreFinished("${getString(R.string.errorStartingRestore)}: ${e.message}")
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun doFallThroughJob(jobCode: Int){

        var fallThrough = false

        fun doJob(jCode: Int, workingObject: Any?){

            if (!cancelAll && (fallThrough || jobCode == jCode)) {

                fallThrough = true

                workingObject?.let {

                    if (jCode == JOBCODE_RESTORE_END) {
                        restoreFinished("")
                        fallThrough = false
                    }
                    else {

                        currentTask = try {
                            when (jCode) {
                                JOBCODE_RESTORE_SMS -> SmsRestoreEngine(jCode, workingObject as ArrayList<SmsPacketKotlin>)
                                JOBCODE_RESTORE_CALLS -> CallsRestoreEngine(jCode, workingObject as ArrayList<CallsPacketKotlin>)
                                JOBCODE_RESTORE_WIFI -> WifiRestoreEngine(jCode, workingObject as WifiPacketKotlin)
                                JOBCODE_RESTORE_APP -> AppRestoreEngine(jCode, workingObject as ArrayList<AppPacketsKotlin>, notificationFix)
                                JOBCODE_RESTORE_SETTINGS -> SettingsRestoreEngine(jCode, workingObject as SettingsPacketKotlin)
                                else -> null
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            addError("$ERROR_RESTORE_SERVICE_ERROR: DO_JOB ${e.message}")
                            null
                        }

                        fallThrough = currentTask == null
                        currentTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }

                }
            }
        }

        doJob(JOBCODE_RESTORE_SMS, smsDataPackets.let { if (it.isNotEmpty()) it else null })
        doJob(JOBCODE_RESTORE_CALLS, callsDataPackets.let { if (it.isNotEmpty()) it else null })
        doJob(JOBCODE_RESTORE_WIFI, wifiPacket)

        doJob(JOBCODE_RESTORE_APP, appPackets)

        // restore all settings after restoring apps
        doJob(JOBCODE_RESTORE_SETTINGS, if (isSettingsNull) null else settingsPacket)

        doJob(JOBCODE_RESTORE_END, Any())
    }

    override fun onRestoreComplete(jobCode: Int, jobSuccess: Boolean, jobResults: ArrayList<String>?)  {

        when (jobCode) {

            JOBCODE_RESTORE_SMS -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                doFallThroughJob(JOBCODE_RESTORE_CALLS)
            }

            JOBCODE_RESTORE_CALLS -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                doFallThroughJob(JOBCODE_RESTORE_WIFI)
            }

            JOBCODE_RESTORE_WIFI -> {
                if (!jobSuccess) jobResults?.let { addError(it) }
                doFallThroughJob(JOBCODE_RESTORE_APP)
            }

            JOBCODE_RESTORE_APP -> {
                jobResults?.let { addError(it, false) }
                doFallThroughJob(JOBCODE_RESTORE_SETTINGS)
            }

            JOBCODE_RESTORE_SETTINGS -> {
                jobResults?.let { addError(it) }
                restoreFinished("")
            }
        }
    }

    private fun restoreFinished(errorTitle: String){

        val title = when {
            errorTitle != "" -> errorTitle
            criticalErrors.size != 0 -> getString(R.string.finished_with_errors)
            else -> getString(R.string.finished)
        }

        try {
            if (allErrors.size == 0 && errorTitle == "") {
                errorWriter?.write("--- No errors! ---\n")
            } else {
                if (errorTitle != "") errorWriter?.write("$errorTitle\n\n")
                for (e in allErrors) {
                    errorWriter?.write("$e\n")
                }
            }
            if (cancelAll) errorWriter?.write("--- Cancelled! ---\n")
            errorWriter?.write("--- Migrate version ${getString(R.string.current_version_name)} ---\n")
            progressWriter?.write("--- Migrate version ${getString(R.string.current_version_name)} ---\n")
        }
        catch (e: Exception){
            e.printStackTrace()
        }

        endTime = timeInMillis()

        val returnIntent = Intent(ACTION_RESTORE_PROGRESS)
                .apply {
                    putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_FINISHED)
                    putExtra(EXTRA_TITLE, title)
                    putStringArrayListExtra(EXTRA_ERRORS, criticalErrors)
                    putExtra(EXTRA_IS_CANCELLED, cancelAll)
                    putExtra(EXTRA_TOTAL_TIME, endTime - startTime)
                    putExtra(EXTRA_PROGRESS_PERCENTAGE, if (criticalErrors.size == 0 && !cancelAll) 100 else lastDeterminateProgress)
                }

        commonTools.LBM?.sendBroadcast(returnIntent)

        AppInstance.notificationManager.cancel(NOTIFICATION_ID_CANCELLING)

        AppInstance.notificationManager.notify(NOTIFICATION_ID_FINISHED,
                NotificationCompat.Builder(this, CHANNEL_RESTORE_END)
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentIntent(
                                PendingIntent.getActivity(serviceContext, PENDING_INTENT_REQUEST_ID,
                                        Intent(this, ProgressShowActivity::class.java).putExtras(returnIntent),
                                        PendingIntent.FLAG_UPDATE_CURRENT))
                        .build())

        stopSelf()
    }

    private fun timeInMillis() = Calendar.getInstance().timeInMillis

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(cancelReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(requestProgressReceiver) }
        commonTools.tryIt { unregisterReceiver(cancelReceiver) }

        commonTools.tryIt { currentTask?.cancel(true) }

        commonTools.tryIt { progressWriter?.close() }
        commonTools.tryIt { errorWriter?.close() }
    }
}