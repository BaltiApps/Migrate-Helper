package balti.migrate.helper.emergencyRestore.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import balti.migrate.helper.R
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import kotlinx.android.synthetic.main.apps_not_installed_item.view.*

class MissingPackagesAdapter(private val context: Context, private val missingPackages: ArrayList<String>) : BaseAdapter() {

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

        val packageName = missingPackages[position]

        viewHolder.appName.text = packageName
        viewHolder.appIcon.setImageResource(R.drawable.ic_app)
        viewHolder.installButton.setOnClickListener {
            playStoreLink(packageName)
        }

        return view!!
    }

    override fun getItem(position: Int): Any = missingPackages[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = missingPackages.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position

    private class ViewHolder {
        lateinit var appIcon: ImageView
        lateinit var appName: TextView
        lateinit var installButton: Button
    }
}