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
                     columnToGetTaskLog:Int, errorTag: String, updateFunc: (Int, String) -> Unit): ArrayList<String> {

        val errors = ArrayList<String>(0)

        try {
            var c = 0
            cursor.moveToFirst()
            do {
                var taskLog = ""
                try {
                    val contentValues = ContentValues()

                    for (i in mirror.indices){

                        fun setTaskLog(content: Any){
                            if (i == columnToGetTaskLog)
                                taskLog = content.toString()
                        }

                        val m = mirror[i]
                        val k = m.split(":")[0]
                        when (m.split(":")[1]) {
                            "s" -> contentValues.put(k, cursor.getString(cursor.getColumnIndex(projection[i])).apply { setTaskLog(this) })
                            "i" -> contentValues.put(k, cursor.getInt(cursor.getColumnIndex(projection[i])).apply { setTaskLog(this) })
                            "l" -> contentValues.put(k, cursor.getLong(cursor.getColumnIndex(projection[i])).apply { setTaskLog(this) })
                            "d" -> contentValues.put(k, cursor.getDouble(cursor.getColumnIndex(projection[i])).apply { setTaskLog(this) })
                            "f" -> contentValues.put(k, cursor.getFloat(cursor.getColumnIndex(projection[i])).apply { setTaskLog(this) })
                        }
                    }

                    cr.insert(uri, contentValues)

                } catch (e: Exception) {
                    errors.add("$errorTag: $tableName:$c - ${e.message.toString()}")
                }

                updateFunc(c, taskLog)
                c++

            } while (cursor.moveToNext())

            try { cursor.close() } catch (_: Exception){}
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$errorTag: $tableName - ${e.message.toString()}")
        }

        return errors
    }

}