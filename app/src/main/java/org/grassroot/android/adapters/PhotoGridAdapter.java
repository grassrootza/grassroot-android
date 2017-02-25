package org.grassroot.android.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.grassroot.android.R;
import org.grassroot.android.models.ImageRecord;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;

import java.util.List;

/**
 * Created by luke on 2017/02/25.
 */

public class PhotoGridAdapter extends ArrayAdapter<ImageRecord> {

    private static final String TAG = PhotoGridAdapter.class.getSimpleName();

    private List<ImageRecord> imageRecords;
    private LayoutInflater inflater;

    private final String urlBase;

    public PhotoGridAdapter(Context context, List<ImageRecord> imageRecords, String taskType) {
        super(context, R.layout.row_grid_photo, imageRecords);
        this.imageRecords = imageRecords;
        this.inflater = LayoutInflater.from(context);

        this.urlBase = Constant.restUrl + "task/image/fetch/"
                + RealmUtils.loadPreferencesFromDB().getMobileNumber() + "/"
                + RealmUtils.loadPreferencesFromDB().getToken() + "/"
                + taskType + "/";
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_grid_photo, parent, false);
        }

        final ImageRecord record = imageRecords.get(position);
        final String imageUrl = urlBase + record.getKey();
        Picasso
                .with(ApplicationLoader.applicationContext)
                .load(imageUrl)
                .into((ImageView) convertView, new Callback() {
                    @Override
                    public void onSuccess() {}

                    @Override
                    public void onError() {
                        Log.e(TAG, "error in Picasso");
                    }
                });

        return convertView;
    }

}
