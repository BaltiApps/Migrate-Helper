package balti.migratehelper.extrasRestore.engines

import android.support.v4.app.Fragment
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import balti.migratehelper.R
import balti.migratehelper.extrasRestore.ExtraRestoreProgress

class ContactsRestoreEngine: Fragment() {

    private val contactsView by lazy {
        View.inflate(activity, R.layout.contacts_dialog_view, null).apply {
            val holder = this.findViewById<LinearLayout>(R.id.contact_files_display_holder)
            for (cp in ExtraRestoreProgress.contactsPackets){
                if (cp.isSelected) {
                    holder.addView(TextView(activity).apply {
                        text = cp.vcfFile.name
                    })
                }
            }
        }
    }


}