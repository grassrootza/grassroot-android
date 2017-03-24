package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.grassroot.android.R;
import org.grassroot.android.fragments.dialogs.EditTextDialogFragment;
import org.grassroot.android.interfaces.TaskConstants;
import org.grassroot.android.models.ImageRecord;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.RealmUtils;
import org.grassroot.android.utils.image.NetworkImageUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by luke on 2017/03/14.
 */

public class PhotoViewFragment extends Fragment {

    private static final String TAG  = PhotoViewFragment.class.getSimpleName();
    private static final SimpleDateFormat captionFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());

    private ImageRecord imageRecord;
    private String mobileNumber;
    private String taskType;

    Unbinder unbinder;
    @BindView(R.id.photo_main_view) ImageView mainView;
    @BindView(R.id.photo_caption_text) TextView caption;
    @BindView(R.id.photo_edit_buttons) ViewGroup editButtons;

    private ProgressDialog progressDialog;

    private Action1<ImageRecord> deletionSubscriber;
    private boolean openOnFaceCountDialog;

    public static PhotoViewFragment newInstance(final String taskType, ImageRecord imageRecord,
                                                Action1<ImageRecord> deletionSubscriber, boolean openOnFaceCount) {
        PhotoViewFragment fragment = new PhotoViewFragment();
        Bundle args = new Bundle();
        args.putString(TaskConstants.TASK_TYPE_FIELD, taskType);
        args.putParcelable("RECORD", imageRecord);
        args.putBoolean("OPEN_ON_FACES", openOnFaceCount);
        fragment.setArguments(args);
        fragment.deletionSubscriber = deletionSubscriber;
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
        ButterKnife.bind(this, v);

        mobileNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.wait_message)); // todo : make it say loading or something
        progressDialog.show();

        imageRecord = getArguments().getParcelable("RECORD");
        taskType = getArguments().getString(TaskConstants.TASK_TYPE_FIELD);
        openOnFaceCountDialog = getArguments().getBoolean("OPEN_ON_FACES", false);

        setCaptionAndButtons();
        loadTaskPhoto();

        if (openOnFaceCountDialog) {
            changePhotoCount();
        }

        return v;
    }

    private void loadTaskPhoto() {
        String urlStart = Constant.restUrl + "task/image/fetch/";
        String urlEnd = mobileNumber + "/"
                + RealmUtils.loadPreferencesFromDB().getToken() + "/"
                + taskType + "/" + imageRecord.getKey();

        Picasso.with(getContext())
                .load(urlStart + urlEnd)
                .placeholder(R.drawable.ic_logo_splashscreen) // todo : really need a loading ...
                .error(R.drawable.ic_logo_splashscreen)
                .centerInside()
                .fit()
                .into(mainView, new Callback() {
                    @Override
                    public void onSuccess() {
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onError() {
                        progressDialog.dismiss();
                    }
                });
    }

    private void setCaptionAndButtons() {
        final String dateTaken = captionFormat.format(new Date(imageRecord.getCreationTime()));
        String captionText = getString(R.string.vt_photo_caption_1, dateTaken, imageRecord.getUserDisplayName()) +
                (imageRecord.isCountModified() ? getString(R.string.vt_photo_faces_mod, imageRecord.getRevisedFaces())
                : imageRecord.isAnalyzed() ? imageRecord.hasFoundFaces() ? getString(R.string.vt_photo_faces_no_mod, imageRecord.getNumberFaces())
                : getString(R.string.vt_photo_faces_none) : "");
        caption.setText(captionText);

        boolean thisUserCreated = mobileNumber.equals(imageRecord.getUserPhoneNumber());
        editButtons.setVisibility(thisUserCreated ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.photo_delete)
    public void deletePhoto() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.photo_delete_confirm)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteImageDo();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setCancelable(true)
                .show();
    }

    private void deleteImageDo() {
        NetworkImageUtils.removeTaskImage(taskType, imageRecord)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() { }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(), ErrorUtils.isAccessDeniedError(e) ?
                                        R.string.photo_delete_failed_access : R.string.photo_delete_failed_other,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(String s) {
                        Toast.makeText(getContext(), R.string.photo_delete_succeeded, Toast.LENGTH_SHORT).show();
                        if (deletionSubscriber != null) {
                            deletionSubscriber.call(imageRecord);
                        }
                    }
                });
    }

    @OnClick(R.id.photo_edit_count)
    public void changePhotoCount() {
        editFaceCountDialog(R.string.photo_edit_faces_confirm);
    }

    private void editFaceCountDialog(int titleRes) {
        EditTextDialogFragment dialogFragment = EditTextDialogFragment.newInstance(
                titleRes,
                "" + (imageRecord.getNumberFaces() == null ? "" : imageRecord.getNumberFaces()),
                new EditTextDialogFragment.EditTextDialogListener() {
                    @Override
                    public void confirmClicked(String textEntered) {
                        editPictureDo(textEntered);
                    }
                }
        );
        dialogFragment.show(getFragmentManager(), "MODIFY_FACES");
    }

    private void editPictureDo(String textEntered) {
        try {
            NetworkImageUtils.updateImageFaceCount(imageRecord.getKey(), taskType, Integer.parseInt(textEntered))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<ImageRecord>() {
                        @Override
                        public void call(ImageRecord alteredRecord) {
                            Toast.makeText(getContext(), R.string.photo_count_update_succeeded, Toast.LENGTH_SHORT).show();
                            RealmUtils.saveDataToRealm(alteredRecord);
                            PhotoViewFragment.this.imageRecord = alteredRecord;
                            setCaptionAndButtons();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.e(TAG, "inside error, which is: " + throwable);
                            int toastText = ErrorUtils.isAccessDeniedError(throwable) ?
                                    R.string.photo_count_update_failed_access : R.string.photo_count_update_failed_other;
                            Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (NumberFormatException e) {
            // show dialog again

        }
    }

}
