package balti.migratehelper.simpleActivities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import balti.migratehelper.AppInstance
import balti.migratehelper.R
import balti.migratehelper.utilities.CommonToolsKotlin
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.PREF_ANDROID_VERSION_WARNING
import balti.migratehelper.utilities.CommonToolsKotlin.Companion.REPORTING_EMAIL
import kotlinx.android.synthetic.main.activity_main.*

class MainActivityKotlin: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val main by lazy { AppInstance.sharedPrefs }
    private val editor by lazy { main.edit() }

    private val progressReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@MainActivityKotlin, ProgressShowActivity::class.java)
                        .apply {
                            intent?.let {
                                this.putExtras(it)
                                this.action = it.action
                            }
                        }
                )
                commonTools.tryIt { commonTools.LBM?.unregisterReceiver(this) }
                finish()
            }
        }
    }

    private val endOnDisable by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) = finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cpuAbi = Build.SUPPORTED_ABIS[0]

        if (cpuAbi == "armeabi-v7a" || cpuAbi == "arm64-v8a" || cpuAbi == "x86" || cpuAbi == "x86_64") {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && !main.getBoolean(PREF_ANDROID_VERSION_WARNING, false)) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.too_fast)
                        .setMessage(R.string.too_fast_desc)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(R.string.dont_show_again) { _, _ ->
                            editor.putBoolean(PREF_ANDROID_VERSION_WARNING, true)
                            editor.commit()
                        }
                        .show()
            }
        }
        else {

            restoreButton.visibility = View.GONE

            AlertDialog.Builder(this)
                    .setTitle(R.string.unsupported_device)
                    .setMessage(getString(R.string.cpu_arch_is) + "\n" + cpuAbi + "\n\n" + getString(R.string.currently_supported_cpu))
                    .setPositiveButton(R.string.close) { _, _ ->
                        finish()
                    }
                    .setNegativeButton(R.string.contact) { _, _ ->
                        val email = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORTING_EMAIL))
                            putExtra(Intent.EXTRA_SUBJECT, "Unsupported device")
                            putExtra(Intent.EXTRA_TEXT, commonTools.deviceSpecifications)
                        }
                        try {
                            startActivity(Intent.createChooser(email, getString(R.string.select_mail)))
                        } catch (e: Exception) {
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setCancelable(false)
                    .show()
        }

        restoreButton.setOnClickListener {

        }
    }

}