package org.grassroot.android.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import org.grassroot.android.utils.ImageUtils;
import org.grassroot.android.utils.RealmUtils;
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
        if (group == null) {

        }
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
                cursor.close();
                String compressedFilePath = ImageUtils.decodeFile(localImagePath);
                uploadFile(compressedFilePath, mimeType);
                Bitmap bitmap = BitmapFactory.decodeFile(compressedFilePath);
                ivAvatar.setImageBitmap(ImageUtils.getRoundedShape(bitmap));
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

        }
        if (imageUrl != null) {
            getAvatar(imageUrl);
        }

    }

    private void getAvatar(final String imageUrl) {
        Picasso.with(this).load(imageUrl)
                .placeholder(R.drawable.ic_groups_default_avatar)
                .error(R.drawable.ic_groups_default_avatar)
                .transform(new CircularImageTransformer())
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(ivAvatar, new Callback() {
                    @Override
                    public void onSuccess() {
                      showRemoveButton();
                    }

                    @Override
                    public void onError() {
                        Picasso.with(GroupAvatarActivity.this).
                                load(imageUrl)
                                .placeholder(R.drawable.ic_groups_default_avatar)
                                .transform(new CircularImageTransformer())
                                .error(R.drawable.ic_groups_default_avatar)
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

        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();
        final String groupUid = group.getGroupUid();
        ConfirmCancelDialogFragment dialogFragment = ConfirmCancelDialogFragment.newInstance(getString(R.string.gp_dlg_cnfrm), new ConfirmCancelDialogFragment.ConfirmDialogListener() {
            @Override
            public void doConfirmClicked() {
                GrassrootRestService.getInstance().getApi().removeImage(phoneNumber, code, groupUid).enqueue(new retrofit2.Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful()) {
                            hideRemoveButton();
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
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String code = RealmUtils.loadPreferencesFromDB().getToken();
        String groupUid = group.getGroupUid();
        progressBar.setVisibility(View.VISIBLE);
        GrassrootRestService.getInstance().getApi().uploadImage(phoneNumber, code, groupUid, image).enqueue(new retrofit2.Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                progressBar.setVisibility(View.GONE);
                showRemoveButton();
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




    private void hideRemoveButton(){
        ivAvatar.setImageResource(R.drawable.ic_groups_default_avatar);
        btAvatar.setText(R.string.gp_bt_txt_set);
        btAvatarRemove.setVisibility(View.GONE);

    }

    private void showRemoveButton(){
        if (btAvatarRemove.getVisibility() == View.INVISIBLE ||
                btAvatarRemove.getVisibility() == View.GONE)
            btAvatarRemove.setVisibility(View.VISIBLE);

    }

}