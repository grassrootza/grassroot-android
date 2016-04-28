package com.techmorphosis.grassroot.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by paballo on 2016/04/26.
 */
public class OtpScreenFragment extends Fragment {


    EditText et_otp;

    @BindView(R.id.txt_resend)
    TextView txtResend;

    @BindView(R.id.bt_submit_otp)
    Button bt_submit_otp;

    private OnOtpScreenFragmentListener onOtpScreenFragmentListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater,  ViewGroup container,  Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.container_otp, container, false);
        ButterKnife.bind(this, view);
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
        onOtpScreenFragmentListener.onSubmitClick();
    }

    @OnClick
    public void textResendClicked(){
        onOtpScreenFragmentListener.onTextResendClick();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface OnOtpScreenFragmentListener {

         void onTextResendClick();

         void onSubmitClick();



    }
}
