package balti.migratehelper.restoreSelectorActivity.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.DataSetObserver
import android.graphics.Color
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.RestoreSelectorKotlin.Companion.appPackets
import balti.migratehelper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_FDROID
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PROPERTY_APP_SELECTION
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PROPERTY_DATA_SELECTION
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PROPERTY_PERMISSION_SELECTION
import balti.migratehelper.utilities.IconTools
import kotlinx.android.synthetic.main.app_info.view.*
import kotlinx.android.synthetic.main.app_item.view.*
import java.io.File

class AppRestoreAdapter(val context: Context,
                        val allAppSelect: CheckBox,
                        val allDataSelect: CheckBox,
                        val allPermissionSelect: CheckBox): BaseAdapter() {

    private val pm: PackageManager by lazy { context.packageManager }
    private var appAllChangeFromScanning = false
    private var dataAllChangeFromScanning = false
    private var permissionAllChangeFromScanning = false
    private var externalDataSetChanged = true
    private val iconTools by lazy { IconTools() }

    init {
        appPackets.sortWith(Comparator { o1, o2 ->
            String.CASE_INSENSITIVE_ORDER.compare(o1.appName, o2.appName)
        })
    }

    init {

        fun listener(property: String, isChecked: Boolean) {
            when (property){
                PROPERTY_APP_SELECTION -> {
                    if (appAllChangeFromScanning) appAllChangeFromScanning = false
                    else for (dp in appPackets) {
                        dp.APP = isChecked && dp.apkName != null
                    }
                }

                PROPERTY_DATA_SELECTION -> {
                    if (dataAllChangeFromScanning) dataAllChangeFromScanning = false
                    else for (dp in appPackets) {
                        dp.DATA = isChecked && dp.dataName != null
                    }
                }

                PROPERTY_PERMISSION_SELECTION -> {
                    if (permissionAllChangeFromScanning) permissionAllChangeFromScanning = false
                    else for (dp in appPackets) {
                        dp.PERMISSION = isChecked && dp.isPermission
                    }
                }
            }
            externalDataSetChanged = false
            notifyDataSetChanged()
        }

        allAppSelect.setOnCheckedChangeListener { _, isChecked -> listener(PROPERTY_APP_SELECTION, isChecked) }
        allDataSelect.setOnCheckedChangeListener { _, isChecked -> listener(PROPERTY_DATA_SELECTION, isChecked) }
        allPermissionSelect.setOnCheckedChangeListener { _, isChecked -> listener(PROPERTY_PERMISSION_SELECTION, isChecked) }

        this.registerDataSetObserver(object : DataSetObserver(){
            override fun onChanged() {
                super.onChanged()
                if (externalDataSetChanged) {
                    updateAllCheckbox(PROPERTY_APP_SELECTION, allAppSelect.isChecked)
                    updateAllCheckbox(PROPERTY_DATA_SELECTION, allDataSelect.isChecked)
                    updateAllCheckbox(PROPERTY_PERMISSION_SELECTION, allPermissionSelect.isChecked)
                }
                else externalDataSetChanged = true
            }
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

        val appItem = appPackets[position]

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
            adView.app_info_size.text = context.getString(R.string.data_size) + " " + appItem.dataSize + "\n" +
                    context.getString(R.string.system_size) + " " + appItem.systemSize

            AlertDialog.Builder(context)
                    .setTitle(viewHolder.appName.text)
                    .setView(adView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }


        if (appItem.isSystemApp)
            viewHolder.appName.setTextColor(Color.RED)
        else viewHolder.appName.setTextColor(Color.YELLOW)

        appItem.packageName?.let { viewHolder.appCheckBox.setFromProperty(appItem) }
        appItem.packageName?.let { viewHolder.dataCheckBox.setFromProperty(appItem) }
        appItem.packageName?.let { viewHolder.permCheckBox.setFromProperty(appItem) }

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


    private fun updateAllCheckbox(property: String, allSelected: Boolean, immediateSelection: Boolean = true) {
        if (!immediateSelection) {
            if (allSelected) {
                when (property) {
                    PROPERTY_APP_SELECTION -> {
                        appAllChangeFromScanning = true
                        allAppSelect.isChecked = false
                    }
                    PROPERTY_DATA_SELECTION -> {
                        dataAllChangeFromScanning = true
                        allDataSelect.isChecked = false
                    }
                    PROPERTY_PERMISSION_SELECTION -> {
                        permissionAllChangeFromScanning = true
                        allPermissionSelect.isChecked = false
                    }
                }
            }
        }
        else {
            loop@ for (i in 0 until appPackets.size) {
                val dp = appPackets[i]
                when (property) {
                    PROPERTY_APP_SELECTION ->
                        if (dp.APP || dp.apkName == null) {
                            if (i == appPackets.size - 1) if (!allSelected) appAllChangeFromScanning = true
                        } else {
                            if (allSelected) appAllChangeFromScanning = true
                            break@loop
                        }

                    PROPERTY_DATA_SELECTION ->
                        if (dp.DATA || dp.dataName == null) {
                            if (i == appPackets.size - 1) if (!allSelected) dataAllChangeFromScanning = true
                        } else {
                            if (allSelected) dataAllChangeFromScanning = true
                            break@loop
                        }

                    PROPERTY_PERMISSION_SELECTION ->
                        if (dp.PERMISSION || !dp.isPermission) {
                            if (i == appPackets.size - 1) if (!allSelected) permissionAllChangeFromScanning = true
                        } else {
                            if (allSelected) permissionAllChangeFromScanning = true
                            break@loop
                        }
                }
            }

            if (appAllChangeFromScanning) allAppSelect.isChecked = !allAppSelect.isChecked
            if (dataAllChangeFromScanning) allDataSelect.isChecked = !allDataSelect.isChecked
            if (permissionAllChangeFromScanning) allPermissionSelect.isChecked = !allPermissionSelect.isChecked
        }
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
                PROPERTY_APP_SELECTION -> {
                    appItem.APP = isChecked
                    updateAllCheckbox(property, allAppSelect.isChecked, isChecked)
                }
                PROPERTY_DATA_SELECTION -> {
                    appItem.DATA = isChecked
                    updateAllCheckbox(property, allDataSelect.isChecked, isChecked)
                }
                PROPERTY_PERMISSION_SELECTION -> {
                    appItem.PERMISSION = isChecked
                    updateAllCheckbox(property, allPermissionSelect.isChecked, isChecked)
                }
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

    override fun getItem(position: Int): Any = appPackets[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = appPackets.size
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