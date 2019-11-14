package balti.migrate.helper.postJobs.utils

import android.content.Context
import android.content.Intent
import android.os.Build

class RestartWatcherConstants {

    companion object {

        val WATCHER_PACKAGE_NAME = "balti.migrate.helper.watcher"
        val WATCHER_INTENT_DUMMY_ACTIVITY = "balti.migrate.helper.watcher.START"
        val WATCHER_INTENT_RECEIVER = "balti.migrate.helper.watcher.RECEIVER"

        val EXTRA_DO_START = "do_start"
        val EXTRA_ALIVE_CHECK = "alive_check"
        val EXTRA_PID = "pid"
        val EXTRA_OVERRIDE_IS_ALIVE = "override_is_alive"

        fun restartWatcher(context: Context, doStart: Boolean = false, intent: Intent? = null) {
            try {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                    context.startActivity(Intent(WATCHER_INTENT_DUMMY_ACTIVITY)
                            .putExtra(EXTRA_DO_START, doStart)
                            .putExtra(EXTRA_PID, android.os.Process.myPid())
                            .apply {
                                intent?.let { putExtras(intent) }
                            }
                    )
            } catch (_: Exception){}
        }
    }

}