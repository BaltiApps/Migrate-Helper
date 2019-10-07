package balti.migratehelper.restoreSelectorActivity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import balti.migratehelper.R
import balti.migratehelper.utilities.CommonToolsKotlin

class RestoreSelectorKotlin: AppCompatActivity(), OnReadComplete {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.restore_selector)
    }

    private fun doJob(){

    }

    override fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any) {

    }

}