package balti.migratehelper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migratehelper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_SETTINGS_GET_TRY_CATCH
import balti.migratehelper.utilities.constants.SettingsFields
import org.json.JSONObject
import java.io.FileFilter

class GetSettingsKotlin(jobCode: Int,
                        metadataHolderPath: String,
                        context: Context,
                        progressBar: ProgressBar,
                        waitingText: TextView): ParentGetter(jobCode, metadataHolderPath, context, progressBar, waitingText, R.string.getting_wifi) {

    override var fileFilter = FileFilter {
        it.name == CommonToolsKotlin.BACKUP_NAME_SETTINGS
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (files.isNotEmpty()) {

                val contents = StringBuffer("")
                files[0].readLines().forEach { line ->
                    contents.append("$line\n")
                }

                val jsonObject = JSONObject(contents.toString())

                val dpiText = SettingsFields.JSON_FIELD_DPI_TEXT.let { if (jsonObject.has(it)) jsonObject.getString(it) else null }
                val adbState = SettingsFields.JSON_FIELD_ADB_TEXT.let { if (jsonObject.has(it)) jsonObject.getInt(it) else null }
                val fontScale = SettingsFields.JSON_FIELD_FONT_SCALE.let { if (jsonObject.has(it)) jsonObject.getDouble(it) else null }
                val keyboardText = SettingsFields.JSON_FIELD_KEYBOARD_TEXT.let { if (jsonObject.has(it)) jsonObject.getString(it) else null }

                if (!RestoreSelectorKotlin.cancelAll) Thread.sleep(DUMMY_WAIT_TIME)

                return SettingsPacketKotlin(dpiText, adbState, fontScale, keyboardText)

            }
        } catch (e: Exception) {
            errors.add("$ERROR_SETTINGS_GET_TRY_CATCH: ${e.message.toString()}")
        }

        return 0
    }
}