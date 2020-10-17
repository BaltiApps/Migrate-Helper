package balti.migrate.helper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migrate.helper.restoreSelectorActivity.utils.GetterType
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_APP_JSON
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_APP_JSON_TRY_CATCH
import org.json.JSONObject

class GetAppPackets(jobCode: Int,
                    metadataHolderPath: String,
                    context: Context,
                    progressBar: ProgressBar,
                    waitingText: TextView): ParentGetter(jobCode, metadataHolderPath, context,
        progressBar, waitingText, R.string.getting_apps, GetterType.GETTER_TYPE_APPS) {

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (files.isNotEmpty()) {

                val appPackets = ArrayList<AppPacketsKotlin>(0)

                var c = 0
                files.forEach {
                    if (!RestoreSelectorKotlin.cancelLoading) {
                        try {
                            val contents = StringBuffer("")
                            it.readLines().forEach { line ->
                                contents.append("$line\n")
                            }
                            appPackets.add(AppPacketsKotlin(JSONObject(contents.toString()), it))
                        } catch (e: Exception) {
                            errors.add("$ERROR_APP_JSON: ${it.name} - ${e.message.toString()}")
                        }
                        publishProgress(++c)
                    }
                }

                return appPackets
            }
        }
        catch (e: Exception){
            errors.add("$ERROR_APP_JSON_TRY_CATCH: ${e.message.toString()}")
        }

        return 0
    }
}