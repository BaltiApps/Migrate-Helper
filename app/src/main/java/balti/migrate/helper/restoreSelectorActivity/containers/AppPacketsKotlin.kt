package balti.migrate.helper.restoreSelectorActivity.containers

import balti.migrate.helper.utilities.constants.MtdConstants
import org.json.JSONObject
import java.io.File

class AppPacketsKotlin(jsonObject: JSONObject, val jsonFile: File) {

    var appName: String? = null
    var isSystemApp: Boolean = false
    var appIcon: String? = null
    var packageName: String? = null
    var apkName: String? = null
    var dataName: String? = null
    var version: String? = null
    var dataSize = 0L
    var systemSize = 0L
    var isPermission = false
    var iconFileName: String? = null
    var installerName: String? = null

    var APP = false
    var DATA = false
    var PERMISSION = false

    val IS_SELECTED: Boolean
        get() = APP || DATA || PERMISSION

    init {
        MtdConstants.MTD_APP_NAME.let {if (jsonObject.has(it)) appName = jsonObject.getString(it)}
        MtdConstants.MTD_IS_SYSTEM.let {if (jsonObject.has(it)) isSystemApp = jsonObject.getBoolean(it)}
        MtdConstants.MTD_APP_ICON.let {if (jsonObject.has(it)) appIcon = jsonObject.getString(it)}
        MtdConstants.MTD_PACKAGE_NAME.let {if (jsonObject.has(it)) packageName = jsonObject.getString(it)}
        MtdConstants.MTD_APK.let {if (jsonObject.has(it)) apkName = jsonObject.getString(it)}
        MtdConstants.MTD_DATA.let {if (jsonObject.has(it)) dataName = jsonObject.getString(it)}
        MtdConstants.MTD_VERSION.let {if (jsonObject.has(it)) version = jsonObject.getString(it)}
        MtdConstants.MTD_DATA_SIZE.let {if (jsonObject.has(it)) dataSize = jsonObject.getLong(it)}
        MtdConstants.MTD_SYSTEM_SIZE.let {if (jsonObject.has(it)) systemSize = jsonObject.getLong(it)}
        MtdConstants.MTD_PERMISSION.let {if (jsonObject.has(it)) isPermission = jsonObject.getBoolean(it)}
        MtdConstants.MTD_ICON_FILE_NAME.let {if (jsonObject.has(it)) iconFileName = jsonObject.getString(it)}
        MtdConstants.MTD_INSTALLER_NAME.let {if (jsonObject.has(it)) installerName = jsonObject.getString(it)}

        apkName = apkName?.let { if (it == "" || it == "NULL") null else it}
        dataName = dataName?.let { if (it == "" || it == "NULL") null else it}
        appIcon = appIcon?.let { if (it == "" || it == "NULL") null else it}
        iconFileName = iconFileName?.let { if (it == "" || it == "NULL") null else it}

        APP = apkName != null
        DATA = dataName != null
        PERMISSION = isPermission
    }
}