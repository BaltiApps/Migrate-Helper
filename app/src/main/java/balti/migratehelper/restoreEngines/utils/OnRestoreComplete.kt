package balti.migratehelper.restoreEngines.utils

interface OnRestoreComplete {
    fun onRestoreComplete(jobCode: Int, jobSuccess: Boolean, jobResults: ArrayList<String>?)
}