package balti.migrate.helper.restoreSelectorActivity.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_FDROID
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PROPERTY_APP_SELECTION
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PROPERTY_DATA_SELECTION
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PROPERTY_PERMISSION_SELECTION
import balti.migrate.helper.utilities.IconTools
import kotlinx.android.synthetic.main.app_info.view.*
import kotlinx.android.synthetic.main.app_item.view.*
import java.io.File

class SearchAppAdapter(val tmpList: ArrayList<AppPacketsKotlin>, val context: Context): BaseAdapter() {

    private val iconTools by lazy { IconTools() }
    private val commonTools by lazy { CommonToolsKotlin(context) }

    init {
        tmpList.sortWith(Comparator { o1, o2 ->
            String.CASE_INSENSITIVE_ORDER.compare(o1.appName, o2.appName)
        })
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val viewHolder : ViewHolder
        var view = convertView

        if (view == null){

            view = View.inflate(context, R.layout.app_item, null)

            viewHolder = ViewHolder()
            viewHolder.appIcon = view.appIcon
            viewHolder.appName = view.appName
            viewHolder.appInfo = view.appInfo
            viewHolder.appCheckBox = view.appCheckbox
            viewHolder.dataCheckBox = view.dataCheckbox
            viewHolder.permCheckBox = view.permissionsCheckbox

            view.tag = viewHolder
        }
        else viewHolder = view.tag as ViewHolder

        val appItem = tmpList[position]

        viewHolder.appName.text = appItem.appName
        appItem.iconFileName?.run{
            iconTools.setIconFromFile(viewHolder.appIcon, File(METADATA_HOLDER_DIR, this))
        }
        appItem.appIcon?.run{
            iconTools.setIconFromIconString(viewHolder.appIcon, this)
        }

        viewHolder.appInfo.setOnClickListener {

            val adView = View.inflate(context, R.layout.app_info, null)
            adView.app_info_package_name.text = context.getString(R.string.packageName) + " " + appItem.packageName
            adView.app_info_version_name.text = context.getString(R.string.versionName) + " " + appItem.version
            adView.app_info_installer.text = context.getString(R.string.installer) + " " + when (appItem.installerName) {
                PACKAGE_NAME_PLAY_STORE -> context.getString(R.string.play_store)
                PACKAGE_NAME_FDROID -> context.getString(R.string.f_droid)
                else -> appItem.installerName.let { if (it != "" && it != "NULL") it else "" }
            }
            adView.app_info_size.text = context.getString(R.string.data_size) + " " + commonTools.getHumanReadableStorageSpace(appItem.dataSize) + "\n" +
                    context.getString(R.string.system_size) + " " + commonTools.getHumanReadableStorageSpace(appItem.systemSize)

            AlertDialog.Builder(context)
                    .setTitle(viewHolder.appName.text)
                    .setView(adView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }


        if (appItem.isSystemApp)
            viewHolder.appName.setTextColor(Color.RED)

        viewHolder.appCheckBox.setFromProperty(appItem)
        viewHolder.dataCheckBox.setFromProperty(appItem)
        viewHolder.permCheckBox.setFromProperty(appItem)

        view?.setOnClickListener {
            val isAllSelected = (appItem.APP || appItem.apkName == null)
                    && (appItem.DATA || appItem.dataName == null)
                    && (appItem.PERMISSION || !appItem.isPermission)

            if (appItem.apkName != null)
                viewHolder.appCheckBox.isChecked = !isAllSelected

            if (appItem.dataName != null)
                viewHolder.dataCheckBox.isChecked = !isAllSelected

            if (appItem.isPermission)
                viewHolder.permCheckBox.isChecked = !isAllSelected

        }

        return view!!
    }

    private fun CheckBox.setFromProperty(appItem: AppPacketsKotlin){

        val property = when (this.id){
            R.id.appCheckbox -> PROPERTY_APP_SELECTION
            R.id.dataCheckbox -> PROPERTY_DATA_SELECTION
            R.id.permissionsCheckbox -> PROPERTY_PERMISSION_SELECTION
            else -> ""
        }

        this.setOnCheckedChangeListener {_, isChecked ->
            when (property) {
                PROPERTY_APP_SELECTION -> appItem.APP = isChecked
                PROPERTY_DATA_SELECTION -> appItem.DATA = isChecked
                PROPERTY_PERMISSION_SELECTION -> appItem.PERMISSION = isChecked
            }
        }

        if (appItem.apkName == null && property == PROPERTY_APP_SELECTION) this.visibility = View.INVISIBLE
        if (appItem.dataName == null && property == PROPERTY_DATA_SELECTION) this.visibility = View.INVISIBLE
        if (!appItem.isPermission && property == PROPERTY_PERMISSION_SELECTION) this.visibility = View.INVISIBLE

        if (this.visibility == View.VISIBLE) this.isChecked = when (property) {
            PROPERTY_APP_SELECTION -> appItem.APP
            PROPERTY_DATA_SELECTION -> appItem.DATA
            PROPERTY_PERMISSION_SELECTION -> appItem.PERMISSION
            else -> false
        }
    }

    override fun getItem(position: Int): Any = tmpList[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = tmpList.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position

    private class ViewHolder {

        lateinit var appIcon: ImageView
        lateinit var appName: TextView
        lateinit var appInfo: ImageView
        lateinit var appCheckBox: CheckBox
        lateinit var dataCheckBox: CheckBox
        lateinit var permCheckBox: CheckBox

    }
}