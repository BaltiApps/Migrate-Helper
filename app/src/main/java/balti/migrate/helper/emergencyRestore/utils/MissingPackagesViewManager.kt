package balti.migrate.helper.emergencyRestore.utils

import android.content.Context
import android.view.View
import android.widget.ImageButton
import androidx.core.widget.TextViewCompat.setTextAppearance
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin
import kotlinx.android.synthetic.main.apps_not_installed_layout.view.*

class MissingPackagesViewManager(private val missingPackages: ArrayList<String>, private val context: Context) {

    private val commonTools by lazy { CommonToolsKotlin(context) }
    private val mainView by lazy { View.inflate(context, R.layout.apps_not_installed_layout, null) }

    fun getView(): View {

        mainView.apps_not_installed_header.apply {
            setText(R.string.please_install_these)
            setTextAppearance(this, android.R.style.TextAppearance_Medium)
            setPadding(10, 10, 10, 0)
        }
        mainView.apps_not_installed_footer.visibility = View.GONE

        var adapter: MissingPackagesAdapter? = null

        commonTools.doBackgroundTask({
            adapter = MissingPackagesAdapter(context, missingPackages)
        }, {
            mainView.apps_not_installed_listView.invalidate()
            mainView.apps_not_installed_listView.adapter = adapter
        })

        return mainView
    }

    fun getRefreshButton(): ImageButton {
        return (mainView.apps_not_installed_refresh as ImageButton)
    }

}