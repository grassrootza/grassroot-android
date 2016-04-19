package com.techmorphosis.grassroot.ui.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.techmorphosis.grassroot.Interface.SortInterface;
import com.techmorphosis.grassroot.R;

/**
 * Created by ravi on 13/4/16.
 */
public class SortFragment extends android.support.v4.app.DialogFragment
{

    View view;
    private TextView tvDate;
    private TextView tvrole;
    private TextView tvdefaults;
    private static final String TAG = "SortFragment";
    public boolean date=false,role=false,defaults=false;
    private SortInterface fragmentsCall;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
         //super.onCreateView(inflater, container, savedInstanceState);
        view=inflater.inflate(R.layout.fragment_sort,container,false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle b= getArguments();
        if (b!=null)
        {
            date= b.getBoolean("Date");
        role=b.getBoolean("Role");
        defaults=b.getBoolean("Default");

        }
        else
        {

        }

        findAllViews();
        updateui(date, role, defaults);

    }

    private void updateui(boolean date, boolean role, boolean defaults)
    {
        if (date)
        {

            tvDate.setTypeface(null, Typeface.BOLD);


            tvDate.setTextColor(getResources().getColor(R.color.primaryColor));
            tvrole.setTextColor(getResources().getColor(R.color.grey));
            tvdefaults.setTextColor(getResources().getColor(R.color.grey));

        }
        else if (role)
        {
            tvDate.setTextColor(getResources().getColor(R.color.grey));
            tvrole.setTextColor(getResources().getColor(R.color.primaryColor));
            tvdefaults.setTextColor(getResources().getColor(R.color.grey));

            tvrole.setTypeface(null, Typeface.BOLD);

        }
        else if (defaults)
        {
            tvdefaults.setTypeface(null, Typeface.BOLD);

            tvDate.setTextColor(getResources().getColor(R.color.grey));
            tvrole.setTextColor(getResources().getColor(R.color.grey));
            tvdefaults.setTextColor(getResources().getColor(R.color.primaryColor));


        }
    }

    private void findAllViews() {
        tvDate = (TextView) view.findViewById(R.id.tv_Date);
        tvrole = (TextView) view.findViewById(R.id.role);
        tvdefaults = (TextView) view.findViewById(R.id.Default);

        tvDate.setOnClickListener(tvDateClick());
        tvrole.setOnClickListener(roleClick());
        tvdefaults.setOnClickListener(defaultsClick());

    }

    private View.OnClickListener tvDateClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                date = true;
                role=false;
                defaults=false;

                tvDate.setTypeface(null, Typeface.BOLD);


                tvDate.setTextColor(getResources().getColor(R.color.primaryColor));
                tvrole.setTextColor(getResources().getColor(R.color.grey));
                tvdefaults.setTextColor(getResources().getColor(R.color.grey));

               // ((Group_Homepage) getActivity()).tvDateClick(date,role,defaults);
                fragmentsCall.tvDateClick(date,role,defaults);

                getDialog().dismiss();
            }
        };
    }

    private View.OnClickListener roleClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                date = false;
                role=true;
                defaults=false;

                tvDate.setTextColor(getResources().getColor(R.color.grey));
                tvrole.setTextColor(getResources().getColor(R.color.primaryColor));
                tvdefaults.setTextColor(getResources().getColor(R.color.grey));

                tvrole.setTypeface(null, Typeface.BOLD);

               // ((Group_Homepage) getActivity()).roleClick(date, role, defaults);
                fragmentsCall.roleClick(date, role, defaults);
                getDialog().dismiss();
            }
        };
    }

    private View.OnClickListener defaultsClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                date = false;
                role=false;
                defaults=true;

                tvdefaults.setTypeface(null, Typeface.BOLD);

                tvDate.setTextColor(getResources().getColor(R.color.grey));
                tvrole.setTextColor(getResources().getColor(R.color.grey));
                tvdefaults.setTextColor(getResources().getColor(R.color.primaryColor));


               // ((Group_Homepage) getActivity()).defaultsClick(date, role, defaults);
                fragmentsCall.defaultsClick(date, role, defaults);
                getDialog().dismiss();
            }
        };
    }


    public void setListener(SortInterface fragmentsCalllistner)
    {
        fragmentsCall = fragmentsCalllistner;
    }

}
