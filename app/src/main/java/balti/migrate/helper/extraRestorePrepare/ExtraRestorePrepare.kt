package balti.migrate.helper.extraRestorePrepare

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreSelectorActivity.RestoreSelectorKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.GetterMarker
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migrate.helper.simpleActivities.ProgressShowActivity
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ACTION_RESTORE_PROGRESS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_AUTO_INSTALL_HELPER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_NOTIFICATION_FIX
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
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_INSTALL_WATCHER
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_SMS_APP
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_RESTORE_START_ANIMATION
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_USE_WATCHER
import balti.migrate.helper.utilities.constants.RestartWatcherConstants.Companion.WATCHER_PACKAGE_NAME
import kotlinx.android.synthetic.main.contacts_dialog_view.view.*
import kotlinx.android.synthetic.main.extra_prep_item.view.*
import kotlinx.android.synthetic.main.extra_restore_prepare.*
import kotlinx.android.synthetic.main.install_watcher_dialog_view.view.*

class ExtraRestorePrepare: AppCompatActivity() {

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
    private var autoInstallHelper = false

    private val commonTools by lazy { CommonToolsKotlin(this) }

    private var contactsCount = 0

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler

    private var keyboardSettingsItem : SettingsPacketKotlin.SettingsItem? = null
    private var notificationFix = false

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
                commonTools.tryIt { commonTools.LBM?.unregisterReceiver(this) }
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
                    SettingsPacketKotlin.SETTINGS_TYPE_DPI -> erpItemDpi = view
                    SettingsPacketKotlin.SETTINGS_TYPE_ADB -> erpItemAdb = view
                    SettingsPacketKotlin.SETTINGS_TYPE_FONT_SCALE -> erpItemFontScale = view
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

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_RESTORE_PROGRESS))

        doFallThroughJob(JOBCODE_PREP_APP)
    }

    private fun getERPItem(iconResource: Int, textResource: Int): View {
        return getERPItem(iconResource, getString(textResource))
    }

    private fun getERPItem(iconResource: Int, text: String): View {
        return View.inflate(this, R.layout.extra_prep_item, null).apply {
            commonTools.tryIt { this.erp_item_icon.setImageResource(iconResource) }
            commonTools.tryIt { this.erp_text.text = text }
        }
    }

    private fun toggleERPItemStatusIcon(erpView: View?, isDone: Int, doneMessage: String? = null){

        if (erpView == null) return

        if (isDone == DONE || isDone == CANCEL) {

            if (isDone == DONE){
                commonTools.tryIt { erpView.erp_state_icon.setImageResource(R.drawable.ic_item_done) }
            }
            else if (isDone == CANCEL){
                commonTools.tryIt { erpView.erp_state_icon.setImageResource(R.drawable.ic_item_cancel) }
            }

            commonTools.tryIt { erpView.erp_item_progressBar.visibility = View.GONE }
            commonTools.tryIt { erpView.erp_state_icon.visibility = View.VISIBLE }
        }
        else {
            commonTools.tryIt { erpView.erp_item_progressBar.visibility = View.VISIBLE }
            commonTools.tryIt { erpView.erp_state_icon.visibility = View.GONE }
        }

        commonTools.tryIt {
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

    private fun doFallThroughJob(jobCode: Int) {

        var fallThrough = false

        fun doJob(jCode: Int, func: () -> Unit) {

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
                    toggleERPItemStatusIcon(view, WAIT)
                    restore_countdown.visibility = View.GONE
                    Handler().postDelayed({func()}, DUMMY_WAIT_TIME)
                    false
                }
                else true

            }
        }

        doJob(JOBCODE_PREP_APP) {

            fun proceed(status: Int, label: String){
                toggleERPItemStatusIcon(erpItemApps, status, label)
                doFallThroughJob(JOBCODE_PREP_KEYBOARD)
            }

            val appsNotInstalled = ArrayList<AppPacketsKotlin>(0)
            val appsInstalled = ArrayList<AppPacketsKotlin>(0)

            commonTools.doBackgroundTask({
                for (p in appPackets){
                    if (cancelChecks) break
                    p.packageName?.let {
                        if (p.IS_SELECTED) {
                            if (commonTools.isPackageInstalled(it) || (p.apkName != null && p.APP))
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
                            commonTools.tryIt {
                                ad.dismiss()
                                doFallThroughJob(JOBCODE_PREP_APP)
                            }
                        }
                    }
                }
            })
        }

        doJob(JOBCODE_PREP_KEYBOARD) {

            fun proceed(status: Int, label: String){
                toggleERPItemStatusIcon(erpItemKeyboard, status, label)
                doFallThroughJob(JOBCODE_PREP_WIFI)
            }

            var executed = false
            keyboardSettingsItem?.value.toString().let {

                val kPackageName =
                        if (it.contains('/')) it.split('/')[0]
                        else it

                if (!commonTools.isPackageInstalled(kPackageName)){
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
                                    if (commonTools.isPackageInstalled(PACKAGE_NAME_PLAY_STORE)){
                                        setNeutralButton(R.string.install_from_playstore) {_, _ ->
                                            commonTools.playStoreLink(kPackageName)
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

            fun proceed(status: Int, label: String){
                toggleERPItemStatusIcon(erpItemWifi, status, label)
                doFallThroughJob(JOBCODE_PREP_CONTACTS)
            }

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

            fun proceed(status: Int, label: String){
                toggleERPItemStatusIcon(erpItemContacts, status, label)
                doFallThroughJob(JOBCODE_PREP_SMS)
            }

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

            fun proceed(status: Int, label: String){
                toggleERPItemStatusIcon(erpItemSms, status, label)
                doFallThroughJob(JOBCODE_PREP_CALLS)
            }

            if (!commonTools.areWeDefaultSmsApp()) {

                AlertDialog.Builder(this)
                        .setTitle(R.string.smsPermission)
                        .setMessage(getText(R.string.smsPermission_desc))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            sharedPrefs.edit().putString(PREF_DEFAULT_SMS_APP, commonTools.getDefaultSmsApp()).apply()
                            commonTools.setDefaultSms(packageName, JOBCODE_PREP_SMS)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            smsDataPackets.clear()
                            proceed(CANCEL, getString(R.string.cancelled))
                        }
                        .setCancelable(false)
                        .show()
            }
            else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && sharedPrefs.getBoolean(PREF_USE_WATCHER, true)
                        && !commonTools.isPackageInstalled(WATCHER_PACKAGE_NAME)){

                    val v = View.inflate(this, R.layout.install_watcher_dialog_view, null)
                    v.install_watcher_by_root.run {
                        autoInstallHelper = isChecked
                        setOnCheckedChangeListener { _, isChecked ->
                            autoInstallHelper = isChecked
                        }
                    }
                    v.know_more_watcher.setOnClickListener {
                        AlertDialog.Builder(this)
                                .setMessage(R.string.watcher_extended_desc)
                                .setNeutralButton(R.string.close, null)
                                .show()
                    }

                    AlertDialog.Builder(this)
                            .setView(v)
                            .setPositiveButton(R.string.install) {_, _ ->
                                if (!autoInstallHelper)
                                    commonTools.installWatcherByPackageManager(JOBCODE_RESTORE_INSTALL_WATCHER)
                                else proceed(DONE, getString(R.string.ready_to_be_restored))
                            }
                            .setNeutralButton(R.string.dont_use_watcher) {_, _ ->
                                autoInstallHelper = false
                                proceed(DONE, getString(R.string.ready_to_be_restored))
                            }
                            .setCancelable(false)
                            .show()

                }
                else {
                    proceed(DONE, getString(R.string.ready_to_be_restored))
                }
            }
        }

        doJob(JOBCODE_PREP_CALLS) {

            fun proceed(status: Int, label: String){
                toggleERPItemStatusIcon(erpItemCalls, status, label)
                doFallThroughJob(JOBCODE_PREP_DPI)
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED){

                AlertDialog.Builder(this)
                        .setTitle(R.string.callsPermission)
                        .setMessage(getText(R.string.callsPermission_desc))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CALL_LOG), JOBCODE_PREP_CALLS)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            callsDataPackets.clear()
                            proceed(CANCEL, getString(R.string.cancelled))
                        }
                        .setCancelable(false)
                        .show()

            }
            else {
                proceed(DONE, getString(R.string.ready_to_be_restored))
            }
        }

        doJob(JOBCODE_PREP_DPI) {
            toggleERPItemStatusIcon(erpItemDpi, DONE, getString(R.string.will_be_restored_later))
            doFallThroughJob(JOBCODE_PREP_ADB)
        }

        doJob(JOBCODE_PREP_ADB) {
            toggleERPItemStatusIcon(erpItemAdb, DONE, getString(R.string.ready_to_be_restored))
            doFallThroughJob(JOBCODE_PREP_FONT_SCALE)
        }

        doJob(JOBCODE_PREP_FONT_SCALE) {
            toggleERPItemStatusIcon(erpItemFontScale, DONE, getString(R.string.ready_to_be_restored))
            doFallThroughJob(JOBCODE_PREP_END)
        }

        doJob(JOBCODE_PREP_END) {

            fun act(){
                if (cancelChecks) finishThis()
                else {
                    Intent(this, RestoreServiceKotlin::class.java)
                            .putExtra(EXTRA_NOTIFICATION_FIX, notificationFix)
                            .putExtra(EXTRA_AUTO_INSTALL_HELPER, autoInstallHelper)
                            .run {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                    startForegroundService(this)
                                else startService(this)
                            }
                }
            }

            if (!cancelChecks){

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
                                        commonTools.tryIt { handler.removeCallbacks(runnable) }
                                        act()
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
            JOBCODE_PREP_SMS -> doFallThroughJob(JOBCODE_PREP_SMS)
            JOBCODE_RESTORE_INSTALL_WATCHER -> doFallThroughJob(JOBCODE_PREP_SMS)
        }
    }

    private fun finishThis(){
        commonTools.tryIt { handler.removeCallbacks(runnable) }
        startActivity(Intent(this, RestoreSelectorKotlin::class.java))
        finish()
    }

    override fun onBackPressed() {
        finishThis()
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { handler.removeCallbacks(runnable) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
    }
}