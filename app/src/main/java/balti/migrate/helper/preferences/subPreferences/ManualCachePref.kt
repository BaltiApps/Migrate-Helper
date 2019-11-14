package balti.migrate.helper.preferences.subPreferences

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import balti.migrate.helper.AppInstance
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_MIGRATE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_MANUAL_CACHE

class ManualCachePref(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {

    override fun onBindView(view: View?) {
        super.onBindView(view)
        if (view == null) return

        val title by lazy { view.findViewById<TextView>(R.id.pref_et_title) }
        val editText by lazy { view.findViewById<EditText>(R.id.pref_et_editText) }
        val okButton by lazy { view.findViewById<Button>(R.id.pref_et_ok) }

        title.setText(R.string.manual_cache)

        AppInstance.sharedPrefs.run {

            val editor = edit()

            editText.hint = PREF_DEFAULT_MIGRATE_CACHE
            editText.setText(
                    if (MIGRATE_CACHE != PREF_DEFAULT_MIGRATE_CACHE)
                        MIGRATE_CACHE
                    else ""
            )

            okButton.setOnClickListener {
                val toStore = editText.text.toString().let { if (it == "") PREF_DEFAULT_MIGRATE_CACHE else it }
                Toast.makeText(context, toStore, Toast.LENGTH_SHORT).show()
                editor.putString(PREF_MANUAL_CACHE, toStore).apply()
            }
        }
    }

}