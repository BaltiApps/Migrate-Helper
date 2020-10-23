package balti.migrate.helper.emergencyRestore.tasks

import android.os.Build
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc
import java.io.File

class EmergencyAppData() : ParentCoroutineTask() {

    private val shell by lazy { Runtime.getRuntime().exec("su") }
    private val title by lazy { getStringFromRes(R.string.restoring_app_data) }
    private val errors by lazy { ArrayList<String>(0) }

    override val suShell: Process = shell

    private val busyboxBinaryPath by lazy {
        val cpuAbi = Build.SUPPORTED_ABIS[0]
        (if (cpuAbi == "x86" || cpuAbi == "x86_64")
            unpackAssetToInternal("busybox-x86", "busybox")
        else unpackAssetToInternal("busybox", "busybox")).apply {
            File(this).setExecutable(true)
        }
    }

    override suspend fun onPreExecute() {
        super.onPreExecute()
        sendProgress(title, "", "")
    }

    override suspend fun doInBackground(arg: Any?): Any? {
        val listNotInstalled = runScript(if (arg is Boolean) arg else true)
        sleepTask(1000)
        return listNotInstalled
    }


    private fun runScript(notificationFix: Boolean = true): ArrayList<String> {
        writeNext("cd $MIGRATE_CACHE")
        writeNext("mv *.tar.gz /data/data 2>/dev/null")
        writeNext("cd /data/data")
        // ls -1p | grep -v / | grep -E "\.tar.gz$"
        writeNext("ls -1p | grep -v / | grep -E \"\\.tar.gz\$\"")
        flushShell()
        val dataTars = getOutput()
        val restoreDataScriptPath = unpackAssetToInternal("restoreDataScript.sh", "restoreDataScript.sh")
        restoreDataScriptPath.let { if (it != "") File(it).setExecutable(true) }

        writeNext("echo \"--- RESTORE PID: $$\"")
        writeNext("chmod -R 777 \"${MIGRATE_CACHE}\"")
        writeNext("echo \" \"")

        val packageList = ArrayList<String>(0)

        for (tar in dataTars) {
            val packageName = tar.substring(0, tar.length - 7)
            packageList.add(packageName)
            val index = dataTars.indexOf(tar)+1
            val percent = Misc.getPercentage(index, dataTars.size)
            sendProgress("$title [$index/${dataTars.size}]", packageName, "\n\n${getStringFromRes(R.string.restoring_data_for_package)}:\n$packageName\n\n", percent)
            writeNext("sh $restoreDataScriptPath $busyboxBinaryPath $tar $packageName $notificationFix $METADATA_HOLDER_DIR $MIGRATE_CACHE ${Build.VERSION.SDK_INT}")
            flushShell()
            getOutput().let {
                var op = ""
                it.forEach {
                    op += "$it\n"
                    if (it.startsWith("ERROR::")) errors.add("$packageName: $it")
                }
                sendProgress(title, packageName, op, percent)
            }
        }

        if (dataTars.isNotEmpty()) sendProgress(getStringFromRes(R.string.restore_app_data_done), "", "", 100)
        closeShell()
        sendErrors(errors.apply { addAll(getAllErrors()) })

        return ArrayList(packageList.filter { pkg -> !Misc.isPackageInstalled(pkg) })
    }
}