package org.grassroot.android.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.grassroot.android.R;
import org.grassroot.android.models.ImageRecord;
import org.grassroot.android.services.ApplicationLoader;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.RealmUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by luke on 2017/02/25.
 */

public class PhotoGridAdapter extends ArrayAdapter<ImageRecord> {

    private static final String TAG = PhotoGridAdapter.class.getSimpleName();

    private List<ImageRecord> imageRecords;
    private LayoutInflater inflater;

    private final String urlBase;
    private static final SimpleDateFormat captionFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());

    public PhotoGridAdapter(Context context, List<ImageRecord> imageRecords, String taskType) {
        super(context, R.layout.row_grid_photo, imageRecords);
        this.imageRecords = imageRecords;
        this.inflater = LayoutInflater.from(context);

        this.urlBase = Constant.restUrl + "task/image/fetch/micro/"
                + RealmUtils.loadPreferencesFromDB().getMobileNumber() + "/"
                + RealmUtils.loadPreferencesFromDB().getToken() + "/"
                + taskType + "/";
    }

    public void removeImage(ImageRecord imageRecord) {
        this.imageRecords.remove(imageRecord);
        notifyDataSetChanged(); // because images will move around
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_grid_photo, parent, false);
        }

        final ImageView thumbnail = (ImageView) convertView.findViewById(R.id.photo_thumbnail);
        final TextView caption = (TextView) convertView.findViewById(R.id.photo_taker);

        final ImageRecord record = imageRecords.get(position);

        final String dateTaken = captionFormat.format(new Date(record.getCreationTime()));

        final String captionText = TextUtils.isEmpty(record.getUserDisplayName()) ?
                getContext().getString(R.string.taken_on_caption, dateTaken) :
                getContext().getString(R.string.taken_by_caption, record.getUserDisplayName(), dateTaken);

        caption.setText(captionText);

        final String imageUrl = urlBase + record.getKey();
        Picasso
                .with(ApplicationLoader.applicationContext)
                .load(imageUrl)
                .placeholder(R.drawable.img_loading_photo)
                .error(R.drawable.img_loading_photo)
                .centerInside()
                .fit()
                .into(thumbnail, new Callback() {
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
