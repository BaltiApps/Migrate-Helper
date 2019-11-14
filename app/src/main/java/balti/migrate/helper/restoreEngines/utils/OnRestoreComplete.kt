package balti.migrate.helper.restoreEngines.utils

interface OnRestoreComplete {
    fun onRestoreComplete(jobCode: Int, jobSuccess: Boolean, jobResults: ArrayList<String>?)
}