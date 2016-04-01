package com.techmorphosis.grassroot.ui.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.utils.listener.AlertDialogListener;

/**
 * Created by admin on 25-Mar-16.
 */
public class AlertDialogFragment extends DialogFragment{

    View v;
    TextView txtMessage;
    private TextView bt_right;
    private TextView bt_left;
    Boolean cancel;
    private AlertDialogListener mAlertDialogListener;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v=inflater.inflate(R.layout.alertdialog,container,false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return v;

    }

    private void findViews() {
        txtMessage = (TextView) v.findViewById(R.id.txt_message);
        bt_right=(TextView)v.findViewById(R.id.bt_right);
        bt_left=(TextView)v.findViewById(R.id.bt_left);
        bt_right.setOnClickListener(right());
        bt_left.setOnClickListener(left());

    }



    private View.OnClickListener left() {
            return  new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                mAlertDialogListener.setLeftButton();

                }
            };
    }

    private View.OnClickListener right() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialogListener.setRightButton();

            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        findViews();
        Bundle b= getArguments();
        if (b!=null)
        {
            txtMessage.setText(b.getString("message"));
            cancel=b.getBoolean("cancelable");
            bt_left.setText(b.getString("left"));
            bt_right.setText(b.getString("right"));
            getDialog().setCancelable(cancel);
        }
        else
        {
            txtMessage.setText("error");
        }


    }

    public void setListener(AlertDialogListener alertdialoglistener)
    {
         mAlertDialogListener = alertdialoglistener;
    }

    


}
