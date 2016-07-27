package org.grassroot.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.activities.HomeScreenActivity;

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

	private int btnOneLabelRes;
	private View.OnClickListener btnOneListener;
	private int btnTwoLabelRes;
	private View.OnClickListener btnTwoListener;

	public static GiantMessageFragment newInstance(final int headerString, final String bodyText,
																								 boolean btnOneVisible, boolean btnTwoVisible) {
		GiantMessageFragment fragment = new GiantMessageFragment();
		fragment.headerRes = headerString;
		fragment.bodyText = bodyText;
		fragment.btnOneVisible = btnOneVisible;
		fragment.btnTwoVisible = btnTwoVisible;
		return fragment;
	}

	// todo : convert to builder pattern
	public void setButtonOne(int buttonText, View.OnClickListener listener) {
		this.btnOneLabelRes = buttonText;
		this.btnOneListener = listener;
	}

	public void setButtonTwo(int buttonText, View.OnClickListener listener) {
		this.btnTwoLabelRes = buttonText;
		this.btnTwoListener = listener;
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

		homeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), HomeScreenActivity.class); // todo : decide back behavior (should be okay to show again)
				startActivity(i);
				getActivity().finish();
			}
		});

		return viewToReturn;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

}
