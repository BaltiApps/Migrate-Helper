package balti.migrate.helper.postJobs.utils

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.helper.AppInstance
import balti.migrate.helper.postJobs.PostJobsActivity
import balti.migrate.helper.simpleActivities.MainActivityKotlin
import balti.migrate.helper.simpleActivities.ProgressShowActivity
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_START_POST_JOBS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IS_POST_JOBS_NEEDED
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.EXTRA_OVERRIDE_IS_ALIVE

class ResurrectorActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppInstance.sharedPrefs.getBoolean(PREF_IS_POST_JOBS_NEEDED, false)) {
            if (!PostJobsActivity.IS_ALIVE || intent.getBooleanExtra(EXTRA_OVERRIDE_IS_ALIVE, false))
                startActivity(
                        if (intent.hasExtra(EXTRA_PROGRESS_TYPE)) {
                            Log.d(CommonToolsKotlin.DEBUG_TAG, "resurrecting ProgressShowActivity")
                            Intent(this, ProgressShowActivity::class.java)
                                    .putExtras(intent)
                                    .putExtra(EXTRA_DO_START_POST_JOBS, true)
                        } else {
                            Log.d(CommonToolsKotlin.DEBUG_TAG, "resurrecting MainActivityKotlin")
                            Intent(this, MainActivityKotlin::class.java)
                                    .putExtra(EXTRA_DO_START_POST_JOBS, true)
                        }
                )
        }
        finish()
    }
}