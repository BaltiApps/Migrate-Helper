package balti.migratehelper.restoreSelectorActivity

interface OnReadComplete {
    fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any)
}