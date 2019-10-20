package balti.migratehelper.extraRestorePrepare.utils

import android.content.Context
import android.view.View
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin
import kotlinx.android.synthetic.main.apps_not_installed_layout.view.*

class AppsNotInstalledViewManager(private val appListNotInstalled: ArrayList<AppPacketsKotlin>, private val context: Context) {

    private val commonTools by lazy { CommonToolsKotlin(context) }

    fun getView(): View {

        val view = View.inflate(context, R.layout.apps_not_installed_layout, null)
        var adapter: AppsNotInstalledAdapter? = null

        commonTools.doBackgroundTask({
            adapter = AppsNotInstalledAdapter(context, appListNotInstalled)
        }, {
            view.apps_not_installed_listView.invalidate()
            view.apps_not_installed_listView.adapter = adapter
        })

        return view
    }

}