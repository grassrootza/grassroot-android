package org.grassroot.android.fragments;

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

/**
 * Created by paballo on 2016/06/10.
 */
public class JoinCodeFragment extends Fragment {

    public static final String TAG = JoinCodeFragment.class.getCanonicalName();

    @BindView(R.id.txt_cde_prefix)
    TextView txt_title;
    @BindView(R.id.txt_code)
    TextView txt_code;
    @BindView(R.id.txt_cde_postfix)
    TextView txt_postfix;

    private String joinCode;

    public static JoinCodeFragment newInstance(String joinCode){
        JoinCodeFragment fragment = new JoinCodeFragment();
        Bundle args = new Bundle();
        args.putString(GroupConstants.JOIN_CODE, joinCode);
        fragment.setArguments(args);
        return fragment;
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
        ButterKnife.bind(this, view);

        if(!joinCode.equals(getString(R.string.none))){
            txt_title.setText(getString(R.string.cf_join_code));
            txt_code.setText(joinCode);
            txt_postfix.setText("#");
        }
        else {
            txt_title.setText((getString(R.string.none)));
        }

        return view;
    }

  }



