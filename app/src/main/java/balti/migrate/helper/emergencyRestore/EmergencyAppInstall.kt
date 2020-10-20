package balti.migrate.helper.emergencyRestore

import android.util.Log
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.module.baltitoolbox.functions.FileHandlers

class EmergencyAppInstall() : ParentCoroutineTask() {

    private val shell by lazy { Runtime.getRuntime().exec("su") }
    override val suShell: Process = shell

    override suspend fun doInBackground(arg: Any?): Any? {
        runScript()
        return null
    }


    private fun runScript(){
        writeNext("cd ${CommonToolsKotlin.MIGRATE_CACHE}")
        writeNext("ls -1p | grep / | grep -E \"\\.app/\$\"")
        flushShell()
        val apps = getOutput().toString()
        val installScriptPath = FileHandlers.unpackAssetToInternal("installScript.sh", "installScript.sh")
        Log.d(CommonToolsKotlin.DEBUG_TAG, apps)

        /*writeNext("echo \"--- RESTORE PID: $$\"")
        writeNext("chmod -R 777 \"${CommonToolsKotlin.MIGRATE_CACHE}\"")
        writeNext("echo \" \"")

        if (Build.VERSION.SDK_INT <= 29)
            writeNext("settings put global package_verifier_enable 0")
        else writeNext("settings put global verifier_verify_adb_installs 0")

        for (app in apps) {
        }*/

        closeShell()
    }
}