package org.grassroot.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

;

/**
 * Created by paballo on 2016/04/26.
 */
public class OtpScreenFragment extends Fragment {

    @BindView(R.id.et_otp)
    EditText et_otp;

    @BindView(R.id.txt_resend)
    TextView txtResend;

    @BindView(R.id.bt_submit_otp)
    Button bt_submit_otp;

    private String purpose; // i.e., for login or for register

    private OnOtpScreenFragmentListener onOtpScreenFragmentListener;

    public static OtpScreenFragment newInstance(String otpPassed, String purpose) {
        OtpScreenFragment otpScreenFragment = new OtpScreenFragment();
        otpScreenFragment.checkBuildFlavorIntegrity(otpPassed);
        Bundle args = new Bundle();
        args.putString("verification_code", otpPassed);
        args.putString("purpose", purpose);
        otpScreenFragment.setArguments(args);
        return otpScreenFragment;
    }

    public void setOtpDisplayed(String otpPassed) {
        checkBuildFlavorIntegrity(otpPassed);
        et_otp.setText(otpPassed);
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    private void checkBuildFlavorIntegrity(String otpPassed) throws UnsupportedOperationException {
        if (!TextUtils.isEmpty(otpPassed) && BuildConfig.FLAVOR.equals(Constant.PROD))
            throw new UnsupportedOperationException("Error! Passing OTP to fragment in production");
    }

    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.container_otp, container, false);
        ButterKnife.bind(this, view);
        et_otp.setText(getArguments().getString("verification_code"));
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onOtpScreenFragmentListener = (OnOtpScreenFragmentListener) context;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Error! Activity must implement otp listener");
        }
    }


    @OnClick(R.id.bt_submit_otp)
    public void submitButtonClicked(){
        if (TextUtils.isEmpty(et_otp.getText().toString())) {
            et_otp.setError(getResources().getString(R.string.OTP_empty));
        } else {
            onOtpScreenFragmentListener.onOtpSubmitButtonClick(et_otp.getText().toString(), purpose);
        }
    }

    @OnClick(R.id.txt_resend)
    public void textResendClicked(){
        onOtpScreenFragmentListener.onTextResendClick(purpose);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface OnOtpScreenFragmentListener {
        void onTextResendClick(String purpose);
        void onOtpSubmitButtonClick(String otp, String purpose);
    }
}
