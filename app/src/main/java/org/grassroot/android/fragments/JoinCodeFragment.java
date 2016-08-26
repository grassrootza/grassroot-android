package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.interfaces.GroupConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by paballo on 2016/06/10.
 */
public class JoinCodeFragment extends Fragment {

    public static final String TAG = JoinCodeFragment.class.getCanonicalName();

    @BindView(R.id.jc_text_row1) TextView tvRow1;
    @BindView(R.id.jc_text_row2) TextView tvRow2;
    @BindView(R.id.jc_text_row3) TextView tvRow3;

    private Unbinder unbinder;

    private String joinCode;
    private JoinCodeListener listener;

    public interface JoinCodeListener {
        void joinCodeClose();
    }

    public static JoinCodeFragment newInstance(String joinCode){
        JoinCodeFragment fragment = new JoinCodeFragment();
        Bundle args = new Bundle();
        args.putString(GroupConstants.JOIN_CODE, joinCode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context) ;
        try {
            listener = (JoinCodeListener) context;
        } catch (ClassCastException e) {
            throw new UnsupportedOperationException("Error! Activity holding join code fragment must implement listener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            joinCode = args.getString(GroupConstants.JOIN_CODE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_code, container, false);
        unbinder = ButterKnife.bind(this, view);

        if(!joinCode.equals(getString(R.string.none))){
            tvRow1.setText(R.string.jc_row_one);
            tvRow2.setText(R.string.jc_row_two);
            tvRow3.setText(String.format(getString(R.string.jc_row_three), joinCode));
        } else {
            tvRow2.setText((getString(R.string.none)));
            tvRow1.setVisibility(View.GONE);
            tvRow2.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.jc_iv_back)
    public void onBackClicked() {
        listener.joinCodeClose();
    }

}