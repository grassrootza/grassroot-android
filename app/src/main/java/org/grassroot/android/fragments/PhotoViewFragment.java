package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;

/**
 * Created by luke on 2017/03/14.
 */

public class PhotoViewFragment extends Fragment {

    private static final String TAG  = PhotoViewFragment.class.getSimpleName();

    private String imageKey;
    private ImageView mainView;
    private ProgressDialog progressDialog;

    public static PhotoViewFragment newInstance(final String taskType, final String imageKey) {
        PhotoViewFragment fragment = new PhotoViewFragment();
        Bundle args = new Bundle();
        args.putString(TaskConstants.TASK_TYPE_FIELD, taskType);
        args.putString("KEY", imageKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            throw new UnsupportedOperationException("Error! Photo view fragment needs an image key");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View v = inflater.inflate(R.layout.fragment_view_photo, container, false);
        mainView = (ImageView) v.findViewById(R.id.photo_main_view);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.wait_message)); // todo : make it say loading or something
        progressDialog.show();

        loadTaskPhoto(getArguments());

        return v;
    }

    private void loadTaskPhoto(Bundle args) {
        imageKey = args.getString("KEY");
        String taskType = args.getString(TaskConstants.TASK_TYPE_FIELD);

        String urlStart = Constant.restUrl + "task/image/fetch/";
        String urlEnd = RealmUtils.loadPreferencesFromDB().getMobileNumber() + "/"
                + RealmUtils.loadPreferencesFromDB().getToken() + "/"
                + taskType + "/" + imageKey;

        Picasso.with(getContext())
                .load(urlStart + urlEnd)
                .placeholder(R.drawable.ic_logo_splashscreen) // todo : really need a loading ...
                .error(R.drawable.ic_logo_splashscreen)
                .centerInside()
                .fit()
                .into(mainView, new Callback() {
                    @Override
                    public void onSuccess() {
                        Log.e(TAG, "loaded!");
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onError() {
                        Log.e(TAG, "failure!");
                        progressDialog.dismiss();
                    }
                });
    }

}
