package balti.migrate.helper.revert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import balti.migrate.helper.R
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.DIR_REVERT_DIR
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.FILE_REVERT_HEAD
import balti.migrate.helper.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_FILE_PERMISSION
import kotlinx.android.synthetic.main.revert_settings.*
import java.io.File
import java.io.FileFilter

class RevertSettingsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.revert_settings)

        refreshFiles()
        refresh_revert.setOnClickListener { refreshFiles() }
        grant_revert.setOnClickListener { requestReadPermission() }
    }

    private fun refreshFiles() {

        fun toggleLayout(case: Int){
            val views = arrayOf(revert_content_layout, revert_no_files_layout, revert_no_permission)
            for (i in views.indices) {
                if (case == i) views[i].visibility = View.VISIBLE
                else views[i].visibility = View.GONE
            }
        }

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
                restore_file_list.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, display)
            }
        }
        else {
            toggleLayout(2)
        }
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
}