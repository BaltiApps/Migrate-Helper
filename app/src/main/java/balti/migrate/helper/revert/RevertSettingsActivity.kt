package balti.migrate.helper.revert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import balti.migrate.helper.R
import balti.migrate.helper.restoreSelectorActivity.containers.SettingsPacketKotlin
import balti.migrate.helper.revert.engine.RevertEngine
import balti.migrate.helper.utilities.CommonToolsKotlin
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DIR_REVERT_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.ERROR_REVERT_READ
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_REBOOT
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.EXTRA_DO_UNINSTALL
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_REVERT_ERROR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_REVERT_HEAD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_FILE_PERMISSION
import balti.migrate.helper.utilities.UninstallServiceKotlin
import balti.migrate.helper.utilities.constants.SettingsFields
import kotlinx.android.synthetic.main.revert_settings.*
import org.json.JSONObject
import java.io.*

class RevertSettingsActivity: AppCompatActivity(), OnRevert {

    companion object {
        var cancelRevert = false
    }

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val revertErrorWriter by lazy {
        File(commonTools.workingDir, FILE_REVERT_ERROR).let {
            it.delete()
            BufferedWriter(FileWriter(it))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.revert_settings)

        refreshFiles()
        refresh_revert.setOnClickListener { refreshFiles() }
        grant_revert.setOnClickListener { requestReadPermission() }
    }

    private fun toggleLayout(case: Int){
        val views = arrayOf(revert_content_layout, revert_no_files_layout, revert_no_permission, revert_waiting_layout)
        for (i in views.indices) {
            if (case == i) views[i].visibility = View.VISIBLE
            else views[i].visibility = View.GONE
        }
    }

    private fun refreshFiles() {

        if (isFilePermissionGranted()) {

            val display = ArrayList<String>(0)
            val revertFiles = File(DIR_REVERT_DIR).apply { mkdirs() }.listFiles(FileFilter {
                it.isFile && it.name.startsWith(FILE_REVERT_HEAD)
            })

            //reverse sort
            revertFiles.sortWith(Comparator { o1, o2 ->
                String.CASE_INSENSITIVE_ORDER.compare(o2.name, o1.name)
            })

            revertFiles.forEach { display.add(it.name) }

            if (display.isEmpty()) toggleLayout(1)
            else {
                toggleLayout(0)
                restore_file_list.apply {
                    adapter = ArrayAdapter<String>(this@RevertSettingsActivity, android.R.layout.simple_list_item_1, display)
                    setOnItemClickListener { _, _, position, _ ->
                        startRestore(revertFiles[position])
                    }
                }
            }
        }
        else {
            toggleLayout(2)
        }
    }

    private fun startRestore(file: File) {

        var settingsObject: SettingsPacketKotlin? = null
        var error = ""

        toggleLayout(3)
        cancelRevert = false

        commonTools.doBackgroundTask({

            try {
                val text = StringBuilder()
                val reader = BufferedReader(FileReader(file))

                reader.readLines().forEach {
                    text.append(it)
                }
                val jsonObject = JSONObject(text.toString())

                jsonObject.run {

                    var dpiText: String? = null
                    var adbState: Int? = null
                    var fontScale: Double? = null

                    SettingsFields.JSON_FIELD_DPI_TEXT.let { if (this.has(it)) dpiText = this.getString(it) }
                    SettingsFields.JSON_FIELD_ADB_TEXT.let { if (this.has(it)) adbState = this.getInt(it) }
                    SettingsFields.JSON_FIELD_FONT_SCALE.let { if (this.has(it)) fontScale = this.getDouble(it) }

                    settingsObject = SettingsPacketKotlin(dpiText, adbState, fontScale, null, file)
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                error = "$ERROR_REVERT_READ: ${e.message.toString()}"
            }

        }, {

            if (settingsObject == null && error == "")
                error = getString(R.string.null_revert_object)

            if (error == "") {

                settingsObject?.let {

                    val message = StringBuilder()
                    it.internalPackets.forEach { v ->
                        message.append("${v.displayText}: ${v.value}")
                    }

                    AlertDialog.Builder(this).apply {

                        setTitle(R.string.following_will_be_restored)
                        setMessage(message.toString())
                        setPositiveButton(R.string.proceed) {_, _ ->

                            revert_text_status.setText(R.string.restoring_revert_file)
                            revert_cancel.setOnClickListener { cancelRevert = true }

                            RevertEngine(it, this@RevertSettingsActivity).execute()
                        }
                        setNegativeButton(android.R.string.cancel) {_, _ ->
                            toggleLayout(0)
                        }
                        setCancelable(false)
                    }
                            .show()
                }

            }
            else showErrorDialog(error)

        })

    }

    private fun showErrorDialog(message: String) {


        AlertDialog.Builder(this).apply {
            setTitle(R.string.error_revert_alert)
            setMessage(message)
            setNegativeButton(R.string.close) {_, _ -> finish() }
            setPositiveButton(R.string.report) {_, _ ->
                try {
                    revertErrorWriter.write(message)
                    revertErrorWriter.close()
                    commonTools.reportLogs(false)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@RevertSettingsActivity, R.string.error_send_screenshot, Toast.LENGTH_LONG).show()
                }
            }
            setCancelable(false)
        }
                .show()

    }
    
    private fun isFilePermissionGranted() =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestReadPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE), JOBCODE_READ_FILE_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == JOBCODE_READ_FILE_PERMISSION) refreshFiles()
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { revertErrorWriter.close() }
    }

    override fun onRevert(errors: ArrayList<String>) {
        if (errors.isEmpty()){
            AlertDialog.Builder(this).apply {
                setTitle(R.string.settings_reverted)
                setMessage(R.string.please_reboot)
                setPositiveButton(R.string.reboot) {_, _ ->
                    startService(
                            Intent(this@RevertSettingsActivity, UninstallServiceKotlin::class.java).apply {
                                putExtra(EXTRA_DO_REBOOT, true)
                                putExtra(EXTRA_DO_UNINSTALL, false)
                            }
                    )
                }
                setNegativeButton(R.string.close) {_, _ ->
                    finish()
                }
                setCancelable(false)
            }
                    .show()
        }
        else {
            val text = StringBuilder()
            errors.forEach { text.append(it + "\n") }
            showErrorDialog(text.toString().trim())
        }
    }
}