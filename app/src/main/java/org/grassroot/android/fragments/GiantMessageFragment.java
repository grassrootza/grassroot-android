package org.grassroot.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.activities.HomeScreenActivity;
import org.grassroot.android.activities.NoGroupWelcomeActivity;
import org.grassroot.android.utils.RealmUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by luke on 2016/07/27.
 */
public class GiantMessageFragment extends Fragment {

	private static final String TAG = GiantMessageFragment.class.getSimpleName();

	private Unbinder unbinder;

	@BindView(R.id.gm_header) TextView messageHeader;
	@BindView(R.id.gm_body) TextView messageBody;

	@BindView(R.id.gm_btn_1) Button button1;
	@BindView(R.id.gm_btn_2) Button button2;
	@BindView(R.id.gm_btn_home) Button homeButton;

	private int headerRes;
	private String bodyText;
	private boolean btnOneVisible;
	private boolean btnTwoVisible;
	private boolean homeBtnVisible = true;

	private int btnOneLabelRes;
	private View.OnClickListener btnOneListener;
	private int btnTwoLabelRes;
	private View.OnClickListener btnTwoListener;

	public static class Builder {
		private int headerRes;
		private String bodyText;

		private boolean btnOneVisible = false;
		private boolean btnTwoVisible = false;

		private int btnOneLabelRes;
		private View.OnClickListener btnOneListener;
		private int btnTwoLabelRes;
		private View.OnClickListener btnTwoListener;

		private boolean showHomeButton = true;

		public Builder(int headerRes) {
			this.headerRes = headerRes;
		}

		public Builder setBody(String bodyText) {
			this.bodyText = bodyText;
			return this;
		}

		public Builder setButtonOne(int label, View.OnClickListener listener) {
			this.btnOneVisible = true;
			this.btnOneLabelRes = label;
			this.btnOneListener = listener;
			return this;
		}

		public Builder setButtonTwo(int label, View.OnClickListener listener) {
			this.btnTwoVisible = true;
			this.btnTwoLabelRes = label;
			this.btnTwoListener = listener;
			return this;
		}

		public Builder showHomeButton(boolean showHomeButton) {
			this.showHomeButton = showHomeButton;
			return this;
		}

		public GiantMessageFragment build() {
			return GiantMessageFragment.newInstance(this);
		}

	}

	private static GiantMessageFragment newInstance(Builder builder) {
		GiantMessageFragment fragment = new GiantMessageFragment();
		fragment.headerRes = builder.headerRes;
		fragment.bodyText = builder.bodyText;

		fragment.btnOneVisible = builder.btnOneVisible;
		fragment.btnOneLabelRes = builder.btnOneLabelRes;
		fragment.btnOneListener = builder.btnOneListener;

		fragment.btnTwoVisible = builder.btnTwoVisible;
		fragment.btnTwoLabelRes = builder.btnTwoLabelRes;
		fragment.btnTwoListener = builder.btnTwoListener;

		fragment.homeBtnVisible = builder.showHomeButton;

		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
																		 Bundle savedInstanceState) {
		View viewToReturn = inflater.inflate(R.layout.fragment_giant_message, container, false);
		unbinder = ButterKnife.bind(this, viewToReturn);

		messageHeader.setText(headerRes);
		messageBody.setText(bodyText);

		if (btnOneVisible) {
			button1.setVisibility(View.VISIBLE);
			button1.setText(btnOneLabelRes);
			if (btnOneListener != null) {
				button1.setOnClickListener(btnOneListener);
			}
		} else {
			button1.setVisibility(View.GONE);
		}

		if (btnTwoVisible) {
			button2.setVisibility(View.VISIBLE);
			button2.setText(btnTwoLabelRes);
			if (btnTwoListener != null) {
				button2.setOnClickListener(btnTwoListener);
			}
		} else {
			button2.setVisibility(View.GONE);
		}

		if (homeBtnVisible) {
			homeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean hasGroups = RealmUtils.loadPreferencesFromDB().isHasGroups();
					Log.d(TAG, "going home ... has groups set to ... " + hasGroups);
					Intent i = RealmUtils.loadPreferencesFromDB().isHasGroups() ?
							new Intent(getActivity(), HomeScreenActivity.class) : new Intent(getActivity(), NoGroupWelcomeActivity.class);
					startActivity(i);
					getActivity().finish();
				}
			});
		}
		homeButton.setVisibility(homeBtnVisible ? View.VISIBLE : View.GONE);

		return viewToReturn;
	}

	@Override
	public void onResume() {
		super.onResume();
		// Android incredible awfulness on keyboard management necessitates the below (and note does not mean "always" hidden, but just hidden doesn't hide ... Android)
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

}
