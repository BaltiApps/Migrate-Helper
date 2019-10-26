package balti.migratehelper.restoreEngines.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import java.io.File

class DBTools {

    fun getDataBase(dataBaseFile: File): SQLiteDatabase {
        var dataBase: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(dataBaseFile.absolutePath, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            dataBase = SQLiteDatabase.openDatabase(dataBaseFile.absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READWRITE)
        return dataBase
    }

    fun getTableRestoreCursor(db: SQLiteDatabase, tableName: String, projection: Array<String>,
                              initFunc: (Int) -> Unit): Cursor?{
        return try {
            val cursor = db.query(tableName, projection, null, null, null, null, null)
            if (cursor != null) initFunc(cursor.count)
            cursor
        }
        catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    fun restoreTable(cr: ContentResolver, cursor: Cursor, uri: Uri,
                     tableName: String, mirror: Array<String>, projection: Array<String>,
                     projectionToGetTaskLog:String, errorTag: String, breakFunc: () -> Boolean, updateFunc: (Int, String) -> Unit): ArrayList<String> {

        val errors = ArrayList<String>(0)

        try {

            var c = 0
            cursor.moveToFirst()

            outerLoop@ do {
                var taskLog = ""
                try {
                    val contentValues = ContentValues()

                    for (i in mirror.indices){

                        if (breakFunc()) break@outerLoop

                        fun setTaskLog(projection: String, data: Any?){
                            if (projection == projectionToGetTaskLog)
                                taskLog = data.toString()
                        }

                        val m = mirror[i]
                        val k = m.split(":")[0]
                        when (m.split(":")[1]) {
                            "s" -> contentValues.put(k, cursor.getString(cursor.getColumnIndex(projection[i])).apply { setTaskLog(projection[i], this) })
                            "i" -> contentValues.put(k, cursor.getInt(cursor.getColumnIndex(projection[i])).apply { setTaskLog(projection[i], this) })
                            "l" -> contentValues.put(k, cursor.getLong(cursor.getColumnIndex(projection[i])).apply { setTaskLog(projection[i], this) })
                            "d" -> contentValues.put(k, cursor.getDouble(cursor.getColumnIndex(projection[i])).apply { setTaskLog(projection[i], this) })
                            "f" -> contentValues.put(k, cursor.getFloat(cursor.getColumnIndex(projection[i])).apply { setTaskLog(projection[i], this) })
                        }
                    }

                    cr.insert(uri, contentValues)

                } catch (e: Exception) {
                    e.printStackTrace()
                    errors.add("$errorTag: $tableName:$c - ${e.message.toString()}")
                }

                updateFunc(c, taskLog)
                c++

            } while (cursor.moveToNext() && !breakFunc())

            try { cursor.close() } catch (_: Exception){}
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$errorTag: $tableName - ${e.message.toString()}")
        }

        return errors
    }

}