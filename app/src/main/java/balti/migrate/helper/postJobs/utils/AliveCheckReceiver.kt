package balti.migrate.helper.postJobs.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import balti.migrate.helper.postJobs.PostJobsActivity
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.EXTRA_ALIVE_CHECK
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.EXTRA_PID
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.WATCHER_INTENT_RECEIVER
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME

class AliveCheckReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            context?.sendBroadcast(Intent(WATCHER_INTENT_RECEIVER)
                    .setPackage(WATCHER_PACKAGE_NAME)
                    .putExtra(EXTRA_ALIVE_CHECK, PostJobsActivity.IS_ALIVE)
                    .putExtra(EXTRA_PID, android.os.Process.myPid())
            )
        } catch (_: Exception){}
    }
}