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
			frag.progressBar = progressBar; // todo : check memory leaks
			return frag;
    }

    @Override
		@NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
			int message = getArguments().getInt("message");

			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			final boolean offlineSelected = RealmUtils.loadPreferencesFromDB().getOnlineStatus().equals(NetworkUtils.OFFLINE_SELECTED);

			builder.setMessage(message)
					.setPositiveButton(offlineSelected ? R.string.alert_go_online : R.string.alert_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							if (progressBar != null) {
								progressBar.setVisibility(View.VISIBLE);
							}

							// note : need to leave a gap in the call to sync the local queue in case the call to this
							// needs to submit first (otherwise get transaction errors on server (at some point probably need a proper call queuing system)

							NetworkUtils.trySwitchToOnline(getContext(), false, AndroidSchedulers.mainThread())
									.subscribe(new Action1<String>() {
										@Override
										public void call(String s) {
											subscriber.onNext(NetworkUtils.ONLINE_DEFAULT);
											Log.e(TAG, "and now queuing up the send sync");
											NetworkUtils.sendQueueAfterDelay();
										}
									}, new Action1<Throwable>() {
										@Override
										public void call(Throwable throwable) {
											subscriber.onNext(NetworkUtils.CONNECT_ERROR);
										}
									});
						}
					});

			if (!offlineSelected) {
				builder.setNegativeButton(R.string.work_offline, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						NetworkUtils.setOfflineSelected();
						subscriber.onNext(NetworkUtils.OFFLINE_SELECTED);
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

			builder.setCancelable(true);
			return builder.create();
    }

	@Override
	public void onCancel(DialogInterface dialogInterface) {
		super.onCancel(dialogInterface);
		subscriber.onNext(NetworkUtils.OFFLINE_SELECTED); // since this is effectively the same
	}

}