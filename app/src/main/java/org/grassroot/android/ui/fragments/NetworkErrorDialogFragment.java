package org.grassroot.android.ui.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;

/**
 * Created by paballo on 2016/06/02.
 */
public class NetworkErrorDialogFragment extends DialogFragment {

    NetworkErrorDialogListener mListener;
    private static final  String TAG = NetworkErrorDialogFragment.class.getCanonicalName();

    public void setListener(NetworkErrorDialogListener listener) {
        this.mListener = listener;
    }

    public static NetworkErrorDialogFragment newInstance(int message, NetworkErrorDialogListener listener) {
        NetworkErrorDialogFragment frag = new NetworkErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt("message", message);
        frag.setArguments(args);
        frag.setListener(listener);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int message = getArguments().getInt("message");
        Log.e(TAG, getActivity().getLocalClassName());
       return new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.retryClicked();
                    }
                })
                .setNegativeButton(R.string.alert_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NetworkErrorDialogFragment.this.dismiss();

                    }
                })
                .setNeutralButton("Check Network Settings", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.checkNetworkSettingsClicked(getActivity());
                    }
                })
                .create();

}

}