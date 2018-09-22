package balti.migratehelper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.List;

class RestoreA_DB extends AsyncTask {

    private List<File> dbFiles;
    private String projection[];
    private String mirror[][];
    private ProgressBar progressBar;
    private String tableName;
    private Uri uri;
    Context context;
    private int code;
    TextView statusText;

    OnDBRestoreComplete classContext;

    public RestoreA_DB(List<File> dbFiles, String[] projection, String[][] mirror, ProgressBar progressBar, TextView statusText, String tableName, Uri restoreUri, Context context, int code) {
        this.dbFiles = dbFiles;
        this.projection = projection;
        this.mirror = mirror;
        this.progressBar = progressBar;
        this.statusText = statusText;
        this.tableName = tableName;
        uri = restoreUri;
        this.context = context;
        this.code = code;

        classContext = (OnDBRestoreComplete)context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(false);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("--");
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        for (int j = 0; j < dbFiles.size(); j++) {

            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFiles.get(j), null);

            Cursor cursor = db.query(tableName, projection, null, null, null, null, null);

            ContentResolver contentResolver = context.getContentResolver();

            if (cursor == null || cursor.getCount() <= 0) {
                continue;
            } else {

                progressBar.setMax(cursor.getCount());

                int c = 0;
                progressBar.setProgress(c);

                cursor.moveToFirst();

                do {

                    try {
                        ContentValues contentValues = new ContentValues();

                        for (int i = 0; i < mirror.length; i++){
                            switch (mirror[i][1]) {
                                case "s":
                                    contentValues.put(mirror[i][0], cursor.getString(cursor.getColumnIndex(projection[i])));
                                    break;
                                case "i":
                                    contentValues.put(mirror[i][0], cursor.getInt(cursor.getColumnIndex(projection[i])));
                                    break;
                                case "l":
                                    contentValues.put(mirror[i][0], cursor.getLong(cursor.getColumnIndex(projection[i])));
                                    break;
                                case "d":
                                    contentValues.put(mirror[i][0], cursor.getDouble(cursor.getColumnIndex(projection[i])));
                                    break;
                                case "f":
                                    contentValues.put(mirror[i][0], cursor.getFloat(cursor.getColumnIndex(projection[i])));
                                    break;
                            }
                        }

                        contentResolver.insert(uri, contentValues);

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        publishProgress(++c);
                    }

                } while (cursor.moveToNext());

            }

            try {
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
        progressBar.setProgress((int)values[0]);
        statusText.setText(progressBar.getProgress() + "/" + progressBar.getMax());
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        statusText.setText(R.string.done);
        classContext.onDBRestoreComplete(code);
    }
}