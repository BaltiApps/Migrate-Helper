package balti.migratehelper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

public class SetAppIcon extends AsyncTask<String, Void, Bitmap> {

    ImageView iconHolder;

    SetAppIcon(ImageView iconHolder){
        this.iconHolder = iconHolder;
    }


    @Override
    protected Bitmap doInBackground(String... icon) {

        Bitmap bmp = null;
        String[] bytes = icon[0].split("_");

        try {
            byte imageData[] = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                imageData[i] = Byte.parseByte(bytes[i]);
            }
            bmp = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            //Log.d("migrate", "icon: " + bmp);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return bmp;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if (bitmap != null) {
            iconHolder.setImageBitmap(bitmap);
        }
    }
}
