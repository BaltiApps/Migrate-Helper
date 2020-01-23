package balti.migrate.helper.extraRestorePrepare.utils

interface OnPermissionAsked {
    fun onPermissionAsked(requestCode: Int, result: Boolean)
}