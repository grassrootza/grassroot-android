package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.grassroot.android.R;

/**
 * Created by luke on 2016/07/25.
 */
public class ImageDetailFragment extends Fragment {

    public static final String IMAGE_DATA_EXTRA = "resId";
    private int imageResource;
    private ImageView imageView;

    public static ImageDetailFragment newInstance(int imageRes) {
        final ImageDetailFragment f = new ImageDetailFragment();
        final Bundle args = new Bundle();
        args.putInt(IMAGE_DATA_EXTRA, imageRes);
        f.setArguments(args);
        return f;
    }

    public ImageDetailFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageResource = getArguments() != null ? getArguments().getInt(IMAGE_DATA_EXTRA) :
                R.drawable.ic_groups_default_avatar;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_image_detail, container, false);
        imageView = (ImageView) v.findViewById(R.id.image_detail_view);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        imageView.setImageResource(imageResource);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageView = null;
    }

}
