package balti.migrate.helper.restoreSelectorActivity

//import balti.migrate.helper.AppInstance.Companion.sharedPrefs
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import balti.migrate.helper.AppInstance.Companion.appPackets
import balti.migrate.helper.AppInstance.Companion.callsDataPackets
import balti.migrate.helper.AppInstance.Companion.contactDataPackets
import balti.migrate.helper.AppInstance.Companion.notificationFixGlobal
import balti.migrate.helper.AppInstance.Companion.settingsPacket
import balti.migrate.helper.AppInstance.Companion.smsDataPackets
import balti.migrate.helper.AppInstance.Companion.wifiPacket
import balti.migrate.helper.R
import balti.migrate.helper.emergencyRestore.EmergencyRestoreProgressShow
import balti.migrate.helper.emergencyRestore.EmergencyRestoreService
import balti.migrate.helper.extraRestorePrepare.ExtraRestorePrepare
import balti.migrate.helper.progressShow.ProgressShowActivity
import balti.migrate.helper.restoreSelectorActivity.containers.*
import balti.migrate.helper.restoreSelectorActivity.getters.*
import balti.migrate.helper.restoreSelectorActivity.utils.AppRestoreAdapter
import balti.migrate.helper.restoreSelectorActivity.utils.OnReadComplete
import balti.migrate.helper.restoreSelectorActivity.utils.RootCopyTask
import balti.migrate.helper.restoreSelectorActivity.utils.SearchAppAdapter
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_RESTORE_DATA
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_MAIN_READ_TRY_CATCH
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_NOTIFICATION_FIX
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_VIEW_COUNT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_END_ALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_APP_JSON
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_CALLS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_CONTACTS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_SETTINGS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_SMS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_GET_WIFI
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_ROOT_COPY
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PACKAGE_MIGRATE_FLASHER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_EXTRAS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_READ_ERRORS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_LOAD_EXTRAS_ON_UI_THREAD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.SU_INIT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.TIMEOUT_WAITING_TO_KILL
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import kotlinx.android.synthetic.main.app_search_layout.view.*
import kotlinx.android.synthetic.main.app_selector_header.view.*
import kotlinx.android.synthetic.main.emergency_restore_dialog.view.*
import kotlinx.android.synthetic.main.extra_item.view.*
import kotlinx.android.synthetic.main.extras_picker.view.*
import kotlinx.android.synthetic.main.restore_selector.*
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter

class RestoreSelectorKotlin: AppCompatActivity(), OnReadComplete {

    companion object {
        var cancelLoading: Boolean = false
        private set
    }

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val allErrors by lazy { ArrayList<String>(0) }

    private val extrasContainer by lazy { layoutInflater.inflate(R.layout.extras_picker, app_list, false) }
    private val appBar by lazy { layoutInflater.inflate(R.layout.app_selector_header, app_list, false) }

    private var storageRequestCode = 157

    private var currentTask : AsyncTask<Any, Any, Any>? = null

    private var adapter: AppRestoreAdapter? = null

    private lateinit var forceStopDialog: AlertDialog

    private val progressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@RestoreSelectorKotlin, ProgressShowActivity::class.java)
                        .apply {
                            intent?.let {
                                this.putExtras(it)
                                this.action = it.action
                            }
                        }
                )
                tryIt { commonTools.LBM?.unregisterReceiver(this) }
                finish()
            }
        }
    }

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

                            Runtime.getRuntime().exec(SU_INIT).apply {
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
                            tryIt { cancelTask() }
                        }
                    }
                    cancelLoading = true
                }
            }
        }

        notification_check_toggle.apply {
            isChecked = notificationFixGlobal
            setOnCheckedChangeListener { _, isChecked ->
                notificationFixGlobal = isChecked
            }
        }

        installMigrateFlasher.apply {
            visibility = View.GONE
            setOnClickListener {
                Misc.playStoreLink(PACKAGE_MIGRATE_FLASHER)
            }
        }

        emergency_restore_code_error.apply {
            visibility = View.GONE
            setOnClickListener {
                val startIntent = Intent(this@RestoreSelectorKotlin, EmergencyRestoreProgressShow::class.java)
                if (EmergencyRestoreService.wasStarted) startActivity(startIntent)
                else {
                    val view = View.inflate(this@RestoreSelectorKotlin, R.layout.emergency_restore_dialog, null)
                    view.emergency_restore_notification_fix.apply {
                        isChecked = notificationFixGlobal
                        setOnCheckedChangeListener { _, isChecked ->
                            notificationFixGlobal = isChecked
                        }
                    }
                    AlertDialog.Builder(this@RestoreSelectorKotlin)
                            .setView(view)
                            .setPositiveButton(R.string.proceed) { _, _ ->
                                startActivity(startIntent)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                }
            }
        }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))
        commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_RESTORE_DATA))

        doJob(JOBCODE_ROOT_COPY)
    }

    private fun showError(mainMessage: String, description: String){

        waiting_layout.visibility = View.VISIBLE
        app_list.visibility = View.GONE

        if (mainMessage == getString(R.string.nothing_to_restore))
            installMigrateFlasher.visibility = View.VISIBLE
        else installMigrateFlasher.visibility = View.GONE

        if (mainMessage == getString(R.string.code_error))
            emergency_restore_code_error.visibility = View.VISIBLE
        else emergency_restore_code_error.visibility = View.GONE

        just_a_progress.visibility = View.INVISIBLE
        restore_selector_error_icon.visibility = View.VISIBLE
        waiting_status_text.text = mainMessage
        waiting_desc.text = description
        restore_selector_action_button.apply {
            setText(R.string.close)
            background = getDrawable(R.drawable.next)
            setOnClickListener { finish() }
        }
    }

    private fun showError(mainMessage: String, messages: ArrayList<String>){
        val builder = StringBuilder("")
        messages.forEach {
            builder.append("$it\n")
        }
        showError(mainMessage, builder.toString())
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

                fun addErrors(){
                    if (jobResult is ArrayList<*>)
                        allErrors.addAll(jobResult.map { it.toString() })
                    else allErrors.add(jobResult.toString())
                }

                if (cancelLoading) {
                    displayAllData()
                    return
                }

                if (nextJob == JOBCODE_END_ALL){
                    if (jobResult !is Int) {
                        if (jobSuccess) func()
                        else addErrors()
                    }
                    displayAllData()
                }
                else {
                    if (!jobSuccess) {
                        addErrors()
                        if (getPrefBoolean(PREF_IGNORE_READ_ERRORS, false))
                            doJob(nextJob)
                        else displayAllData()
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
                    else {
                        // check if allowed to read
                        val isOk = try {
                            File(METADATA_HOLDER_DIR).listFiles()
                                    .let {if (it == null) throw (Exception("manual exception null list"))}
                            true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
                            false
                        }
                        if (isOk) doJob(JOBCODE_GET_APP_JSON)
                        else {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE), storageRequestCode)
                        }
                    }
                }

                JOBCODE_GET_APP_JSON ->
                    handleResults(
                            if (!getPrefBoolean(PREF_IGNORE_EXTRAS, false)) JOBCODE_GET_CONTACTS
                            else JOBCODE_END_ALL) {
                        appPackets.clear()
                        appPackets.addAll(jobResult as ArrayList<AppPacketsKotlin>)

                        if (appPackets.isEmpty())
                            notification_check_toggle.visibility = View.GONE
                        else notification_check_toggle.visibility = View.VISIBLE

                        notification_check_toggle.isChecked = appPackets.isNotEmpty()
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

        restore_selecter_back_button.setOnClickListener {
            onBackPressed()
        }

        if (!getPrefBoolean(PREF_IGNORE_READ_ERRORS, false) && allErrors.isNotEmpty()){
            showError(getString(R.string.code_error), allErrors)
            return
        }

        val allExtras: ArrayList<GetterMarker> = ArrayList(0)
        allExtras.addAll(contactDataPackets)
        allExtras.addAll(smsDataPackets)
        allExtras.addAll(callsDataPackets)
        wifiPacket?.let { allExtras.add(it) }
        settingsPacket?.let { allExtras.addAll(it.internalPackets) }

        var triedUiThread = false

        if (!cancelLoading && (appPackets.isNotEmpty() || allExtras.isNotEmpty())) {

            if (allExtras.isNotEmpty())
                extrasContainer.visibility = View.VISIBLE
            else extrasContainer.visibility = View.GONE

            commonTools.doBackgroundTask({

                fun load() {

                    try {

                        if (!getPrefBoolean(PREF_IGNORE_EXTRAS, false) && allExtras.isNotEmpty()) {

                            tryIt { app_list.removeHeaderView(extrasContainer) }
                            app_list.addHeaderView(extrasContainer, null, false)

                            val extrasAllListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                                for (i in 0 until extrasContainer.restore_selector_extras_container.childCount) {
                                    tryIt {
                                        val item = extrasContainer.restore_selector_extras_container.getChildAt(i)
                                        item.findViewById<CheckBox>(R.id.extras_item_select).isChecked = isChecked
                                    }
                                }
                            }

                            // this function toggles the checkbox in extra banner or app list banner,
                            // when an individual item is toggled
                            fun checkAll(immediateSelection: Boolean) {

                                fun toggleMasterCheckbox(state: Boolean) {
                                    extrasContainer.extras_select_all.apply {
                                        setOnCheckedChangeListener(null)
                                        isChecked = state
                                        setOnCheckedChangeListener(extrasAllListener)
                                    }
                                }

                                var isAnyNotSelected = false
                                // flag. If any item is not in checked state,
                                // do not check the other items, just mark banner checkbox as not selected

                                if (!immediateSelection) {
                                    toggleMasterCheckbox(false)
                                } else {
                                    for (i in 0 until extrasContainer.restore_selector_extras_container.childCount) {
                                        tryIt {
                                            val item = extrasContainer.restore_selector_extras_container.getChildAt(i)
                                            if (!item.findViewById<CheckBox>(R.id.extras_item_select).isChecked)
                                                isAnyNotSelected = true
                                        }
                                        if (isAnyNotSelected) break
                                    }
                                    toggleMasterCheckbox(!isAnyNotSelected)
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

                                when (item) {
                                    is SettingsPacketKotlin.SettingsItem -> when (item.settingsType) {
                                        SettingsPacketKotlin.SETTINGS_TYPE_DPI -> v.extra_item_desc.apply {
                                            visibility = View.VISIBLE
                                            text = getString(R.string.reboot_is_necessary)
                                        }
                                    }
                                    is WifiPacketKotlin -> v.extra_item_desc.apply {
                                        visibility = View.VISIBLE
                                        text = getString(R.string.wifi_reboot_is_necessary)
                                    }
                                    else -> v.extra_item_desc.visibility = View.VISIBLE
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

                    } catch (e: Exception) {
                        e.printStackTrace()
                        error = e.message.toString() + "\n"
                    }

                }

                if (getPrefBoolean(PREF_LOAD_EXTRAS_ON_UI_THREAD, false)) {
                    runOnUiThread { load() }
                    triedUiThread = true
                }
                else load()


                if (error.trim() != "" && !triedUiThread) {
                    runOnUiThread { error = ""; load() }
                    triedUiThread = true
                }

                try {

                    if (appPackets.isNotEmpty()) {
                        tryIt {
                            tryIt { app_list.removeHeaderView(appBar) }
                            app_list.addHeaderView(appBar, null, false)
                        }
                        adapter = AppRestoreAdapter(this, appBar.appAllSelect, appBar.dataAllSelect, appBar.permissionsAllSelect)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    error += e.message.toString()
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

                tryIt {
                    if (extrasContainer.restore_selector_extras_container.childCount == 0) {
                        tryIt { app_list.removeHeaderView(extrasContainer) }
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
                                    .setTitle(R.string.apps_will_be_closed)
                                    .setMessage(R.string.do_not_use_desc)
                                    .setPositiveButton(R.string.goAhead) { _, _ ->
                                        startActivity(
                                                Intent(this@RestoreSelectorKotlin, ExtraRestorePrepare::class.java)
                                                        .putExtra(EXTRA_NOTIFICATION_FIX, notificationFixGlobal)
                                        )
                                        finish()
                                    }
                                    .setNegativeButton(R.string.later, null)
                                    .show()
                        }
                    }

                }
            })

        }
        else {

            if (cancelLoading) {
                showError(getString(R.string.cancelled_loading), "")
                tryIt { Thread.sleep(DUMMY_WAIT_TIME) }
                tryIt { forceStopDialog.dismiss() }
                finish()
            }
            else showError(getString(R.string.nothing_to_restore), getString(R.string.no_metadata_found))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storageRequestCode){
            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                doJob(JOBCODE_GET_APP_JSON)
            else {
                showError(getString(R.string.permission_denied), getString(R.string.storage_perm_needed))
                Toast.makeText(this, R.string.storage_perm_needed, Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onDestroy() {
        cancelLoading = false
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        tryIt { forceStopDialog.dismiss() }
        super.onDestroy()
    }

}