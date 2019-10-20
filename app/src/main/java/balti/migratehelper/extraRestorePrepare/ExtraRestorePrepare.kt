package balti.migratehelper.extraRestorePrepare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Telephony
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import balti.migratehelper.AppInstance.Companion.actualDefaultSmsAppName
import balti.migratehelper.AppInstance.Companion.appPackets
import balti.migratehelper.AppInstance.Companion.callsDataPackets
import balti.migratehelper.AppInstance.Companion.contactDataPackets
import balti.migratehelper.AppInstance.Companion.settingsPacket
import balti.migratehelper.AppInstance.Companion.smsDataPackets
import balti.migratehelper.AppInstance.Companion.wifiPacket
import balti.migratehelper.R
import balti.migratehelper.extraRestorePrepare.utils.AppsNotInstalledViewManager
import balti.migratehelper.restoreEngines.RestoreServiceKotlin
import balti.migratehelper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migratehelper.restoreSelectorActivity.containers.GetterMarker
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.DUMMY_WAIT_TIME
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_ADB
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_APP
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_CALLS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_CONTACTS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_DPI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_END
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_FONT_SCALE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_KEYBOARD
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_SMS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_WIFI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_CONTACTS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_SET_THIS_AS_DEFAULT_SMS_APP
import kotlinx.android.synthetic.main.contacts_dialog_view.view.*
import kotlinx.android.synthetic.main.extra_prep_item.view.*
import kotlinx.android.synthetic.main.extra_restore_prepare.*

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

    private val commonTools by lazy { CommonToolsKotlin(this) }

    private var contactsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.extra_restore_prepare)

        filterSelected(contactDataPackets)
        filterSelected(smsDataPackets)
        filterSelected(callsDataPackets)
        settingsPacket?.let {
            it.dpiItem = filterSelected(it.dpiItem)
            it.adbItem = filterSelected(it.adbItem)
            it.fontScaleItem = filterSelected(it.fontScaleItem)
            it.keyboardItem = filterSelected(it.keyboardItem)
            filterSelected(it.internalPackets)
        }
        wifiPacket = filterSelected(wifiPacket)

        if (contactDataPackets.isNotEmpty()) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_contact_icon, R.string.contacts).apply { erpItemContacts = this })
        if (smsDataPackets.isNotEmpty()) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_sms_icon, R.string.sms).apply { erpItemSms = this })
        if (callsDataPackets.isNotEmpty()) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_call_log_icon, R.string.calls).apply { erpItemCalls = this })
        if (settingsPacket?.dpiItem != null) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_dpi_icon, R.string.dpi).apply { erpItemDpi = this })
        if (settingsPacket?.adbItem != null) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_adb_icon, R.string.adb_state).apply { erpItemAdb = this })
        if (settingsPacket?.fontScaleItem != null) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_font_scale_icon, R.string.font_scale).apply { erpItemFontScale = this })
        if (settingsPacket?.keyboardItem != null) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_keyboard_icon, R.string.keyboard).apply { erpItemKeyboard = this })

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            if (wifiPacket != null) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_wifi_icon, R.string.wifi).apply { erpItemWifi = this })

        if (appPackets.isNotEmpty()) extra_perm_check_holder.addView(getERPItem(R.drawable.ic_app, R.string.apps).apply { erpItemApps = this })

        doFallThroughJob(JOBCODE_PREP_CONTACTS)
    }

    private fun getERPItem(iconResource: Int, textResource: Int): View {
        return View.inflate(this, R.layout.extra_prep_item, null).apply {
            commonTools.tryIt { this.erp_item_icon.setImageResource(iconResource) }
            commonTools.tryIt { this.erp_text.setText(textResource) }
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

    private fun <T : GetterMarker> filterSelected(packet: T?): T? {
        return if (cancelChecks) packet
        else if (packet != null && packet.isSelected) packet
        else null
    }

    private fun doFallThroughJob(jobCode: Int) {

        var fallThrough = false

        fun doJob(jCode: Int, func: () -> Unit) {

            if ((jCode == JOBCODE_PREP_END || !cancelChecks) && (fallThrough || jobCode == jCode)) {

                val view : View? = when (jCode) {
                    JOBCODE_PREP_CONTACTS -> erpItemContacts
                    JOBCODE_PREP_SMS -> erpItemSms
                    JOBCODE_PREP_CALLS -> erpItemCalls
                    JOBCODE_PREP_DPI -> erpItemDpi
                    JOBCODE_PREP_ADB -> erpItemAdb
                    JOBCODE_PREP_FONT_SCALE -> erpItemFontScale
                    JOBCODE_PREP_KEYBOARD -> erpItemKeyboard
                    JOBCODE_PREP_WIFI -> erpItemWifi
                    JOBCODE_PREP_APP -> erpItemApps
                    else -> null
                }

                fallThrough = if (view != null || jCode == JOBCODE_PREP_END){
                    toggleERPItemStatusIcon(view, WAIT)
                    Handler().postDelayed({func()}, DUMMY_WAIT_TIME)
                    false
                }
                else true

            }
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
                        toggleERPItemStatusIcon(erpItemContacts, CANCEL, getString(R.string.cancelled))
                        doFallThroughJob(JOBCODE_PREP_SMS)
                    }
                    .setCancelable(false)
                    .show()
        }

        doJob(JOBCODE_PREP_SMS) {

            fun getDefaultSmsApp(): String {
                return Telephony.Sms.getDefaultSmsPackage(this)
            }

            fun areWeDefaultSmsApp(): Boolean {
                return getDefaultSmsApp() == packageName
            }

            if (!areWeDefaultSmsApp()) {

                AlertDialog.Builder(this)
                        .setTitle(R.string.smsPermission)
                        .setMessage(getText(R.string.smsPermission_desc))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            actualDefaultSmsAppName = getDefaultSmsApp()
                            commonTools.setDefaultSms(packageName, JOBCODE_SET_THIS_AS_DEFAULT_SMS_APP)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            toggleERPItemStatusIcon(erpItemSms, CANCEL, getString(R.string.cancelled))
                            doFallThroughJob(JOBCODE_PREP_CALLS)
                        }
                        .setCancelable(false)
                        .show()
            }
            else {
                toggleERPItemStatusIcon(erpItemSms, DONE, getString(R.string.ready_to_be_restored))
                doFallThroughJob(JOBCODE_PREP_CALLS)
            }
        }

        doJob(JOBCODE_PREP_CALLS) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED){

                AlertDialog.Builder(this)
                        .setTitle(R.string.callsPermission)
                        .setMessage(getText(R.string.callsPermission_desc))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CALL_LOG), JOBCODE_PREP_CALLS)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            toggleERPItemStatusIcon(erpItemCalls, CANCEL, getString(R.string.cancelled))
                            doFallThroughJob(JOBCODE_PREP_DPI)
                        }
                        .setCancelable(false)
                        .show()

            }
            else {
                toggleERPItemStatusIcon(erpItemCalls, DONE, getString(R.string.ready_to_be_restored))
                doFallThroughJob(JOBCODE_PREP_DPI)
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
            doFallThroughJob(JOBCODE_PREP_KEYBOARD)
        }

        doJob(JOBCODE_PREP_KEYBOARD) {
            toggleERPItemStatusIcon(erpItemKeyboard, DONE, getString(R.string.will_be_restored_later))
            doFallThroughJob(JOBCODE_PREP_APP)
        }

        doJob(JOBCODE_PREP_APP) {

            val appsNotInstalled = ArrayList<AppPacketsKotlin>(0)

            commonTools.doBackgroundTask({
                val appFiltered = ArrayList<AppPacketsKotlin>(0)

                for (p in appPackets){
                    if (cancelChecks) break
                    if (p.IS_SELECTED) appFiltered.add(p)
                    p.packageName?.let {
                        if (p.IS_SELECTED && !(commonTools.isPackageInstalled(it) || p.apkName != null))
                            appsNotInstalled.add(p)
                    }
                }
                if (!cancelChecks) {
                    appPackets.clear()
                    appPackets.addAll(appFiltered)
                }
            }, {
                if (appsNotInstalled.isEmpty()) {
                    toggleERPItemStatusIcon(erpItemApps, DONE)
                    doFallThroughJob(JOBCODE_PREP_END)
                }
                else {
                    AppsNotInstalledViewManager(appsNotInstalled, this).run {
                        AlertDialog.Builder(this@ExtraRestorePrepare)
                                .setView(this.getView())
                                .setPositiveButton(R.string.continue_) {_, _ ->
                                    toggleERPItemStatusIcon(erpItemApps, DONE)
                                    doFallThroughJob(JOBCODE_PREP_END)
                                }
                                .setNegativeButton(android.R.string.cancel) {_, _ ->
                                    toggleERPItemStatusIcon(erpItemApps, CANCEL)
                                    cancelChecks = true
                                    doFallThroughJob(JOBCODE_PREP_END)
                                }
                                .setCancelable(false)
                                .show()
                    }
                }
            })
        }

        doJob(JOBCODE_PREP_END) {
            if (!cancelChecks) Intent(this, RestoreServiceKotlin::class.java).run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(this)
                else startService(this)
            }
            else finish()
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
        when (requestCode){
            JOBCODE_RESTORE_CONTACTS -> nextContact()
            JOBCODE_PREP_SMS -> doFallThroughJob(JOBCODE_PREP_SMS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}