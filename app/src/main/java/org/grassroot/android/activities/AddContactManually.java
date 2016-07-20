package org.grassroot.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;
import org.grassroot.android.models.Member;
import org.grassroot.android.utils.Constant;
import org.grassroot.android.utils.Utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AddContactManually extends PortraitActivity {

  private static final String TAG = AddContactManually.class.getSimpleName();

  private Member memberBeingEdited;
  private int positionBeingEdited;

  @BindView(R.id.add_member_manual) RelativeLayout activityRegister;

  @BindView(R.id.amm_et_name) TextInputEditText displayName;
  @BindView(R.id.amm_et_number) TextInputEditText phoneNumber;
  @BindView(R.id.amm_bt_add) Button addOrSave;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_contact_manually);
    ButterKnife.bind(this);

    // todo : check if we have a member passed to us, to edit
    memberBeingEdited = getIntent().getParcelableExtra(GroupConstants.MEMBER_OBJECT);
    if (memberBeingEdited != null) {
      positionBeingEdited = getIntent().getIntExtra(Constant.INDEX_FIELD, -1);
      displayName.setText(memberBeingEdited.getDisplayName());
      phoneNumber.setText(Utilities.stripPrefixFromNumber(memberBeingEdited.getPhoneNumber()));
      addOrSave.setText(R.string.amm_edit_done);
    }
  }

  @OnClick(R.id.amm_bt_add) public void register() {
    try {
      InputMethodManager im =
          (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
      im.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    } catch (Exception e) {
      Log.e(TAG, "InputMethod error! " + e.toString());
    }

    if (registerFormValidation()) {
      Intent i = new Intent();
      if (memberBeingEdited == null) {
        i.putExtra("name", displayName.getText().toString());
        i.putExtra("selectedNumber", phoneNumber.getText().toString());
      } else {
        memberBeingEdited.setDisplayName(displayName.getText().toString());
        memberBeingEdited.setPhoneNumber(phoneNumber.getText().toString());
        i.putExtra(GroupConstants.MEMBER_OBJECT, memberBeingEdited);
        i.putExtra(Constant.INDEX_FIELD, positionBeingEdited);
      }

      setResult(RESULT_OK, i);
      finish();
    }
    ;
  }

  private boolean registerFormValidation() {
    String regex = "^((?:\\+27|27)|0)(\\d{2})-?(\\d{3})-?(\\d{4})$";
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(phoneNumber.getText().toString().trim());
    if (displayName.getText().toString().trim().isEmpty() || phoneNumber.getText()
        .toString()
        .isEmpty()) {
      showSnackBar(getResources().getString(R.string.Either_field_empty), Snackbar.LENGTH_SHORT);
      return false;
    } else {
      if (phoneNumber.getText().toString().length() != 10
          && phoneNumber.getText().toString().length() < 10) {
        showSnackBar(getResources().getString(R.string.Cellphone_number_invalid), Snackbar.LENGTH_SHORT);
        return false;
      } else {
        if (Integer.parseInt(String.valueOf(phoneNumber.getText().toString().charAt(0)))
            != 0) {
          showSnackBar(getResources().getString(R.string.Cellphone_number_invalid), Snackbar.LENGTH_SHORT);
          return false;
        } else if (Integer.parseInt(
            String.valueOf(phoneNumber.getText().toString().charAt(1))) == 0
            || Integer.parseInt(String.valueOf(phoneNumber.getText().toString().charAt(1)))
            == 9) {
          showSnackBar(getResources().getString(R.string.Cellphone_number_invalid), Snackbar.LENGTH_SHORT);
          return false;
        } else {
          return true;
        }
      }
    }
  }

  private void showSnackBar(String string, int lengthShort) {
    Snackbar.make(activityRegister, string, lengthShort).show();
  }
}
