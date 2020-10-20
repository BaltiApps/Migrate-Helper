package balti.migrate.helper.emergencyRestore

import android.os.Build
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.module.baltitoolbox.functions.FileHandlers
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes

class EmergencyAppInstall() : ParentCoroutineTask() {

    private val shell by lazy { Runtime.getRuntime().exec("su") }
    private val title by lazy { getStringFromRes(R.string.installing_apps) }

    override val suShell: Process = shell

    override suspend fun onPreExecute() {
        super.onPreExecute()
        sendProgress(title, "", "")
    }

    override suspend fun doInBackground(arg: Any?): Any? {
        runScript()
        sleepTask(1000)
        return null
    }


    private fun runScript(){
        writeNext("cd $MIGRATE_CACHE")
        // ls -1p | grep / | grep -E "\.app/$"
        writeNext("ls -1p | grep / | grep -E \"\\.app/\$\"")
        flushShell()
        val apps = getOutput()
        val installScriptPath = FileHandlers.unpackAssetToInternal("installScript.sh", "installScript.sh")

        writeNext("echo \"--- RESTORE PID: $$\"")
        writeNext("chmod -R 777 \"${MIGRATE_CACHE}\"")
        writeNext("echo \" \"")

        if (Build.VERSION.SDK_INT <= 29)
            writeNext("settings put global package_verifier_enable 0")
        else writeNext("settings put global verifier_verify_adb_installs 0")

        for (app in apps) {
            val packageName = app.substring(0, app.length - 5)
            sendProgress(title, packageName, "")
            writeNext("sh $installScriptPath $MIGRATE_CACHE $packageName.app $packageName.apk $packageName NULL $METADATA_HOLDER_DIR $packageName ${Build.VERSION.SDK_INT}")
            flushShell()
            getOutput().let {
                var op = ""
                it.forEach { op += "$it\n" }
                sendProgress(title, packageName, op)
            }
        }

        sendProgress(getStringFromRes(R.string.installs_done), "", "")

        sendErrors(getAllErrors())

        closeShell()
    }
}