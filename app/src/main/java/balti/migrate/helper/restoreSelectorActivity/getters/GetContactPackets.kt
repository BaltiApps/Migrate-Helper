package balti.migrate.helper.restoreSelectorActivity.getters

import android.content.Context
import android.widget.ProgressBar
import android.widget.TextView
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.ContactsPacketKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_CONTACTS_GET_TRY_CATCH
import java.io.FileFilter

class GetContactPackets(jobCode: Int,
                        metadataHolderPath: String,
                        context: Context,
                        progressBar: ProgressBar,
                        waitingText: TextView): ParentGetter(jobCode, metadataHolderPath, context, progressBar, waitingText, R.string.getting_contacts) {

    override var fileFilter = FileFilter {
        it.name.endsWith(".vcf")
    }

    override fun doInBackground(vararg params: Any?): Any {
        try {
            if (files.isNotEmpty()) {

                val contactPackets = ArrayList<ContactsPacketKotlin>(0)

                var c = 0
                files.forEach {
                    if (!RestoreSelectorKotlin.cancelLoading) {
                        contactPackets.add(ContactsPacketKotlin(it, true))
                        publishProgress(++c)
                    }
                }

                if (!RestoreSelectorKotlin.cancelLoading) Thread.sleep(DUMMY_WAIT_TIME)

                return contactPackets
            }
        }
        catch (e: Exception){
            errors.add("$ERROR_CONTACTS_GET_TRY_CATCH: ${e.message.toString()}")
        }

        return 0
    }
}