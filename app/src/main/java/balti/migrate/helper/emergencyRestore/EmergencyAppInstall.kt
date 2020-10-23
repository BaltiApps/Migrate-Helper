package balti.migrate.helper.emergencyRestore

import android.os.Build
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.module.baltitoolbox.functions.FileHandlers
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc
import java.io.File

class EmergencyAppInstall() : ParentCoroutineTask() {

    private val shell by lazy { Runtime.getRuntime().exec("su") }
    private val title by lazy { getStringFromRes(R.string.installing_apps) }
    private val errors by lazy { ArrayList<String>(0) }

    override val suShell: Process = shell

    override suspend fun onPreExecute() {
        super.onPreExecute()
        sendProgress(title, "", "")
    }

    override suspend fun doInBackground(arg: Any?): Any? {
        val listNotInstalled = runScript()
        sleepTask(1000)
        return listNotInstalled
    }


    private fun runScript(): ArrayList<String> {
        writeNext("cd $MIGRATE_CACHE")
        // ls -1p | grep / | grep -E "\.app/$"
        writeNext("ls -1p | grep / | grep -E \"\\.app/\$\"")
        flushShell()
        val apps = getOutput()
        val installScriptPath = FileHandlers.unpackAssetToInternal("installScript.sh", "installScript.sh")
        installScriptPath.let { if (it != "") File(it).setExecutable(true) }

        writeNext("echo \"--- RESTORE PID: $$\"")
        writeNext("chmod -R 777 \"${MIGRATE_CACHE}\"")
        writeNext("echo \" \"")

        if (Build.VERSION.SDK_INT <= 29)
            writeNext("settings put global package_verifier_enable 0")
        else writeNext("settings put global verifier_verify_adb_installs 0")

        val packageList = ArrayList<String>(0)

        for (app in apps) {
            val packageName = app.substring(0, app.length - 5)
            packageList.add(packageName)
            val index = apps.indexOf(app)+1
            val percent = Misc.getPercentage(index, apps.size)
            sendProgress("$title [$index/${apps.size}]", packageName, "\n\n${getStringFromRes(R.string.installing_apk_for_package)}:\n$packageName\n\n", percent)
            writeNext("sh $installScriptPath $MIGRATE_CACHE $packageName.app $packageName.apk $packageName NULL $METADATA_HOLDER_DIR $packageName ${Build.VERSION.SDK_INT}")
            flushShell()
            getOutput().let {
                var op = ""
                it.forEach {
                    op += "$it\n"
                    if (it.startsWith("ERROR::") || it.startsWith("FAILURE")) errors.add("$packageName: $it")
                }
                sendProgress(title, packageName, op, percent)
            }
        }

        if (apps.isNotEmpty()) sendProgress(getStringFromRes(R.string.installs_done), "", "", 100)
        closeShell()
        sendErrors(errors.apply { addAll(getAllErrors()) })

        return ArrayList(packageList.filter { pkg -> !Misc.isPackageInstalled(pkg) })
    }
}