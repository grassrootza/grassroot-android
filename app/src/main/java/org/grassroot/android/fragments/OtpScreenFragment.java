package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by paballo on 2016/04/26.
 */
public class OtpScreenFragment extends Fragment {

    private static final String TAG = OtpScreenFragment.class.getSimpleName();

    public interface OtpListener {
        void requestNewOtp(String purpose);
        void onOtpSubmitButtonClick(String otp, String purpose);
    }

    EditText otpInput;

    private String purpose; // i.e., for login or for register
    private OtpListener onOtpScreenFragmentListener;

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
        View view = inflater.inflate(R.layout.container_otp, container, false);
        otpInput = (EditText) view.findViewById(R.id.et_otp);
        otpInput.setText(getArguments().getString("verification_code"));
        purpose = getArguments().getString("purpose");
        return view;
    }

    @Override
    public void onDestroyView() {
        otpInput = null;
    }

    public void setOtpDisplayed(String otpPassed) {
        checkBuildFlavorIntegrity(otpPassed);
        otpInput.setText(otpPassed);
    }


    @OnClick(R.id.bt_submit_otp)
    public void submitButtonClicked(){
        if (TextUtils.isEmpty(otpInput.getText().toString())) {
            otpInput.setError(getResources().getString(R.string.OTP_empty));
        } else {
            Log.d(TAG, "OTP submit clicked, for purpose: " + purpose);
            onOtpScreenFragmentListener.onOtpSubmitButtonClick(otpInput.getText().toString(), purpose);
        }
    }

    @OnClick(R.id.txt_resend)
    public void textResendClicked(){
        onOtpScreenFragmentListener.requestNewOtp(purpose);
    }

}
