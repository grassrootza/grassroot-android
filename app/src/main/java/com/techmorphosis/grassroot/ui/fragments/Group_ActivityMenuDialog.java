package com.techmorphosis.grassroot.ui.fragments;


import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.ui.activities.Blank;


public class Group_ActivityMenuDialog extends android.support.v4.app.DialogFragment {

    public static final String TAG = Group_ActivityMenuDialog.class.getSimpleName();
    public static final String ARG_ITEM = "item";
    public static final String ARG_ITEM_POSITION = "position";
    private View view;
    private ImageView icHomeVoteActive;
    private ImageView icHomeCallMeetingActive;
    private ImageView icHomeToDoActive;
    public  Boolean Vote=false,Meeting=false,ToDo=false;

    public Group_ActivityMenuDialog() {
    }


    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setWindowAnimations(R.style.animation_slide_from_right);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =inflater.inflate(R.layout.group_activties, container, false);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));

        return view;

    }

    private void findView()
    {
        icHomeVoteActive = (ImageView) view.findViewById(R.id.ic_home_vote_active);
        icHomeCallMeetingActive = (ImageView) view.findViewById(R.id.ic_home_call_meeting_active);
        icHomeToDoActive = (ImageView) view.findViewById(R.id.ic_home_to_do_active);

        icHomeCallMeetingActive.setOnClickListener(icHomeCallMeetingActive());
        icHomeVoteActive.setOnClickListener(icHomeVoteActive());
        icHomeToDoActive.setOnClickListener(icHomeToDoActive());

        int height = icHomeVoteActive.getDrawable().getIntrinsicWidth();
        int width = icHomeVoteActive.getDrawable().getIntrinsicHeight();

   /*     Toast.makeText(getActivity(), "height is " + height, Toast.LENGTH_SHORT).show();
        Toast.makeText(getActivity(), "width is " + width, Toast.LENGTH_SHORT).show();

        L.e(TAG, "height is ", "" + height);
        L.e(TAG, "width is ", "" + width);
*/
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        findView();
        Bundle b= getArguments();


      if (b!=null)
        {
            Meeting = b.getBoolean("Meeting");
            Vote = b.getBoolean("Vote");
            ToDo = b.getBoolean("ToDo");

            Log.e(TAG,"Meeting is " + Meeting);
            Log.e(TAG,"Vote is " + Vote);
            Log.e(TAG,"ToDo is " + ToDo);

        }
        else
        {
            Log.e(TAG, "null is ");

        }

        if (!Meeting)
        {
            icHomeCallMeetingActive.setImageResource(R.drawable.ic_home_call_meeting_inactive);
        }
        if (!Vote)
        {
            icHomeVoteActive.setImageResource(R.drawable.ic_home_vote_inactive);
        }
        if (!ToDo)
        {
            icHomeToDoActive.setImageResource(R.drawable.ic_home_to_do_inactive);
        }



    }

    private View.OnClickListener icHomeToDoActive() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ToDo)
                {
                    Intent ToDo= new Intent(getActivity(), Blank.class);
                    ToDo.putExtra("title","ToDo");
                    startActivity(ToDo);
                    getDialog().dismiss();
                }
                else
                {
                    getDialog().dismiss();
                }

            }
        };
    }

    private View.OnClickListener icHomeVoteActive() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Vote)
                {
                    Intent Vote= new Intent(getActivity(), Blank.class);
                   Vote.putExtra("title","Vote");
                    startActivity(Vote);
                    getDialog().dismiss();
                }
                else
                {
                    getDialog().dismiss();
                }

            }
        };
    }

    private View.OnClickListener icHomeCallMeetingActive() {
                return  new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        if (Meeting)
                        {
                            Intent Meeting= new Intent(getActivity(), Blank.class);
                            Meeting.putExtra("title","Meeting");
                            startActivity(Meeting);
                            getDialog().dismiss();
                        }
                        else
                        {
                            getDialog().dismiss();
                        }


                    }
                };
    }


}