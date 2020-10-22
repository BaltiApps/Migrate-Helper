package balti.migrate.helper.emergencyRestore

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import balti.migrate.helper.AppInstance
import balti.migrate.helper.AppInstance.Companion.emFailedAppInstalls
import balti.migrate.helper.AppInstance.Companion.emFailedCombined
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_EM_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_EM_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.CHANNEL_EMERGENCY_RESTORE_RUNNING
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_APPEND_LOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_LOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_SUBTASK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_EM_TITLE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.NOTIFICATION_ID_ONGOING_EM
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.THIS_VERSION
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class EmergencyRestoreService: Service() {
    companion object {
        lateinit var emergencyServiceContext: Context
    }
    override fun onBind(intent: Intent?): IBinder? = null

    private var progressWriter: BufferedWriter? = null
    private var errorWriter: BufferedWriter? = null

    private var lastTitle = ""
    private var appendLogs = false

    private val loadingNotification by lazy {
        NotificationCompat.Builder(this, CHANNEL_EMERGENCY_RESTORE_RUNNING)
                .setContentTitle(getString(R.string.loading))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(PendingIntent.getActivity(this, 90,
                        Intent(this, EmergencyRestoreProgressShow::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT)
                )
    }

    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val commonTools by lazy { CommonToolsKotlin(this) }

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.run {
                    EXTRA_EM_TITLE.let {
                        if (hasExtra(it)) getStringExtra(it).trim().let { t ->
                            if (t != lastTitle) {
                                lastTitle = t
                                loadingNotification.setContentTitle(t)
                                notificationManager.notify(NOTIFICATION_ID_ONGOING_EM, loadingNotification.build())
                            }
                        }
                    }
                    EXTRA_EM_SUBTASK.let { if (hasExtra(it)) progressWriter?.write(it + "\n\n") }
                    EXTRA_EM_LOG.let { if (hasExtra(it)) progressWriter?.write(it + "\n") }
                }
            }
        }
    }

    private val errorReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.run {
                    EXTRA_EM_ERRORS.let { if (hasExtra(it)) getStringArrayListExtra(it).forEach {
                        errorWriter?.write("$it\n")
                    } }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        emergencyServiceContext = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Misc.makeNotificationChannel(CHANNEL_EMERGENCY_RESTORE_RUNNING, CHANNEL_EMERGENCY_RESTORE_RUNNING, NotificationManager.IMPORTANCE_LOW)
        }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_EM_PROGRESS))
        commonTools.LBM?.registerReceiver(errorReceiver, IntentFilter(ACTION_EM_ERRORS))
        startForeground(NOTIFICATION_ID_ONGOING_EM, loadingNotification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appendLogs = intent?.getBooleanExtra(EXTRA_APPEND_LOG, false) ?: false
        File(CommonToolsKotlin.INFO_HOLDER_DIR).run {
            if (!appendLogs) deleteRecursively()
            mkdirs()
            progressWriter = BufferedWriter(FileWriter(File(this, CommonToolsKotlin.FILE_PROGRESSLOG), appendLogs))
            errorWriter = BufferedWriter(FileWriter(File(this, CommonToolsKotlin.FILE_ERRORLOG), appendLogs))
            if (appendLogs) {
                progressWriter?.write("\nAppending...\n\n")
                errorWriter?.write("\nAppending...\n\n")
            }
            progressWriter?.write("=====EMERGENCY RESTORE!!=====\n\n")
            errorWriter?.write("=====EMERGENCY RESTORE!!=====\n\n")
        }

        startRestore()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startRestore(){
        MainScope().launch {
            val emFailedAppData by lazy { ArrayList<String>(0) }
            if (!appendLogs) { // this means we are not in "retrying" state. this is first run.
                emFailedAppInstalls.clear()
                EmergencyAppInstall().executeWithResult().let {
                    tryIt {
                        emFailedAppInstalls.addAll(it as ArrayList<String>)
                    }
                }
            }
            EmergencyAppData().executeWithResult(AppInstance.notificationFixGlobal).let {
                tryIt {
                    emFailedAppData.addAll(it as ArrayList <String>)
                }
            }
            if (appendLogs) {
                emFailedAppInstalls.apply {
                    val temp = this.filter { pkg -> !Misc.isPackageInstalled(pkg) }
                    clear()
                    addAll(temp)
                }
            }

            emFailedCombined.clear()
            emFailedCombined.addAll((emFailedAppInstalls + emFailedAppData).distinct())

            withContext(IO) {
                errorWriter?.write("--- Migrate helper version ${getString(R.string.current_version_name)} ---\n")
                progressWriter?.write("--- Migrate helper version ${getString(R.string.current_version_name)} ---\n")
                progressWriter?.write("--- Build $THIS_VERSION ---\n")
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        tryIt { commonTools.LBM?.unregisterReceiver(errorReceiver) }
        tryIt { progressWriter?.close() }
        tryIt { errorWriter?.close() }
    }
}