package org.grassroot.android.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.grassroot.android.R;
import org.grassroot.android.fragments.AvatarViewFragment;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Group;
import org.grassroot.android.models.exceptions.ApiCallException;
import org.grassroot.android.services.GroupService;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.image.CircularImageTransformer;
import org.grassroot.android.utils.image.LocalImageUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

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
    @BindView(R.id.avatar_left_arrow) ImageButton leftArrow;
    @BindView(R.id.avatar_right_arrow) ImageButton rightArrow;

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
            return AvatarViewFragment.newInstance(imageResIds[position]);
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
        AvatarPagerAdapter defaultAdapter = new AvatarPagerAdapter(getSupportFragmentManager(), imageResIds.length);
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

    @OnClick(R.id.avatar_right_arrow)
    public void onClickAvatarRight() {
        defaultAvatars.setCurrentItem(defaultAvatars.getCurrentItem() + 1, true);
    }

    @OnClick(R.id.avatar_left_arrow)
    public void onClickAvaterLeft() {
        defaultAvatars.setCurrentItem(defaultAvatars.getCurrentItem() - 1, true);
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
        leftArrow.setVisibility(View.INVISIBLE);
        leftArrow.setClickable(false);
        rightArrow.setVisibility(View.INVISIBLE);
        rightArrow.setClickable(false);

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
                        Snackbar.make(rootView, R.string.gp_image_load_error, Snackbar.LENGTH_SHORT).show();
                        setViewToAvatars();
                    }
                });
    }

    @OnClick({ R.id.gp_bt_other, R.id.iv_gp_avatar })
    public void selectImageForUpload() {
        if (NetworkUtils.isOnline()) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, EXTERNAL_CONTENT_URI);
            if (ErrorUtils.isCallable(galleryIntent)) {
                startActivityForResult(galleryIntent, IMAGE_RESULT_INT);
            } else {
                galleryIntent = new Intent(Intent.ACTION_GET_CONTENT, EXTERNAL_CONTENT_URI);
                if (ErrorUtils.isCallable(galleryIntent)) {
                    startActivityForResult(galleryIntent, IMAGE_RESULT_INT);
                } else {
                    Snackbar.make(rootView, R.string.local_error_pick_activity, Snackbar.LENGTH_SHORT).show();
                }
            }
        } else {
            checkForOfflineFirst();
        }
    }

    private void checkForOfflineFirst() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gp_offline_alert_message)
            .setNegativeButton(R.string.gp_alert_later, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .setPositiveButton(R.string.gp_alert_online, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressBar.setVisibility(View.VISIBLE);
                    NetworkUtils.trySwitchToOnline(GroupAvatarActivity.this, false, AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) {
                                progressBar.setVisibility(View.GONE);
                                selectImageForUpload();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                Log.e(TAG, "error!");
                                NetworkUtils.setOfflineSelected(); // i.e., resetting
                                Snackbar.make(rootView, R.string.gp_offline_connect_fail,
                                    Snackbar.LENGTH_LONG).show();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                }
            })
            .setCancelable(true)
            .create()
            .show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_RESULT_INT && resultCode == RESULT_OK && data != null) {
            cleanUpCompressedFileIfExists();
            progressBar.setVisibility(View.VISIBLE);
            Uri selectedImage = data.getData();
            compressBitmap(selectedImage).subscribe(new Consumer<Bitmap>() {
                @Override
                public void accept(Bitmap bitmap) {
                    ivAvatar.setImageBitmap(LocalImageUtils.getRoundedShape(bitmap));
                    progressBar.setVisibility(View.GONE);
                    customImageChanged = true;
                    setViewToCustomImage();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    throwable.printStackTrace();
                    Snackbar.make(rootView, R.string.process_error_image, Snackbar.LENGTH_SHORT);
                }
            });
        }
    }

    private Observable<Bitmap> compressBitmap(final Uri selectedImage) {
        return Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(ObservableEmitter<Bitmap> subscriber) {
                mimeType = LocalImageUtils.getMimeType(selectedImage);
                final String localImagePath = LocalImageUtils.getLocalFileNameFromURI(selectedImage);
                compressedFilePath = LocalImageUtils.getCompressedFileFromImage(localImagePath, true);
                Bitmap bitmap = BitmapFactory.decodeFile(compressedFilePath);
                subscriber.onNext(bitmap);
                subscriber.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @OnClick(R.id.gp_bt_default)
    public void switchToDefault() {
        setViewToAvatars();
    }

    @OnClick(R.id.gp_bt_save)
    public void saveChanges() {
        if (!customImage) {
            uploadDefaultImageChoice();
        } else if (customImageChanged) {
            sendCustomImageToServer();
        }
    }

    private void uploadDefaultImageChoice() {
        final String defaultSelected = defaultImageSequence[defaultAvatars.getCurrentItem()];
        final int defaultSelectedRes = imageResIds[defaultAvatars.getCurrentItem()];
        final boolean defaultImageChanged = defaultSelectedRes != group.getDefaultImageRes();
        if (defaultImageChanged || group.hasCustomImage()) {
            progressBar.setVisibility(View.VISIBLE);
            GroupService.getInstance().changeGroupDefaultImage(group, defaultSelected, defaultSelectedRes, null).
                subscribe(new Observer<String>() {
                    @Override public void onSubscribe(Disposable d) { }

                    @Override
                    public void onError(Throwable e) {
                        progressBar.setVisibility(View.GONE);
                        if (e instanceof ApiCallException) {
                            if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                                handleNetworkErrorDefaultImage();
                            } else if (NetworkUtils.SERVER_ERROR.equals(e.getMessage())) {
                                Snackbar.make(rootView, ((ApiCallException) e).errorTag, Snackbar.LENGTH_LONG).show();
                            } else {
                                Snackbar.make(rootView, R.string.error_generic, Snackbar.LENGTH_SHORT).show();
                            }
                        } else {
                            Snackbar.make(rootView, R.string. error_generic, Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onNext(String s) {
                        progressBar.setVisibility(View.GONE);
                        finish();
                    }

                    @Override
                    public void onComplete() { }
                });
        }
    }

    private void handleNetworkErrorDefaultImage() {
        Snackbar snackbar = Snackbar.make(rootView, R.string.connect_error_group_default, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(Color.RED);
        snackbar.setAction(R.string.bt_done, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        snackbar.show();
    }

    private void sendCustomImageToServer() {
        progressBar.setVisibility(View.VISIBLE);
        GroupService.getInstance().uploadCustomImage(group.getGroupUid(), compressedFilePath, mimeType, null)
            .subscribe(new SingleObserver<String>() {
                @Override public void onSubscribe(Disposable d) {}

                @Override
                public void onError(Throwable e) {
                    progressBar.setVisibility(View.GONE);
                    if (e instanceof ApiCallException) {
                        if (NetworkUtils.SERVER_ERROR.equals(e.getMessage())) {
                            final String errorMsg = ErrorUtils.serverErrorText(((ApiCallException) e).errorTag);
                            Snackbar.make(rootView, errorMsg, Snackbar.LENGTH_LONG).show();
                        } else if (NetworkUtils.CONNECT_ERROR.equals(e.getMessage())) {
                            handleConnectionErrorOnUpload();
                        }
                    }
                }

                @Override
                public void onSuccess(String s) {
                    progressBar.setVisibility(View.GONE);
                    finish();
                }
            });
    }

    private void handleConnectionErrorOnUpload() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.gp_tried_upload_connect_fail)
            .setPositiveButton(R.string.gp_alert_online, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressBar.setVisibility(View.VISIBLE);
                    NetworkUtils.trySwitchToOnline(GroupAvatarActivity.this, false, null)
                        .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            Log.e(TAG, "we are online ... send image to server");
                            sendCustomImageToServer();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            handleConnectionErrorOnUpload();
                        }
                    });
                }
            })
            .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .create()
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanUpCompressedFileIfExists();
    }

    private void cleanUpCompressedFileIfExists() {
        Log.e(TAG, "cleaning up compressed image, if it still exists ...");
        if (!TextUtils.isEmpty(compressedFilePath)) {
            try {
                final File file = new File(compressedFilePath);
                if (file.exists()) {
                    Log.e(TAG, "image still exists ... deleting it");
                    file.delete();
                }
            } catch (Exception e) { // just to guard against exceptions (don't fully trust exists on Android ...
                e.printStackTrace();
            }
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
        leftArrow.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
        leftArrow.setClickable(position > 0);
        rightArrow.setVisibility(position < (defaultImageSequence.length - 1) ? View.VISIBLE : View.INVISIBLE);
        rightArrow.setClickable(position < defaultImageSequence.length);
    }

    private void switchPageTracker(final int position) {
        final int trackers = pagerTracker.getChildCount();
        for (int i = 0; i < trackers; i++) {
            pagerTracker.getChildAt(i).setBackgroundResource(i == position ? R.color.primaryColor : R.color.text_grey);
        }
    }

}