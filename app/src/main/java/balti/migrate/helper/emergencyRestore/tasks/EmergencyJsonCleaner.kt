package balti.migrate.helper.emergencyRestore.tasks

import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.METADATA_HOLDER_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.SU_INIT
import balti.module.baltitoolbox.functions.GetResources

class EmergencyJsonCleaner: ParentCoroutineTask() {

    private val shell by lazy { Runtime.getRuntime().exec(SU_INIT) }
    private val title by lazy { GetResources.getStringFromRes(R.string.cleaning_metadata) }

    override val suShell: Process = shell

    override suspend fun onPreExecute() {
        super.onPreExecute()
        sendProgress(title, "", "")
    }

    override suspend fun doInBackground(arg: Any?): Any? {
        clean()
        return null
    }

    private fun clean() {
        try {
            writeNext("cd $METADATA_HOLDER_DIR")
            writeNext("ls -1p | grep -v / | grep -E \"\\.app.marker\$\"")
            flushShell()
            val appMarkersPackage = getOutput().map { it.substring(0, it.length - 11) }
            writeNext("ls -1p | grep -v / | grep -E \"\\.data.marker\$\"")
            flushShell()
            val dataMarkersPackage = getOutput().map { it.substring(0, it.length - 12) }

            for (dmp in dataMarkersPackage) {
                writeNext("rm -f $MIGRATE_CACHE/${dmp}.json 2>/dev/null")
            }
            flushShell()

            val onlyApps = appMarkersPackage - dataMarkersPackage

            for (oa in onlyApps) {
                writeNext("if [[ ! -e /data/data/${oa}.tar.gz ]]; then")
                writeNext("    rm -f $MIGRATE_CACHE/${oa}.json 2>/dev/null")
                writeNext("fi")
                flushShell()
            }

            closeShell()
        } catch (e: Exception){
            sendErrors(arrayListOf("CLEANER: " + e.message.toString()))
            e.printStackTrace()
        }
    }
}