package balti.migratehelper.extrasRestore

import android.app.PendingIntent
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Build
import android.support.v4.app.NotificationCompat
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.extrasRestore.utils.OnRestoreComplete
import balti.migratehelper.restoreEngines.RestoreServiceKotlin
import balti.migratehelper.restoreEngines.RestoreServiceKotlin.Companion.serviceContext
import balti.migratehelper.simpleActivities.ProgressShowActivity
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_ABORT
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_SUBTASK
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_TASKLOG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_TITLE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_ONGOING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PENDING_INTENT_REQUEST_ID
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PENDING_INTENT_RESTORE_ABORT_ID
import java.io.File

abstract class ParentRestoreClass(private val intentType: String): AsyncTask<Any, Any, Any>() {


    val engineContext by lazy { serviceContext }
    val sharedPreferences by lazy { AppInstance.sharedPrefs }

    val onRestoreComplete by lazy { engineContext as OnRestoreComplete }

    val commonTools by lazy { CommonToolsKotlin(engineContext) }

    private var lastProgress = 0
    private var isIndeterminate = true

    private val actualBroadcast by lazy {
        Intent(ACTION_RESTORE_PROGRESS).putExtra(EXTRA_PROGRESS_TYPE, intentType)
    }

    private val onGoingNotification by lazy {
        NotificationCompat.Builder(engineContext, CommonToolsKotlin.CHANNEL_RESTORE_RUNNING)
                .setContentTitle(engineContext.getString(R.string.loading))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .addAction(
                        NotificationCompat.Action(0, serviceContext.getString(android.R.string.cancel),
                                PendingIntent.getBroadcast(serviceContext, PENDING_INTENT_RESTORE_ABORT_ID,
                                        Intent(ACTION_RESTORE_ABORT), 0))
                )
    }

    private val activityIntent by lazy { Intent(serviceContext, ProgressShowActivity::class.java) }


    fun getDataBase(dataBaseFile: File): SQLiteDatabase {
        var dataBase: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(dataBaseFile.absolutePath, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            dataBase = SQLiteDatabase.openDatabase(dataBaseFile.absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READWRITE)
        return dataBase
    }

    private fun updateNotification(subTask: String, progressPercent: Int){

        actualBroadcast.let {

            if (it.hasExtra(EXTRA_TITLE)) {
                onGoingNotification.apply {
                    setContentTitle(it.getStringExtra(EXTRA_TITLE))
                    setContentText(subTask)
                    setProgress(100, progressPercent, progressPercent == -1)
                    setContentIntent(
                            PendingIntent.getActivity(serviceContext, PENDING_INTENT_REQUEST_ID,
                                    activityIntent.putExtras(actualBroadcast),
                                    PendingIntent.FLAG_UPDATE_CURRENT)
                    )
                }
                AppInstance.notificationManager.notify(NOTIFICATION_ID_ONGOING, onGoingNotification.build())
            }
        }
    }

    fun broadcastProgress(subTask: String, taskLog: String, showNotification: Boolean, progressPercent: Int = -1){

        if (RestoreServiceKotlin.cancelAll) return

        val progress = if (progressPercent == -1) (if (isIndeterminate) progressPercent else lastProgress) else progressPercent
        lastProgress = progress

        commonTools.LBM?.sendBroadcast(
                actualBroadcast.apply {
                    putExtra(EXTRA_SUBTASK, subTask)
                    putExtra(EXTRA_TASKLOG, taskLog)
                    putExtra(EXTRA_PROGRESS_PERCENTAGE, progress)
                }
        )

        if (showNotification)
            updateNotification(subTask, progress)
    }

    fun resetBroadcast(isIndeterminateProgress: Boolean, title: String, newIntentType: String = ""){

        this.isIndeterminate = isIndeterminateProgress
        val progress = if (!isIndeterminateProgress) 0 else -1

        actualBroadcast.apply {
            if (newIntentType != "") putExtra(EXTRA_PROGRESS_TYPE, newIntentType)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_SUBTASK, "")
            putExtra(EXTRA_TASKLOG, "")
            putExtra(EXTRA_PROGRESS_PERCENTAGE, progress)
        }

        commonTools.LBM?.sendBroadcast(actualBroadcast)
        updateNotification("", progress)

    }

    var customPreExecuteFunction: (() -> Unit)? = null

    abstract fun postExecuteFunction()

    override fun onPreExecute() {
        super.onPreExecute()
        customPreExecuteFunction?.invoke()
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        postExecuteFunction()
    }

}