package balti.migratehelper.postJobs.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import balti.migratehelper.postJobs.PostJobsActivity
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.EXTRA_ALIVE_CHECK
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.WATCHER_INTENT_RECEIVER
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME

class AliveCheckReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            context?.sendBroadcast(Intent(WATCHER_INTENT_RECEIVER)
                    .setPackage(WATCHER_PACKAGE_NAME)
                    .putExtra(EXTRA_ALIVE_CHECK, PostJobsActivity.IS_ALIVE)
            )
        } catch (_: Exception){}
    }
}