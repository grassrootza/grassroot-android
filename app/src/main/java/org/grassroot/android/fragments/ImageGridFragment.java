package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import org.grassroot.android.R;
import org.grassroot.android.adapters.PhotoGridAdapter;
import org.grassroot.android.models.ImageRecord;
import org.grassroot.android.services.TaskService;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.functions.Action1;

/**
 * Created by luke on 2017/02/27.
 */

public class ImageGridFragment extends Fragment {

    private static final String TAG = ImageGridFragment.class.getSimpleName();

    Unbinder unbinder;

    @BindView(R.id.vt_photo_grid) GridView taskPhotoGrid;
    @BindView(R.id.progressBar) ProgressBar progressBar;

    // if we shift this to, say, a group, then add a parameter
    private String taskType;
    private String taskUid;

    private Action1<String> imageClickObserver;

    public static ImageGridFragment newInstance(final String taskType, final String taskUid, Action1<String> imageClickObserver) {
        ImageGridFragment fragment = new ImageGridFragment();
        Bundle args = new Bundle();
        args.putString("TYPE", taskType);
        args.putString("UID", taskUid);
        fragment.setArguments(args);
        fragment.imageClickObserver = imageClickObserver;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            throw new UnsupportedOperationException("Error! Grid fragment must have images");
        }

        taskType = getArguments().getString("TYPE");
        taskUid = getArguments().getString("UID");

        if (TextUtils.isEmpty(taskType) || TextUtils.isEmpty(taskUid)) {
            throw new UnsupportedOperationException("Error! Grid fragment must have task type and UID set");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        View v = inflater.inflate(R.layout.fragment_image_gallery, container, false);
        unbinder = ButterKnife.bind(this, v);

        TaskService.getInstance().fetchTaskImages(taskType, taskUid)
                .subscribe(new Action1<List<ImageRecord>>() {
                    @Override
                    public void call(List<ImageRecord> imageRecords) {
                        Log.e(TAG, "fetched image records, this many: " + imageRecords.size());
                        if (!imageRecords.isEmpty()) {
                            loadImageGrid(imageRecords);
                        }
                    }
                });

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void loadImageGrid(final List<ImageRecord> imageRecords) {
        PhotoGridAdapter adapter = new PhotoGridAdapter(getContext(), imageRecords, taskType);
        taskPhotoGrid.setAdapter(adapter);
        taskPhotoGrid.setVisibility(View.VISIBLE);
        taskPhotoGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                imageClickObserver.call(imageRecords.get(i).getKey());
            }
        });
        adapter.notifyDataSetChanged();
    }

}