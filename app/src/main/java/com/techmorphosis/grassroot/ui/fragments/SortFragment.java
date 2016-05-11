package com.techmorphosis.grassroot.ui.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.techmorphosis.grassroot.Interface.SortInterface;
import com.techmorphosis.grassroot.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ravi on 13/4/16.
 */
public class SortFragment extends android.support.v4.app.DialogFragment
{

    @BindView(R.id.tv_Date)
    TextView tvDate;
    @BindView(R.id.role)
    TextView tvrole;
    @BindView(R.id.Default)
    TextView tvdefaults;
    private static final String TAG = "SortFragment";
    public boolean date=false,role=false,defaults=false;
    private SortInterface fragmentsCall;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view=inflater.inflate(R.layout.fragment_sort,container,false);
        ButterKnife.bind(this,view);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle b= getArguments();
        date= b.getBoolean("Date");
        role=b.getBoolean("Role");
        defaults=b.getBoolean("Default");
        updateui(date, role, defaults);

    }

    private void updateui(boolean date, boolean role, boolean defaults)
    {
        if (date) {
            tvDate.setTypeface(null, Typeface.BOLD);
            tvDate.setTextColor(ContextCompat.getColor(getActivity(),R.color.primaryColor));
            tvrole.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
            tvdefaults.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
        }
        else if (role)
        {
            tvDate.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
            tvrole.setTextColor(ContextCompat.getColor(getActivity(),R.color.primaryColor));
            tvdefaults.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
            tvrole.setTypeface(null, Typeface.BOLD);

        }
        else if (defaults)
        {
            tvdefaults.setTypeface(null, Typeface.BOLD);

            tvDate.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
            tvrole.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
            tvdefaults.setTextColor(ContextCompat.getColor(getActivity(),R.color.primaryColor));


        }
    }


    @OnClick(R.id.tv_Date)
    public void tvDateClick() {

                date = true;
                role=false;
                defaults=false;

                tvDate.setTypeface(null, Typeface.BOLD);


                tvDate.setTextColor(ContextCompat.getColor(getActivity(),R.color.primaryColor));
                tvrole.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
                tvdefaults.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));

               // ((Group_Homepage) getActivity()).tvDateClick(date,role,defaults);
                fragmentsCall.tvDateClick(date,role,defaults);

                getDialog().dismiss();

    }

    @OnClick(R.id.role)
    public void roleClick() {

                date = false;
                role=true;
                defaults=false;
                tvDate.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
                tvrole.setTextColor(ContextCompat.getColor(getActivity(),R.color.primaryColor));
                tvdefaults.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
                tvrole.setTypeface(null, Typeface.BOLD);

                fragmentsCall.roleClick(date, role, defaults);
                getDialog().dismiss();

    }

    @OnClick(R.id.Default)
   public void defaultsClick() {

                date = false;
                role=false;
                defaults=true;
                tvdefaults.setTypeface(null, Typeface.BOLD);
                tvDate.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
                tvrole.setTextColor(ContextCompat.getColor(getActivity(),R.color.grey));
                tvdefaults.setTextColor(ContextCompat.getColor(getActivity(),R.color.primaryColor));
                fragmentsCall.defaultsClick(date, role, defaults);
                getDialog().dismiss();

    }


    public void setListener(SortInterface fragmentsCalllistner)
    {
        fragmentsCall = fragmentsCalllistner;
    }

}
