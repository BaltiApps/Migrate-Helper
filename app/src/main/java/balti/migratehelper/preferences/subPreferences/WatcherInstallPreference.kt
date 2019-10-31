package balti.migratehelper.preferences.subPreferences

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import balti.migratehelper.R
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME
import balti.migratehelper.utilities.CommonToolsKotlin
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class WatcherInstallPreference(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {

    lateinit var progressBar: ProgressBar
    lateinit var installButton: Button
    lateinit var installLabel: TextView

    lateinit var commonTools : CommonToolsKotlin

    override fun onBindView(view: View?) {
        super.onBindView(view)
        if (view == null) return

        commonTools = CommonToolsKotlin(context)

        progressBar = view.findViewById(R.id.useWatcherInstallProgressBar)
        installButton = view.findViewById(R.id.watcherInstallButton)
        installLabel = view.findViewById(R.id.watcherInstallLabel)

        progressBar.visibility = View.GONE

        installButton.setOnClickListener {

            progressBar.visibility = View.VISIBLE

            commonTools.doBackgroundTask({

                Runtime.getRuntime().exec("su").run {
                    val writer = BufferedWriter(OutputStreamWriter(outputStream))

                    val watcherPath = commonTools.unpackAssetToInternal("watcher.apk", "watcher.apk")
                    writer.write("mv $watcherPath /data/local/tmp/watcher.apk\n")
                    writer.write("pm install /data/local/tmp/watcher.apk\n")
                    writer.write("rm /data/local/tmp/watcher.apk\n")

                    writer.write("exit\n")
                    writer.flush()
                    waitFor()
                }

            }, {
                if (commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME)) {
                    Toast.makeText(context, R.string.installed_watcher, Toast.LENGTH_SHORT).show()
                    installButton.isEnabled = false
                    installLabel.isEnabled = false
                }
                else Toast.makeText(context, R.string.failed_watcher_install, Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            })
        }
    }
}