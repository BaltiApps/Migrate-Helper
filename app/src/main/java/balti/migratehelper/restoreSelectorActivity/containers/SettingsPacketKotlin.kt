package balti.migratehelper.restoreSelectorActivity.containers

import balti.migratehelper.AppInstance
import balti.migratehelper.R

data class SettingsPacketKotlin(val dpiText: String?, val adbState: Int?,
                                val fontScale: Double?, val keyboardText: String?) {

    class DpiInternalPacket(val dpiText: String?): GetterMarker() {
        override var iconResource: Int = R.drawable.ic_dpi_icon
        override var displayText: String = AppInstance.appContext.getString(R.string.dpi)
        override var isSelected: Boolean = dpiText != null
    }

    class AdbInternalPacket(val adbState: Int?): GetterMarker() {
        override var iconResource: Int = R.drawable.ic_adb_icon
        override var displayText: String = AppInstance.appContext.getString(R.string.adb_state)
        override var isSelected: Boolean = adbState != null
    }

    class FontScaleInternalPacket(val fontScale: Double?): GetterMarker() {
        override var iconResource: Int = R.drawable.ic_font_scale_icon
        override var displayText: String = AppInstance.appContext.getString(R.string.font_scale)
        override var isSelected: Boolean = fontScale != null
    }

    class KeyboardInternalPacket(val keyboardText: String?): GetterMarker() {
        override var iconResource: Int = R.drawable.ic_keyboard_icon
        override var displayText: String = AppInstance.appContext.getString(R.string.keyboard)
        override var isSelected: Boolean = keyboardText != null
    }

    val internalPackets: ArrayList<GetterMarker> = ArrayList(0)

    init {
        dpiText?.let { internalPackets.add(DpiInternalPacket(dpiText)) }
        adbState?.let { internalPackets.add(AdbInternalPacket(adbState)) }
        fontScale?.let { internalPackets.add(FontScaleInternalPacket(fontScale)) }
        keyboardText?.let { internalPackets.add(KeyboardInternalPacket(keyboardText)) }
    }

}