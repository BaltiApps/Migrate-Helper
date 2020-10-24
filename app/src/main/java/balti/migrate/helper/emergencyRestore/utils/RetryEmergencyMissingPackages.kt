package balti.migrate.helper.emergencyRestore.utils

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.helper.AppInstance
import balti.migrate.helper.AppInstance.Companion.emFailedCombined
import balti.migrate.helper.R
import balti.migrate.helper.emergencyRestore.EmergencyRestoreService
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.module.baltitoolbox.functions.Misc

class RetryEmergencyMissingPackages: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDialog()
    }

    fun showDialog(){

        fun proceedToRestore(){
            Intent(this, EmergencyRestoreService::class.java)
                    .putExtra(CommonToolsKotlin.EXTRA_NOTIFICATION_FIX, AppInstance.notificationFixGlobal)
                    .putExtra(CommonToolsKotlin.EXTRA_APPEND_LOG, true)
                    .run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            startForegroundService(this)
                        else startService(this)
                    }
            setResult(RESULT_OK)
            finish()
        }

        val notInstalled = ArrayList(emFailedCombined.filter { !Misc.isPackageInstalled(it) })

        if (notInstalled.isEmpty()) proceedToRestore()
        else {
            MissingPackagesViewManager(notInstalled, this).run {

                val view = this.getView()
                val ad = AlertDialog.Builder(this@RetryEmergencyMissingPackages)
                        .setView(view)
                        .setPositiveButton(R.string.continue_) { _, _ ->
                            val c = getSuccessfullyInstalledApps()
                            if (c == emFailedCombined.size) {
                                proceedToRestore()
                            } else {
                                AlertDialog.Builder(this@RetryEmergencyMissingPackages)
                                        .setTitle(R.string.failed_apps_not_yet_installed)
                                        .setMessage(getString(R.string.failed_apps_not_yet_installed_desc) + "\n\n" +
                                                getString(R.string.installed) + " " + c + "\n" +
                                                getString(R.string.total_to_be_installed) + " " + emFailedCombined.size
                                        )
                                        .setNegativeButton(R.string.continue_anyway) { _, _ ->
                                            proceedToRestore()
                                        }
                                        .setPositiveButton(R.string.wait) { _, _ ->
                                            showDialog()
                                        }
                                        .setCancelable(false)
                                        .show()
                            }
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .create()
                ad.show()

                getRefreshButton().setOnClickListener {
                    Misc.tryIt {
                        ad.dismiss()
                        if (getSuccessfullyInstalledApps() == emFailedCombined.size) proceedToRestore()
                        else showDialog()
                    }
                }
            }
        }

    }

    fun getSuccessfullyInstalledApps(): Int {
        var c = 0
        emFailedCombined.forEach { packageName ->
            if (Misc.isPackageInstalled(packageName)) c++
        }
        return c
    }

}