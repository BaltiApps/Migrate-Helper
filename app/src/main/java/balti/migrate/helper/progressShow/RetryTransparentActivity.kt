package balti.migrate.helper.progressShow

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.TextViewCompat.setTextAppearance
import balti.migrate.helper.AppInstance.Companion.appPackets
import balti.migrate.helper.AppInstance.Companion.failedAppInstalls
import balti.migrate.helper.AppInstance.Companion.notificationFixGlobal
import balti.migrate.helper.R
import balti.migrate.helper.extraRestorePrepare.utils.AppsNotInstalledViewManager
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_APPEND_LOG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_NOTIFICATION_FIX
import balti.module.baltitoolbox.functions.Misc
import kotlinx.android.synthetic.main.apps_not_installed_layout.view.*

class RetryTransparentActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDialog()
    }

    fun showDialog(){

        fun proceedToRestore(){
            appPackets.clear()
            failedAppInstalls.forEach {
                it.APP = false
                appPackets.add(it)
            }
            Intent(this, RestoreServiceKotlin::class.java)
                    .putExtra(EXTRA_NOTIFICATION_FIX, notificationFixGlobal)
                    .putExtra(EXTRA_APPEND_LOG, true)
                    .run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            startForegroundService(this)
                        else startService(this)
                    }
            setResult(RESULT_OK)
            finish()
        }

        val notInstalled = ArrayList<AppPacketsKotlin>(0)
        failedAppInstalls.forEach { packet ->
            packet.packageName?.let { it1 ->
                if (!Misc.isPackageInstalled(it1)) notInstalled.add(packet)
            }
        }
        if (notInstalled.isEmpty()) proceedToRestore()
        else {
            AppsNotInstalledViewManager(notInstalled, this).run {

                val view = this.getView()
                view.apps_not_installed_header.apply {
                    setText(R.string.please_install_these)
                    setTextAppearance(this, android.R.style.TextAppearance_Medium)
                    setPadding(10, 10, 10, 0)
                }
                view.apps_not_installed_footer.visibility = View.GONE

                val ad = AlertDialog.Builder(this@RetryTransparentActivity)
                        .setView(view)
                        .setPositiveButton(R.string.continue_) { _, _ ->
                            val c = getSuccessfullyInstalledApps()
                            if (c == failedAppInstalls.size) {
                                proceedToRestore()
                            } else {
                                AlertDialog.Builder(this@RetryTransparentActivity)
                                        .setTitle(R.string.failed_apps_not_yet_installed)
                                        .setMessage(getString(R.string.failed_apps_not_yet_installed_desc) + "\n\n" +
                                                getString(R.string.installed) + " " + c + "\n" +
                                                getString(R.string.total_to_be_installed) + " " + failedAppInstalls.size
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
                        if (getSuccessfullyInstalledApps() == failedAppInstalls.size) proceedToRestore()
                        else showDialog()
                    }
                }
            }
        }
    }

    fun getSuccessfullyInstalledApps(): Int {
        var c = 0
        failedAppInstalls.forEach { packet ->
            packet.packageName?.let { it1 ->
                if (Misc.isPackageInstalled(it1)) c++
            }
        }
        return c
    }

}