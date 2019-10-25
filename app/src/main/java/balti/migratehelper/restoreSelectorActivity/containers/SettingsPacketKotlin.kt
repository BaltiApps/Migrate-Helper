package balti.migratehelper.restoreSelectorActivity.containers

import balti.migratehelper.AppInstance
import balti.migratehelper.R

data class SettingsPacketKotlin(private val dpiText: String?, private val adbState: Int?,
                                private val fontScale: Double?, private val keyboardText: String?) {

    companion object {
        val SETTINGS_TYPE_DPI = "TYPE_DPI"
        val SETTINGS_TYPE_ADB = "TYPE_ADB"
        val SETTINGS_TYPE_FONT_SCALE = "TYPE_FONT_SCALE"
        val SETTINGS_TYPE_KEYBOARD = "TYPE_KEYBOARD"
    }

    abstract class SettingsItem : GetterMarker() {
        abstract var commandsToRestore: List<String>
        abstract var settingsType: String
        abstract var value : Any
    }

    //private var dpiItem: DpiInternalPacket? = null
    //var adbItem: AdbInternalPacket? = null
    //var fontScaleItem: FontScaleInternalPacket? = null
    //var keyboardItem: KeyboardInternalPacket? = null

    private inner class DpiInternalPacket(dpiText: String): SettingsItem() {
        override var settingsType = SETTINGS_TYPE_DPI
        override var value: Any = dpiText
        override var iconResource: Int = R.drawable.ic_dpi_icon
        override var displayText: String = AppInstance.appContext.getString(R.string.dpi)
        override var isSelected: Boolean = true
        override var commandsToRestore = dpiText.let {
            val lines = it.split("\\n")
            var oDensity = 0
            var pDensity = 0
            for (line in lines) {
                line.trim().let { l ->
                    if (l.startsWith("Physical density:")) {
                        pDensity = try { Integer.parseInt(l.substring(l.lastIndexOf(' ')).trim()) } catch (e: Exception) {0}
                    } else if (l.startsWith("Override density:")) {
                        oDensity = try { Integer.parseInt(l.substring(l.lastIndexOf(' ')).trim()) } catch (e: Exception) {0}
                    }
                }
            }
            val valueToRetore = if (oDensity > 0) oDensity
            else if (pDensity > 0) pDensity
            else -1

            if (valueToRetore > 0){
                listOf("wm density $valueToRetore")
            }
            else listOf("")
        }
    }

    private inner class AdbInternalPacket(adbState: Int): SettingsItem() {
        override var settingsType = SETTINGS_TYPE_ADB
        override var value: Any = adbState
        override var iconResource: Int = R.drawable.ic_adb_icon
        override var displayText: String = AppInstance.appContext.getString(R.string.adb_state)
        override var isSelected: Boolean = true
        override var commandsToRestore = listOf(
                "echo \"ADB: $adbState\"",
                "echo \" \"",
                "settings put global adb_enabled $adbState"
        )
    }

    private inner class FontScaleInternalPacket(fontScale: Double): SettingsItem() {
        override var settingsType = SETTINGS_TYPE_FONT_SCALE
        override var value: Any = fontScale
        override var iconResource: Int = R.drawable.ic_font_scale_icon
        override var displayText: String = AppInstance.appContext.getString(R.string.font_scale)
        override var isSelected: Boolean = true
        override var commandsToRestore = listOf(
                "echo \"Font scale: $fontScale\"",
                "echo \" \"",
                "settings put system font_scale $fontScale"
        )
    }

    private inner class KeyboardInternalPacket(keyboardText: String): SettingsItem() {
        override var settingsType = SETTINGS_TYPE_KEYBOARD
        override var value: Any = keyboardText
        override var iconResource: Int = R.drawable.ic_keyboard_icon
        override var displayText: String = AppInstance.appContext.getString(R.string.keyboard)
        override var isSelected: Boolean = true
        override var commandsToRestore = listOf("ime enable $keyboardText")
    }

    val internalPackets: ArrayList<SettingsItem> = ArrayList(0)

    init {
        internalPackets.clear()
        dpiText?.let { internalPackets.add(DpiInternalPacket(it)) }
        adbState?.let { internalPackets.add(AdbInternalPacket(adbState)) }
        fontScale?.let { internalPackets.add(FontScaleInternalPacket(fontScale)) }
        keyboardText?.let { internalPackets.add(KeyboardInternalPacket(keyboardText)) }
    }

}