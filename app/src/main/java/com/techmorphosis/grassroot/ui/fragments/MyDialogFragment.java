package com.techmorphosis.grassroot.ui.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.MyRecyclerAdapter;

import java.util.ArrayList;

/**
 * Created by ravi on 6/4/16.
 */
public class MyDialogFragment extends DialogFragment {
    private RecyclerView mRecyclerView;
    private MyRecyclerAdapter adapter;
    View v;
    private ArrayList<String> customList;

    // this method create view for your Dialog
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        //inflate layout with recycler view
        v = inflater.inflate(R.layout.dialog, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return v;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        findAllViews();
       // init();

        Bundle b= getArguments();
        if (b!=null)
        {
            customList= new ArrayList<>();
            customList=b.getStringArrayList("numberList");

        }
        else
        {
            Log.e("TAG","error");
        }
        mRecyclerView();

    }

    private void mRecyclerView()
    {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
       // adapter = new MyRecyclerAdapter(getActivity(), customList);
        mRecyclerView.setAdapter(adapter);

    }

    private void init() {
        customList= new ArrayList<>();
        customList.add("Ravi");
        customList.add("Ravi");
        customList.add("Ravi");
        customList.add("Ravi");
        customList.add("Ravi");
        customList.add("Ravi");
        customList.add("Ravi");
        customList.add("Ravi");
    }



    public  void findAllViews()
    {
        mRecyclerView = (RecyclerView) v.findViewById(R.id.rcv_dialog);


    }


}
