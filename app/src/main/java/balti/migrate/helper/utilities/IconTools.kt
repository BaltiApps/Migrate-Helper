package balti.migrate.helper.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.ImageView
import balti.migrate.helper.AppInstance
import balti.migrate.helper.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class IconTools(private val multipleLoad: Boolean = false) {

    private val commonTools by lazy { CommonToolsKotlin(AppInstance.appContext) }

    fun setIconFromIconString(iconView: ImageView, iconString: String){

        class Setter : AsyncTask<Any, Any, Bitmap?>(){

            override fun doInBackground(vararg params: Any?): Bitmap? {

                return try {

                    val byteChunks = iconString.trim().split("_")

                    val filterData = ArrayList<Byte>(0)
                    for (byte in byteChunks) {
                        try {
                            filterData.add(java.lang.Byte.parseByte(byte))
                        }
                        catch (e: Exception){}
                    }

                    val imageData = ByteArray(filterData.size)
                    for (d in imageData.indices) imageData[d] = filterData[d]

                    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(result: Bitmap?) {
                super.onPostExecute(result)
                try {
                    if (result != null) {
                        iconView.setImageBitmap(result)
                    } else {
                        iconView.setImageResource(R.drawable.ic_app)
                    }
                }
                catch (e: Exception) {
                    iconView.setImageResource(R.drawable.ic_app)
                }
            }
        }
        if (multipleLoad) Setter().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        else Setter().execute()
    }

    fun setIconFromFile(iconView: ImageView, file: File){

        var bitmap: Bitmap? = null
        val icon = StringBuffer("")

        class Setter : AsyncTask<Any, Any, String>() {
            override fun doInBackground(vararg params: Any?): String {

                if (file.name.endsWith(".png")) {
                    commonTools.tryIt { bitmap = BitmapFactory.decodeFile(file.absolutePath) }
                }
                else {
                    try {
                        if (file.exists() && file.canRead()) {
                            BufferedReader(FileReader(file)).readLines().forEach {
                                icon.append(it)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return icon.toString()
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                if (bitmap != null) commonTools.tryIt { iconView.setImageBitmap(bitmap) }
                else setIconFromIconString(iconView, result)
            }
        }
        if (multipleLoad) Setter().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        else Setter().execute()
    }
}