package balti.migratehelper.extraRestorePrepare.utils

import android.content.Context
import android.view.View
import android.widget.Button
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin
import kotlinx.android.synthetic.main.apps_not_installed_layout.view.*

class AppsNotInstalledViewManager(private val appListNotInstalled: ArrayList<AppPacketsKotlin>, private val context: Context) {

    private val commonTools by lazy { CommonToolsKotlin(context) }
    private val mainView by lazy { View.inflate(context, R.layout.apps_not_installed_layout, null) }

    fun getView(): View {

        var adapter: AppsNotInstalledAdapter? = null

        commonTools.doBackgroundTask({
            adapter = AppsNotInstalledAdapter(context, appListNotInstalled)
        }, {
            mainView.apps_not_installed_listView.invalidate()
            mainView.apps_not_installed_listView.adapter = adapter
        })

        return mainView
    }

    fun getRefreshButton(): Button {
        return (mainView.apps_not_installed_refresh as Button)
    }

}