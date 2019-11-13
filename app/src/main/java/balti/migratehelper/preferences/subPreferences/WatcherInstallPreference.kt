package balti.migratehelper.preferences.subPreferences

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.*
import balti.migratehelper.R
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREFERENCES_INSTALL_WATCHER
import balti.migratehelper.utilities.WatcherInstallerCommands
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class WatcherInstallPreference(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {

    private lateinit var progressBar: ProgressBar
    private lateinit var installButton: Button
    private lateinit var installLabel: TextView
    private lateinit var autoInstall: CheckBox

    private lateinit var commonTools : CommonToolsKotlin

    override fun onBindView(view: View?) {
        super.onBindView(view)
        if (view == null) return

        commonTools = CommonToolsKotlin(context)

        progressBar = view.findViewById(R.id.useWatcherInstallProgressBar)
        installButton = view.findViewById(R.id.watcherInstallButton)
        installLabel = view.findViewById(R.id.watcherInstallLabel)
        autoInstall = view.findViewById(R.id.installWatcherByRootPreferences)

        progressBar.visibility = View.GONE

        if (commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME))
            installLabel.text = context.getString(R.string.installed_watcher)

        installButton.setOnClickListener {

            if (autoInstall.isChecked) {

                progressBar.visibility = View.VISIBLE

                commonTools.doBackgroundTask({

                    Runtime.getRuntime().exec("su").run {
                        val writer = BufferedWriter(OutputStreamWriter(outputStream))

                        WatcherInstallerCommands.getCommands(context).forEach {
                            writer.write(it)
                        }

                        writer.write("exit\n")
                        writer.flush()
                        waitFor()
                    }

                }, {
                    if (commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME)) {
                        Toast.makeText(context, R.string.installed_watcher, Toast.LENGTH_SHORT).show()
                        installButton.isEnabled = false
                        installLabel.isEnabled = false
                        autoInstall.isEnabled = false
                        installLabel.text = context.getString(R.string.installed_watcher)
                    } else Toast.makeText(context, R.string.failed_watcher_install, Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                })
            }
            else {
                commonTools.installWatcherByPackageManager(JOBCODE_PREFERENCES_INSTALL_WATCHER)
            }

        }
    }
}