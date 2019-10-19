package balti.migratehelper.extrasRestore

import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.utilities.CommonToolsKotlin
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

    private val commonTools by lazy { CommonToolsKotlin(this) }

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
    }

    private fun getERPItem(iconResource: Int, textResource: Int): View {
        return View.inflate(this, R.layout.extra_prep_item, null).apply {
            commonTools.tryIt { this.erp_item_icon.setImageResource(iconResource) }
            commonTools.tryIt { this.erp_text.setText(textResource) }
        }
    }

    private fun toggleERPItemStatusIcon(erpView: View, isDone: Int, doneMessage: Int? = null){

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}