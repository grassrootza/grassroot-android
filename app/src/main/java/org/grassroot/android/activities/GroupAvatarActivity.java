package org.grassroot.android.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.grassroot.android.R;
import org.grassroot.android.events.GroupPictureChangedEvent;
import org.grassroot.android.fragments.dialogs.ConfirmCancelDialogFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.GenericResponse;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.CircularImageTransformer;
import org.grassroot.android.utils.PreferenceUtils;
import org.grassroot.android.utils.ScalingUtilities;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by paballo on 2016/07/08.
 */
public class GroupAvatarActivity extends PortraitActivity {

    private static final String TAG = GroupAvatarActivity.class.getCanonicalName();
    private static final int IMAGE_RESULT_INT = 25;
    private Group group;

    @BindView(R.id.tlb_gp)
    Toolbar toolbar;

    @BindView(R.id.gp_avt_main_rl)
    RelativeLayout g_avt_relative;

    @BindView(R.id.iv_gp_avatar)
    ImageView ivAvatar;

    @BindView(R.id.bt_gp_set)
    Button btAvatar;

    @BindView(R.id.bt_gp_remove)
    Button btAvatarRemove;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_avatar);
        ButterKnife.bind(this);
        Bundle extras = getIntent().getExtras();
        group = extras.getParcelable(GroupConstants.OBJECT_FIELD);
        setViews(group);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == IMAGE_RESULT_INT && resultCode == RESULT_OK
                    && null != data) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String localImagePath = cursor.getString(columnIndex);
                String mimeType = getMimeType(this, selectedImage);
                uploadFile(localImagePath, mimeType);
                cursor.close();
                String compressedFilePath = decodeFile(localImagePath);
                Bitmap bitmap = BitmapFactory.decodeFile(compressedFilePath);
                ivAvatar.setImageBitmap(ScalingUtilities.getRoundedShape(bitmap));
                if (btAvatarRemove.getVisibility() == View.INVISIBLE ||
                        btAvatarRemove.getVisibility() == View.GONE)
                    btAvatarRemove.setVisibility(View.VISIBLE);

            } else {

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void setViews(Group group) {
        setTitle(getString(R.string.gp_txt_title));
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        String imageUrl = group.getImageUrl();
        if (group.canEditGroup()) {
            btAvatar.setVisibility(View.VISIBLE);
            String buttonText = imageUrl == null ? getString(R.string.gp_bt_txt_set) : getString(R.string.gp_bt_txt_update);
            btAvatar.setText(buttonText);
            if (imageUrl != null)
                btAvatarRemove.setVisibility(View.VISIBLE);
        }
        if (imageUrl != null) {
            getAvatar(imageUrl);
        }

    }

    private void getAvatar(final String imageUrl) {
        Picasso.with(this).load(imageUrl)
                .error(R.drawable.ic_profile_image)
                .transform(new CircularImageTransformer())
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(ivAvatar, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                        Picasso.with(GroupAvatarActivity.this).
                                load(imageUrl)
                                .transform(new CircularImageTransformer())
                                .error(R.drawable.ic_profile_image)
                                .into(ivAvatar);
                    }
                });
    }


    @OnClick(R.id.bt_gp_set)
    public void selectImageForUpload() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, IMAGE_RESULT_INT);

    }

    @OnClick(R.id.bt_gp_remove)
    public void removePicture() {

        final String phoneNumber = PreferenceUtils.getPhoneNumber();
        final String code = PreferenceUtils.getAuthToken();
        final String groupUid = group.getGroupUid();
        ConfirmCancelDialogFragment dialogFragment = ConfirmCancelDialogFragment.newInstance(getString(R.string.gp_dlg_cnfrm), new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                GrassrootRestService.getInstance().getApi().removeImage(phoneNumber, code, groupUid).enqueue(new retrofit2.Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            ivAvatar.setImageResource(R.drawable.ic_profile_image);
                            btAvatarRemove.setVisibility(View.GONE);
                            btAvatar.setText(R.string.gp_bt_txt_set);
                            Snackbar.make(g_avt_relative, R.string.gp_remove_success, Snackbar.LENGTH_LONG).show();
                            EventBus.getDefault().post(new GroupPictureChangedEvent());
                        } else {
                            Snackbar.make(g_avt_relative, R.string.gp_remove_failure, Snackbar.LENGTH_LONG).show();

                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Snackbar.make(g_avt_relative, R.string.gp_remove_failure, Snackbar.LENGTH_LONG).show();
                    }
                });

            }
        });
        dialogFragment.show(getSupportFragmentManager(), "");


    }


    private void uploadFile(final String path, final String mimeType) {

        File file = new File(path);
        RequestBody requestFile =
                RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody.Part image =
                MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        String phoneNumber = PreferenceUtils.getPhoneNumber();
        String code = PreferenceUtils.getAuthToken();
        String groupUid = group.getGroupUid();
        progressBar.setVisibility(View.VISIBLE);
        GrassrootRestService.getInstance().getApi().uploadImage(phoneNumber, code, groupUid, image).enqueue(new retrofit2.Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(g_avt_relative, R.string.gp_update_success, Snackbar.LENGTH_LONG).show();
                EventBus.getDefault().post(new GroupPictureChangedEvent());
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(g_avt_relative, R.string.gp_update_failure, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    public static String getMimeType(Context context, Uri uri) {
        String mimeType = null;
        String extension;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(context.getContentResolver().getType(uri));
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

    private String decodeFile(String path) {
        String strMyImagePath = null;
        Bitmap scaledBitmap;

        try {
            Bitmap unscaledBitmap = ScalingUtilities.decodeFile(path, 192, 192, ScalingUtilities.ScalingLogic.FIT);
            if (!(unscaledBitmap.getWidth() <= 640 && unscaledBitmap.getHeight() <= 640)) {
                scaledBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, 192, 192, ScalingUtilities.ScalingLogic.FIT);

            } else {
                unscaledBitmap.recycle();
                return path;
            }
            String extr = Environment.getExternalStorageDirectory().toString();
            File mFolder = new File(extr + "/myTmpDir");
            if (!mFolder.exists()) {
                mFolder.mkdir();
            }
            String s = "tmp.png";
            File file = new File(mFolder.getAbsolutePath(), s);
            strMyImagePath = file.getAbsolutePath();
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 70, fos);
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

}