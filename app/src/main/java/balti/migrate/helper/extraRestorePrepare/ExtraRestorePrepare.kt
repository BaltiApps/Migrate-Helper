package balti.migrate.helper.extraRestorePrepare

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import balti.migrate.helper.AppInstance.Companion.appPackets
import balti.migrate.helper.AppInstance.Companion.callsDataPackets
import balti.migrate.helper.AppInstance.Companion.contactDataPackets
import balti.migrate.helper.AppInstance.Companion.settingsPacket
import balti.migrate.helper.AppInstance.Companion.sharedPrefs
import balti.migrate.helper.AppInstance.Companion.smsDataPackets
import balti.migrate.helper.AppInstance.Companion.wifiPacket
import balti.migrate.helper.R
import balti.migrate.helper.extraRestorePrepare.utils.AppsNotInstalledViewManager
import balti.migrate.helper.extraRestorePrepare.utils.CommunicatorAddon
import balti.migrate.helper.extraRestorePrepare.utils.OnPermissionAsked
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.GetterMarker
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migrate.helper.simpleActivities.ProgressShowActivity
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_ADDON_DO_ABORT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_INSTALL_SETTINGS_ADDON
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_INSTALL_SMS_CALLS_ADDON
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_NOTIFICATION_FIX
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SETTINGS_ADDON_OK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SMS_CALLS_ADDON_FILES
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_SMS_CALLS_ADDON_OK
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_LAUNCH_ADDON_INSTALLER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_ADB
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_APP
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_CALLS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_CONTACTS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_DPI
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_END
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_FONT_SCALE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_KEYBOARD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_SMS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_WIFI
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_CONTACTS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_RESTORE_START_ANIMATION
import balti.module.baltitoolbox.functions.Misc.isPackageInstalled
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.contacts_dialog_view.view.*
import kotlinx.android.synthetic.main.extra_prep_item.view.*
import kotlinx.android.synthetic.main.extra_restore_prepare.*

class ExtraRestorePrepare: AppCompatActivity(), OnPermissionAsked {

    private var erpItemContacts : View? = null
    private var erpItemSms : View? = null
    private var erpItemCalls : View? = null
    private var erpItemDpi : View? = null
    private var erpItemAdb : View? = null
    private var erpItemFontScale : View? = null
    private var erpItemKeyboard : View? = null
    private var erpItemWifi : View? = null
    private var erpItemApps : View? = null

    private val DONE = 6
    private val WAIT = 7
    private val CANCEL = 8

    private var cancelChecks = false

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val communicatorAddon by lazy { CommunicatorAddon(this) }

    private var contactsCount = 0

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler

    private var keyboardSettingsItem : SettingsPacketKotlin.SettingsItem? = null
    private var dpiSettingsItem : SettingsPacketKotlin.SettingsItem? = null
    private var adbSettingsItem : SettingsPacketKotlin.SettingsItem? = null
    private var fontScaleSettingsItem : SettingsPacketKotlin.SettingsItem? = null
    //private var grantedSettingsChange = false

    private var notificationFix = false
    private var alreadyTriggered = false

    private var doInstallSmsCallsAddon = false
    private var doInstallSettingsAddon = false

    private var addonSmsCallsSuccessful = false
    private var addonSettingsSuccessful = false

    private val jobcodeOrder by lazy { arrayListOf(JOBCODE_PREP_APP, JOBCODE_PREP_KEYBOARD, JOBCODE_PREP_WIFI,
            JOBCODE_PREP_CONTACTS, JOBCODE_PREP_SMS, JOBCODE_PREP_CALLS, JOBCODE_PREP_DPI, JOBCODE_PREP_ADB,
            JOBCODE_PREP_FONT_SCALE, JOBCODE_PREP_END) }
    private val viewOrder by lazy { arrayListOf(erpItemApps, erpItemKeyboard, erpItemWifi, erpItemContacts, erpItemSms,
            erpItemCalls, erpItemDpi, erpItemAdb, erpItemFontScale) }

    private val progressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@ExtraRestorePrepare, ProgressShowActivity::class.java)
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
        setContentView(R.layout.extra_restore_prepare)

        notificationFix = intent.getBooleanExtra(EXTRA_NOTIFICATION_FIX, false)

        restore_countdown.visibility = View.GONE

        // filter data
        filterSelected(contactDataPackets)
        filterSelected(smsDataPackets)
        filterSelected(callsDataPackets)
        settingsPacket?.run { filterSelected(internalPackets) }

        // add apps
        if (appPackets.isNotEmpty()) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_app, R.string.apps).apply { erpItemApps = this })

        // add keyboard first. Store others in an arrayList
        val otherViews = ArrayList<View>(0)
        settingsPacket?. let {
            for (p in it.internalPackets) {

                val view = getERPItem(p.iconResource, p.displayText)
                when (p.settingsType) {
                    SettingsPacketKotlin.SETTINGS_TYPE_DPI -> { erpItemDpi = view; dpiSettingsItem = p }
                    SettingsPacketKotlin.SETTINGS_TYPE_ADB -> { erpItemAdb = view; adbSettingsItem = p }
                    SettingsPacketKotlin.SETTINGS_TYPE_FONT_SCALE -> { erpItemFontScale = view; fontScaleSettingsItem = p }
                    SettingsPacketKotlin.SETTINGS_TYPE_KEYBOARD -> {
                        erpItemKeyboard = view
                        keyboardSettingsItem = p
                        extra_perm_check_holder.addView(view)
                    }
                }
                if (p.settingsType != SettingsPacketKotlin.SETTINGS_TYPE_KEYBOARD) otherViews.add(view)
            }
        }

        // add wifi
        wifiPacket?.let {
            if (it.isSelected) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                    extra_perm_check_holder.addView(getERPItem(R.drawable.ic_wifi_icon, R.string.wifi).apply { erpItemWifi = this })
                else wifiPacket = null
            }
            else wifiPacket = null
        }

        // add contacts, sms, calls
        if (contactDataPackets.isNotEmpty()) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_contact_icon, R.string.contacts).apply { erpItemContacts = this })
        if (smsDataPackets.isNotEmpty()) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_sms_icon, R.string.sms).apply { erpItemSms = this })
        if (callsDataPackets.isNotEmpty()) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_call_log_icon, R.string.calls).apply { erpItemCalls = this })

        // add other settings
        otherViews.forEach { extra_perm_check_holder.addView(it) }

        erp_close_button.setOnClickListener {
            cancelChecks = true
            doFallThroughJob(JOBCODE_PREP_END)
        }

        extra_restore_prep_cancel_button.setOnClickListener {
            cancelChecks = true
            doFallThroughJob(JOBCODE_PREP_END)
        }

        doInstallSmsCallsAddon = smsDataPackets.isNotEmpty() || callsDataPackets.isNotEmpty()
        doInstallSettingsAddon = try { settingsPacket?.internalPackets?.isNotEmpty()!! } catch (_: Exception) {false}

        if (doInstallSettingsAddon || doInstallSmsCallsAddon) installAddons()
        else doFallThroughJob(JOBCODE_PREP_APP)

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))
    }

    private fun installAddons(){
        startActivityForResult(Intent(this, AddonInstallerActivity::class.java).apply {

            putExtra(EXTRA_DO_INSTALL_SMS_CALLS_ADDON, doInstallSmsCallsAddon)
            putExtra(EXTRA_DO_INSTALL_SETTINGS_ADDON, doInstallSettingsAddon)

            val filePaths = ArrayList<String>(0)
            for (p in callsDataPackets) { filePaths.add(p.callDBFile.absolutePath) }
            for (p in smsDataPackets) { filePaths.add(p.smsDBFile.absolutePath) }

            putStringArrayListExtra(EXTRA_SMS_CALLS_ADDON_FILES, filePaths)
        }, JOBCODE_LAUNCH_ADDON_INSTALLER)
    }

    private fun getERPItem(iconResource: Int, textResource: Int): View {
        return getERPItem(iconResource, getString(textResource))
    }

    private fun getERPItem(iconResource: Int, text: String): View {
        return View.inflate(this, R.layout.extra_prep_item, null).apply {
            tryIt { this.erp_item_icon.setImageResource(iconResource) }
            tryIt { this.erp_text.text = text }
        }
    }

    private fun toggleERPItemStatusIcon(erpView: View?, isDone: Int, doneMessage: String? = null){

        if (erpView == null) return

        if (isDone == DONE || isDone == CANCEL) {

            if (isDone == DONE){
                tryIt { erpView.erp_state_icon.setImageResource(R.drawable.ic_item_done) }
            }
            else if (isDone == CANCEL){
                tryIt { erpView.erp_state_icon.setImageResource(R.drawable.ic_item_cancel) }
            }

            tryIt { erpView.erp_item_progressBar.visibility = View.GONE }
            tryIt { erpView.erp_state_icon.visibility = View.VISIBLE }
        }
        else {
            tryIt { erpView.erp_item_progressBar.visibility = View.VISIBLE }
            tryIt { erpView.erp_state_icon.visibility = View.GONE }
        }

        tryIt {
            doneMessage?.let {
                erpView.erp_desc.text = it
                erpView.erp_desc.visibility = View.VISIBLE
            }
        }
    }

    private fun <T : GetterMarker> filterSelected(packets: ArrayList<T>) {
        val selected = ArrayList<T>(0)
        for (p in packets) {
            if (cancelChecks) break
            if (p.isSelected) selected.add(p)
        }
        if (!cancelChecks) {
            packets.clear()
            packets.addAll(selected)
        }
    }

    private fun jobProceed(jobCode: Int, status: Int, label: String){
        jobcodeOrder.indexOf(jobCode).let {
            toggleERPItemStatusIcon(viewOrder[it], status, label)
            doFallThroughJob(jobcodeOrder[it+1])
        }
    }

    private fun doFallThroughJob(jobCode: Int) {

        var fallThrough = false
        var workingJobCode = 0

        fun doJob(jCode: Int, func: (jCode: Int) -> Unit) {

            if ((jCode == JOBCODE_PREP_END || !cancelChecks) && (fallThrough || jobCode == jCode)) {

                val view : View? = when (jCode) {
                    JOBCODE_PREP_APP -> erpItemApps
                    JOBCODE_PREP_KEYBOARD -> erpItemKeyboard
                    JOBCODE_PREP_WIFI -> erpItemWifi
                    JOBCODE_PREP_CONTACTS -> erpItemContacts
                    JOBCODE_PREP_SMS -> erpItemSms
                    JOBCODE_PREP_CALLS -> erpItemCalls
                    JOBCODE_PREP_DPI -> erpItemDpi
                    JOBCODE_PREP_ADB -> erpItemAdb
                    JOBCODE_PREP_FONT_SCALE -> erpItemFontScale
                    else -> null
                }

                fallThrough = if (view != null || jCode == JOBCODE_PREP_END){
                    workingJobCode = jCode
                    toggleERPItemStatusIcon(view, WAIT)
                    restore_countdown.visibility = View.GONE
                    Handler().postDelayed({func(jCode)}, DUMMY_WAIT_TIME)
                    false
                }
                else true

            }
        }

        fun proceed(status: Int, label: String) = jobProceed(workingJobCode, status, label)

        doJob(JOBCODE_PREP_APP) {

            val appsNotInstalled = ArrayList<AppPacketsKotlin>(0)
            val appsInstalled = ArrayList<AppPacketsKotlin>(0)

            commonTools.doBackgroundTask({
                for (p in appPackets){
                    if (cancelChecks) break
                    p.packageName?.let {
                        if (p.IS_SELECTED) {
                            if (isPackageInstalled(it) || (p.apkName != null && p.APP))
                                appsInstalled.add(p)
                            else appsNotInstalled.add(p)
                        }
                    }
                }
            }, {
                if (appsNotInstalled.isEmpty()) {
                    appPackets.clear()
                    appPackets.addAll(appsInstalled)
                    proceed(DONE, "${getString(R.string.number_of_selected_apps)}: ${appPackets.size}")
                }
                else {
                    AppsNotInstalledViewManager(appsNotInstalled, this).run {

                        val ad = AlertDialog.Builder(this@ExtraRestorePrepare)
                                .setView(this.getView())
                                .setPositiveButton(R.string.continue_) {_, _ ->
                                    appPackets.clear()
                                    appPackets.addAll(appsInstalled)
                                    proceed(DONE, "${getString(R.string.number_of_selected_apps)}: ${appPackets.size}")
                                }
                                .setNegativeButton(R.string.abort) {_, _ ->
                                    finishThis()
                                }
                                .setNeutralButton(R.string.skip_apps) {_, _ ->
                                    appPackets.clear()
                                    proceed(CANCEL, getString(R.string.cancelled))
                                }
                                .setCancelable(false)
                                .create()
                        ad.show()

                        getRefreshButton().setOnClickListener {
                            tryIt {
                                ad.dismiss()
                                doFallThroughJob(JOBCODE_PREP_APP)
                            }
                        }
                    }
                }
            })
        }

        doJob(JOBCODE_PREP_KEYBOARD) {

            var executed = false
            keyboardSettingsItem?.value.toString().let {

                val kPackageName =
                        if (it.contains('/')) it.split('/')[0]
                        else it

                if (!isPackageInstalled(kPackageName)){
                    var isPresent = false
                    for (app in appPackets){
                        if (app.packageName.toString() == kPackageName && app.APP){
                            isPresent = true
                            break
                        }
                    }

                    if (!isPresent) {
                        AlertDialog.Builder(this)
                                .setTitle(R.string.keyboard_not_present)
                                .setMessage("$kPackageName\n${getString(R.string.keyboard_not_present_desc)}")
                                .setPositiveButton(R.string.skip_keyboard){_, _ ->
                                    keyboardSettingsItem?.isSelected = false
                                    proceed(CANCEL, getString(R.string.cancelled))
                                }
                                .setNegativeButton(R.string.abort){_, _ ->
                                    finishThis()
                                }
                                .setCancelable(false)
                                .apply {
                                    if (isPackageInstalled(PACKAGE_NAME_PLAY_STORE)){
                                        setNeutralButton(R.string.install_from_playstore) {_, _ ->
                                            playStoreLink(kPackageName)
                                            finishThis()
                                        }
                                    }
                                }
                                .show()
                    } else proceed(DONE, getString(R.string.will_be_restored_later))
                } else proceed(DONE, getString(R.string.will_be_restored_later))

                executed = true
            }

            if (!executed) proceed(CANCEL, "null")
        }

        doJob(JOBCODE_PREP_WIFI) {

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (!wifiManager.isWifiEnabled){
                AlertDialog.Builder(this).apply {
                    setTitle(R.string.wifiPrepareTitle)
                    setMessage(R.string.wifiPrepareDesc)
                    setPositiveButton(R.string.proceed) {_, _ -> doFallThroughJob(JOBCODE_PREP_WIFI)}
                    setNegativeButton(R.string.abort) {_, _ -> finishThis()}
                    setNeutralButton(R.string.skip_wifi) {_, _ ->
                        proceed(CANCEL, getString(R.string.cancelled))
                    }
                    setCancelable(false)
                }
                        .show()
            }
            else proceed(DONE, getString(R.string.ready_to_be_restored))
        }

        doJob(JOBCODE_PREP_CONTACTS) {

            val contactsView = View.inflate(this, R.layout.contacts_dialog_view, null).apply {
                for (cp in contactDataPackets) {
                    this.contact_files_display_holder.addView(TextView(this@ExtraRestorePrepare).apply { text = cp.vcfFile.name })
                }
            }

            AlertDialog.Builder(this)
                    .setTitle(R.string.restore_contacts_dialog_header)
                    .setView(contactsView)
                    .setPositiveButton(R.string.proceed) { _, _ -> nextContact() }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        contactDataPackets.clear()
                        proceed(CANCEL, getString(R.string.cancelled))
                    }
                    .setCancelable(false)
                    .show()
        }

        doJob(JOBCODE_PREP_SMS) {

            if (doInstallSmsCallsAddon) {
                if (addonSmsCallsSuccessful) communicatorAddon.setSmsCallsAddonAsDefaultSmsApp(it)
                else proceed(CANCEL, getString(R.string.addon_not_installed))
            }
            else proceed(DONE, getString(R.string.nothing_to_restore))

        }

        doJob(JOBCODE_PREP_CALLS) {

            if (doInstallSmsCallsAddon) {
                if (addonSmsCallsSuccessful) communicatorAddon.grantWriteCallLogToSmsCallsAddon(it)
                else proceed(CANCEL, getString(R.string.addon_not_installed))

            }
            else proceed(DONE, getString(R.string.nothing_to_restore))
        }

        doJob(JOBCODE_PREP_DPI) {
            proceed(DONE, getString(R.string.will_be_restored_later))
        }

        doJob(JOBCODE_PREP_ADB) {
            proceed(DONE, getString(R.string.ready_to_be_restored))
        }

        doJob(JOBCODE_PREP_FONT_SCALE) {
            proceed(DONE, getString(R.string.ready_to_be_restored))
        }

        doJob(JOBCODE_PREP_END) {

            fun act(){
                if (cancelChecks) finishThis()
                else {
                    Intent(this, RestoreServiceKotlin::class.java)
                            .putExtra(EXTRA_NOTIFICATION_FIX, notificationFix)
                            .run {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                    startForegroundService(this)
                                else startService(this)
                            }
                }
            }

            if (!cancelChecks){

                just_start_restore.setOnClickListener {
                    act()
                    alreadyTriggered = true
                    tryIt { handler.removeCallbacks(runnable) }
                }

                if (sharedPrefs.getBoolean(PREF_RESTORE_START_ANIMATION, true)) {

                    restore_countdown.visibility = View.VISIBLE
                    var c = 3
                    var firstSlideIn = true
                    restore_countdown_text.text = c.toString()

                    val slideOut = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right)
                    val slideIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)

                    slideOut.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {}
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            restore_countdown_text.text = (--c).toString()
                            restore_countdown_text.startAnimation(slideIn)
                        }
                    })

                    slideIn.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {}
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            if (firstSlideIn) {
                                handler = Handler()
                                runnable = Runnable {
                                    if (c == 1) {
                                        tryIt { handler.removeCallbacks(runnable) }
                                        if (!alreadyTriggered) act()
                                    } else {
                                        restore_countdown_text.startAnimation(slideOut)
                                        handler.postDelayed(runnable, 1000)
                                    }
                                }
                                handler.post(runnable)
                                firstSlideIn = false
                            }
                        }
                    })

                    restore_countdown_text.startAnimation(slideIn)
                }
                else {
                    act()
                }
            }
            else act()
        }
    }

    private fun nextContact(){
        when {
            cancelChecks -> doFallThroughJob(JOBCODE_PREP_END)
            contactsCount < contactDataPackets.size -> try {
                startActivityForResult(Intent(Intent.ACTION_VIEW).apply {

                    val packet = contactDataPackets[contactsCount]

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        setDataAndType(FileProvider.getUriForFile(this@ExtraRestorePrepare,
                                "migrate.helper.provider", packet.vcfFile), "text/x-vcard")
                    else setDataAndType(Uri.fromFile(packet.vcfFile), "text/x-vcard")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    contactsCount++

                }, JOBCODE_RESTORE_CONTACTS)
            } catch (e: Exception){
                e.printStackTrace()
                toggleERPItemStatusIcon(erpItemContacts, CANCEL, e.message.toString())
            }
            else -> {
                toggleERPItemStatusIcon(erpItemContacts, DONE)
                doFallThroughJob(JOBCODE_PREP_SMS)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            JOBCODE_PREP_CALLS -> doFallThroughJob(JOBCODE_PREP_CALLS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            JOBCODE_RESTORE_CONTACTS -> nextContact()

            JOBCODE_LAUNCH_ADDON_INSTALLER -> {
                when {
                    resultCode == Activity.RESULT_OK -> {
                        addonSmsCallsSuccessful = true
                        addonSettingsSuccessful = true
                        doFallThroughJob(JOBCODE_PREP_APP)
                    }
                    data != null -> {
                        data.run {
                            if (getBooleanExtra(EXTRA_ADDON_DO_ABORT, false)){
                                cancelChecks = true
                                doFallThroughJob(JOBCODE_PREP_END)
                            }
                            else {
                                addonSmsCallsSuccessful = getBooleanExtra(EXTRA_SMS_CALLS_ADDON_OK, false)
                                addonSettingsSuccessful = getBooleanExtra(EXTRA_SETTINGS_ADDON_OK, false)
                                doFallThroughJob(JOBCODE_PREP_APP)
                            }
                        }
                    }
                    else -> {
                        AlertDialog.Builder(this).apply {
                            setMessage(R.string.addon_installer_data_null)
                            setNegativeButton(R.string.abort) { _, _ -> cancelChecks = true; doFallThroughJob(JOBCODE_PREP_END) }
                            setPositiveButton(R.string.restore_only_apps) { _, _ -> doFallThroughJob(JOBCODE_PREP_APP) }
                        }
                    }
                }
            }
        }
    }

    private fun finishThis(){
        tryIt { handler.removeCallbacks(runnable) }
        startActivity(Intent(this, RestoreSelectorKotlin::class.java))
        finish()
    }

    override fun onBackPressed() {
        finishThis()
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { handler.removeCallbacks(runnable) }
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
    }

    override fun onPermissionAsked(requestCode: Int, result: Boolean) {
        jobProceed(requestCode,
                if (result) DONE else CANCEL,
                getString(if (result) R.string.ready_to_be_restored else R.string.denied))
    }
}