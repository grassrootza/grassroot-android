package org.grassroot.android.ui.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.AlertDialogListener;
import org.grassroot.android.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by admin on 25-Mar-16.
 */
public class AlertDialogFragment extends DialogFragment {

    private static final String TAG = AlertDialogFragment.class.getCanonicalName();

    @BindView(R.id.txt_message)
    TextView txtMessage;
    @BindView(R.id.bt_right)
    TextView bt_right;
    @BindView(R.id.bt_left)
    TextView bt_left;

    private AlertDialogListener mAlertDialogListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle b = getArguments();
        final String title = b.getString("title");
        final int style;

        if (title == null || title.trim().equals("")) {
            Log.e(TAG, "no title! setting the style");
            style = DialogFragment.STYLE_NO_TITLE;
        } else {
            Log.e(TAG, "found a title! here : " + title);
            getDialog().setTitle(title);
            style = DialogFragment.STYLE_NORMAL;
        }
        setStyle(style, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.alertdialog, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.bt_left)
    public void leftButtonListener() {
        mAlertDialogListener.setLeftButton();
    }

    @OnClick(R.id.bt_right)
    public void rightButtonListener() {
        mAlertDialogListener.setRightButton();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Bundle b = getArguments();
        if (b != null) {
            final boolean cancel = b.getBoolean(Constant.CANCELLABLE);
            txtMessage.setText(b.getString("message"));
            bt_left.setText(b.getString("left"));
            bt_right.setText(b.getString("right"));
            getDialog().setCancelable(cancel);
            Log.e(TAG, "button left visibility, text: " + bt_left.getVisibility() + ", text=" + bt_left.getText());
        } else {
            txtMessage.setText("error");
        }
    }

    public void setListener(AlertDialogListener alertdialoglistener) {
        mAlertDialogListener = alertdialoglistener;
    }
}
