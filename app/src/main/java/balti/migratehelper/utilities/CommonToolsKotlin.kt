package balti.migratehelper.utilities

import android.app.Activity
import android.app.NotificationChannel
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.support.v4.content.FileProvider
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import kotlinx.android.synthetic.main.error_report_layout.view.*
import java.io.*

class CommonToolsKotlin(val context: Context) {

    companion object {

        val DEBUG_TAG = "migrate_helper_tag"
        val TEMP_DIR_NAME = "/data/local/tmp/migrate_cache"
        val METADATA_HOLDER_DIR = "/sdcard/Android/data/balti.migratehelper/cache"

        val FILE_MAIN_PREF = "main"

        val FILE_PROGRESSLOG = "progressLog.txt"
        val FILE_ERRORLOG = "errorLog.txt"
        val FILE_DEVICE_INFO = "device_info.txt"
        val FILE_RESTORE_SCRIPT = "the_restore_script.sh"
        val FILE_PACKAGE_DATA = "package-data"

        val CHANNEL_INIT = "Initializing"
        val CHANNEL_RESTORE_END = "Restore finished notification"
        val CHANNEL_RESTORE_RUNNING = "Restore running notification"
        val CHANNEL_RESTORE_ABORTING = "Aborting restore"
        val CHANNEL_UNINSTALLING = "Uninstalling helper"

        val ACTION_RESTORE_PROGRESS = "Helper restore progress broadcast"
        val ACTION_RESTORE_ABORT = "Helper abort broadcast"
        val ACTION_REQUEST_RESTORE_DATA = "get data"
        val ACTION_TRIGGER_RESTORE = "trigger restore"
        val ACTION_RESTORE_SERVICE_STARTED = "restore service started"
        val ACTION_END_ALL = "helper_end_all"

        val ERROR_APP_JSON_TRY_CATCH = "APP_JSON_TRY_CATCH"
        val ERROR_APP_JSON = "APP_JSON"
        val ERROR_CONTACTS_GET_TRY_CATCH = "CONTACTS_GET_TRY_CATCH"
        val ERROR_SMS_GET_TRY_CATCH = "SMS_GET_TRY_CATCH"
        val ERROR_CALLS_GET_TRY_CATCH = "CALLS_GET_TRY_CATCH"
        val ERROR_SETTINGS_GET_TRY_CATCH = "SETTINGS_GET_TRY_CATCH"
        val ERROR_WIFI_GET_TRY_CATCH = "WIFI_GET_TRY_CATCH"

        val DUMMY_WAIT_TIME = 100L

        val EXTRA_DPI_VALUE = "dpiValue"
        val EXTRA_DO_REBOOT = "doReboot"
        val EXTRA_DO_UNINSTALL = "doUninstall"

        val PENDING_INIT_REQUEST_ID = 231
        val PENDING_INIT_NOTIFICATION_ID = 232

        val SIMPLE_LOG_VIEWER_HEAD = "slg_head"
        val SIMPLE_LOG_VIEWER_FILEPATH = "slg_filePath"

        val PREF_TEMPORARY_DISABLE = "temporaryDisable"
        val PREF_IS_DISABLED = "isDisabled"
        val PREF_ANDROID_VERSION_WARNING = "android_version_warning"

        val BACKUP_NAME_SETTINGS = "settings.json"

        val WIFI_FILE_NAME = "WifiConfigStore.xml"

        val HELPER_STATUS = "HELPER_STATUS"

        val UNINSTALL_START_ID = 234

        val REPORTING_EMAIL = "help.baltiapps@gmail.com"
        val TG_LINK = "https://t.me/migrateApp"
        val TG_DEV_LINK = "https://t.me/SayantanRC"

        val TG_CLIENTS = arrayOf(
                "org.telegram.messenger",          // Official Telegram app
                "org.thunderdog.challegram",       // Telegram X
                "org.telegram.plus"                // Plus messenger
        )
    }

    var LBM : LocalBroadcastManager? = null

    init {
        if (context is Activity || context is Service)
            LBM = LocalBroadcastManager.getInstance(context)
    }


    fun unpackAssetToInternal(assetFileName: String, targetFileName: String): String {

        val assetManager = context.assets
        val unpackFile = File(context.filesDir, targetFileName)

        var read: Int
        val buffer = ByteArray(4096)

        return try {
            val inputStream = assetManager.open(assetFileName)
            val writer = FileOutputStream(unpackFile)
            while (true) {
                read = inputStream.read(buffer)
                if (read > 0) writer.write(buffer, 0, read)
                else break
            }
            writer.close()
            unpackFile.setExecutable(true)
            return unpackFile.absolutePath
        } catch (e: IOException){
            e.printStackTrace()
            ""
        }
    }


    fun reportLogs(isErrorLogMandatory: Boolean) {

        val progressLog = File(context.externalCacheDir, FILE_PROGRESSLOG)
        val errorLog = File(context.externalCacheDir, FILE_ERRORLOG)
        val theRestoreScript = File(context.externalCacheDir, FILE_RESTORE_SCRIPT)

        val packageDatas = context.externalCacheDir.listFiles { f: File ->
            f.name.startsWith(FILE_PACKAGE_DATA) && f.name.endsWith(".txt")
        }

        if (isErrorLogMandatory && !errorLog.exists()) {
            AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(context.getString(R.string.error_log_does_not_exist))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
        else if (errorLog.exists() || progressLog.exists() || theRestoreScript.exists() || packageDatas.isNotEmpty()){

            val eView = View.inflate(context, R.layout.error_report_layout, null)

            eView.share_progress_checkbox.isChecked = progressLog.exists()
            eView.share_progress_checkbox.isEnabled = progressLog.exists()

            eView.share_script_checkbox.isChecked = theRestoreScript.exists()
            eView.share_script_checkbox.isEnabled = theRestoreScript.exists()

            eView.share_package_data.isChecked = packageDatas.isNotEmpty()
            eView.share_package_data.isEnabled = packageDatas.isNotEmpty()

            eView.share_errors_checkbox.isChecked = errorLog.exists()
            eView.share_errors_checkbox.isEnabled = errorLog.exists() && !isErrorLogMandatory

            eView.report_button_what_is_shared.setOnClickListener {
                AlertDialog.Builder(context)
                        .setTitle(R.string.what_is_shared)
                        .setMessage(R.string.shared_desc)
                        .setPositiveButton(R.string.close, null)
                        .show()
            }

            eView.report_button_join_group.setOnClickListener {
                openWebLink(TG_LINK)
            }

            fun getUris(): ArrayList<Uri>{

                val uris = ArrayList<Uri>(0)
                try {

                    if (eView.share_errors_checkbox.isChecked) uris.add(getUri(errorLog))
                    if (eView.share_progress_checkbox.isChecked) uris.add(getUri(progressLog))
                    if (eView.share_script_checkbox.isChecked) uris.add(getUri(theRestoreScript))
                    if (eView.share_package_data.isChecked) for (f in packageDatas) uris.add(getUri(f))

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
                }

                return uris
            }

            eView.report_button_old_email.setOnClickListener {
                sendIntent(getUris(), true)
            }

            var isTgClientInstalled = false
            for (i in TG_CLIENTS.indices){
                if (isPackageInstalled(TG_CLIENTS[i])){
                    isTgClientInstalled = true
                    break
                }
            }

            if (!isTgClientInstalled){
                eView.report_button_telegram.apply {
                    text = context.getString(R.string.install_tg)
                    setOnClickListener { openWebLink("market://details?id=${TG_CLIENTS[0]}") }
                }
            }
            else {
                eView.report_button_telegram.apply {
                    text = context.getString(R.string.send_to_tg)
                    setOnClickListener { sendIntent(getUris()) }
                }
            }

            AlertDialog.Builder(context).setView(eView).show()

        }
        else {

            val msg = context.getString(R.string.progress_log_does_not_exist) + "\n" +
                    context.getString(R.string.error_log_does_not_exist) + "\n" +
                    context.getString(R.string.restore_script_does_not_exist) + "\n"

            AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }

    }

    private fun getUri(file: File) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                FileProvider.getUriForFile(context, "migrate.provider", file)
            else Uri.fromFile(file)

    private fun sendIntent(uris: ArrayList<Uri>, isEmail: Boolean = false){
        Intent().run {

            action = Intent.ACTION_SEND_MULTIPLE
            type = "text/plain"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (isEmail) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORTING_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "Log report for Migrate")
                putExtra(Intent.EXTRA_TEXT, deviceSpecifications)

                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                context.startActivity(Intent.createChooser(this, context.getString(R.string.select_mail)))
            }
            else doBackgroundTask({

                tryIt {
                    val infoFile = File(context.externalCacheDir, FILE_DEVICE_INFO)
                    BufferedWriter(FileWriter(infoFile)).run {
                        write(deviceSpecifications)
                        close()
                    }
                    uris.add(getUri(infoFile))
                }

            }, {
                this.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                context.startActivity(Intent.createChooser(this, context.getString(R.string.select_telegram)))
            })
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean{
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            true
        }
        catch (_: Exception){
            false
        }
    }

    var deviceSpecifications: String =
            "CPU_ABI: " + Build.SUPPORTED_ABIS[0] + "\n" +
                    "Brand: " + Build.BRAND + "\n" +
                    "Manufacturer: " + Build.MANUFACTURER + "\n" +
                    "Model: " + Build.MODEL + "\n" +
                    "Device: " + Build.DEVICE + "\n" +
                    "SDK: " + Build.VERSION.SDK_INT + "\n" +
                    "Board: " + Build.BOARD + "\n" +
                    "Hardware: " + Build.HARDWARE
        private set


    fun openWebLink(url: String) {
        if (url != "") {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }
    }


    fun makeNotificationChannel(channelId: String, channelDesc: CharSequence, importance: Int){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelDesc, importance)
            channel.setSound(null, null)
            AppInstance.notificationManager.createNotificationChannel(channel)
        }
    }



    fun tryIt(f: () -> Unit, showError: Boolean = false, isCancelable: Boolean = true, title: String = ""){
        try {
            f()
        }
        catch (e: Exception){
            if (showError)
            {
                showErrorDialog(e.message.toString(), title, isCancelable)
                e.printStackTrace()
            }
            else e.printStackTrace()
        }
    }

    fun tryIt(f: () -> Unit, title: String = ""){
        tryIt(f, false, true, title)
    }

    fun tryIt(f: () -> Unit){
        tryIt(f, "")
    }

    fun showErrorDialog(message: String, title: String = "", isCancelable: Boolean = true){
        try {
            AlertDialog.Builder(context)
                    .setIcon(R.drawable.ic_error)
                    .setMessage(message).apply {

                        if (isCancelable)
                            setNegativeButton(R.string.close, null)
                        else {
                            setCancelable(false)
                            setNegativeButton(R.string.close) { _, _ ->
                                if (context is AppCompatActivity) {
                                    (context as AppCompatActivity).finish()
                                }
                            }
                        }

                        if (title == "")
                            setTitle(R.string.error_occurred)
                        else setTitle(title)

                    }
                    .show()
        } catch (e: Exception){
            e.printStackTrace()
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception){}
        }
    }

    fun getPercentage(count: Int, total: Int): Int {
        return if (total != 0) (count*100)/total
        else 0
    }

    fun doBackgroundTask(job: () -> Unit, postJob: () -> Unit){
        class Class : AsyncTask<Any, Any, Any>(){
            override fun doInBackground(vararg params: Any?): Any {
                job()
                return 0
            }

            override fun onPostExecute(result: Any?) {
                super.onPostExecute(result)
                postJob()
            }
        }
        Class().execute()
    }
}