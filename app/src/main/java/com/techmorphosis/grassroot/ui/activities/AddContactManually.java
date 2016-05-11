package com.techmorphosis.grassroot.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.techmorphosis.grassroot.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AddContactManually extends PortraitActivity {

    @BindView(R.id.et_userName)
    EditText et_userName;
    @BindView(R.id.et_mobile_register)
    EditText et_mobile_register;
    @BindView(R.id.bt_register)
    Button bt_register;
    private Snackbar snackbar;
    @BindView(R.id.activity_register)
    RelativeLayout activityRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact_manually);
        ButterKnife.bind(this);

    }

    @OnClick(R.id.bt_register)
    public void  register() {
        {
            try {
                InputMethodManager im = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            registerFormValidation();
        }

    }

    private void registerFormValidation() {


        if (et_userName.getText().toString().trim().isEmpty() || et_mobile_register.getText().toString().isEmpty()) {
            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Either_field_empty), "", 0, Snackbar.LENGTH_SHORT);

        } else {
            if (et_mobile_register.getText().toString().length() != 10 && et_mobile_register.getText().toString().length() < 10) {
                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

            } else {

                if (Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(0))) != 0) {
                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

                } else if (Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(1))) == 0 || Integer.parseInt(String.valueOf(et_mobile_register.getText().toString().charAt(1))) == 9) {
                    showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);

                } else
                {

                    Intent i = new Intent();
                    i.putExtra("name",et_userName.getText().toString());
                    i.putExtra("selectedNumber",et_mobile_register.getText().toString());

                    setResult(2,i);
                    finish();

                }
            }
        }
    }

    private void showSnackBar(Context applicationContext, String s, String string, String s1, int i, int lengthShort)
    {
        snackbar=Snackbar.make(activityRegister, string ,lengthShort);
        snackbar.show();
    }


}
