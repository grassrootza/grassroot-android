package org.grassroot.android.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.grassroot.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

;

/**
 * Created by paballo on 2016/04/26.
 */
public class OtpScreenFragment extends Fragment {


    @BindView(R.id.et_otp)
    public EditText et_otp;

    @BindView(R.id.txt_resend)
    TextView txtResend;

    @BindView(R.id.bt_submit_otp)
    Button bt_submit_otp;

    private OnOtpScreenFragmentListener onOtpScreenFragmentListener;


    public static OtpScreenFragment newInstance(String data){
        OtpScreenFragment otpScreenFragment = new OtpScreenFragment();
        Bundle args = new Bundle();
        args.putString("verification_code", data);
        otpScreenFragment.setArguments(args);

        return otpScreenFragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        Activity activity =(Activity)context;
        try{
            onOtpScreenFragmentListener = (OnOtpScreenFragmentListener)activity;

        }catch (ClassCastException e){

        }
    }


    @OnClick(R.id.bt_submit_otp)
    public void submitButtonClicked(){
        onOtpScreenFragmentListener.onOtpSubmitButtonClick(et_otp);
    }

    @OnClick(R.id.txt_resend)
    public void textResendClicked(){
        onOtpScreenFragmentListener.onTextResendClick();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface OnOtpScreenFragmentListener {

        void onTextResendClick();

        void onOtpSubmitButtonClick(EditText et_otp);



    }
}
