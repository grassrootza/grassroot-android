package org.grassroot.android.activities;

import android.os.Bundle;
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

import rx.functions.Action1;

/**
 * Created by luke on 2017/03/14.
 */

public class ImageDisplayActivity extends AppCompatActivity {

    private static final String TAG = ImageDisplayActivity.class.getSimpleName();

    private String taskType;
    private String taskUid;

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

        setUpToolbar();

        ImageGridFragment fragment = ImageGridFragment.newInstance(taskType, taskUid, new Action1<String>() {
            @Override
            public void call(String s) {
                displayFullImage(s);
            }
        });

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.ida_fragment_holder, fragment, ImageGridFragment.class.getCanonicalName())
                .commit();
    }

    private void displayFullImage(String imageKey) {
        Log.e(TAG, "displaying full image for key: " + imageKey);
        PhotoViewFragment photoFragment = PhotoViewFragment.newInstance(taskType, imageKey);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.ida_fragment_holder, photoFragment, PhotoViewFragment.class.getCanonicalName())
                .addToBackStack("photo")
                .commit();
        isImageFullScreen = true;
        setToolbarToPhoto();
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
                    getSupportFragmentManager().popBackStack();
                    isImageFullScreen = false;
                    setToolbarToGrid();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
