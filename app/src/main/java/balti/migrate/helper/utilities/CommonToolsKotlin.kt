package balti.migrate.helper.utilities

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.provider.Telephony
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.helper.AppInstance
import balti.migrate.helper.R
import balti.migrate.helper.utilities.constants.AddonSmsCallsConstants.Companion.ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME
import balti.module.baltitoolbox.functions.Misc.isPackageInstalled
import balti.module.baltitoolbox.functions.Misc.openWebLink
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import kotlinx.android.synthetic.main.error_report_layout.view.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter

class CommonToolsKotlin(val context: Context) {

    companion object {

        val THIS_VERSION = 40

        val DEBUG_TAG = "migrate_helper_tag"
        val LAST_SUPPORTED_ANDROID_API = 30

        val JOB_RESULT_OK = 0
        val JOB_RESULT_DENIED = 1
        val JOB_RESULT_TIMEOUT = -1

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
        val FILE_RESTORE_SETTINGS_SCRIPT = "settings_restore_script.sh"
        val FILE_PACKAGE_DATA = "package-data"
        val FILE_FILE_LIST = "fileList.txt"
        val FILE_RAW_LIST = "rawList.txt"

        val DIR_TWRP_UNINSTALL = "/sdcard/Migrate/"

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

        val EXTRA_SMS_CALLS_ADDON_OK = "sms_calls_addon_ok"
        val EXTRA_ADDON_DO_ABORT = "addon_abort"

        val EXTRA_DO_INSTALL_SMS_CALLS_ADDON = "install_addon_smsCalls"
        val EXTRA_SMS_CALLS_ADDON_FILES = "sms_calls_addon_files"

        val ERROR_PRE_EXECUTE = "PARENT_PRE_EXECUTE"

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
        val ERROR_GENERIC_SMS_CALLS = "SMS_CALLS_ERROR"
        val ERROR_WIFI = "WIFI_ERROR"
        val ERROR_APP_MAKING_SCRIPT = "RESTORE_SCRIPT_ERROR"
        val ERROR_APP_MAKING_SCRIPT_TRY_CATCH = "RESTORE_SCRIPT_TRY_CATCH"
        val ERROR_APP_RESTORE_TRY_CATCH = "RUN_TRY_CATCH"
        val ERROR_APP_RESTORE = "RUN"
        val ERROR_APP_RESTORE_MESSAGE = "RUN_M"
        val ERROR_APP_RESTORE_SUPPRESSED = "RUN_SUPPRESSED"
        val ERROR_CLEANING_SUPPRESSED = "CLEANING_ERROR_SUPPRESSED"
        val ERROR_SETTINGS_SCRIPT = "ERROR_SETTINGS_SCRIPT"

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
        //val JOBCODE_RESTORE_SMS = 45001
        //val JOBCODE_RESTORE_CALLS = 55001
        val JOBCODE_RESTORE_SMS_CALLS = 48001
        val JOBCODE_RESTORE_SETTINGS = 62001
        val JOBCODE_RESTORE_WIFI = 75000
        val JOBCODE_RESTORE_APP = 85001
        val JOBCODE_RESTORE_CLEAN = 95001
        val JOBCODE_RESTORE_END = 95002

        val JOBCODE_READ_FILE_PERMISSION = 22773

        val JOBCODE_LAUNCH_ADDON_INSTALLER = 430
        val JOBCODE_ADDON_INSTALL_SMS_CALLS = 433
        val JOBCODE_ADDON_END = 432

        val TIMEOUT_WAITING_TO_CANCEL_TASK = 500L
        val TIMEOUT_WAITING_TO_KILL = 3000L

        val TIMEOUT_ADDON_DELAY = 500L

        val EXTRA_DO_REBOOT = "doReboot"
        val EXTRA_DO_UNINSTALL = "doUninstall"
        val EXTRA_DO_REMOVE_CACHE = "doRemoveCache"
        val EXTRA_SCAN_SYSTEM_APK = "doScanSystemForApk"

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

        val PREF_DEFAULT_SMS_APP = "default_sms_app"
        val PREF_WAS_CANCELLED = "was_cancelled"
        val PREF_IS_WIFI_RESTORED = "is_wifi_restored"

        val PREF_REMOUNT_DATA = "remount_data"
        val PREF_LOAD_EXTRAS_ON_UI_THREAD = "loadExtrasOnUiThread"
        val PREF_TRACK_RESTORE_FINISHED = "track_restore_finished"
        val PREF_REMOUNT_ALL_TO_UNINSTALL = "remount_all_to_uninstall"

        val PREF_DEFAULT_MIGRATE_CACHE = "/data/local/tmp/migrate_cache"
        val PREF_DEFAULT_METADATA_HOLDER = AppInstance.appContext.externalCacheDir?.absolutePath ?: "/sdcard/Android/data/balti.migrate.helper/cache"
        val MIGRATE_TEMP_DIR = "/data/migrateTemp"

        val PREF_DO_LOAD_ICON_IN_LIST = "doLoadIconInList"
        val PREF_DO_LOAD_MULTIPLE_ICON_IN_LIST = "doLoadMultipleIconInList"
        val PREF_ICON_CHECK_LOW_MEMORY = "checkLowMemory"

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
                    getPrefString(PREF_MANUAL_CACHE, default).run {
                        return this ?: default
                    }
                }
            }


        val METADATA_HOLDER_DIR: String
            get() {
                PREF_DEFAULT_METADATA_HOLDER.let {default ->
                    getPrefString(PREF_MANUAL_METADATA_HOLDER, default).run {
                        (this ?: default).let {
                            File(it).run { if (!exists()) mkdirs() }
                            return it
                        }
                    }
                }
            }

        val INFO_HOLDER_DIR: String
        get() {
            return try {
                this.METADATA_HOLDER_DIR
            }
            catch (e: Exception) {
                e.printStackTrace()
                this.PREF_DEFAULT_METADATA_HOLDER
            }.let { File(it).parentFile.absolutePath + "/info" }
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

    //val workingDir = context.externalCacheDir

    init {
        if (context is Activity || context is Service)
            LBM = LocalBroadcastManager.getInstance(context)
    }

    fun reportLogs(isErrorLogMandatory: Boolean) {

        val progressLog = File(INFO_HOLDER_DIR, FILE_PROGRESSLOG)
        val errorLog = File(INFO_HOLDER_DIR, FILE_ERRORLOG)
        val theRestoreScript = File(INFO_HOLDER_DIR, FILE_RESTORE_SCRIPT)

        val workingDir = File(METADATA_HOLDER_DIR)

        val packageDatas = workingDir.let {
            it.listFiles { f: File ->
                f.name.startsWith(FILE_PACKAGE_DATA) && f.name.endsWith(".txt")
            }
        }

        val fileLists = workingDir.let {
            it.listFiles { f: File ->
                f.name.startsWith(FILE_FILE_LIST) && f.name.endsWith(".txt")
            }
        }

        val rawLists = workingDir.let {
            it.listFiles { f: File ->
                f.name.startsWith(FILE_RAW_LIST) && f.name.endsWith(".txt")
            }
        }

        if (isErrorLogMandatory && !errorLog.exists()) {
            AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(context.getString(R.string.error_log_does_not_exist))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
        else if (errorLog.exists() || progressLog.exists() || theRestoreScript.exists() ||
                packageDatas.isNotEmpty() || fileLists.isNotEmpty() || rawLists.isNotEmpty()){

            val eView = View.inflate(context, R.layout.error_report_layout, null)

            eView.share_package_data.isChecked = packageDatas.isNotEmpty()
            eView.share_package_data.isEnabled = packageDatas.isNotEmpty()

            eView.share_fileLists_checkbox.isChecked = fileLists.isNotEmpty()
            eView.share_fileLists_checkbox.isEnabled = fileLists.isNotEmpty()

            eView.share_rawLists_checkbox.isChecked = rawLists.isNotEmpty()
            eView.share_rawLists_checkbox.isEnabled = rawLists.isNotEmpty()

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
                    if (eView.share_rawLists_checkbox.isChecked) for (f in rawLists) uris.add(getUri(f))

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
                    context.getString(R.string.file_lists_does_not_exist) + "\n" +
                    context.getString(R.string.raw_lists_does_not_exist)

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
                    val infoFile = File(INFO_HOLDER_DIR, FILE_DEVICE_INFO)
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

    fun applyNamingCorrectionForShell(name: String) =
            name
                    .replace("`", "\\`")
                    .replace("!", "\\!")
                    .replace("#", "\\#")
                    .replace("$", "\\$")
                    .replace("&", "\\&")
                    .replace("*", "\\*")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace(">", "\\>")
                    .replace("<", "\\<")
                    .replace(" ", "\\ ")
                    .replace(":", "\\:")
                    .replace(";", "\\;")
                    .replace("\"", "\\\"")
                    .replace("\'", "\\\'")

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

    fun isAddonDefaultSmsApp(): Boolean {
        return getDefaultSmsApp() == ADDON_SMS_CALLS_RECEIVER_PACKAGE_NAME
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
}