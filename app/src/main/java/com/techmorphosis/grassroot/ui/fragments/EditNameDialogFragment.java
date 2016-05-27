package com.techmorphosis.grassroot.ui.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.adapters.DefaultAdapter;
import com.techmorphosis.grassroot.models.NavDrawerItem;
import com.techmorphosis.grassroot.ui.views.MDRootLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by RAVI on 08-05-2016.
 */
public class EditNameDialogFragment extends DialogFragment   implements
        View.OnClickListener, AdapterView.OnItemClickListener {

    public static final String TAG = EditNameDialogFragment.class.getSimpleName();

    ArrayList<NavDrawerItem> arrayList= new ArrayList<>();
    private ListView listview;
    private TextView title;
    private DefaultAdapter adapter;
    protected MDRootLayout view;
    //private MDButton button;
    private String type;
    private String selectedvalue;
    private TextView titletxt;
    private HashMap<String, String> hmap;
    List<String> value = null;
    List<String> key = null;

    public EditNameDialogFragment() {

    }

    public static EditNameDialogFragment newInstance(String type,String selectedvalue) {

        Log.e(TAG, "type is " + type);
        Log.e(TAG, "selectedvalue is " + selectedvalue);
        EditNameDialogFragment frag = new EditNameDialogFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        args.putString("selectedvalue", selectedvalue);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.dialog, container);
        view = (MDRootLayout) inflater.inflate(R.layout.dialog, container);
    return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findAllViews(view);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        Bundle bundle;
        if (savedInstanceState == null) {
            bundle = getArguments();
        } else {
            bundle = savedInstanceState;
        }

        type = bundle.getString("type");
        selectedvalue = bundle.getString("selectedvalue");



        if (type.equalsIgnoreCase("language"))
        {
            titletxt.setText(getString(R.string.pp_language_dialog_title));
            value = Arrays.asList(getResources().getStringArray(R.array.language));
            key = Arrays.asList(getResources().getStringArray(R.array.languagekey));

        }
        else if (type.equalsIgnoreCase("Notifications"))
        {
            titletxt.setText(getString(R.string.pp_notifications_dialog_title));

            value = Arrays.asList(getResources().getStringArray(R.array.Notifications));
            key = Arrays.asList(getResources().getStringArray(R.array.Notificationskey));


        }

          /* This is how to declare HashMap */
        hmap = new HashMap<String, String>();

      /*Adding elements to HashMap*/
        for (int i = 0; i < key.size(); i++)
        {
            hmap.put(key.get(i), value.get(i));
            NavDrawerItem model = new NavDrawerItem();
            if (key.get(i).equalsIgnoreCase(selectedvalue))
            {
                model.setIsChecked(true);
            }
            else
                model.setIsChecked(false);

            Log.e(TAG,"key " + key.get(i));
            Log.e(TAG,"hmap.get(selectedvalue) " + hmap.get(selectedvalue));
            model.setTitle(value.get(i));
            arrayList.add(model);
        }


         adapter = new DefaultAdapter(arrayList,getActivity());
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(this);
    }

    private void findAllViews(View view) {
        listview = (ListView) view.findViewById(R.id.contentListView);
        titletxt = (TextView) view.findViewById(R.id.titletxt);
       // button = (MDButton) view.findViewById(R.id.buttonDefaultPositive);
        //button.setOnClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        for (int i = 0; i <  arrayList.size(); i++) {
            NavDrawerItem model = arrayList.get(i);
            if (i==position)
            {
                model.setIsChecked(true);
            }
            else
                model.setIsChecked(false);

        }
        adapter.notifyDataSetChanged();

        if (type.equalsIgnoreCase("language"))
        {
            getLanguageListener().onLanguage(key.get(position));
        }
        else if (type.equalsIgnoreCase("Notifications"))
        {
            getNotificationsListener().onNotifications(key.get(position));
        }


        getDialog().dismiss();
    }

    @Override
    public void onClick(View v) {
      /*  if (v == button) {
            getDialog().dismiss();
        }*/

    }

    public interface OnEditlanguageListener {
        void onLanguage(String language);
    }

    private OnEditlanguageListener getLanguageListener() {
        OnEditlanguageListener listener = (OnEditlanguageListener) getTargetFragment();
        if (listener==null)
        {
            listener= (OnEditlanguageListener) getActivity();
        }
        return listener;
    }


    public interface OnEditNotificationsListener {
        void onNotifications(String notifications);
    }

    private OnEditNotificationsListener getNotificationsListener() {
        OnEditNotificationsListener listener = (OnEditNotificationsListener) getTargetFragment();
        if (listener==null)
        {
            listener= (OnEditNotificationsListener) getActivity();
        }
        return listener;
    }



}