package balti.migratehelper.restoreSelectorActivity

import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Toast
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.restoreEngines.RestoreServiceKotlin
import balti.migratehelper.restoreSelectorActivity.containers.*
import balti.migratehelper.restoreSelectorActivity.getters.*
import balti.migratehelper.restoreSelectorActivity.utils.AppRestoreAdapter
import balti.migratehelper.restoreSelectorActivity.utils.OnReadComplete
import balti.migratehelper.restoreSelectorActivity.utils.RootCopyTask
import balti.migratehelper.restoreSelectorActivity.utils.SearchAppAdapter
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
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
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.TIMEOUT_WAITING_TO_KILL
import kotlinx.android.synthetic.main.app_search_layout.view.*
import kotlinx.android.synthetic.main.app_selector_header.view.*
import kotlinx.android.synthetic.main.extra_item.view.*
import kotlinx.android.synthetic.main.extras_picker.view.*
import kotlinx.android.synthetic.main.restore_selector.*
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class RestoreSelectorKotlin: AppCompatActivity(), OnReadComplete {

    companion object {
        var cancelLoading: Boolean = false
        private set
    }

    private val appPackets = AppInstance.appPackets
    private val contactDataPackets = AppInstance.contactDataPackets
    private val smsDataPackets = AppInstance.smsDataPackets
    private val callsDataPackets = AppInstance.callsDataPackets
    private var settingsPacket = AppInstance.settingsPacket
    private var wifiPacket = AppInstance.wifiPacket

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val allErrors by lazy { ArrayList<String>(0) }

    private val extrasContainer by lazy { layoutInflater.inflate(R.layout.extras_picker, app_list, false) }
    private val appBar by lazy { layoutInflater.inflate(R.layout.app_selector_header, app_list, false) }

    private var currentTask : AsyncTask<Any, Any, Any>? = null

    private var adapter: AppRestoreAdapter? = null

    private lateinit var forceStopDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.restore_selector)

        cancelLoading = false

        waiting_layout.visibility = View.VISIBLE
        app_list.visibility = View.GONE

        restore_selector_action_button.apply {

            setText(android.R.string.cancel)
            background = getDrawable(R.drawable.cancel_root_request)

            setOnClickListener {

                if (text == getString(R.string.force_stop)){

                    forceStopDialog = AlertDialog.Builder(this@RestoreSelectorKotlin).apply {

                        this.setTitle(getString(R.string.force_stop_alert_title))
                        this.setMessage(getString(R.string.force_stop_alert_desc))

                        setPositiveButton(R.string.kill_app) { _, _ ->

                            Runtime.getRuntime().exec("su").apply {
                                BufferedWriter(OutputStreamWriter(this.outputStream)).run {
                                    this.write("am force-stop $packageName\n")
                                    this.write("exit\n")
                                    this.flush()
                                }
                            }

                            val handler = Handler()
                            handler.postDelayed({
                                Toast.makeText(this@RestoreSelectorKotlin, R.string.killing_programmatically, Toast.LENGTH_SHORT).show()
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, TIMEOUT_WAITING_TO_KILL)
                        }

                        setNegativeButton(R.string.wait_to_cancel, null)

                    }.create()

                    forceStopDialog.show()

                }
                else {
                    text = getString(R.string.force_stop)
                    currentTask?.run {
                        if (this is RootCopyTask) {
                            commonTools.tryIt { cancelTask() }
                        }
                    }
                    cancelLoading = true
                }
            }
        }

        doJob(JOBCODE_ROOT_COPY)
    }

    private fun showError(mainMessage: String, description: String){

        waiting_layout.visibility = View.VISIBLE
        app_list.visibility = View.GONE

        just_a_progress.visibility = View.INVISIBLE
        restore_selector_error_icon.visibility = View.VISIBLE
        waiting_status_text.text = mainMessage
        waiting_desc.text = description
        restore_selector_action_button.apply {
            setText(R.string.close)
            background = getDrawable(R.drawable.next)
        }
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

        currentTask = task
        task?.execute()
    }

    override fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any) {

        try {

            fun handleResults(nextJob: Int, func: () -> Unit) {

                if (cancelLoading) {
                    displayAllData()
                    return
                }

                if (nextJob == JOBCODE_END_ALL){
                    if (jobResult !is Int) func()
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
                    if (cancelLoading) displayAllData()
                    else if (!jobSuccess)
                        showError(getString(R.string.are_you_rooted), "${getString(R.string.are_you_rooted_desc)}\n\n$jobResult".trim())
                    else doJob(JOBCODE_GET_APP_JSON)
                }

                JOBCODE_GET_APP_JSON ->
                    handleResults(
                            if (!AppInstance.sharedPrefs.getBoolean(PREF_IGNORE_EXTRAS, false)) JOBCODE_GET_CONTACTS
                            else JOBCODE_END_ALL) {
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

        var error = ""

        val allExtras: ArrayList<GetterMarker> = ArrayList(0)
        allExtras.addAll(contactDataPackets)
        allExtras.addAll(smsDataPackets)
        allExtras.addAll(callsDataPackets)
        wifiPacket?.let { allExtras.add(it) }
        settingsPacket?.let { allExtras.addAll(it.internalPackets) }

        if (!cancelLoading && (appPackets.isNotEmpty() || allExtras.isNotEmpty())) {

            if (allExtras.isNotEmpty())
                extrasContainer.visibility = View.VISIBLE
            else extrasContainer.visibility = View.GONE

            commonTools.doBackgroundTask({
                try {

                    if (!AppInstance.sharedPrefs.getBoolean(PREF_IGNORE_EXTRAS, false) && allExtras.isNotEmpty()) {

                        commonTools.tryIt { app_list.removeHeaderView(extrasContainer) }
                        app_list.addHeaderView(extrasContainer, null, false)

                        val extrasAllListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            for (i in 0 until extrasContainer.restore_selector_extras_container.childCount) {
                                commonTools.tryIt {
                                    val item = extrasContainer.restore_selector_extras_container.getChildAt(i)
                                    item.findViewById<CheckBox>(R.id.extras_item_select).isChecked = isChecked
                                }
                            }
                        }

                        fun checkAll(immediateSelection: Boolean) {

                            var isAnyNotSelected = false

                            if (!immediateSelection) {
                                extrasContainer.extras_select_all.apply {
                                    setOnCheckedChangeListener(null)
                                    isChecked = false
                                    setOnCheckedChangeListener(extrasAllListener)
                                }
                            } else {
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

                            v.extra_item_icon.setImageResource(item.iconResource)
                            v.extra_item_name.text = item.displayText
                            v.setOnClickListener { v.extras_item_select.apply { isChecked = !isChecked } }

                            extrasContainer.restore_selector_extras_container.addView(v)
                        }

                        if (allExtras.isNotEmpty()) {
                            checkAll(true)
                            extrasContainer.extras_select_all.setOnCheckedChangeListener(extrasAllListener)
                        }
                    }

                    if (appPackets.isNotEmpty()) {
                        commonTools.tryIt {
                            commonTools.tryIt { app_list.removeHeaderView(appBar) }
                            app_list.addHeaderView(appBar, null, false)
                        }
                        adapter = AppRestoreAdapter(this, appBar.appAllSelect, appBar.dataAllSelect, appBar.permissionsAllSelect)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    error = e.message.toString()
                }

            }, {

                when {
                    error != "" -> showError(getString(R.string.code_error), error)
                    allExtras.isNotEmpty() || adapter != null -> {
                        app_list.visibility = View.VISIBLE
                        waiting_layout.visibility = View.GONE
                        app_list.adapter = adapter
                    }
                    else -> showError(getString(R.string.code_error), getString(R.string.null_adapter))
                }

                commonTools.tryIt {
                    if (extrasContainer.restore_selector_extras_container.childCount == 0) {
                        commonTools.tryIt { app_list.removeHeaderView(extrasContainer) }
                        extrasContainer.visibility = View.GONE
                    }
                }

                if (error == "") {

                    fun toggleCheckbox(cb: CheckBox, isChecked: Boolean? = null) {
                        if (isChecked == null) {
                            cb.isChecked = true
                            cb.isChecked = false
                        } else cb.isChecked = isChecked
                    }

                    clear_all.setOnClickListener {
                        toggleCheckbox(extrasContainer.extras_select_all)
                        toggleCheckbox(appBar.appAllSelect)
                        toggleCheckbox(appBar.dataAllSelect)
                        toggleCheckbox(appBar.permissionsAllSelect)
                    }

                    select_all.setOnClickListener {
                        toggleCheckbox(extrasContainer.extras_select_all, true)
                        toggleCheckbox(appBar.appAllSelect, true)
                        toggleCheckbox(appBar.dataAllSelect, true)
                        toggleCheckbox(appBar.permissionsAllSelect, true)
                    }

                    appSearch.setOnClickListener {

                        val searchAD = AlertDialog.Builder(this)
                                .setCancelable(false)
                                .create()

                        val searchView = View.inflate(this, R.layout.app_search_layout, null)

                        searchView.app_search_close.setOnClickListener {
                            searchAD.dismiss()
                            adapter?.notifyDataSetChanged()
                        }

                        searchView.app_search_editText.addTextChangedListener(object : TextWatcher {

                            var loadApps: LoadSearchApps? = null

                            override fun afterTextChanged(s: Editable?) {
                                try {
                                    loadApps?.cancel(true)
                                } catch (_: Exception) {
                                }

                                loadApps = LoadSearchApps(s.toString())
                                loadApps?.let { it.execute() }
                            }

                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}


                            inner class LoadSearchApps(val term: String) : AsyncTask<Any, Any, Any>() {

                                lateinit var adapter: SearchAppAdapter
                                var tmpList = ArrayList<AppPacketsKotlin>(0)

                                override fun onPreExecute() {
                                    super.onPreExecute()
                                    searchView.app_search_list.adapter = null
                                    searchView.app_search_loading.visibility = View.VISIBLE
                                    searchView.app_search_app_unavailable.visibility = View.GONE
                                }

                                override fun doInBackground(vararg params: Any?): Any? {

                                    if (term.trim() != "") makeTmpList(term)
                                    else tmpList.clear()

                                    if (tmpList.size > 0) adapter = SearchAppAdapter(tmpList, this@RestoreSelectorKotlin)

                                    return null
                                }

                                override fun onPostExecute(result: Any?) {
                                    super.onPostExecute(result)
                                    searchView.app_search_loading.visibility = View.GONE
                                    if (tmpList.size > 0) searchView.app_search_list.adapter = adapter
                                    else {
                                        searchView.app_search_list.invalidate()
                                        if (term.trim() != "") searchView.app_search_app_unavailable.visibility = View.VISIBLE
                                    }
                                }

                                fun makeTmpList(term: String) {
                                    tmpList.clear()
                                    for (dp in appPackets) {
                                        if (dp.packageName.let { it?.contains(term) == true } || dp.appName.let { it?.contains(term) == true })
                                            tmpList.add(dp)
                                    }
                                }
                            }
                        })

                        searchAD.setView(searchView)
                        searchAD.window?.run { setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) }
                        searchAD.show()

                    }

                    restore_selector_action_button.apply {

                        setText(R.string.restore)
                        background = getDrawable(R.drawable.next)

                        setOnClickListener {
                            AlertDialog.Builder(this@RestoreSelectorKotlin)
                                    .setTitle(R.string.turn_off_internet_and_updates)
                                    .setMessage(R.string.do_not_use_desc)
                                    .setPositiveButton(R.string.goAhead) { _, _ ->

                                        Intent(this@RestoreSelectorKotlin, RestoreServiceKotlin::class.java).run {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                startForegroundService(this)
                                            } else {
                                                startService(this)
                                            }
                                        }

                                    }
                                    .setNegativeButton(R.string.later, null)
                                    .show()
                        }
                    }

                }
            })

        }
        else {

            waiting_layout.visibility = View.VISIBLE
            app_list.visibility = View.GONE

            if (cancelLoading) {
                showError(getString(R.string.cancelled_loading), "")
                commonTools.tryIt { Thread.sleep(DUMMY_WAIT_TIME) }
                commonTools.tryIt { forceStopDialog.dismiss() }
                finish()
            }
            else showError(getString(R.string.nothing_to_restore), getString(R.string.no_metadata_found))
        }
    }

    override fun onDestroy() {
        cancelLoading = false
        commonTools.tryIt { forceStopDialog.dismiss() }
        super.onDestroy()
    }

}