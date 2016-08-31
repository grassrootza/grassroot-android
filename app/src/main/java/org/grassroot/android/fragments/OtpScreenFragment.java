package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by paballo on 2016/04/26.
 */
public class OtpScreenFragment extends Fragment {

    private static final String TAG = OtpScreenFragment.class.getSimpleName();

    private Unbinder unbinder;
    @BindView(R.id.otp_input_field) TextInputEditText otpInput;

    private String purpose; // i.e., for login or for register

    private OtpListener onOtpScreenFragmentListener;

    public interface OtpListener {
        void requestResendOtp(String purpose);
        void onOtpSubmitButtonClick(String otp, String purpose);
    }

    public static OtpScreenFragment newInstance(String otpPassed, String purpose) {
        OtpScreenFragment otpScreenFragment = new OtpScreenFragment();
        otpScreenFragment.checkBuildFlavorIntegrity(otpPassed);
        Bundle args = new Bundle();
        args.putString("verification_code", otpPassed);
        args.putString("purpose", purpose);
        otpScreenFragment.setArguments(args);
        return otpScreenFragment;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    private void checkBuildFlavorIntegrity(String otpPassed) throws UnsupportedOperationException {
        if (!TextUtils.isEmpty(otpPassed) && BuildConfig.FLAVOR.equals(Constant.PROD))
            throw new UnsupportedOperationException("Error! Passing OTP to fragment in production");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onOtpScreenFragmentListener = (OtpListener) context;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Error! Activity must implement otp listener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_otp_screen, container, false);
        unbinder = ButterKnife.bind(this, view);
        otpInput.setText(getArguments().getString("verification_code"));
        purpose = getArguments().getString("purpose");
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void setOtpDisplayed(String otpPassed) {
        checkBuildFlavorIntegrity(otpPassed);
        otpInput.setText(otpPassed);
    }

    @OnClick(R.id.bt_submit_otp)
    public void submitButtonClicked(){
        if (TextUtils.isEmpty(otpInput.getText().toString())) {
            otpInput.setError(getResources().getString(R.string.input_error_otp_empty));
        } else {
            onOtpScreenFragmentListener.onOtpSubmitButtonClick(otpInput.getText().toString(), purpose);
        }
    }

    @OnClick(R.id.txt_resend)
    public void textResendClicked(){
        onOtpScreenFragmentListener.requestResendOtp(purpose);
    }

}
