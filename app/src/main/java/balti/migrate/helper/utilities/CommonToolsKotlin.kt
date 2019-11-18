package balti.migrate.helper.utilities

import android.app.Activity
import android.app.NotificationChannel
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.provider.Telephony
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.helper.AppInstance
import balti.migrate.helper.R
import kotlinx.android.synthetic.main.error_report_layout.view.*
import java.io.*

class CommonToolsKotlin(val context: Context) {

    companion object {

        val DEBUG_TAG = "migrate_helper_tag"
        val LAST_SUPPORTED_ANDROID_API = 29

        val PENDING_INTENT_REQUEST_ID = 914
        val PENDING_INTENT_RESTORE_ABORT_ID = 916

        val NOTIFICATION_ID_ONGOING = 229
        val NOTIFICATION_ID_FINISHED = 230
        val NOTIFICATION_ID_CANCELLING = 231

        val FILE_MAIN_PREF = "main"

        val FILE_PROGRESSLOG = "progressLog.txt"
        val FILE_ERRORLOG = "errorLog.txt"
        val FILE_DEVICE_INFO = "device_info.txt"
        val FILE_RESTORE_SCRIPT = "the_restore_script.sh"
        val FILE_PACKAGE_DATA = "package-data"
        val FILE_FILE_LIST = "fileList.txt"

        val CHANNEL_INIT = "Initializing"
        val CHANNEL_RESTORE_END = "Restore finished notification"
        val CHANNEL_RESTORE_RUNNING = "Restore running notification"
        val CHANNEL_RESTORE_ABORTING = "Aborting restore"
        val CHANNEL_UNINSTALLING = "Uninstalling helper"

        val ACTION_RESTORE_PROGRESS = "Helper restore restore_progress_layout broadcast"
        val ACTION_RESTORE_ABORT = "Helper abort broadcast"
        val ACTION_REQUEST_RESTORE_DATA = "get data"
        val ACTION_TRIGGER_RESTORE = "trigger restore"
        val ACTION_RESTORE_SERVICE_STARTED = "restore service started"
        val ACTION_END_ALL = "helper_end_all"

        val EXTRA_PROGRESS_TYPE = "type"
        val EXTRA_PROGRESS_TYPE_SMS = "sms_progress"
        val EXTRA_PROGRESS_TYPE_CALLS = "calls_progress"
        val EXTRA_PROGRESS_TYPE_WIFI = "wifi_progress"
        val EXTRA_PROGRESS_TYPE_ADB = "adb_progress"
        val EXTRA_PROGRESS_TYPE_FONT_SCALE = "font_scale_progress"
        val EXTRA_PROGRESS_TYPE_KEYBOARD = "keyboard_progress"
        val EXTRA_PROGRESS_APP_RESTORE = "app_restore_progress"
        val EXTRA_PROGRESS_MAKING_SCRIPTS = "app_making_script_progress"
        val EXTRA_PROGRESS_TYPE_CLEANING = "cleaning_progress"
        val EXTRA_PROGRESS_WAITING_FOR_VCF = "waiting_for_vcf_progress"
        val EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL = "waiting_to_cancel"
        val EXTRA_PROGRESS_TYPE_FINISHED = "finished"
        val EXTRA_DO_START_POST_JOBS = "do_start_post_jobs"

        val EXTRA_TITLE = "title"
        val EXTRA_SUBTASK = "subtask"
        val EXTRA_TASKLOG = "tasklog"
        val EXTRA_PROGRESS_PERCENTAGE = "restore_progress_layout"
        val EXTRA_ERRORS = "errors"
        val EXTRA_IS_CANCELLED = "isCancelled"
        val EXTRA_TOTAL_TIME = "total_time"

        val EXTRA_NOTIFICATION_FIX = "notification_fix"
        val EXTRA_AUTO_INSTALL_HELPER = "autoInstallHelper"

        val EXTRA_POST_JOBS_ON_FINISH = "post_jobs_on_finish"

        val ERROR_APP_JSON_TRY_CATCH = "APP_JSON_TRY_CATCH"
        val ERROR_APP_JSON = "APP_JSON"
        val ERROR_CONTACTS_GET_TRY_CATCH = "CONTACTS_GET_TRY_CATCH"
        val ERROR_SMS_GET_TRY_CATCH = "SMS_GET_TRY_CATCH"
        val ERROR_CALLS_GET_TRY_CATCH = "CALLS_GET_TRY_CATCH"
        val ERROR_SETTINGS_GET_TRY_CATCH = "SETTINGS_GET_TRY_CATCH"
        val ERROR_WIFI_GET_TRY_CATCH = "WIFI_GET_TRY_CATCH"
        val ERROR_MAIN_READ_TRY_CATCH = "MAIN_READ_TRY_CATCH"

        val ERROR_SMS_RESTORE = "SMS_RESTORE_ERROR"
        val ERROR_CALLS_RESTORE = "CALLS_RESTORE_ERROR"
        val ERROR_GENERIC_SETTINGS = "SETTINGS_ERROR"
        val ERROR_WIFI = "WIFI_ERROR"
        val ERROR_APP_MAKING_SCRIPT = "RESTORE_SCRIPT_ERROR"
        val ERROR_APP_MAKING_SCRIPT_TRY_CATCH = "RESTORE_SCRIPT_TRY_CATCH"
        val ERROR_APP_RESTORE_TRY_CATCH = "RUN_TRY_CATCH"
        val ERROR_APP_RESTORE = "RUN"
        val ERROR_APP_RESTORE_SUPPRESSED = "RUN_SUPPRESSED"
        val ERROR_CLEANING_SUPPRESSED = "CLEANING_ERROR_SUPPRESSED"

        val ERROR_RESTORE_SERVICE_ERROR = "RESTORE_SERVICE"

        val ALL_SUPPRESSED_ERRORS = arrayOf(ERROR_APP_RESTORE_SUPPRESSED, ERROR_CLEANING_SUPPRESSED)

        val DUMMY_WAIT_TIME = 100L
        val DUMMY_WAIT_TIME_LONGER = 500L

        val PROPERTY_APP_SELECTION = "app"        // used to set property in AppRestoreAdapter
        val PROPERTY_DATA_SELECTION = "data"        // used to set property in AppRestoreAdapter
        val PROPERTY_PERMISSION_SELECTION = "permission"        // used to set property in AppRestoreAdapter

        val JOBCODE_ROOT_COPY = 10000
        val JOBCODE_GET_APP_JSON = 20000
        val JOBCODE_GET_CONTACTS = 30000
        val JOBCODE_GET_SMS = 40000
        val JOBCODE_GET_CALLS = 50000
        val JOBCODE_GET_SETTINGS = 60000
        val JOBCODE_GET_WIFI = 70000
        val JOBCODE_END_ALL = 80000

        val JOBCODE_PREP_CONTACTS = 35000
        val JOBCODE_PREP_SMS = 45000
        val JOBCODE_PREP_CALLS = 55000
        val JOBCODE_PREP_DPI = 62000
        val JOBCODE_PREP_ADB = 64000
        val JOBCODE_PREP_FONT_SCALE = 66000
        val JOBCODE_PREP_KEYBOARD = 68000
        val JOBCODE_PREP_WIFI = 75000
        val JOBCODE_PREP_APP = 85000
        val JOBCODE_PREP_END = 95000

        val JOBCODE_RESTORE_CONTACTS = 35001
        val JOBCODE_RESTORE_SMS = 45001
        val JOBCODE_RESTORE_INSTALL_WATCHER = 45500
        val JOBCODE_RESTORE_CALLS = 55001
        val JOBCODE_RESTORE_SETTINGS = 62001
        val JOBCODE_RESTORE_WIFI = 75000
        val JOBCODE_RESTORE_APP = 85001
        val JOBCODE_RESTORE_CLEAN = 95001
        val JOBCODE_RESTORE_END = 95002

        val JOBCODE_RESET_SMS_APP = 45010

        val JOBCODE_PREFERENCES_INSTALL_WATCHER = 45510

        val TIMEOUT_WAITING_TO_CANCEL_TASK = 500L
        val TIMEOUT_WAITING_TO_KILL = 3000L

        val EXTRA_DPI_VALUE = "dpiValue"
        val EXTRA_DO_REBOOT = "doReboot"
        val EXTRA_DO_UNINSTALL = "doUninstall"

        val PENDING_INIT_REQUEST_ID = 231
        val PENDING_INIT_NOTIFICATION_ID = 232

        val SIMPLE_LOG_VIEWER_HEAD = "slg_head"
        val SIMPLE_LOG_VIEWER_FILEPATH = "slg_filePath"

        val PREF_TEMPORARY_DISABLE = "temporaryDisable"
        val PREF_IS_DISABLED = "isDisabled"
        val PREF_MANUAL_CACHE = "manualCache"
        val PREF_MANUAL_METADATA_HOLDER = "manualMetadataHolder"
        val PREF_ANDROID_VERSION_WARNING = "android_version_warning"
        val PREF_IGNORE_READ_ERRORS = "ignore_read_errors"
        val PREF_IGNORE_EXTRAS = "ignore_extras"
        val PREF_RESTORE_START_ANIMATION = "restore_start_animation"
        val PREF_USE_WATCHER = "use_watcher"

        val PREF_DEFAULT_SMS_APP = "default_sms_app"
        val PREF_LAST_DPI = "last_read_dpi"
        val PREF_WAS_CANCELLED = "was_cancelled"
        val PREF_IS_WIFI_RESTORED = "is_wifi_restored"
        val PREF_IS_POST_JOBS_NEEDED = "is_post_jobs_needed"

        val PREF_REMOUNT_DATA = "remount_data"

        val PREF_DEFAULT_MIGRATE_CACHE = "/data/local/tmp/migrate_cache"
        val PREF_DEFAULT_METADATA_HOLDER = "/sdcard/Android/data/balti.migrate.helper/cache/"

        val PACKAGE_NAME_PLAY_STORE = "com.android.vending"
        val PACKAGE_NAME_FDROID = "org.fdroid.fdroid.privileged"

        val EXTRA_VIEW_COUNT = 500

        val BACKUP_NAME_SETTINGS = "settings.json"
        val WIFI_FILE_NAME = "WifiConfigStore.xml"
        val WIFI_FILE_PATH = "/data/misc/wifi/$WIFI_FILE_NAME"

        val APP_MARKER = "app.marker"
        val DATA_MARKER = "data.marker"
        val EXTRAS_MARKER = "extras.marker"

        val HELPER_STATUS = "HELPER_STATUS"

        val UNINSTALL_START_ID = 234

        val MIGRATE_CACHE: String
            get() {
                PREF_DEFAULT_MIGRATE_CACHE.let {default ->
                    AppInstance.sharedPrefs.getString(PREF_MANUAL_CACHE, default).run {
                        return this ?: default
                    }
                }
            }


        val METADATA_HOLDER_DIR: String
            get() {
                PREF_DEFAULT_METADATA_HOLDER.let {default ->
                    AppInstance.sharedPrefs.getString(PREF_MANUAL_METADATA_HOLDER, default).run {
                        return this ?: default
                    }
                }
            }

        val KNOWN_CONTACT_APPS = arrayOf("com.google.android.contacts", "com.android.contacts")
        val KNOWN_CONTACTS_ELEMENTS = arrayOf(
                "com.google.android.contacts/com.google.android.apps.contacts.vcard.VCardService",
                "com.android.contacts/.vcard.VCardService",
                "com.android.contacts/.common.vcard.VCardService"
                )

        val MIGRATE_STATUS = "MIGRATE_STATUS"

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

    fun unpackAssetToInternal(assetFileName: String, targetFileName: String, toInternal: Boolean = true): String {

        val assetManager = context.assets
        val unpackFile = if (toInternal) File(context.filesDir, targetFileName)
        else File(context.externalCacheDir, targetFileName)

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

    fun getHumanReadableStorageSpace(space: Long): String {
        var res = "KB"

        var s = space.toDouble()

        if (s > 1024) {
            s /= 1024.0
            res = "MB"
        }
        if (s > 1024) {
            s /= 1024.0
            res = "GB"
        }

        return String.format("%.2f", s) + " " + res
    }


    fun reportLogs(isErrorLogMandatory: Boolean) {

        val progressLog = File(context.externalCacheDir, FILE_PROGRESSLOG)
        val errorLog = File(context.externalCacheDir, FILE_ERRORLOG)
        val theRestoreScript = File(context.externalCacheDir, FILE_RESTORE_SCRIPT)

        val packageDatas = context.externalCacheDir.let {
            if (it != null) it.listFiles { f: File ->
                f.name.startsWith(FILE_PACKAGE_DATA) && f.name.endsWith(".txt")
            } else emptyArray<File>()
        }

        val fileLists = context.externalCacheDir.let {
            if (it != null) it.listFiles { f: File ->
                f.name.startsWith(FILE_FILE_LIST) && f.name.endsWith(".txt")
            } else emptyArray<File>()
        }

        if (isErrorLogMandatory && !errorLog.exists()) {
            AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(context.getString(R.string.error_log_does_not_exist))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
        else if (errorLog.exists() || progressLog.exists() || theRestoreScript.exists() || packageDatas.isNotEmpty() || fileLists.isNotEmpty()){

            val eView = View.inflate(context, R.layout.error_report_layout, null)

            eView.share_package_data.isChecked = packageDatas.isNotEmpty()
            eView.share_package_data.isEnabled = packageDatas.isNotEmpty()

            eView.share_fileLists_checkbox.isChecked = fileLists.isNotEmpty()
            eView.share_fileLists_checkbox.isEnabled = fileLists.isNotEmpty()

            eView.share_script_checkbox.isChecked = theRestoreScript.exists()
            eView.share_script_checkbox.isEnabled = theRestoreScript.exists()

            eView.share_progress_checkbox.isChecked = progressLog.exists()
            eView.share_progress_checkbox.isEnabled = progressLog.exists()

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
                    if (eView.share_fileLists_checkbox.isChecked) for (f in fileLists) uris.add(getUri(f))

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
                    setOnClickListener { playStoreLink(TG_CLIENTS[0]) }
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
                    context.getString(R.string.restore_script_does_not_exist) + "\n" +
                    context.getString(R.string.package_data_does_not_exist) + "\n" +
                    context.getString(R.string.file_lists_does_not_exist)

            AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }

    }

    private fun getUri(file: File) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                FileProvider.getUriForFile(context, "migrate.helper.provider", file)
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

    fun isPackageInstalled(packageName: String): Boolean{
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

    fun playStoreLink(packageName: String){
        openWebLink("market://details?id=$packageName")
    }

    fun makeNotificationChannel(channelId: String, channelDesc: CharSequence, importance: Int){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelDesc, importance)
            channel.setSound(null, null)
            AppInstance.notificationManager.createNotificationChannel(channel)
        }
    }



    fun tryIt(f: () -> Unit, showError: Boolean = false, isCancelable: Boolean = true, title: String = "", printStackTrace : Boolean = true){
        try {
            f()
        }
        catch (e: Exception){
            if (showError)
            {
                showErrorDialog(e.message.toString(), title, isCancelable)
                if (printStackTrace) e.printStackTrace()
            }
            else if (printStackTrace) e.printStackTrace()
        }
    }

    fun tryIt(f: () -> Unit, title: String = "", printStackTrace : Boolean = true){
        tryIt(f, false, true, title, printStackTrace)
    }

    fun tryIt(f: () -> Unit){
        tryIt(f, "", false)
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

    fun getDefaultSmsApp(): String {
        return Telephony.Sms.getDefaultSmsPackage(context)
    }

    fun areWeDefaultSmsApp(): Boolean {
        return getDefaultSmsApp() == context.packageName
    }

    fun setDefaultSms(packageName: String, requestCode: Int) {
        if (context is AppCompatActivity) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            context.startActivityForResult(intent, requestCode)
        }
    }

    fun cancelTask(suProcess: Process?, vararg pids: Int) {

        tryIt {
            val killProcess = Runtime.getRuntime().exec("su")

            val writer = BufferedWriter(OutputStreamWriter(killProcess.outputStream))
            fun killId(pid: Int) {
                writer.write("kill -9 $pid\n")
                writer.write("kill -15 $pid\n")
            }

            for (pid in pids)
                if (pid != -999) killId(pid)

            writer.write("exit\n")
            writer.flush()

            tryIt { killProcess.waitFor() }
            tryIt { suProcess?.waitFor() }
        }

    }

    fun installWatcherByPackageManager(requestCode: Int) {
        if (context is Activity) {

            unpackAssetToInternal("watcher.apk", "watcher.apk", false)
            val apkFile = File(unpackAssetToInternal("watcher.apk", "watcher.apk", false))

            tryIt({

                val intent = Intent(Intent.ACTION_VIEW)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.setDataAndType(getUri(apkFile), "application/vnd.android.package-archive")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                else intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                context.startActivityForResult(intent, requestCode)

            }, context.getString(R.string.failed_watcher_install))
        }
    }
}