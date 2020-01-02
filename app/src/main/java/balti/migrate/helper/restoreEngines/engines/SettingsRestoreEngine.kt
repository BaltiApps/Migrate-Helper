package balti.migrate.helper.restoreEngines.engines

import android.content.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.migrate.helper.R
import balti.migrate.helper.restoreEngines.ParentRestoreClass
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreEngines.utils.OnRestoreComplete
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_ADB
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_DPI
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_FONT_SCALE
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin.Companion.SETTINGS_TYPE_KEYBOARD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DIR_REVERT_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_GENERIC_SETTINGS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRAS_MARKER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_REVERT_HEAD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_SECURE_SETTINGS
import balti.migrate.helper.utilities.constants.AddonReceiverConstants.Companion.ACTION_ADDON_SETTINGS_BROADCAST
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_ERRORS
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_OPERATION_DO_START
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_VALUE_ADB
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_VALUE_DPI
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_VALUE_FONT_SCALE
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_EXTRA_VALUE_KEYBOARD_TEXT
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_RECEIVER_CLASS
import balti.migrate.helper.utilities.constants.AddonSettingsConstants.Companion.ADDON_SETTINGS_RECEIVER_PACKAGE_NAME
import balti.migrate.helper.utilities.constants.SettingsFields
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SettingsRestoreEngine(private val jobcode: Int,
                            private val settingsPacket: SettingsPacketKotlin): ParentRestoreClass("") {

    private val errors by lazy { ArrayList<String>(0) }
    private val suProcess by lazy { Runtime.getRuntime().exec("su") }
    private val suWriter by lazy { BufferedWriter(OutputStreamWriter(suProcess.outputStream)) }
    private val timeStamp by lazy { SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().time)}
    private val LBM by lazy { LocalBroadcastManager.getInstance(engineContext) }

    private var handler : Handler? = null
    private var runnable: Runnable? = null

    var exitWait = false

    private fun flushToSu(cmd: String, ignoreCancel: Boolean = false){
        if (!RestoreServiceKotlin.cancelAll || ignoreCancel){
            suWriter.write("$cmd\n")
            suWriter.flush()
        }
    }

    private val addonResultsReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                exitWait = true
                intent?.let {
                    it.getStringArrayListExtra(ADDON_SETTINGS_EXTRA_ERRORS)?.let {e ->
                        errors.addAll(e)
                    }
                }
            }
        }
    }

    private fun backupSettings() {

        val file = File(DIR_REVERT_DIR, "${FILE_REVERT_HEAD}$timeStamp")

        flushToSu("mkdir -p $DIR_REVERT_DIR")
        flushToSu("touch ${file.absolutePath}")

        flushToSu("am force-stop $ADDON_SETTINGS_RECEIVER_PACKAGE_NAME")

        flushToSu("echo \"{\" > ${file.absolutePath}")
        flushToSu("adbVal=\"$(settings get global adb_enabled)\"")
        flushToSu("echo \"   \\\"${SettingsFields.JSON_FIELD_ADB_TEXT}\\\": \$adbVal,\" >> ${file.absolutePath}")
        flushToSu("echo \"   \\\"${SettingsFields.JSON_FIELD_DPI_TEXT}\\\": \\\"\$(wm density)\\\",\" >> ${file.absolutePath}")
        flushToSu("fontVal=\"$(settings get system font_scale)\"")
        flushToSu("echo \"   \\\"${SettingsFields.JSON_FIELD_FONT_SCALE}\\\": \$fontVal\" >> ${file.absolutePath}")
        flushToSu("echo \"}\" >> ${file.absolutePath}")

        flushToSu("exit", true)
        suProcess.waitFor()
    }

    private fun restorePacket(){
        settingsPacket.let {
            var dpiValue: Int = -1
            var adbValue: Int = -1
            var keyboardText: String = ""
            var fontScale: Double = -1.0

            for (ip in it.internalPackets){
                if (ip.isSelected)
                    when (ip.settingsType) {
                        SETTINGS_TYPE_DPI -> dpiValue = ip.value as Int
                        SETTINGS_TYPE_ADB -> adbValue = ip.value as Int
                        SETTINGS_TYPE_KEYBOARD -> keyboardText = ip.value as String
                        SETTINGS_TYPE_FONT_SCALE -> fontScale = ip.value as Double
                    }
            }

            engineContext.startActivity(Intent().apply {
                component = ComponentName(ADDON_SETTINGS_RECEIVER_PACKAGE_NAME, ADDON_SETTINGS_RECEIVER_CLASS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ADDON_SETTINGS_EXTRA_OPERATION_DO_START, true)
                putExtra(ADDON_SETTINGS_EXTRA_VALUE_DPI, dpiValue)
                putExtra(ADDON_SETTINGS_EXTRA_VALUE_ADB, adbValue)
                putExtra(ADDON_SETTINGS_EXTRA_VALUE_KEYBOARD_TEXT, keyboardText)
                putExtra(ADDON_SETTINGS_EXTRA_VALUE_FONT_SCALE, fontScale)
            })

            Log.d(DEBUG_TAG, "started restore")

            runnable = Runnable {
                handler?.run {
                    //Log.d(DEBUG_TAG, "stat: ${RestoreServiceKotlin.cancelAll}")
                    if (exitWait || RestoreServiceKotlin.cancelAll) {
                        removeCallbacks(runnable)
                        finishJob()
                    }
                    else postDelayed(runnable, 500)
                }
            }

            handler = Handler(Looper.getMainLooper()).apply {
                post(runnable)
            }
        }
    }

    init {
        customPreExecuteFunction = {
            LBM.registerReceiver(addonResultsReceiver, IntentFilter(ACTION_ADDON_SETTINGS_BROADCAST))
        }
    }

    private fun finishJob(){
        commonTools.tryIt { LBM.unregisterReceiver(addonResultsReceiver) }

        if (errors.size == 0) File("${settingsPacket.settingsFile.absolutePath}.$EXTRAS_MARKER").createNewFile()    // mark for cleaning

        (engineContext as OnRestoreComplete).run {
            onRestoreComplete(jobcode, errors.size == 0, errors)
        }
    }

    override fun postExecuteFunction() {}

    override fun doInBackground(vararg params: Any?): Any {

        resetBroadcast(true, engineContext.getString(R.string.restoring_settings))

        try {
            if (sharedPreferences.getBoolean(PREF_BACKUP_SECURE_SETTINGS, true)) {
                backupSettings()
            }

            restorePacket()

            BufferedReader(InputStreamReader(suProcess.inputStream)).readLines().forEach {
                broadcastProgress("", it, false)
            }

            BufferedReader(InputStreamReader(suProcess.errorStream)).readLines().forEach {
                errors.add("$ERROR_GENERIC_SETTINGS: $it")
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("${ERROR_GENERIC_SETTINGS}: ${e.message}")
        }

        return 0
    }

}