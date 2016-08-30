package org.grassroot.android.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.NavigationConstants;
import org.grassroot.android.utils.NetworkUtils;
import org.grassroot.android.utils.RealmUtils;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by paballo on 2016/06/02.
 */
public class NetworkErrorDialogFragment extends DialogFragment {

	private static final  String TAG = NetworkErrorDialogFragment.class.getCanonicalName();

	Subscriber<String> subscriber;
	ProgressBar progressBar;

    public static NetworkErrorDialogFragment newInstance(int message, ProgressBar progressBar,
																												 Subscriber<String> subscriber) {
			NetworkErrorDialogFragment frag = new NetworkErrorDialogFragment();
			Bundle args = new Bundle();
			args.putInt("message", message);
			frag.setArguments(args);
			frag.subscriber = subscriber;
			frag.progressBar = progressBar; // todo : make sure no memory leaks
			return frag;
    }

    @Override
		@NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
			int message = getArguments().getInt("message");

			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setMessage(message) // todo : change button text if offline mode selected
					.setPositiveButton(R.string.alert_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							if (!RealmUtils.loadPreferencesFromDB().getOnlineStatus().equals(NetworkUtils.ONLINE_DEFAULT)) {
								if (progressBar != null) {
									progressBar.setVisibility(View.VISIBLE);
								}
								NetworkUtils.trySwitchToOnlineRx(getContext(), false, AndroidSchedulers.mainThread())
										.subscribe(new Action1<String>() {
											@Override
											public void call(String s) {
												subscriber.onNext(NetworkUtils.ONLINE_DEFAULT);
											}
										}, new Action1<Throwable>() {
											@Override
											public void call(Throwable throwable) {
												Log.e(TAG, "we got an error!");
												subscriber.onNext(NetworkUtils.CONNECT_ERROR);
											}
										});
							} else {
								subscriber.onNext(NetworkUtils.ONLINE_DEFAULT);
							}
						}
					});

			if (!RealmUtils.loadPreferencesFromDB().getOnlineStatus().equals(NetworkUtils.OFFLINE_SELECTED)) {
				builder.setNegativeButton(R.string.work_offline, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						NetworkUtils.setOfflineSelected();
						NetworkErrorDialogFragment.this.dismiss();
					}
				});
			}

			builder.setNeutralButton(R.string.alert_check_network, new DialogInterface.OnClickListener(){
										@Override
										public void onClick(DialogInterface dialog, int which) {
											getActivity().startActivityForResult(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
													NavigationConstants.NETWORK_SETTINGS_DIALOG); }
								});

			return builder.create();
    }

}