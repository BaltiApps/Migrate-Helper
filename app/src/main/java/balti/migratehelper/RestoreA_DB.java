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
                                case "d":
                                    contentValues.put(mirror[i][0], cursor.getDouble(cursor.getColumnIndex(projection[i])));
                                    break;
                                case "f":
                                    contentValues.put(mirror[i][0], cursor.getFloat(cursor.getColumnIndex(projection[i])));
                                    break;
                            }
                        }
                            /*contentValues.put(Telephony.Sms.ADDRESS, cursor.getString(cursor.getColumnIndex("smsAddress")));
                            contentValues.put(Telephony.Sms.BODY, cursor.getString(cursor.getColumnIndex("smsBody")));
                            contentValues.put(Telephony.Sms.TYPE, cursor.getString(cursor.getColumnIndex("smsType")));
                            contentValues.put(Telephony.Sms.DATE, cursor.getString(cursor.getColumnIndex("smsDate")));
                            contentValues.put(Telephony.Sms.DATE_SENT, cursor.getString(cursor.getColumnIndex("smsDateSent")));
                            contentValues.put(Telephony.Sms.CREATOR, cursor.getString(cursor.getColumnIndex("smsCreator")));
                            contentValues.put(Telephony.Sms.PERSON, cursor.getString(cursor.getColumnIndex("smsPerson")));
                            contentValues.put(Telephony.Sms.PROTOCOL, cursor.getString(cursor.getColumnIndex("smsProtocol")));
                            contentValues.put(Telephony.Sms.SEEN, cursor.getString(cursor.getColumnIndex("smsSeen")));
                            contentValues.put(Telephony.Sms.SERVICE_CENTER, cursor.getString(cursor.getColumnIndex("smsServiceCenter")));
                            contentValues.put(Telephony.Sms.STATUS, cursor.getString(cursor.getColumnIndex("smsStatus")));
                            contentValues.put(Telephony.Sms.SUBJECT, cursor.getString(cursor.getColumnIndex("smsSubject")));

                            contentValues.put(Telephony.Sms.ERROR_CODE, cursor.getInt(cursor.getColumnIndex("smsError")));
                            contentValues.put(Telephony.Sms.READ, cursor.getInt(cursor.getColumnIndex("smsRead")));
                            contentValues.put(Telephony.Sms.LOCKED, cursor.getInt(cursor.getColumnIndex("smsLocked")));
                            contentValues.put(Telephony.Sms.REPLY_PATH_PRESENT, cursor.getInt(cursor.getColumnIndex("smsReplyPathPresent")));*/

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
        classContext.onDBRestoreComplete(code);
        statusText.setText(R.string.done);
    }
}