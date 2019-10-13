package balti.migratehelper.restoreSelectorActivity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.containers.*
import balti.migratehelper.restoreSelectorActivity.getters.*
import balti.migratehelper.restoreSelectorActivity.utils.AppRestoreAdapter
import balti.migratehelper.restoreSelectorActivity.utils.OnReadComplete
import balti.migratehelper.restoreSelectorActivity.utils.RootCopyTask
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.ERROR_MAIN_READ_TRY_CATCH
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.EXTRA_VIEW_COUNT
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_END_ALL
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_APP_JSON
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_CALLS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_CONTACTS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_SETTINGS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_SMS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_WIFI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_ROOT_COPY
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_EXTRAS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_READ_ERRORS
import kotlinx.android.synthetic.main.app_selector_header.view.*
import kotlinx.android.synthetic.main.extra_item.view.*
import kotlinx.android.synthetic.main.extras_picker.view.*
import kotlinx.android.synthetic.main.restore_selector.*

class RestoreSelectorKotlin: AppCompatActivity(), OnReadComplete {

    companion object {
        val appPackets by lazy { ArrayList<AppPacketsKotlin>(0) }
        val contactDataPackets by lazy { ArrayList<ContactsPacketKotlin>(0) }
        val smsDataPackets by lazy { ArrayList<SmsPacketKotlin>(0) }
        val callsDataPackets by lazy { ArrayList<CallsPacketKotlin>(0) }
        var settingsPacket: SettingsPacketKotlin? = null
        var wifiPacket: WifiPacketKotlin? = null
    }

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val allErrors by lazy { ArrayList<String>(0) }

    private val extrasContainer by lazy { layoutInflater.inflate(R.layout.extras_picker, app_list, false) }
    private val appBar by lazy { layoutInflater.inflate(R.layout.app_selector_header, app_list, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.restore_selector)
        doJob(JOBCODE_ROOT_COPY)
    }

    private fun showError(mainMessage: String, description: String){

        waiting_layout.visibility = View.VISIBLE
        app_list.visibility = View.GONE

        just_a_progress.visibility = View.INVISIBLE
        restore_selector_error_icon.visibility = View.VISIBLE
        waiting_status_text.text = mainMessage
        waiting_desc.text = description
        restore_selector_action_button.setText(R.string.close)
        restore_selector_action_button.background = getDrawable(R.drawable.next)
        restore_selector_action_button.visibility = View.VISIBLE
    }

    private fun doJob(jCode: Int){
        val task = when (jCode){
            JOBCODE_ROOT_COPY -> RootCopyTask(jCode, MIGRATE_CACHE, this)
            JOBCODE_GET_APP_JSON -> GetAppPackets(jCode, METADATA_HOLDER_DIR, this, restore_selector_progress_bar, waiting_status_text)
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

            fun handleResults(nextJob: Int, func: () -> Unit) {
                if (nextJob == JOBCODE_END_ALL){
                    displayAllData()
                }
                else {
                    if (!jobSuccess) {
                        if (AppInstance.sharedPrefs.getBoolean(PREF_IGNORE_READ_ERRORS, false)) {
                            allErrors.add(jobResult.toString())
                            doJob(nextJob)
                        } else showError(getString(R.string.code_error), jobResult.toString())
                    } else {
                        if (jobResult !is Int) func()
                        doJob(nextJob)
                    }
                }
            }

            when (jobCode) {

                JOBCODE_ROOT_COPY -> {
                    if (!jobSuccess)
                        showError(getString(R.string.are_you_rooted), "${getString(R.string.are_you_rooted_desc)}\n\n$jobResult".trim())
                    else doJob(JOBCODE_GET_APP_JSON)
                }

                JOBCODE_GET_APP_JSON ->
                    handleResults(JOBCODE_GET_CONTACTS) {
                        appPackets.clear()
                        appPackets.addAll(jobResult as ArrayList<AppPacketsKotlin>)
                    }

                JOBCODE_GET_CONTACTS ->
                    handleResults(JOBCODE_GET_SMS) {
                        contactDataPackets.clear()
                        contactDataPackets.addAll(jobResult as ArrayList<ContactsPacketKotlin>)
                    }

                JOBCODE_GET_SMS ->
                    handleResults(JOBCODE_GET_CALLS) {
                        smsDataPackets.clear()
                        smsDataPackets.addAll(jobResult as ArrayList<SmsPacketKotlin>)
                    }

                JOBCODE_GET_CALLS ->
                    handleResults(JOBCODE_GET_SETTINGS) {
                        callsDataPackets.clear()
                        callsDataPackets.addAll(jobResult as ArrayList<CallsPacketKotlin>)
                    }

                JOBCODE_GET_SETTINGS ->
                    handleResults(JOBCODE_GET_WIFI) { settingsPacket = jobResult as SettingsPacketKotlin }

                JOBCODE_GET_WIFI ->
                    handleResults(JOBCODE_END_ALL) { wifiPacket = jobResult as WifiPacketKotlin }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            showError(getString(R.string.code_error), "$ERROR_MAIN_READ_TRY_CATCH: ${e.message}")
        }
    }

    private fun displayAllData(){

        var adapter: AppRestoreAdapter? = null
        var error = ""
        var extrasExecuted = false

        extrasContainer.visibility = View.VISIBLE

        commonTools.doBackgroundTask({
            try {

                if ((contactDataPackets.isNotEmpty() || smsDataPackets.isNotEmpty() || callsDataPackets.isNotEmpty() ||
                        settingsPacket != null || wifiPacket != null) && !AppInstance.sharedPrefs.getBoolean(PREF_IGNORE_EXTRAS, false)) {

                    commonTools.tryIt { app_list.removeHeaderView(extrasContainer) }
                    app_list.addHeaderView(extrasContainer, null, false)

                    val extrasAllListener = CompoundButton.OnCheckedChangeListener{ _, isChecked ->
                        for (i in 0 until extrasContainer.restore_selector_extras_container.childCount) {
                            commonTools.tryIt {
                                val item = extrasContainer.restore_selector_extras_container.getChildAt(i)
                                item.findViewById<CheckBox>(R.id.extras_item_select).isChecked = isChecked
                            }
                        }
                    }

                    fun checkAll(immediateSelection: Boolean){

                        var isAnyNotSelected = false

                        if (!immediateSelection) {
                            extrasContainer.extras_select_all.apply {
                                setOnCheckedChangeListener(null)
                                isChecked = false
                                setOnCheckedChangeListener(extrasAllListener)
                            }
                        }
                        else {
                            for (i in 0 until extrasContainer.restore_selector_extras_container.childCount) {
                                commonTools.tryIt {
                                    val item = extrasContainer.restore_selector_extras_container.getChildAt(i)
                                    if (!item.findViewById<CheckBox>(R.id.extras_item_select).isChecked)
                                        isAnyNotSelected = true
                                }
                                if (isAnyNotSelected) break
                            }
                            extrasContainer.extras_select_all.isChecked = !isAnyNotSelected
                        }
                    }

                    var c = EXTRA_VIEW_COUNT

                    val allExtras: ArrayList<GetterMarker> = ArrayList(0)
                    allExtras.addAll(contactDataPackets)
                    allExtras.addAll(smsDataPackets)
                    allExtras.addAll(callsDataPackets)
                    wifiPacket?.let { allExtras.add(it) }
                    settingsPacket?.let { allExtras.addAll(it.internalPackets) }

                    for (item in allExtras) {

                        val v = View.inflate(this, R.layout.extra_item, null)
                        v.id = c++
                        v.extras_item_select.apply {
                            isChecked = item.isSelected
                            setOnCheckedChangeListener { _, isChecked ->
                                checkAll(isChecked)
                                item.isSelected = isChecked
                            }
                        }

                        v.extra_item_icon.setImageResource(when (item) {
                            is ContactsPacketKotlin -> R.drawable.ic_contact_icon
                            is SmsPacketKotlin -> R.drawable.ic_sms_icon
                            is CallsPacketKotlin -> R.drawable.ic_call_log_icon
                            is WifiPacketKotlin -> R.drawable.ic_wifi_icon
                            is SettingsPacketKotlin.DpiInternalPacket -> R.drawable.ic_dpi_icon
                            is SettingsPacketKotlin.AdbInternalPacket -> R.drawable.ic_adb_icon
                            is SettingsPacketKotlin.FontScaleInternalPacket -> R.drawable.ic_font_scale_icon
                            is SettingsPacketKotlin.KeyboardInternalPacket -> R.drawable.ic_keyboard_icon
                            else -> R.drawable.ic_app
                        })

                        v.extra_item_name.text = when (item) {
                            is ContactsPacketKotlin -> item.vcfFile.name
                            is SmsPacketKotlin -> item.smsDBFile.name
                            is CallsPacketKotlin -> item.callDBFile.name
                            is WifiPacketKotlin -> getString(R.string.wifi)
                            is SettingsPacketKotlin.DpiInternalPacket -> getString(R.string.dpi)
                            is SettingsPacketKotlin.AdbInternalPacket -> getString(R.string.adb_state)
                            is SettingsPacketKotlin.FontScaleInternalPacket -> getString(R.string.font_scale)
                            is SettingsPacketKotlin.KeyboardInternalPacket -> getString(R.string.keyboard)
                            else -> ""
                        }

                        v.setOnClickListener { v.extras_item_select.apply { isChecked = !isChecked } }

                        extrasContainer.restore_selector_extras_container.addView(v)
                    }

                    if (allExtras.isNotEmpty()) {
                        checkAll(true)
                        extrasContainer.extras_select_all.setOnCheckedChangeListener(extrasAllListener)
                    }

                    extrasExecuted = true
                }

                if (appPackets.isNotEmpty()) {
                    commonTools.tryIt {
                        commonTools.tryIt { app_list.removeHeaderView(appBar) }
                        app_list.addHeaderView(appBar, null, false)
                    }
                    adapter = AppRestoreAdapter(this, appBar.appAllSelect, appBar.dataAllSelect, appBar.permissionsAllSelect)
                }

            } catch (e: Exception){
                e.printStackTrace()
                error = e.message.toString()
            }
        }, {
            when {
                error != "" -> showError(getString(R.string.code_error), error)
                !extrasExecuted && adapter == null -> showError(getString(R.string.code_error), getString(R.string.null_adapter))
                else -> {
                    app_list.visibility = View.VISIBLE
                    waiting_layout.visibility = View.GONE
                    app_list.adapter = adapter
                }
            }
            commonTools.tryIt {
                if (extrasContainer.restore_selector_extras_container.childCount == 0) {
                    commonTools.tryIt { app_list.removeHeaderView(extrasContainer) }
                    extrasContainer.visibility = View.GONE
                }
            }
        })
    }

}