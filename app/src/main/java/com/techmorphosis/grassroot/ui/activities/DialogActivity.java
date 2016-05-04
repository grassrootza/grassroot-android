package com.techmorphosis.grassroot.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.techmorphosis.grassroot.Interface.ClickListener;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.RecyclerView.RecyclerTouchListener;
import com.techmorphosis.grassroot.adapters.MyRecyclerAdapter;
import com.techmorphosis.grassroot.models.ContactsModel;
import com.techmorphosis.grassroot.ui.fragments.AlertDialogFragment;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class DialogActivity extends Activity implements  View.OnClickListener {


    @BindView(R.id.rcv_dialog)
    public RecyclerView mRecyclerView;
    private MyRecyclerAdapter adapter;
    private ArrayList<String> numberlist;
    private String TAG = DialogActivity.class.getSimpleName();
    private List<String> list;
    @BindView(R.id.bt_cg_right)
    public TextView bt_cg_right;
    @BindView(R.id.bt_cg_left)
    public TextView bt_cg_left;
    public ArrayList<ContactsModel> multiplenumbers;
    public String selectednumber;
    private UtilClass utilClass;
    private AlertDialogFragment alertDialogFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        setContentView(R.layout.activity_dilog);
        this.setFinishOnTouchOutside(false);
        init();
        multiplenumbers = new ArrayList<>();
        Bundle bundle = getIntent().getExtras();
        numberlist = bundle.getStringArrayList("numberList");
        selectednumber = bundle.getString("selectedNumber");
        Log.e(TAG, "selectedNumber is " + selectednumber);

        if (!TextUtils.isEmpty(selectednumber)) {
            bt_cg_right.setEnabled(true);
        } else {
            bt_cg_right.setEnabled(false);
        }
        for (int i = 0; i < numberlist.size(); i++) {
            ContactsModel contactsModel = new ContactsModel();
            contactsModel.selectedNumber = numberlist.get(i);
            if (selectednumber.equals(numberlist.get(i))) {
                contactsModel.isSelected = true;


            } else {
                contactsModel.isSelected = false;

            }

            multiplenumbers.add(contactsModel);

        }


        mRecyclerView();


    }

    private void init() {
        utilClass = new UtilClass();

    }


    private void mRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        adapter = new MyRecyclerAdapter(getApplicationContext(), multiplenumbers);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), mRecyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {

                //proceed
                for (int i = 0; i < multiplenumbers.size(); i++) {
                    ContactsModel model = multiplenumbers.get(i);

                    if (i == position) {
                        //ipdate the selectednumber
                        model.isSelected = true;
                        selectednumber = model.selectedNumber;

                        bt_cg_right.setEnabled(true);

                    } else {
                        model.isSelected = false;

                    }


                }

                adapter.notifyDataSetChanged();

                /*
                ContactsModel validmodel = multiplenumbers.get(position);
                if (validnumber(validmodel.selectedNumber))
                {
                   //valid number
                    //proceed
                    for (int i = 0; i < multiplenumbers.size(); i++) {
                        ContactsModel model = multiplenumbers.get(i);

                        if (i == position) {
                            model.isSelected = true;
                            selectednumber = model.selectedNumber;

                            bt_cg_right.setEnabled(true);

                        } else {
                            model.isSelected = false;

                        }


                    }

                    adapter.notifyDataSetChanged();


                }
                else
                {
                    //not a valid number

                    alertDialogFragment=utilClass.showAlerDialog(getFragmentManager(), getString(R.string.cg_valid_number),"","OK",true, new AlertDialogListener() {
                        @Override
                        public void setRightButton()
                        {
                                alertDialogFragment.dismiss();
                        }

                        @Override
                        public void setLeftButton()
                        {
                            alertDialogFragment.dismiss();
                        }
                    });



                }

*/


            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

    }

    private boolean validnumber(String selectedNumber) {
        selectedNumber = selectedNumber.trim();
        Log.e(TAG, "selectedNumber is aftr trim " + selectedNumber);

        selectedNumber = selectedNumber.replaceAll("[-.^:,]", "");
        Log.e(TAG, "selectedNumber is aftr replace " + selectedNumber);

        selectedNumber = selectedNumber.replaceAll("\\s", "");
        Log.e(TAG, "selectedNumber is aftr replace " + selectedNumber);

        selectedNumber = selectedNumber.replace("+91", "");
        Log.e(TAG, "selectedNumber is aftr +91 " + selectedNumber);


        if (selectedNumber.length() != 10 && selectedNumber.length() < 10) {
            return false;
        } else {

            if (Integer.parseInt(String.valueOf(selectedNumber.charAt(0))) != 0) {
                return false;

            } else if (Integer.parseInt(String.valueOf(selectedNumber.charAt(1))) == 0 || Integer.parseInt(String.valueOf(selectedNumber.charAt(1))) == 9) {
                return false;

            } else {
                return true;

            }
        }

    }


    @OnClick({R.id.bt_cg_left, R.id.bt_cg_right})
    public void onClick(View v)
    {
        if (v==bt_cg_left)
        {
            Intent intent=new Intent();
            setResult(2, intent);
            finish();
        }
        else
        {//add button
           // Log.e(TAG,"Add Button");
            Log.e(TAG,"finish selectednumber is " + selectednumber);
            Intent intent=new Intent();
            intent.putExtra("selectednumber", selectednumber);
            setResult(1, intent);
            finish();//finishing activity
        }
    }

}
