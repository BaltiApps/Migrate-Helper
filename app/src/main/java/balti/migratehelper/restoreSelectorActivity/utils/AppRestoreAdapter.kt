package balti.migratehelper.restoreSelectorActivity.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import balti.migratehelper.restoreSelectorActivity.RestoreSelectorKotlin.Companion.appPackets

class AppRestoreAdapter(val context: Context,
                        val allAppSelect: CheckBox,
                        val allDataSelect: CheckBox,
                        val allPermissionSelect: CheckBox): BaseAdapter() {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun getItem(position: Int): Any = appPackets[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = appPackets.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position
}