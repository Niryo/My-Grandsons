package niry.mygrandsons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Save image in the background
 */
public class SaveImageTask extends SaveFileAsync {
    private final Context context;

    public SaveImageTask(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected Void doInBackground(String... urls) {
        String url = urls[0];
        Bitmap bitmap = null;
        try {
            InputStream in = new java.net.URL(url).openStream();
            bitmap = BitmapFactory.decodeStream(in);
            //the name of the file will be the current time:
         File fileToSave= allocateFileName(".jpg");
            FileOutputStream out = new FileOutputStream(fileToSave);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

            sendBroadcastIncomingMessage();
            sendNotification();


        } catch (Exception e) {
            saveUrlForLateDownload(url);
        }

        return null;
    }
}
