package org.grassroot.android.activities;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.grassroot.android.R;
import org.grassroot.android.fragments.ImageDetailFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.models.Group;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.CircularImageTransformer;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.ImageUtils;
import org.grassroot.android.utils.NetworkUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;

import static android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

/**
 * Created by paballo on 2016/07/08.
 */
public class GroupAvatarActivity extends PortraitActivity {

    private static final String TAG = GroupAvatarActivity.class.getCanonicalName();
    private static final int IMAGE_RESULT_INT = 25;

    private Group group;

    @BindView(R.id.tlb_gp) Toolbar toolbar;
    @BindView(R.id.gp_avt_main_rl) RelativeLayout rootView;

    @BindView(R.id.iv_gp_avatar) ImageView ivAvatar;
    @BindView(R.id.image_tracker) LinearLayout pagerTracker;
    @BindView(R.id.avatar_pager) ViewPager defaultAvatars;
    @BindView(R.id.image_description) TextView imageDescription;
    private AvatarPagerAdapter defaultAdapter;

    private boolean defaultAvatarsSetUp = false;
    private boolean customImage = false;
    private boolean customImageChanged = false;

    private String compressedFilePath;
    private String mimeType;

    @BindView(R.id.gp_bt_other) Button btOther;
    @BindView(R.id.gp_bt_default) Button btRemove;
    @BindView(R.id.gp_bt_save) Button btSave;

    @BindView(R.id.progressBar) ProgressBar progressBar;

    // nb: these threes arrays must match each other in order
    private static final Integer[] imageResIds = new Integer[] {
            R.drawable.ic_groups_default_avatar,
            R.drawable.ic_group_avatar_hands,
            R.drawable.ic_group_avatar_school,
            R.drawable.ic_group_avatar_money,
            R.drawable.ic_group_avatar_reli
    };

    private static final Integer[] labelResIds = new Integer[] {
            R.string.gp_social_move,
            R.string.gp_hands,
            R.string.gp_school,
            R.string.gp_savings_group,
            R.string.gp_faith_group
    };

    private static final String[] defaultImageSequence = new String[] {
            GroupConstants.SOCIAL_MOVEMENT,
            GroupConstants.COMMUNITY_GROUP,
            GroupConstants.EDUCATION_GROUP,
            GroupConstants.SAVINGS_GROUP,
            GroupConstants.FAITH_GROUP
    };

    public static class AvatarPagerAdapter extends FragmentStatePagerAdapter {

        private final int mSize;

        public AvatarPagerAdapter(FragmentManager fm, int size) {
            super(fm);
            this.mSize = size;
        }

        @Override
        public Fragment getItem(int position) {
            return ImageDetailFragment.newInstance(imageResIds[position]);
        }

        @Override
        public int getCount() {
            return mSize;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_avatar);
        ButterKnife.bind(this);
        Bundle extras = getIntent().getExtras();
        group = extras.getParcelable(GroupConstants.OBJECT_FIELD);
        if (group == null) {
            Log.e(TAG, "Error! This activity requires a group object");
        } else {
            setViews(group);
            if (group.hasCustomImage()) {
                setViewToCustomImage();
                getAvatar(group.getImageUrl(), group.getDefaultImageRes());
            } else {
                setViewToAvatars();
            }
        }
    }

    private void setUpAvatarAdapter() {
        defaultAdapter = new AvatarPagerAdapter(getSupportFragmentManager(), imageResIds.length);
        defaultAvatars.setAdapter(defaultAdapter);
        final int defaultPos = getGroupItem();
        defaultAvatars.setCurrentItem(defaultPos);
        setImageTrackerToPosition(defaultPos);

        defaultAvatars.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setImageTrackerToPosition(position);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        defaultAvatarsSetUp = true;
    }

    private void setViewToAvatars() {
        if (!defaultAvatarsSetUp) {
            setUpAvatarAdapter();
        }
        defaultAvatars.setVisibility(View.VISIBLE);
        imageDescription.setVisibility(View.VISIBLE);
        pagerTracker.setVisibility(View.VISIBLE);
        btOther.setText(R.string.gp_bt_choose_own);
        ivAvatar.setVisibility(View.GONE);
        btRemove.setVisibility(View.GONE);
        customImage = false;
    }

    private void setViewToCustomImage() {
        ivAvatar.setVisibility(View.VISIBLE);
        defaultAvatars.setVisibility(View.GONE);
        pagerTracker.setVisibility(View.GONE);
        imageDescription.setVisibility(View.GONE);
        btOther.setText(R.string.gp_bt_choose_another);
        btRemove.setVisibility(View.VISIBLE);
        customImage = true;
    }

    private void setViews(Group group) {
        setTitle(group.getGroupName());
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btSave.setVisibility(group.canEditGroup() ? View.VISIBLE : View.GONE);
        btOther.setVisibility(group.canEditGroup() ? View.VISIBLE : View.GONE);
    }

    private void getAvatar(final String imageUrl, final int placeholder) {
        Picasso.with(this).load(imageUrl)
                .error(placeholder)
                .transform(new CircularImageTransformer())
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(ivAvatar, new Callback() {
                    @Override
                    public void onSuccess() {
                      Log.e(TAG, "loaded custom image ...");
                    }

                    @Override
                    public void onError() {
                        Snackbar.make(rootView, R.string.gp_image_load_error, Snackbar.LENGTH_LONG);
                        setViewToAvatars();
                    }
                });
    }

    @OnClick({ R.id.gp_bt_other, R.id.iv_gp_avatar })
    public void selectImageForUpload() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, IMAGE_RESULT_INT);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == IMAGE_RESULT_INT && resultCode == RESULT_OK && data != null) {
                // todo : make sure all of this is on a background thread
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String localImagePath = cursor.getString(columnIndex);
                mimeType = ImageUtils.getMimeType(this, selectedImage);
                cursor.close();
                compressedFilePath = ImageUtils.getCompressedFileFromImage(localImagePath);
                Bitmap bitmap = BitmapFactory.decodeFile(compressedFilePath);
                ivAvatar.setImageBitmap(ImageUtils.getRoundedShape(bitmap));
                customImageChanged = true;
                setViewToCustomImage();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @OnClick(R.id.gp_bt_default)
    public void switchToDefault() {
        setViewToAvatars();
    }

    // todo : switch this to Rx and handle offline
    @OnClick(R.id.gp_bt_save)
    public void saveChanges() {
        if (!customImage) {
            final String defaultSelected = defaultImageSequence[defaultAvatars.getCurrentItem()];
            final int defaultSelectedRes = imageResIds[defaultAvatars.getCurrentItem()];
            final boolean defaultImageChanged = defaultSelectedRes != group.getDefaultImageRes();
            if (defaultImageChanged || group.hasCustomImage()) {
                progressBar.setVisibility(View.VISIBLE);
                GroupService.getInstance().changeGroupDefaultImage(group, defaultSelected, defaultSelectedRes, null).
                    subscribe(new Subscriber<String>() {
                        @Override
                        public void onCompleted() { }

                        @Override
                        public void onError(Throwable e) {
                            progressBar.setVisibility(View.GONE);
                            final String errorMsg = ErrorUtils.serverErrorText(e, GroupAvatarActivity.this);
                            Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onNext(String s) {
                            progressBar.setVisibility(View.GONE);
                            finish();
                        }
                    });
            }
        } else if (customImageChanged) {
            progressBar.setVisibility(View.VISIBLE);
            GroupService.getInstance().uploadCustomImage(group.getGroupUid(), compressedFilePath, mimeType, null)
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        if (e instanceof ApiCallException) {
                            ApiCallException apiCallException = (ApiCallException) e;
                            if (NetworkUtils.SERVER_ERROR.equals(apiCallException.errorTag)) {
                                final String errorMsg = ErrorUtils.serverErrorText(apiCallException.errorTag, GroupAvatarActivity.this);
                                Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_LONG).show();
                            } else if (NetworkUtils.CONNECT_ERROR.equals(apiCallException.errorTag)) {
                                // todo : save offline, show a better message, and so forth ..
                                Snackbar.make(rootView, ErrorUtils.serverErrorText(ErrorUtils.GENERIC_ERROR, GroupAvatarActivity.this), Snackbar.LENGTH_SHORT)
                                    .show();
                            }
                        }
                    }

                    @Override
                    public void onNext(String s) {
                        progressBar.setVisibility(View.GONE);
                        finish();
                    }
                });
        }
    }

    private int getGroupItem() {
        for (int i = 0; i < imageResIds.length; i++) {
            if (group.getDefaultImageRes() == imageResIds[i])
                return i;
        }
        return 0;
    }

    private void setImageTrackerToPosition(final int position) {
        imageDescription.setText(labelResIds[position]);
        switchPageTracker(position);
    }

    private void switchPageTracker(final int position) {
        final int trackers = pagerTracker.getChildCount();
        for (int i = 0; i < trackers; i++) {
            pagerTracker.getChildAt(i).setBackgroundResource(i == position ? R.color.primaryColor : R.color.text_grey);
        }
    }

}