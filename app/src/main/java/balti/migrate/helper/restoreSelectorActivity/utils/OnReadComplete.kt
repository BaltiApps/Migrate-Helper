package balti.migrate.helper.restoreSelectorActivity.utils

interface OnReadComplete {
    fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any)
}