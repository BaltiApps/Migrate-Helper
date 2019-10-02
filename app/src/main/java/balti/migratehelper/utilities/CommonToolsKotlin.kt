package balti.migratehelper.utilities

import android.app.NotificationChannel
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import kotlinx.android.synthetic.main.error_report_layout.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CommonToolsKotlin(val context: Context) {

    companion object {

        val DEBUG_TAG = "migrate_helper_tag"
        val TEMP_DIR_NAME = "/data/local/tmp/migrate_cache"

        val FILE_MAIN_PREF = "main"

        val FILE_PROGRESSLOG = "progressLog.txt"
        val FILE_ERRORLOG = "errorLog.txt"
        val FILE_RESTORE_SCRIPT = "the_restore_script.sh"
        val FILE_PACKAGE_DATA = "package-data"

        val CHANNEL_INIT = "Initializing"
        val CHANNEL_RESTORE_END = "Restore finished notification"
        val CHANNEL_RESTORE_RUNNING = "Restore running notification"
        val CHANNEL_RESTORE_ABORTING = "Aborting restore"

        val ACTION_RESTORE_PROGRESS = "Helper restore progress broadcast"
        val ACTION_RESTORE_ABORT = "Helper abort broadcast"
        val ACTION_REQUEST_RESTORE_DATA = "get data"
        val ACTION_TRIGGER_RESTORE = "trigger restore"
        val ACTION_RESTORE_SERVICE_STARTED = "restore service started"

        val PENDING_INIT_REQUEST_ID = 231
        val PENDING_INIT_NOTIFICATION_ID = 232

        val METADATA_HOLDER_DIR = "/sdcard/Android/data/balti.migratehelper/cache"

        val PREF_TEMPORARY_DISABLE = "temporaryDisable"
        val PREF_IS_DISABLED = "isDisabled"

        val HELPER_STATUS = "HELPER_STATUS"

        val REPORTING_EMAIL = "help.baltiapps@gmail.com"
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

        val progressLog = File(METADATA_HOLDER_DIR, FILE_PROGRESSLOG)
        val errorLog = File(METADATA_HOLDER_DIR, FILE_ERRORLOG)
        val theRestoreScript = File(METADATA_HOLDER_DIR, FILE_RESTORE_SCRIPT)

        val packageDatas = File(METADATA_HOLDER_DIR).listFiles { f: File ->
            f.name.startsWith(FILE_PACKAGE_DATA) && f.name.endsWith(".sh")
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

            eView.share_package_data.isChecked = packageDatas.isNotEmpty()
            eView.share_package_data.isEnabled = packageDatas.isNotEmpty()

            eView.share_script_checkbox.isChecked = theRestoreScript.exists()
            eView.share_script_checkbox.isEnabled = theRestoreScript.exists()

            eView.share_errors_checkbox.isChecked = errorLog.exists()
            eView.share_errors_checkbox.isEnabled = errorLog.exists() && !isErrorLogMandatory

            AlertDialog.Builder(context)
                    .setView(eView)
                    .setPositiveButton(R.string.agree_and_send) {_, _ ->

                        val body = deviceSpecifications

                        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            type = "text/plain"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORTING_EMAIL))
                            putExtra(Intent.EXTRA_SUBJECT, "Log report for Migrate")
                            putExtra(Intent.EXTRA_TEXT, body)
                        }

                        val uris = ArrayList<Uri>(0)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if (eView.share_errors_checkbox.isChecked) uris.add(FileProvider.getUriForFile(context, "migrate.helper.provider", errorLog))
                            if (eView.share_progress_checkbox.isChecked) uris.add(FileProvider.getUriForFile(context, "migrate.helper.provider", progressLog))
                            if (eView.share_script_checkbox.isChecked) uris.add(FileProvider.getUriForFile(context, "migrate.helper.provider", theRestoreScript))
                            if (eView.share_package_data.isChecked)
                                for (f in packageDatas)
                                    uris.add(FileProvider.getUriForFile(context, "migrate.helper.provider", f))
                        }
                        else {
                            if (eView.share_errors_checkbox.isChecked) uris.add(Uri.fromFile(errorLog))
                            if (eView.share_progress_checkbox.isChecked) uris.add(Uri.fromFile(progressLog))
                            if (eView.share_script_checkbox.isChecked) uris.add(Uri.fromFile(theRestoreScript))
                            if (eView.share_package_data.isChecked) for (f in packageDatas) uris.add(Uri.fromFile(f))
                        }

                        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)

                        try {
                            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.select_mail)))
                            Toast.makeText(context, context.getString(R.string.select_mail), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        }

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

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