package com.techmorphosis.grassroot.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.techmorphosis.grassroot.R;

public class AddContactManually extends PortraitActivity {

    private EditText et_userName;
    private EditText et_mobile_register;
    private Button bt_register;
    private Snackbar snackbar;
    private RelativeLayout activityRegister;
    private static final String TAG = "AddContactManually";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact_manually);

        findAllViews();
    }

    private void findAllViews() {

        activityRegister = (RelativeLayout) findViewById(R.id.activity_register);
        et_userName = (EditText) findViewById(R.id.et_userName);
        et_mobile_register = (EditText) findViewById(R.id.et_mobile_register);

        bt_register = (Button) findViewById(R.id.bt_register);
        bt_register.setOnClickListener(bt_register(et_userName, et_mobile_register));


    }

    private View.OnClickListener bt_register(EditText et_userName, final EditText et_mobile_register)
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    InputMethodManager im= (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                registerFormValidation(et_mobile_register.getText().toString().trim().replaceAll("[^+0-9]", ""));
            }
        };
    }

    private void registerFormValidation(String et_mobile_register) {


        if (et_userName.getText().toString().trim().isEmpty() || et_mobile_register.isEmpty()) {
            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Either_field_empty), "", 0, Snackbar.LENGTH_SHORT);

        } else {
            if (NumberValidation(et_mobile_register)) {
                Log.e(TAG, "true");
                Intent i = new Intent();
                i.putExtra("name",et_userName.getText().toString());
                i.putExtra("selectedNumber",et_mobile_register);

                setResult(2, i);
                finish();

            } else {
                Log.e(TAG,"false");
            }
        }
    }

    private void showSnackBar(Context applicationContext, String s, String string, String s1, int i, int lengthShort)
    {

        snackbar=Snackbar.make(activityRegister, string ,lengthShort);
        snackbar.show();
    }

    private boolean NumberValidation(String number) {

        if (number.length() == 10) {

            int start=0;
            int end=1;
            String target = "0";

            if (validsubstring(number, target, start, end) && validcharAt(1, number)) {//2nd digit
                return  true;
            }
            else {
                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);
            }


        }
        else if (number.length() == 12) {

            int start=0;
            int end=3;
            String target = "+27";


            if (validsubstring(number,target,start,end) && validcharAt(3, number) ) {//fourth digit should be 6, 7 or 8
                return  true;
            }
            else {
                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);
            }

        }
        else if (number.length() == 13) {

            int start=0;
            int end=4;
            String target = "0027";

            if (validsubstring(number,target,start,end) && validcharAt(4, number) ) {//fifth digit should be 6, 7, or 8
                return  true;
            }
            else {
                showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);
            }

        }
        else {
            showSnackBar(getApplicationContext(), "", getResources().getString(R.string.Cellphone_number_invalid), "", 0, Snackbar.LENGTH_SHORT);
        }
        return false;
    }

    private boolean validsubstring(String source,String target, int start, int end) {


        if (source.substring(start, end).equals(target)) {

            return true;
        }

        return false;
    }

    private boolean validcharAt(int index,String value) {


        int compareint=Integer.parseInt(String.valueOf(value.charAt(index)));

        if (compareint == 6 || compareint == 7 || compareint == 8) {//6 || 7 || 8
            return true;
        }
        return false;
    }

}
