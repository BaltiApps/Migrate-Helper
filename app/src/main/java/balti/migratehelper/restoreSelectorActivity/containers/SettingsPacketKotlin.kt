package balti.migratehelper.restoreSelectorActivity.containers

data class SettingsPacketKotlin(val dpiText: String?, val adbState: Int?,
                                val fontScale: Double?, val keyboardText: String?) {

    class DpiInternalPacket(val dpiText: String?): GetterMarker() {
        override var isSelected: Boolean = dpiText == null
    }

    class AdbInternalPacket(val adbState: Int?): GetterMarker() {
        override var isSelected: Boolean = adbState == null
    }

    class FontScaleInternalPacket(val fontScale: Double?): GetterMarker() {
        override var isSelected: Boolean = fontScale == null
    }

    class KeyboardInternalPacket(val keyboardText: String?): GetterMarker() {
        override var isSelected: Boolean = keyboardText == null
    }

    val internalPackets = arrayListOf(DpiInternalPacket(dpiText), AdbInternalPacket(adbState),
            FontScaleInternalPacket(fontScale), KeyboardInternalPacket(keyboardText))

}