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

    private static final String TAG = ImageUtils.class.getSimpleName();

    public static int JPEG_QUALITY = 70;
    public static int MAX_DIMEN = 640;

    public static String getCompressedFileFromImage(String path) {
        String strMyImagePath = null;
        Bitmap scaledBitmap;

        try {

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = calculateInSampleSize(options);
            options.inJustDecodeBounds = false;
            Bitmap unscaledBitmap = BitmapFactory.decodeFile(path, options);

            scaledBitmap= cropScaleCenter(unscaledBitmap);

            String extr = Environment.getExternalStorageDirectory().toString();
            File mFolder = new File(extr + "/myTmpDir");

            if (!mFolder.exists()) {
                mFolder.mkdir();
            }

            String s = "tmp.jpg";
            File file = new File(mFolder.getAbsolutePath(), s);

            strMyImagePath = file.getAbsolutePath();
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file);
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
                fos.flush();
                fos.close();
                Log.e(TAG, "scaled and compressed bitmap, size = " + scaledBitmap.getHeight() + ", " +
                        "file size = " + (file.length() / 1024));
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

    public static Bitmap cropScaleCenter(Bitmap unscaledBitmap) {
        // first, crop it to a central square
        Log.e(TAG, "cropping and scaling image ...");
        Bitmap destBitmap;
        final int height = unscaledBitmap.getHeight();
        final int width = unscaledBitmap.getWidth();
        if (width > height) {
            destBitmap = Bitmap.createBitmap(unscaledBitmap,
                    width / 2 - height / 2,
                    0,
                    height,
                    height);
        } else {
            destBitmap = Bitmap.createBitmap(unscaledBitmap,
                    0,
                    height / 2 - width /2,
                    width,
                    width);
        }

        // next, if the square is bigger than 640x640, scale it
        final int squaredSide = Math.max(height, width);
        if (squaredSide > MAX_DIMEN) {
            destBitmap = Bitmap.createScaledBitmap(destBitmap, MAX_DIMEN, MAX_DIMEN, true);
        }

        return destBitmap;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int sampleSize = 1;

        if (height > MAX_DIMEN || width > MAX_DIMEN) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / sampleSize) >= MAX_DIMEN && (halfWidth / sampleSize >= MAX_DIMEN)) {
                sampleSize *= 2;
            }
        }

        return sampleSize;

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
