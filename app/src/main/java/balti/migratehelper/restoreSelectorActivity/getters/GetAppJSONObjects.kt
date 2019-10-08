package balti.migratehelper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migratehelper.R
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_APP_JSON
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_APP_JSON_TRY_CATCH
import org.json.JSONObject
import java.io.FileFilter

class GetAppJSONObjects(jobCode: Int,
                        metadataHolderPath: String,
                        context: Context,
                        progressBar: ProgressBar,
                        waitingText: TextView): ParentGetter(jobCode, metadataHolderPath, context, progressBar, waitingText, R.string.getting_apps) {

    override var fileFilter: FileFilter = FileFilter {
        it.endsWith(".json") && it.name != BACKUP_NAME_SETTINGS
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (files.isNotEmpty()) {

                val jsonObjects = ArrayList<JSONObject>(0)

                var c = 0
                files.forEach {
                    try {
                        val contents = StringBuffer("")
                        it.readLines().forEach { line ->
                            contents.append("$line\n")
                        }
                        jsonObjects.add(JSONObject(contents.toString()))
                    }
                    catch (e: Exception){
                        errors.add("$ERROR_APP_JSON: ${it.name} - ${e.message.toString()}")
                    }
                    publishProgress(++c)
                }

                return jsonObjects
            }
        }
        catch (e: Exception){
            errors.add("$ERROR_APP_JSON_TRY_CATCH: ${e.message.toString()}")
        }

        return 0
    }
}