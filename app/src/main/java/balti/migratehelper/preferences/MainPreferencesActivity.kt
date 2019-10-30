package balti.migratehelper.preferences

import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_EXTRAS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_READ_ERRORS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_RESTORE_START_ANIMATION
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_USE_WATCHER

class MainPreferencesActivity: PreferenceActivity() {

    private val ignoreReadErrors: CheckBoxPreference by lazy { findPreference("ignoreReadErrors") as CheckBoxPreference }
    private val ignoreExtras: CheckBoxPreference by lazy { findPreference("ignoreExtras") as CheckBoxPreference }
    private val restoreStartAnimation: CheckBoxPreference by lazy { findPreference("restoreStartAnimation") as CheckBoxPreference }
    private val useWatcher: CheckBoxPreference by lazy { findPreference("useWatcher") as CheckBoxPreference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        AppInstance.sharedPrefs.run {

            val editor = edit()

            fun setValue(checkbox: CheckBoxPreference, field: String, defaultValue: Boolean = false){
                checkbox.isChecked = getBoolean(field, defaultValue)
                checkbox.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    editor.putBoolean(field, newValue as Boolean)
                    editor.apply()
                    true
                }
            }

            setValue(ignoreReadErrors, PREF_IGNORE_READ_ERRORS)
            setValue(ignoreExtras, PREF_IGNORE_EXTRAS)
            setValue(restoreStartAnimation, PREF_RESTORE_START_ANIMATION, true)
            setValue(useWatcher, PREF_USE_WATCHER, true)

        }
    }

}