package balti.migrate.helper.preferences

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import balti.migrate.helper.AppInstance
import balti.migrate.helper.R
import balti.migrate.helper.preferences.subPreferences.WatcherInstallPreference
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREFERENCES_INSTALL_WATCHER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_EXTRAS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_READ_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_REMOUNT_DATA
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_RESTORE_START_ANIMATION
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_USE_WATCHER
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME

class MainPreferencesActivity: PreferenceActivity() {

    private val ignoreReadErrors: CheckBoxPreference by lazy { findPreference("ignoreReadErrors") as CheckBoxPreference }
    private val ignoreExtras: CheckBoxPreference by lazy { findPreference("ignoreExtras") as CheckBoxPreference }
    private val restoreStartAnimation: CheckBoxPreference by lazy { findPreference("restoreStartAnimation") as CheckBoxPreference }

    private val useWatcher: CheckBoxPreference by lazy { findPreference("useWatcher") as CheckBoxPreference }
    private val installWatcherLayout: WatcherInstallPreference by lazy { findPreference("installWatcherLayout") as WatcherInstallPreference }

    private val remountData: CheckBoxPreference by lazy { findPreference("remountData") as CheckBoxPreference }

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val isAbove10 = Build.VERSION.SDK_INT > Build.VERSION_CODES.P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        AppInstance.sharedPrefs.run {

            val editor = edit()

            fun setValue(checkbox: CheckBoxPreference, field: String, defaultValue: Boolean = false){

                if (checkbox == useWatcher) {
                    checkbox.isChecked = if (isAbove10) getBoolean(field, defaultValue) else false
                    toggleWatcherInstaller(checkbox.isChecked)
                }
                else checkbox.isChecked = getBoolean(field, defaultValue)

                checkbox.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    editor.putBoolean(field, newValue as Boolean)
                    editor.apply()
                    if (checkbox == useWatcher) toggleWatcherInstaller(newValue)
                    true
                }
            }

            setValue(ignoreReadErrors, PREF_IGNORE_READ_ERRORS)
            setValue(ignoreExtras, PREF_IGNORE_EXTRAS)
            setValue(restoreStartAnimation, PREF_RESTORE_START_ANIMATION, true)
            setValue(useWatcher, PREF_USE_WATCHER, true)
            setValue(remountData, PREF_REMOUNT_DATA, false)

            if (isAbove10)
                useWatcher.isEnabled = true
            else {
                setValue(useWatcher, PREF_USE_WATCHER, false)
                useWatcher.isEnabled = false
                installWatcherLayout.isEnabled = false
            }

        }
    }

    private fun toggleWatcherInstaller(checkbox: Boolean = true){
        installWatcherLayout.isEnabled =
                if (checkbox)
                    isAbove10 && !commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME)
                else false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == JOBCODE_PREFERENCES_INSTALL_WATCHER) {
            if (commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME))
                useWatcher.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        toggleWatcherInstaller(useWatcher.isChecked)
    }

}