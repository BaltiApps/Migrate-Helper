package balti.migratehelper.extrasRestore.utils

interface OnRestoreComplete {
    fun onRestoreComplete(jobCode: Int, jobSuccess: Boolean, jobResults: ArrayList<String>?)
}