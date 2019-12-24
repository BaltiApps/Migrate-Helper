package balti.migrate.helper.revert

interface OnRevert {
    fun onRevert(errors: ArrayList<String>)
}