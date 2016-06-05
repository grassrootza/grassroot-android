package org.grassroot.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Contact;

import java.util.ArrayList;

/**
 * Created by ravi on 6/4/16.
 */
public class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.MyRecyclerAdapter_ViewHolder> {

    private   String TAG = MyRecyclerAdapter.class.getSimpleName() ;
    private final Context mcontext;
    private final ArrayList<Contact> data;
    private final LayoutInflater inflater;

    public MyRecyclerAdapter(Context context, ArrayList<Contact> List)
    {
        this.mcontext=context;
        this.data=List;
        inflater= LayoutInflater.from(context);
    }

    @Override
    public MyRecyclerAdapter.MyRecyclerAdapter_ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v=inflater.inflate(R.layout.row_dialog,parent,false);
        MyRecyclerAdapter_ViewHolder viewHolder= new MyRecyclerAdapter_ViewHolder(v);
        return viewHolder ;
    }

    @Override
    public void onBindViewHolder(final MyRecyclerAdapter.MyRecyclerAdapter_ViewHolder holder, final int position)
    {

        Contact model= data.get(position);


        holder.tv_dialog_person_name.setText(model.selectedNumber);

        if (model.isSelected)
        {
            holder.iv_dialog_Selected.setImageResource(R.drawable.btn_checked);
        }
        else
        {
            holder.iv_dialog_Selected.setImageResource(R.drawable.btn_unchecked);
        }



    }



    @Override
    public int getItemCount() {
        return data.size();
    }

    public class MyRecyclerAdapter_ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_dialog_person_name;
        ImageView iv_dialog_Selected;
        public MyRecyclerAdapter_ViewHolder(View itemView)
        {
            super(itemView);
            tv_dialog_person_name= (TextView) itemView.findViewById(R.id.tv_dialog_person_name);
            iv_dialog_Selected= (ImageView) itemView.findViewById(R.id.iv_dialog_Selected);
        }
    }
}