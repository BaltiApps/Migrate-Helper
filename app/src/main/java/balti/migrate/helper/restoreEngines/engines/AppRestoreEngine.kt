package balti.migrate.helper.restoreEngines.engines

import android.os.Build
import balti.migrate.helper.R
import balti.migrate.helper.restoreEngines.ParentRestoreClass
import balti.migrate.helper.restoreEngines.RestoreServiceKotlin
import balti.migrate.helper.restoreSelectorActivity.containers.AppPacketsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_APP_MAKING_SCRIPT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_APP_MAKING_SCRIPT_TRY_CATCH
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_APP_RESTORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_APP_RESTORE_MESSAGE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_APP_RESTORE_SUPPRESSED
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_APP_RESTORE_TRY_CATCH
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_APP_RESTORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_MAKING_SCRIPTS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_WAITING_FOR_VCF
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_RESTORE_SCRIPT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.INFO_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.KNOWN_CONTACTS_ELEMENTS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.KNOWN_CONTACT_APPS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_STATUS
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_FDROID
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.PREF_REMOUNT_DATA
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.Misc.getPercentage
import balti.module.baltitoolbox.functions.Misc.isPackageInstalled
import balti.module.baltitoolbox.functions.Misc.iterateBufferedReader
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import java.io.*

class AppRestoreEngine(private val jobcode: Int,
                       private val appPackets: ArrayList<AppPacketsKotlin>,
                       private val doNotificationFix: Boolean): ParentRestoreClass("") {

    companion object {
        var ICON_STRING = ""
    }

    private var RESTORE_PID = -999
    private var isContactAppPresent = false
    private val ignorableErrors : Array<String> by lazy { arrayOf("") } // to be filled in future

    private var scriptFile : File? = null
    private var suProcess : Process? = null

    private val allErrors by lazy { ArrayList<String>(0) }
    private val actualErrors by lazy { ArrayList<String>(0) }

    private val busyboxBinaryPath by lazy {
        val cpuAbi = Build.SUPPORTED_ABIS[0]
        (if (cpuAbi == "x86" || cpuAbi == "x86_64")
            unpackAssetToInternal("busybox-x86", "busybox")
        else unpackAssetToInternal("busybox", "busybox")).apply {
            File(this).setExecutable(true)
        }
    }

    private fun addToActualErrors(err: String){
        actualErrors.add(err)
        allErrors.add(err)
    }

    private fun isDangerousPermission(permission: String): Boolean {

        val dangerousPermissions = arrayOf(
                "android.permission.READ_CALENDAR",
                "android.permission.WRITE_CALENDAR",
                "android.permission.CAMERA",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.GET_ACCOUNTS",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.RECORD_AUDIO",
                "android.permission.READ_PHONE_STATE",
                "android.permission.READ_PHONE_NUMBERS",
                "android.permission.CALL_PHONE",
                "android.permission.ANSWER_PHONE_CALLS",
                "android.permission.READ_CALL_LOG",
                "android.permission.WRITE_CALL_LOG",
                "android.permission.ADD_VOICEMAIL",
                "android.permission.USE_SIP",
                "android.permission.PROCESS_OUTGOING_CALLS",
                "android.permission.BODY_SENSORS",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.READ_SMS",
                "android.permission.RECEIVE_WAP_PUSH",
                "android.permission.RECEIVE_MMS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        )

        return permission.trim() in dangerousPermissions
    }

    private fun makingScripts(): String?{

        resetBroadcast(true, engineContext.getString(R.string.making_scripts), EXTRA_PROGRESS_MAKING_SCRIPTS)

        try {
            val installScriptPath = unpackAssetToInternal("installScript.sh", "installScript.sh")
            val restoreDataScriptPath = unpackAssetToInternal("restoreDataScript.sh", "restoreDataScript.sh")

            installScriptPath.let { if (it != "") File(it).setExecutable(true) }
            restoreDataScriptPath.let { if (it != "") File(it).setExecutable(true) }

            scriptFile = File(engineContext.filesDir, FILE_RESTORE_SCRIPT)
            scriptFile?.let {
                BufferedWriter(FileWriter(it)).run {
                    fun writeNext(line: String) {
                        write("${line}\n")
                    }
                    writeNext("#!sbin/sh\n")
                    writeNext("echo \" \"")
                    writeNext("sleep 1s")
                    writeNext("echo \"--- RESTORE PID: $$\"")
                    writeNext("cp ${it.absolutePath} ${INFO_HOLDER_DIR}/")
                    writeNext("echo \" \"")

                    if (getPrefBoolean(PREF_REMOUNT_DATA, false)) {
                        write("cat /proc/mounts | grep data | while read -r line || [[ -n \"\$line\" ]]; do\n")
                        write("    mp=\"\$(echo \$line | cut -d ' ' -f2)\"\n")
                        write("    md=\"\$(echo \$line | cut -d ' ' -f1)\"\n")
                        write("    if [[ \$mp == \"/data\" || \$mp == \"/\" || \$mp == \"/data/data\" ]]; then\n")
                        write("        mount -o rw,remount \$md \$mp && echo \"Mounted \$md on \$mp\"\n")
                        write("    fi\n")
                        write("done\n")
                    }

                    if (isPackageInstalled(PACKAGE_NAME_PLAY_STORE))
                        writeNext("am force-stop $PACKAGE_NAME_PLAY_STORE 2>/dev/null")

                    if (isPackageInstalled(PACKAGE_NAME_FDROID))
                        writeNext("am force-stop $PACKAGE_NAME_FDROID 2>/dev/null")

                    for (appPacket in appPackets) {

                        if (RestoreServiceKotlin.cancelAll) break
                        try {

                            appPacket.appName?.let { name -> broadcastProgress(name, name, false) }

                            val correctedName: String = commonTools.applyNamingCorrectionForShell(appPacket.appName ?: "null")

                            writeNext("echo \"$MIGRATE_STATUS: $correctedName icon: ${appPacket.iconFileName ?: appPacket.appIcon}\"")

                            val permFile = File(METADATA_HOLDER_DIR, "${appPacket.packageName}.perm")

                            val isApp = appPacket.APP && appPacket.apkName != null
                            val isData = appPacket.DATA && appPacket.dataName != null
                            val isPermission = appPacket.PERMISSION && appPacket.isPermission && permFile.exists()

                            if ((isApp || isData || isPermission) &&
                                    appPacket.packageName in KNOWN_CONTACT_APPS)
                                isContactAppPresent = true

                            if (isApp)
                                writeNext("sh $installScriptPath $MIGRATE_CACHE ${appPacket.packageName}.app ${appPacket.apkName} ${appPacket.packageName} ${appPacket.installerName} $METADATA_HOLDER_DIR $correctedName")

                            if (isData)
                                writeNext("sh $restoreDataScriptPath $busyboxBinaryPath ${appPacket.dataName} ${appPacket.packageName} $doNotificationFix $METADATA_HOLDER_DIR $MIGRATE_CACHE")

                            if (isPermission) {
                                BufferedReader(FileReader(permFile)).readLines().forEach { it1 ->
                                    val pLine = it1.trim()
                                    if (pLine.startsWith("android.permission") && isDangerousPermission(pLine)) {
                                        val displayPerm = pLine.substring(pLine.lastIndexOf('.') + 1).trim()
                                        writeNext("pm grant ${appPacket.packageName} $pLine 2>/dev/null && echo Grant: $displayPerm")
                                    }
                                }
                            }
                        }
                        catch (e: Exception){
                            e.printStackTrace()
                            addToActualErrors("$ERROR_APP_MAKING_SCRIPT: ${e.message}")
                        }
                    }

                    //writeNext("mv -f $MIGRATE_CACHE/$FILE_PACKAGE_DATA* ${engineContext.externalCacheDir}/ 2>/dev/null")
                    //writeNext("cp $MIGRATE_CACHE/$FILE_FILE_LIST* ${engineContext.externalCacheDir}/ 2>/dev/null")

                    writeNext("echo \" \"")
                    writeNext("echo \"--- DONE! ---\"")

                    close()
                }

                it.setExecutable(true)
                return it.absolutePath
            }
        } catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERROR_APP_MAKING_SCRIPT_TRY_CATCH: ${e.message}")
        }

        return null
    }

    private fun runRestoreScript(scriptFileLocation: String){

        try {

            if (!File(scriptFileLocation).exists())
                throw Exception(engineContext.getString(R.string.script_file_does_not_exist))

            resetBroadcast(false, engineContext.getString(R.string.restoring_apps), EXTRA_PROGRESS_APP_RESTORE)

            suProcess = Runtime.getRuntime().exec("su")
            suProcess?.let {
                val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
                val outputStream = BufferedReader(InputStreamReader(it.inputStream))
                val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                suInputStream.write("sh $scriptFileLocation\n")
                suInputStream.write("exit\n")
                suInputStream.flush()

                var c = 0
                var appName = ""
                var progress = 0

                iterateBufferedReader(outputStream, { output ->

                    if (RestoreServiceKotlin.cancelAll) {
                        commonTools.cancelTask(suProcess, RESTORE_PID)
                        return@iterateBufferedReader true
                    }

                    if (output.startsWith("--- RESTORE PID:")){
                        tryIt {
                            RESTORE_PID = output.substring(output.lastIndexOf(" ") + 1).toInt()
                        }
                    }

                    var line = output

                    if (output.startsWith("ERROR::")) {
                        line = output.substring("ERROR:: ".length)
                        addToActualErrors("$ERROR_APP_RESTORE_MESSAGE: $line")
                    }

                    if (output.startsWith(MIGRATE_STATUS)) {

                        line = output.substring(MIGRATE_STATUS.length + 2)

                        if (line.contains("icon:")) {
                            ICON_STRING = line.substring(line.lastIndexOf(' ')).trim()
                            line = line.substring(0, line.indexOf("icon:"))
                        }

                        appName = line
                        progress = getPercentage(++c, appPackets.size)
                        broadcastProgress(appName, "\n${appName}", true, progress)
                    }
                    else broadcastProgress(appName, line, false)

                    return@iterateBufferedReader line == "--- DONE! ---"
                })

                tryIt { it.waitFor() }

                iterateBufferedReader(errorStream, { errorLine ->

                    var ignorable = false

                    ignorableErrors.forEach { warnings ->
                        if (errorLine.endsWith(warnings)) ignorable = true
                    }

                    if (!ignorable)
                        addToActualErrors("$ERROR_APP_RESTORE: $errorLine")
                    else allErrors.add("$ERROR_APP_RESTORE_SUPPRESSED: $errorLine")

                    return@iterateBufferedReader false
                })
            }

        } catch (e: Exception){
            e.printStackTrace()
            addToActualErrors("$ERROR_APP_RESTORE_TRY_CATCH: ${e.message}")
        }
    }

    private fun waitForContactsRestore(): String?{

        if (isContactAppPresent){
            try {

                resetBroadcast(true, engineContext.getString(R.string.waiting_for_contacts), EXTRA_PROGRESS_WAITING_FOR_VCF)
                broadcastProgress("", engineContext.getString(R.string.waiting_for_contacts_desc), false)

                Runtime.getRuntime().exec("su").let {
                    val writer = BufferedWriter(OutputStreamWriter(it.outputStream))
                    val reader = BufferedReader(InputStreamReader(it.inputStream))

                    var output: String

                    do {
                        output = ""

                        KNOWN_CONTACTS_ELEMENTS.forEach { ele ->
                            writer.write("dumpsys activity services | $ele\n")
                        }
                        writer.write("echo DONE\n")
                        writer.flush()

                        iterateBufferedReader(reader, { line ->
                            if (line.trim() != "DONE") output += "$line\n"
                            return@iterateBufferedReader line.trim() == "DONE"
                        })

                        output = output.trim()
                        if (output != "") Thread.sleep(1000)

                    } while (output != "")

                    writer.write("exit\n")
                    writer.flush()
                }

                return null
            }
            catch (e: Exception){
                e.printStackTrace()
                return e.message.toString()
            }
        }
        else return null
    }

    override fun doInBackground(vararg params: Any?): Any {
        val scriptLocation = makingScripts()
        waitForContactsRestore()?.run { addToActualErrors(this) }
        if (!RestoreServiceKotlin.cancelAll) scriptLocation?.let { runRestoreScript(it) }
        return 0
    }

    override fun postExecuteFunction() {
        RESTORE_PID = -999
        onRestoreComplete.onRestoreComplete(jobcode, actualErrors.size == 0, allErrors)
    }
}