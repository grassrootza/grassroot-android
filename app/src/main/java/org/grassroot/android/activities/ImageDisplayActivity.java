package org.grassroot.android.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.fragments.ImageGridFragment;
import org.grassroot.android.fragments.PhotoViewFragment;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.ImageRecord;
import org.grassroot.android.utils.RealmUtils;

import rx.functions.Action1;

/**
 * Created by luke on 2017/03/14.
 */

public class ImageDisplayActivity extends AppCompatActivity {

    private static final String TAG = ImageDisplayActivity.class.getSimpleName();

    public static final String OPEN_ON_IMAGE = "open_on_image";
    private static final String FULL_IMAGE_TAG = "view_full_image_fragment";
    private static final String IMAGE_GRID_TAG = "task_image_grid_fragment";

    private String taskType;
    private String taskUid;

    private Action1<ImageRecord> imageClickObserver;
    private Action1<ImageRecord> imageDeletionObserver;

    private PhotoViewFragment photoViewFragment;
    private ImageGridFragment imageGridFragment;

    private boolean isImageFullScreen;

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);
        toolbar = (Toolbar) findViewById(R.id.image_display_toolbar);

        if (getIntent().getExtras() == null) {
            Toast.makeText(this, R.string.ida_toast_error_exit, Toast.LENGTH_SHORT).show();
            finish();
        }

        taskUid =  getIntent().getStringExtra(TaskConstants.TASK_UID_FIELD);
        taskType = getIntent().getStringExtra(TaskConstants.TASK_TYPE_FIELD);

        boolean openOnImageForEdit = getIntent().hasExtra(OPEN_ON_IMAGE);

        setUpToolbar();

        FragmentManager fm = getSupportFragmentManager();
        imageGridFragment = (ImageGridFragment) fm.findFragmentByTag(IMAGE_GRID_TAG);

        imageDeletionObserver = new Action1<ImageRecord>() {
            @Override
            public void call(ImageRecord imageRecord) {
                deleteImage(imageRecord);
            }
        };
        imageClickObserver = new Action1<ImageRecord>() {
            @Override
            public void call(ImageRecord record) {
                photoViewFragment = PhotoViewFragment.newInstance(taskType, record, imageDeletionObserver, false);
                displayFullImageFragment();
            }
        };

        if (imageGridFragment == null) {
            imageGridFragment = ImageGridFragment.newInstance(taskType, taskUid, imageClickObserver);
        } else {
            imageGridFragment.setImageClickObserver(imageClickObserver);
        }

        if (!imageGridFragment.isAdded()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.ida_fragment_holder, imageGridFragment, IMAGE_GRID_TAG)
                    .commit();
        }

        photoViewFragment = (PhotoViewFragment) fm.findFragmentByTag(FULL_IMAGE_TAG);
        if (photoViewFragment != null) {
            displayFullImageFragment(); // photo fragment is non null, so must have been created before, hence display it
        } else if (openOnImageForEdit) {
            ImageRecord record = getIntent().getParcelableExtra(OPEN_ON_IMAGE);
            photoViewFragment = PhotoViewFragment.newInstance(taskType, record, imageDeletionObserver, true);
            displayFullImageFragment();
        }
    }

    private void displayFullImageFragment() {
        if (photoViewFragment != null && !photoViewFragment.isAdded()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.ida_fragment_holder, photoViewFragment, FULL_IMAGE_TAG)
                    .addToBackStack("photo")
                    .commit();
        } else if (photoViewFragment != null && !photoViewFragment.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .show(photoViewFragment)
                    .addToBackStack("photo") // docs aren't clear if showing counts as a transaction ... ugh, Android
                    .commit();
        }
        isImageFullScreen = true;
        setToolbarToPhoto();
    }

    private void deleteImage(ImageRecord record) {
        closeFullScreen();
        imageGridFragment.removeImage(record);
        RealmUtils.removeObjectFromDatabase(ImageRecord.class, "key", record.getKey());
    }

    private void setUpToolbar() {
        setTitle(getString(R.string.ida_title, getTitlePrefix()));
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationIcon(R.drawable.btn_close_white);
    }

    private void setToolbarToPhoto() {
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        toolbar.setNavigationIcon(R.drawable.btn_back_wt);
    }

    private void setToolbarToGrid() {
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor));
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        toolbar.setNavigationIcon(R.drawable.btn_close_white);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!isImageFullScreen) {
                    finish();
                } else {
                    closeFullScreen();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void closeFullScreen() {
        getSupportFragmentManager().popBackStack();
        isImageFullScreen = false;
        setToolbarToGrid();
        Log.e(TAG, "after closing, is it null? ... " + (getSupportFragmentManager().findFragmentByTag(FULL_IMAGE_TAG) == null));
    }

    private String getTitlePrefix() {
        switch (taskType) {
            case TaskConstants.TODO:
                return "Todo";
            case TaskConstants.MEETING:
                return "Meeting";
            default:
                return "Photo";
        }
    }

}
