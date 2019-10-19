package balti.migratehelper.extrasRestore

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.restoreSelectorActivity.containers.ContactsPacketKotlin
import balti.migratehelper.restoreSelectorActivity.containers.GetterMarker
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_ADB
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_APP
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_CALLS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_CONTACTS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_DPI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_FONT_SCALE
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_KEYBOARD
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_SMS
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_PREP_WIFI
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.JOBCODE_RESTORE_CONTACTS
import kotlinx.android.synthetic.main.contacts_dialog_view.view.*
import kotlinx.android.synthetic.main.extra_prep_item.view.*

class ExtraRestorePrepare: AppCompatActivity() {


    private val appPackets = AppInstance.appPackets
    private val contactDataPackets = AppInstance.contactDataPackets
    private val smsDataPackets = AppInstance.smsDataPackets
    private val callsDataPackets = AppInstance.callsDataPackets
    private var settingsPacket = AppInstance.settingsPacket
    private var wifiPacket = AppInstance.wifiPacket

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

        if (contactDataPackets.isNotEmpty()) erpItemContacts = getERPItem(R.drawable.ic_contact_icon, R.string.contacts)
        if (smsDataPackets.isNotEmpty()) erpItemSms = getERPItem(R.drawable.ic_sms_icon, R.string.sms)
        if (callsDataPackets.isNotEmpty()) erpItemCalls = getERPItem(R.drawable.ic_call_log_icon, R.string.calls)
        if (settingsPacket?.dpiItem != null) erpItemDpi = getERPItem(R.drawable.ic_dpi_icon, R.string.dpi)
        if (settingsPacket?.adbItem != null) erpItemAdb = getERPItem(R.drawable.ic_adb_icon, R.string.adb_state)
        if (settingsPacket?.fontScale != null) erpItemFontScale = getERPItem(R.drawable.ic_font_scale_icon, R.string.font_scale)
        if (settingsPacket?.keyboardItem != null) erpItemKeyboard = getERPItem(R.drawable.ic_keyboard_icon, R.string.keyboard)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            if (wifiPacket != null) erpItemWifi = getERPItem(R.drawable.ic_wifi_icon, R.string.wifi)

        if (appPackets.isNotEmpty()) erpItemApps = getERPItem(R.drawable.ic_app, R.string.apps)

        doFallThroughJob(JOBCODE_PREP_CONTACTS)
    }

    private fun getERPItem(iconResource: Int, textResource: Int): View {
        return View.inflate(this, R.layout.extra_prep_item, null).apply {
            commonTools.tryIt { this.erp_item_icon.setImageResource(iconResource) }
            commonTools.tryIt { this.erp_text.setText(textResource) }
        }
    }

    private fun toggleERPItemStatusIcon(erpView: View?, isDone: Int, doneMessage: Int? = null){

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
                erpView.erp_desc.setText(it)
                erpView.erp_desc.visibility = View.VISIBLE
            }
        }
    }

    private fun doFallThroughJob(jobCode: Int) {

        var fallThrough = false

        fun doJob(jCode: Int, func: (workingObject: Any) -> Unit) {

            if (!cancelChecks && (fallThrough || jobCode == jCode)) {

                fun <T : GetterMarker> filterSelected(packets: ArrayList<T>): ArrayList<T>? {
                    val selected = ArrayList<T>(0)
                    for (p in packets) if (p.isSelected) selected.add(p)
                    return if (selected.size == 0) null else selected
                }

                fun <T : GetterMarker> filterSelected(packet: T?): T? {
                    return if (packet != null && packet.isSelected) packet
                    else null
                }

                val wo: Any? = when (jCode) {
                    JOBCODE_PREP_CONTACTS -> filterSelected(contactDataPackets)
                    JOBCODE_PREP_SMS -> filterSelected(contactDataPackets)
                    JOBCODE_PREP_CALLS -> filterSelected(contactDataPackets)
                    JOBCODE_PREP_DPI -> filterSelected(settingsPacket?.dpiItem)?.dpiText
                    JOBCODE_PREP_ADB -> filterSelected(settingsPacket?.adbItem)?.adbState
                    JOBCODE_PREP_FONT_SCALE -> filterSelected(settingsPacket?.fontScaleItem)?.fontScale
                    JOBCODE_PREP_KEYBOARD -> filterSelected(settingsPacket?.keyboardItem)?.keyboardText
                    JOBCODE_PREP_WIFI -> filterSelected(wifiPacket)?.wifiFile
                    JOBCODE_PREP_APP -> appPackets.let { if (it.isNotEmpty()) it else null }
                    else -> null
                }

                fallThrough = if (wo == null) true
                else {
                    try {
                        func(wo); false
                    } catch (e: Exception) {
                        e.printStackTrace(); true
                    }
                }

            }
        }

        doJob(JOBCODE_PREP_CONTACTS) {
            (it as ArrayList<ContactsPacketKotlin>).let { cps ->
                contactDataPackets.clear()
                contactDataPackets.addAll(cps)
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
                        toggleERPItemStatusIcon(erpItemContacts, CANCEL, R.string.cancelled)
                        doFallThroughJob(JOBCODE_PREP_SMS)
                    }
                    .setCancelable(false)
                    .show()
        }

        doJob(JOBCODE_PREP_SMS) {

        }
    }

    private fun nextContact(){
        if (contactsCount < contactDataPackets.size) {
            startActivityForResult(Intent(Intent.ACTION_VIEW).apply {

                val packet = contactDataPackets[contactsCount]

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    setDataAndType(FileProvider.getUriForFile(this@ExtraRestorePrepare,
                            "migrate.helper.provider", packet.vcfFile), "text/x-vcard")
                else setDataAndType(Uri.fromFile(packet.vcfFile), "text/x-vcard")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                contactsCount++

            }, JOBCODE_RESTORE_CONTACTS)
        }
        else doFallThroughJob(JOBCODE_PREP_SMS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            JOBCODE_RESTORE_CONTACTS -> nextContact()
        }
    }
}