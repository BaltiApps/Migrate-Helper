package balti.migratehelper.restoreSelectorActivity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.containers.*
import balti.migratehelper.restoreSelectorActivity.getters.*
import balti.migratehelper.restoreSelectorActivity.utils.RootCopyTask
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_MAIN_READ_TRY_CATCH
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_APP_JSON
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_CALLS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_CONTACTS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_SETTINGS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_SMS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_WIFI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_ROOT_COPY
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_READ_ERRORS
import kotlinx.android.synthetic.main.restore_selector.*
import org.json.JSONObject

class RestoreSelectorKotlin: AppCompatActivity(), OnReadComplete {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val allErrors by lazy { ArrayList<String>(0) }

    private val appJsons by lazy { ArrayList<JSONObject>(0) }
    private val contactDataPackets by lazy { ArrayList<ContactsPacketKotlin>(0) }
    private val smsDataPackets by lazy { ArrayList<SmsPacketKotlin>(0) }
    private val callsDataPackets by lazy { ArrayList<CallsPacketKotlin>(0) }
    private var settingsPacket : SettingsPacketKotlin? = null
    private var wifiPacket : WifiPacketKotlin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.restore_selector)
        doJob(JOBCODE_ROOT_COPY)
    }

    private fun showError(mainMessage: String, description: String){

        waiting_layout.visibility = View.VISIBLE

        just_a_progress.visibility = View.INVISIBLE
        restore_selector_error_icon.visibility = View.VISIBLE
        waiting_status_text.text = mainMessage
        waiting_desc.text = description
        restore_selector_action_button.setText(R.string.close)
        restore_selector_action_button.background = getDrawable(R.drawable.next)
        restore_selector_action_button.visibility = View.VISIBLE

        restore_selector_content.visibility = View.GONE
    }

    private fun doJob(jCode: Int){
        val task = when (jCode){
            JOBCODE_ROOT_COPY -> RootCopyTask(jCode, MIGRATE_CACHE, this)
            JOBCODE_GET_APP_JSON -> GetAppJSONObjects(jCode, METADATA_HOLDER_DIR, this, restore_selector_progress_bar, waiting_status_text)
            JOBCODE_GET_CONTACTS -> GetContactPackets(jCode, METADATA_HOLDER_DIR, this, restore_selector_progress_bar, waiting_status_text)
            JOBCODE_GET_SMS -> GetSmsPackets(jCode, METADATA_HOLDER_DIR, this, restore_selector_progress_bar, waiting_status_text)
            JOBCODE_GET_CALLS -> GetCallsPackets(jCode, METADATA_HOLDER_DIR, this, restore_selector_progress_bar, waiting_status_text)
            JOBCODE_GET_SETTINGS -> GetSettingsKotlin(jCode, METADATA_HOLDER_DIR, this, restore_selector_progress_bar, waiting_status_text)
            JOBCODE_GET_WIFI -> GetWifiPacketKotlin(jCode, METADATA_HOLDER_DIR, this, restore_selector_progress_bar, waiting_status_text)
            else -> null
        }

        task?.execute()
    }

    override fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any) {

        try {

            fun handleResults(nextJob: Int?, func: () -> Unit) {
                if (!jobSuccess) {
                    if (nextJob != null && AppInstance.sharedPrefs.getBoolean(PREF_IGNORE_READ_ERRORS, false)) {
                        allErrors.add(jobResult.toString())
                        doJob(nextJob)
                    } else showError(getString(R.string.code_error), jobResult.toString())
                } else {
                    func()
                    nextJob?.let { doJob(it) }
                }
            }

            when (jobCode) {

                JOBCODE_ROOT_COPY -> {
                    if (!jobSuccess)
                        showError(getString(R.string.are_you_rooted), "${getString(R.string.are_you_rooted_desc)}\n\n$jobResult".trim())
                    else doJob(JOBCODE_GET_APP_JSON)
                }

                JOBCODE_GET_APP_JSON ->
                    handleResults(JOBCODE_GET_CONTACTS) { appJsons.addAll(jobResult as ArrayList<JSONObject>) }

                JOBCODE_GET_CONTACTS ->
                    handleResults(JOBCODE_GET_SMS) { contactDataPackets.addAll(jobResult as ArrayList<ContactsPacketKotlin>) }

                JOBCODE_GET_SMS ->
                    handleResults(JOBCODE_GET_CALLS) { smsDataPackets.addAll(jobResult as ArrayList<SmsPacketKotlin>) }

                JOBCODE_GET_CALLS ->
                    handleResults(JOBCODE_GET_SETTINGS) { callsDataPackets.addAll(jobResult as ArrayList<CallsPacketKotlin>) }

                JOBCODE_GET_SETTINGS ->
                    handleResults(JOBCODE_GET_WIFI) { settingsPacket = jobResult as SettingsPacketKotlin }

                JOBCODE_GET_WIFI ->
                    handleResults(null) { wifiPacket = jobResult as WifiPacketKotlin }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            showError(getString(R.string.code_error), "$ERROR_MAIN_READ_TRY_CATCH: ${e.message}")
        }
    }

}