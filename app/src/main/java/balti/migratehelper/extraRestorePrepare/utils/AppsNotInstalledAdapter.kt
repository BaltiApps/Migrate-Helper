package balti.migratehelper.extraRestorePrepare.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migratehelper.utilities.IconTools
import kotlinx.android.synthetic.main.apps_not_installed_item.view.*
import java.io.File

class AppsNotInstalledAdapter(private val context: Context, private val appsNotInstalled: ArrayList<AppPacketsKotlin>) : BaseAdapter() {

    private val iconTools by lazy { IconTools() }
    private val commonTools by lazy { CommonToolsKotlin(context) }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val viewHolder : ViewHolder
        var view = convertView

        if (view == null){

            view = View.inflate(context, R.layout.apps_not_installed_item, null)

            viewHolder = ViewHolder()
            viewHolder.appIcon = view.apps_not_installed_icon
            viewHolder.appName = view.apps_not_installed_text
            viewHolder.installButton = view.apps_not_installed_button

            view.tag = viewHolder
        }
        else viewHolder = view.tag as ViewHolder

        val appItem = appsNotInstalled[position]

        viewHolder.appName.text = appItem.appName
        appItem.iconFileName?.run {
            iconTools.setIconFromFile(viewHolder.appIcon, File(METADATA_HOLDER_DIR, this))
        }
        appItem.appIcon?.run {
            iconTools.setIconFromIconString(viewHolder.appIcon, this)
        }
        viewHolder.installButton.setOnClickListener {
            appItem.packageName?.let {commonTools.playStoreLink(it)}
        }

        return view!!
    }

    override fun getItem(position: Int): Any = appsNotInstalled[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = appsNotInstalled.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position

    private class ViewHolder {
        lateinit var appIcon: ImageView
        lateinit var appName: TextView
        lateinit var installButton: Button
    }
}