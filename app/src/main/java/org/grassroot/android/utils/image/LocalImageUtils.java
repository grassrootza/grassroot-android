package org.grassroot.android.utils.image;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.services.ApplicationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by paballo on 2016/07/18.
 */
public class LocalImageUtils {

    private static final String TAG = LocalImageUtils.class.getSimpleName();

    private static int JPEG_QUALITY = 70;
    private static int MAX_DIMEN = 640;

    public static int convertDefaultImageTypeToResource(final String defaultImage) {
        if (TextUtils.isEmpty(defaultImage)) {
            return R.drawable.ic_groups_default_avatar;
        } else {
            switch (defaultImage) {
                case GroupConstants.SOCIAL_MOVEMENT:
                    return R.drawable.ic_groups_default_avatar;
                case GroupConstants.COMMUNITY_GROUP:
                    return R.drawable.ic_group_avatar_hands;
                case GroupConstants.EDUCATION_GROUP:
                    return R.drawable.ic_group_avatar_school;
                case GroupConstants.FAITH_GROUP:
                    return R.drawable.ic_group_avatar_reli;
                case GroupConstants.SAVINGS_GROUP:
                    return R.drawable.ic_group_avatar_money;
                default:
                    return R.drawable.ic_groups_default_avatar;
            }
        }
    }

    public static void setAvatarImage(final ImageView image, final String imageUrl, final int fallBackRes) {
        // at some point soon should revisit and optimize this
        Picasso.with(ApplicationLoader.applicationContext)
            .load(imageUrl)
            .error(fallBackRes)
            .placeholder(fallBackRes)
            .networkPolicy(NetworkPolicy.OFFLINE)
            .transform(new CircularImageTransformer())
            .into(image, new Callback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onError() {
                    Picasso.with(ApplicationLoader.applicationContext).load(imageUrl)
                        .placeholder(fallBackRes)
                        .transform(new CircularImageTransformer())
                        .error(fallBackRes)
                        .into(image);
                }
            });
    }

    public static MultipartBody.Part getImageFromPath(final String path, final String mimeType) {
        try {
            final File file = new File(path);
            Log.d(TAG, "file size : " + (file.length() / 1024));
            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
            return MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getLocalFileNameFromURI(final Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = ApplicationLoader.applicationContext.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        cursor.moveToFirst(); // if null, will throw error to subscriber, so check in here would be redundant
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String localImagePath = cursor.getString(columnIndex);
        cursor.close();
        return localImagePath;
    }

    public static File createImageFileForCamera() throws IOException {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String imageFileName = "GRASSROOT_" + timeStamp + "_";
        File storageDir = ApplicationLoader.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    public static long getImageFileSize(final String path) {
        File f = new File(path);
        return f.exists() ? f.length() : 0;
    }

    public static void addImageToGallery(String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        Log.e(TAG, "adding photo to gallery ...");
        ApplicationLoader.applicationContext.sendBroadcast(mediaScanIntent);
    }

    public static String getCompressedFileFromImage(String path, boolean cropToSquare) {
        String strMyImagePath = null;
        Bitmap scaledBitmap;

        try {

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = calculateInSampleSize(options);
            options.inJustDecodeBounds = false;
            Bitmap unscaledBitmap = BitmapFactory.decodeFile(path, options);

            scaledBitmap = cropToSquare ? cropScaleCenter(unscaledBitmap) : halveImage(unscaledBitmap);

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

    public static Bitmap halveImage(Bitmap unscaledBitmap) {
        final int newHeight = unscaledBitmap.getHeight() / 2;
        final int newWidth = unscaledBitmap.getWidth() / 2;
        return Bitmap.createScaledBitmap(unscaledBitmap, newWidth, newHeight, true);
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

    public static String getMimeType(Uri uri) {
        String mimeType = null;
        String extension;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(ApplicationLoader.applicationContext
                .getContentResolver().getType(uri));
        } else {
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }
        switch (extension) {
            case "jpg":
            case "jpeg":
                mimeType = "image/jpeg";
                break;
            case "png":
                mimeType = "image/png";
                break;
            default:
                break;
        }

        return mimeType;
    }

}
