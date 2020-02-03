package balti.migrate.helper.preferences

import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import balti.migrate.helper.AppInstance
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_SECURE_SETTINGS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_DO_LOAD_ICON_IN_LIST
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_EXTRAS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_READ_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_LOAD_EXTRAS_ON_UI_THREAD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_REMOUNT_ALL_TO_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_REMOUNT_DATA
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_RESTORE_START_ANIMATION
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_TRACK_RESTORE_FINISHED

class MainPreferencesActivity: PreferenceActivity() {

    private val ignoreReadErrors: CheckBoxPreference by lazy { findPreference("ignoreReadErrors") as CheckBoxPreference }
    private val ignoreExtras: CheckBoxPreference by lazy { findPreference("ignoreExtras") as CheckBoxPreference }
    private val restoreStartAnimation: CheckBoxPreference by lazy { findPreference("restoreStartAnimation") as CheckBoxPreference }
    private val loadAppIconsInList: CheckBoxPreference by lazy { findPreference("loadAppIconsInList") as CheckBoxPreference }
    private val backupSecureSettings: CheckBoxPreference by lazy { findPreference("backupSecureSettings") as CheckBoxPreference }

    private val remountData: CheckBoxPreference by lazy { findPreference("remountData") as CheckBoxPreference }
    private val loadExtrasOnUiThread: CheckBoxPreference by lazy { findPreference("loadExtrasOnUiThread") as CheckBoxPreference }
    private val trackRestoreFinished: CheckBoxPreference by lazy { findPreference("trackRestoreFinished") as CheckBoxPreference }
    private val remountRetryUninstall: CheckBoxPreference by lazy { findPreference("remountRetryUninstall") as CheckBoxPreference }

    private val commonTools by lazy { CommonToolsKotlin(this) }

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
            setValue(loadAppIconsInList, PREF_DO_LOAD_ICON_IN_LIST, true)
            setValue(backupSecureSettings, PREF_BACKUP_SECURE_SETTINGS, true)
            setValue(remountData, PREF_REMOUNT_DATA, false)
            setValue(loadExtrasOnUiThread, PREF_LOAD_EXTRAS_ON_UI_THREAD, false)
            setValue(trackRestoreFinished, PREF_TRACK_RESTORE_FINISHED, true)
            setValue(remountRetryUninstall, PREF_REMOUNT_ALL_TO_UNINSTALL, false)
        }
    }

}