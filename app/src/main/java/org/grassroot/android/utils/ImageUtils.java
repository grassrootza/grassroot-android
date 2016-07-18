package org.grassroot.android.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by paballo on 2016/07/18.
 */
public class ImageUtils {



    public static String decodeFile(String path) {
        String strMyImagePath = null;
        Bitmap scaledBitmap;

        try {

            Bitmap unscaledBitmap = BitmapFactory.decodeFile(path);
            if ((unscaledBitmap.getWidth() >= 640 && unscaledBitmap.getHeight() >= 640)) {
                int h=unscaledBitmap.getHeight();
                int w=unscaledBitmap.getWidth();
                h=h/4;
                w=w/4;

                scaledBitmap=Bitmap.createScaledBitmap(unscaledBitmap, w,h , true);
            } else {
                unscaledBitmap.recycle();
                return path;
            }

            String extr = Environment.getExternalStorageDirectory().toString();
            File mFolder = new File(extr + "/myTmpDir");
            if (!mFolder.exists()) {
                mFolder.mkdir();
            }
            String s = "tmp.jpg";
            File file = new File(mFolder.getAbsolutePath(), s);
            strMyImagePath = file.getAbsolutePath();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {

                e.printStackTrace();
            }
            scaledBitmap.recycle();
        } catch (Throwable e) {
        }

        if (strMyImagePath == null) {
            return path;
        }
        return strMyImagePath;

    }


    public static Bitmap getRoundedShape(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap,
                BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);

        squaredBitmap.recycle();
        return bitmap;
    }
}
