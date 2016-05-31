package org.grassroot.android.ui.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.AlertDialogListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by admin on 25-Mar-16.
 */
public class AlertDialogFragment extends DialogFragment {

    View v;
    @BindView(R.id.txt_message)
    TextView txtMessage;
    @BindView(R.id.bt_right)
    TextView bt_right;
    @BindView(R.id.bt_left)
    TextView bt_left;
    Boolean cancel;
    private AlertDialogListener mAlertDialogListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.alertdialog, container, false);
        ButterKnife.bind(this, view);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
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
        Bundle b = getArguments();
        if (b != null) {
            txtMessage.setText(b.getString("message"));
            cancel = b.getBoolean("cancelable");
            bt_left.setText(b.getString("left"));
            bt_right.setText(b.getString("right"));
            getDialog().setCancelable(cancel);
        } else {
            txtMessage.setText("error");
        }
    }

    public void setListener(AlertDialogListener alertdialoglistener) {
        mAlertDialogListener = alertdialoglistener;
    }
}
