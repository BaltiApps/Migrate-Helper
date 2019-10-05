package balti.migratehelper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.containers.ContactsPacketKotlin
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_CALLS_GET_TRY_CATCH
import java.io.FileFilter

class GetCallsPackets(jobCode: Int,
                      directoryPath: String,
                      context: Context,
                      progressBar: ProgressBar,
                      waitingText: TextView): ParentGetter(jobCode, directoryPath, context, progressBar, waitingText, R.string.getting_calls) {

    override var fileFilter = FileFilter {
        it.endsWith(".calls.db")
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (files.isNotEmpty()) {

                val contactPackets = ArrayList<ContactsPacketKotlin>(0)

                var c = 0
                files.forEach {
                    contactPackets.add(ContactsPacketKotlin(it, true))
                    publishProgress(++c)
                }

                Thread.sleep(CommonToolsKotlin.DUMMY_WAIT_TIME)

                return contactPackets
            }
        } catch (e: Exception) {
            errors.add("$ERROR_CALLS_GET_TRY_CATCH: ${e.message.toString()}")
        }

        return 0
    }
}