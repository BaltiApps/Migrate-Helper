package balti.migratehelper.preferences.subPreferences

import android.content.Context
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_METADATA_HOLDER
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_MANUAL_METADATA_HOLDER

class ManualMetadataHolder(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {

    override fun onBindView(view: View?) {
        super.onBindView(view)
        if (view == null) return

        val title by lazy { view.findViewById<TextView>(R.id.pref_et_title) }
        val editText by lazy { view.findViewById<EditText>(R.id.pref_et_editText) }
        val okButton by lazy { view.findViewById<Button>(R.id.pref_et_ok) }

        title.setText(R.string.manual_metadata_holder)

        AppInstance.sharedPrefs.run {

            val editor = edit()

            editText.hint = PREF_DEFAULT_METADATA_HOLDER
            editText.setText(
                    if (METADATA_HOLDER_DIR != PREF_DEFAULT_METADATA_HOLDER)
                        METADATA_HOLDER_DIR
                    else ""
            )

            okButton.setOnClickListener {
                val toStore = editText.text.toString().let { if (it == "") PREF_DEFAULT_METADATA_HOLDER else it }
                Toast.makeText(context, toStore, Toast.LENGTH_SHORT).show()
                editor.putString(PREF_MANUAL_METADATA_HOLDER, toStore).apply()
            }
        }
    }

}