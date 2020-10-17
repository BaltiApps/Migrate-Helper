package balti.migrate.helper.restoreSelectorActivity.getters

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.utils.GetterType
import balti.migrate.helper.restoreSelectorActivity.utils.OnReadComplete
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_PRE_EXECUTE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.WIFI_FILE_NAME
import balti.migrate.helper.utilities.ViewOperations
import java.io.File
import java.io.FileFilter

abstract class ParentGetter(private val jobCode: Int,
                            private val metadataHolderPath: String,
                            private val context: Context,
                            private val progressBar: ProgressBar,
                            private val waitingText: TextView,
                            private val initWaitingTextResId: Int,
                            private val getterType: GetterType): AsyncTask<Any, Any, Any>() {

    private val vOp by lazy { ViewOperations(context) }

    private val onReadComplete by lazy { context as OnReadComplete }

    private val directory by lazy { File(metadataHolderPath) }
    lateinit var files: Array<File>

    val errors by lazy { ArrayList<String>(0) }

    override fun onPreExecute() {
        super.onPreExecute()

        try {
            vOp.textSet(waitingText, initWaitingTextResId)

            val fileFilter =
                    FileFilter {
                        when (getterType) {
                            GetterType.GETTER_TYPE_APPS -> it.name.endsWith(".json") && it.name != CommonToolsKotlin.BACKUP_NAME_SETTINGS
                            GetterType.GETTER_TYPE_CALLS -> it.name.endsWith(".calls.db")
                            GetterType.GETTER_TYPE_CONTACTS -> it.name.endsWith(".vcf")
                            GetterType.GETTER_TYPE_SETTINGS -> it.name == BACKUP_NAME_SETTINGS
                            GetterType.GETTER_TYPE_SMS -> it.name.endsWith(".sms.db")
                            GetterType.GETTER_TYPE_WIFI -> it.name == WIFI_FILE_NAME
                        }
                    }

            files = if (directory.isDirectory) {
                directory.listFiles(fileFilter)
            } else {
                errors.add("$metadataHolderPath ${vOp.getStringFromRes(R.string.path_not_valid_directory)}")
                arrayOf()
            }

            vOp.doSomething {
                progressBar.apply {
                    visibility = View.VISIBLE
                    max = files.size
                    progress = 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errors.add("$ERROR_PRE_EXECUTE: $jobCode - ${e.message}")
        }
    }

    override fun onProgressUpdate(vararg values: Any?) {
        super.onProgressUpdate(*values)
        vOp.doSomething { progressBar.progress = values[0] as Int }
    }

    override fun onPostExecute(result: Any) {
        super.onPostExecute(result)

        vOp.textSet(waitingText, R.string.please_wait)
        vOp.visibilitySet(progressBar, View.INVISIBLE)

        vOp.doSomething {
            if (errors.size == 0)
                onReadComplete.onComplete(jobCode, true, result)
            else onReadComplete.onComplete(jobCode, false, errors)
        }
    }
}