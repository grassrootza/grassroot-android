package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.grassroot.android.R;
import org.grassroot.android.utils.ErrorUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Unbinder;

// todo : make sure all soft input focus, scrolling etc., works
public class GroupSearchStartFragment extends Fragment {

    private static final String TAG = GroupSearchStartFragment.class.getSimpleName();

    Unbinder unbinder;

    @BindView(R.id.gsearch_term) TextInputEditText searchTerm;
    @BindView(R.id.gsearch_radio_name_subject) RadioButton searchNameAndSubject;

    @BindView(R.id.gsearch_geo_switch) SwitchCompat restrictByGeo;
    @BindView(R.id.gsearch_geo_options) RadioGroup geoOptionsGroup;
    private int geoRadius;

    private GroupSearchInputListener listener;

    public interface GroupSearchInputListener {
        void searchTriggered(String searchOption, boolean includeTopics, boolean geoFilter, int geoRadius);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (GroupSearchInputListener) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "error! search fragment initiated without listener");
            startActivity(ErrorUtils.gracefulExitToHome(getActivity()));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_group_search_open, container, false);
        unbinder = ButterKnife.bind(this, viewToReturn);
        return viewToReturn;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void setSearchText(final String searchString) {
        searchTerm.setText(searchString);
    }

    @OnCheckedChanged(R.id.gsearch_geo_switch)
    public void toggleGeoRestriction(boolean checked) {
        geoOptionsGroup.setVisibility(checked ? View.VISIBLE : View.GONE);
        if (checked && geoRadius == 0) {
            geoRadius = 10; // i.e., default
        }
    }

    @OnClick({ R.id.gs_geo_five, R.id.gs_geo_ten, R.id.gs_geo_fifty, R.id.gs_geo_100 })
    public void onGeoRadioButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.gs_geo_five:
                geoRadius = 5;
                break;
            case R.id.gs_geo_ten:
                geoRadius = 10;
                break;
            case R.id.gs_geo_fifty:
                geoRadius = 50;
                break;
            case R.id.gs_geo_100:
                geoRadius = 100;
        }
    }

    @OnClick(R.id.gsearch_submit)
    public void triggerSearch() {
        if (searchTerm.getText().toString().isEmpty()) {
            Snackbar.make((searchTerm), R.string.find_group_no_term,
                Snackbar.LENGTH_SHORT).show();
            searchTerm.setError(getString(R.string.find_group_til_error));
        } else {
            listener.searchTriggered(searchTerm.getText().toString(), searchNameAndSubject.isChecked(),
                restrictByGeo.isChecked(), restrictByGeo.isChecked() ? geoRadius : 0);
        }
    }


}