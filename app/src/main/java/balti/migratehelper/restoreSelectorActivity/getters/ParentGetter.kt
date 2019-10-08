package balti.migratehelper.restoreSelectorActivity.getters

import android.content.Context
import android.os.AsyncTask
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.OnReadComplete
import balti.migratehelper.utilities.ViewOperations
import java.io.File
import java.io.FileFilter

abstract class ParentGetter(private val jobCode: Int,
                            private val metadataHolderPath: String,
                            private val context: Context,
                            private val progressBar: ProgressBar,
                            private val waitingText: TextView,
                            private val initWaitingTextResId: Int): AsyncTask<Any, Any, Any>() {

    abstract var fileFilter: FileFilter
    private val vOp by lazy { ViewOperations(context) }

    private val onReadComplete by lazy { context as OnReadComplete }

    private val directory by lazy { File(metadataHolderPath) }
    lateinit var files: Array<File>

    val errors by lazy { ArrayList<String>(0) }

    override fun onPreExecute() {
        super.onPreExecute()

        vOp.textSet(waitingText, initWaitingTextResId)

        files = if (directory.isDirectory) { directory.listFiles(fileFilter) }
        else {
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