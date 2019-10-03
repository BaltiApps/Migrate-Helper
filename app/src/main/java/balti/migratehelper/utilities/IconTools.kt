package balti.migratehelper.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.ImageView
import balti.migratehelper.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class IconTools {

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
                if (result != null) {
                    iconView.setImageBitmap(result)
                } else {
                    iconView.setImageResource(R.drawable.ic_app)
                }
            }
        }
        Setter().execute()
    }

    fun setIconFromFile(iconView: ImageView, file: File){
        var icon = ""
        BufferedReader(FileReader(file)).readLines().forEach {
            icon += it
        }

        setIconFromIconString(iconView, icon)
    }
}