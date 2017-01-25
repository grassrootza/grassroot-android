package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import org.grassroot.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.functions.Action1;

/**
 * Created by luke on 2017/01/13.
 */

public class AccountTypeFragment extends Fragment {

    private static final String TAG = AccountTypeFragment.class.getSimpleName();

    // type constants
    public static final String STD = "STANDARD";
    public static final String LIGHT = "LIGHT";
    public static final String HEAVY = "HEAVY";

    private Action1<String> subscriber;
    private Unbinder unbinder;

    @BindView(R.id.actype_standard_radio) RadioButton stdRadio;
    @BindView(R.id.actype_light_radio) RadioButton lightRadio;
    @BindView(R.id.actype_heavy_radio) RadioButton heavyRadio;

    private String selectedType;

    public static AccountTypeFragment newInstance(String preSelectedType, Action1<String> subscriber) {
        AccountTypeFragment fragment = new AccountTypeFragment();
        Bundle args = new Bundle();
        args.putString("PRE_SELECTED", preSelectedType);
        fragment.setArguments(args);
        fragment.subscriber = subscriber;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_type_select, container, false);
        unbinder = ButterKnife.bind(this, view);

        final String preSelect = getArguments().getString("PRE_SELECTED");
        selectedType = TextUtils.isEmpty(preSelect) ? STD : preSelect;

        switch (selectedType) {
            case LIGHT:
                lightRadio.setChecked(true);
                break;
            case HEAVY:
                heavyRadio.setChecked(true);
                break;
            default:
                stdRadio.setChecked(true);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.actype_standard_radio, R.id.actype_light_radio, R.id.actype_heavy_radio})
    public void onRadioSelected(View view) {
        if (((RadioButton) view).isChecked()) {
            final int optionSelected = view.getId();
            stdRadio.setChecked(optionSelected == R.id.actype_standard_radio);
            lightRadio.setChecked(optionSelected == R.id.actype_light_radio);
            heavyRadio.setChecked(optionSelected == R.id.actype_heavy_radio);

            selectedType = optionSelected == R.id.actype_heavy_radio ? HEAVY :
                    optionSelected == R.id.actype_light_radio ? LIGHT : STD;
        }

    }

    @OnClick({R.id.actype_standard_header, R.id.actype_light_header, R.id.actype_heavy_header})
    public void onHeaderClicked(View view) {
        switch (view.getId()) {
            case R.id.actype_standard_header:
                stdRadio.setChecked(true);
                onRadioSelected(stdRadio);
                break;
            case R.id.actype_light_header:
                lightRadio.setChecked(true);
                onRadioSelected(lightRadio);
                break;
            case R.id.actype_heavy_header:
                heavyRadio.setChecked(true);
                onRadioSelected(heavyRadio);
                break;
            default:
                Log.e(TAG, "Hmm, that shouldn't happen : ID of header : " + view.getId());
        }
    }

    @OnClick(R.id.next)
    public void onClickNext() {
        passToSubscriber();
    }

    @SuppressWarnings("unchecked")
    private void passToSubscriber() {
        subscriber.call(selectedType);
    }

}
