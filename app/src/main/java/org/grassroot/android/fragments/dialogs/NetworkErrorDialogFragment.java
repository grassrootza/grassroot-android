package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.NetworkErrorDialogListener;

/**
 * Created by paballo on 2016/06/02.
 */
public class NetworkErrorDialogFragment extends DialogFragment {

    private static final  String TAG = NetworkErrorDialogFragment.class.getCanonicalName();

    NetworkErrorDialogListener mListener;

    public static NetworkErrorDialogFragment newInstance(int message, NetworkErrorDialogListener listener) {
        NetworkErrorDialogFragment frag = new NetworkErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt("message", message);
        frag.setArguments(args);
        frag.mListener = listener;
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int message = getArguments().getInt("message");
       return new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(R.string.Alert_Retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.retryClicked();
                    }
                })
                .setNegativeButton(R.string.work_offline, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mListener.offlineClicked();
                        NetworkErrorDialogFragment.this.dismiss();

                    }
                })
                .setNeutralButton(R.string.Network_Settings, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.checkNetworkSettingsClicked(getActivity());
                    }
                })
                .create();
    }

}