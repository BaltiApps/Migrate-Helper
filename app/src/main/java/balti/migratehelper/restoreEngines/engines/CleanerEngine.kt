package balti.migratehelper.restoreEngines.engines

import android.util.Log
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.postJobs.utils.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME
import balti.migratehelper.restoreEngines.ParentRestoreClass
import balti.migratehelper.restoreEngines.RestoreServiceKotlin
import balti.migratehelper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.APP_MARKER
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DATA_MARKER
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME_LONGER
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_CLEANING_SUPPRESSED
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRAS_MARKER
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CLEANING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_USE_WATCHER
import java.io.BufferedWriter
import java.io.File
import java.io.FileFilter
import java.io.OutputStreamWriter

class CleanerEngine(private val jobcode: Int,
                    private val appPackets: ArrayList<AppPacketsKotlin>,
                    private val wasSmsRestored: Boolean): ParentRestoreClass(EXTRA_PROGRESS_TYPE_CLEANING) {

    private val errors by lazy { ArrayList<String>(0) }

    private val removableFilesList by lazy { ArrayList<String>(0) }

    override fun doInBackground(vararg params: Any?): Any {
        try {

            resetBroadcast(true, engineContext.getString(R.string.cleaning_up))

            for (i in appPackets.indices){

                if (RestoreServiceKotlin.cancelAll) break
                val packet = appPackets[i]

                fun isClearable(stringField: String?, booleanField: Boolean, markerString: String): Boolean{
                    return if (stringField == null) true    // clearable if apkName or dataName is null
                    else {
                        if (booleanField) {    // if user selects to restore the app or data, check if it has been restored
                            File(METADATA_HOLDER_DIR, "${packet.packageName}.$markerString").exists()
                        }
                        else false    // the user has not opted to restore, but may do it next time. so don't clear.
                    }
                }

                val isAppClearable = isClearable(packet.apkName, packet.APP, APP_MARKER)
                val isDataClearable = isClearable(packet.dataName, packet.DATA, DATA_MARKER)
                val isPermissionClearable = packet.PERMISSION && packet.isPermission

                Log.d(DEBUG_TAG, "$isAppClearable $isDataClearable $isPermissionClearable ${packet.jsonFile.absolutePath}")

                if (isAppClearable && isDataClearable && isPermissionClearable) {
                    removableFilesList.add(packet.jsonFile.name)
                    packet.iconFileName?.let { removableFilesList.add(it) }
                    removableFilesList.add("${packet.packageName}.perm")
                }

                File(METADATA_HOLDER_DIR, "${packet.packageName}.$APP_MARKER").let { if (it.exists()) it.delete() }
                File(METADATA_HOLDER_DIR, "${packet.packageName}.$DATA_MARKER").let { if (it.exists()) it.delete() }
            }

            val allFiles = File(METADATA_HOLDER_DIR).listFiles(FileFilter {
                it.name.endsWith(EXTRAS_MARKER)
            })

            allFiles.forEach { f ->
                removableFilesList.add(f.name.let { it.substring(0, it.indexOf(EXTRAS_MARKER) - 1) })
                // -1 to exclude the first dot "." in ".extras.marker"
            }

            val doInstallWatcher = wasSmsRestored && AppInstance.sharedPrefs.getBoolean(PREF_USE_WATCHER, true)
                    && !commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME)

            Runtime.getRuntime().exec("su").run {
                val writer = BufferedWriter(OutputStreamWriter(outputStream))
                removableFilesList.forEach {
                    writer.write("rm $MIGRATE_CACHE/$it 2>/dev/null\n")
                }

                if (doInstallWatcher){
                    val watcherPath = commonTools.unpackAssetToInternal("watcher.apk", "watcher.apk")
                    writer.write("mv $watcherPath /data/local/tmp/watcher.apk\n")
                    writer.write("pm install /data/local/tmp/watcher.apk 2>/dev/null\n")
                    writer.write("rm /data/local/tmp/watcher.apk\n")
                }

                writer.write("exit\n")
                writer.flush()
                waitFor()
            }

            if (!doInstallWatcher) Thread.sleep(DUMMY_WAIT_TIME_LONGER)
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("${ERROR_CLEANING_SUPPRESSED}: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onRestoreComplete.onRestoreComplete(jobcode, errors.size == 0, errors)
    }

}