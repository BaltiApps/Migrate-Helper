package balti.migratehelper.postJobs.utils

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import balti.migratehelper.AppInstance
import balti.migratehelper.postJobs.PostJobsActivity
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.EXTRA_OVERRIDE_IS_ALIVE
import balti.migratehelper.simpleActivities.MainActivityKotlin
import balti.migratehelper.simpleActivities.ProgressShowActivity
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_START_POST_JOBS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IS_POST_JOBS_NEEDED

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