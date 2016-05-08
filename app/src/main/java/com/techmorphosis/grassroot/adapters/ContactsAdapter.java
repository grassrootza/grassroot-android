package com.techmorphosis.grassroot.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.techmorphosis.grassroot.ContactLib.SearchablePinnedHeaderListViewAdapter;
import com.techmorphosis.grassroot.ContactLib.StringArrayAlphabetIndexer;
import com.techmorphosis.grassroot.R;
import com.techmorphosis.grassroot.models.SingleContact;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by admin on 04-Apr-16.
 */
// ////////////////////////////////////////////////////////////
// ContactsAdapter //
// //////////////////
public class ContactsAdapter extends SearchablePinnedHeaderListViewAdapter<SingleContact>
{
    public  Context mcontext;
    private LayoutInflater mInflater;
    private ArrayList<SingleContact> mContacts;
    private final int CONTACT_PHOTO_IMAGE_SIZE;
    private final int[] PHOTO_TEXT_BACKGROUND_COLORS;
    private String TAG= ContactsAdapter.class.getSimpleName();
    ArrayList<SingleContact> oldContacts;
    //private final AsyncTaskThreadPool mAsyncTaskThreadPool=new AsyncTaskThreadPool(1,2,10);


    @Override
    public CharSequence getSectionTitle(int sectionIndex) {
        return ((StringArrayAlphabetIndexer.AlphaBetSection)getSections()[sectionIndex]).getName();
    }

    public ContactsAdapter(final ArrayList<SingleContact> contacts, Context context) {
        mInflater= LayoutInflater.from(context);
        this.mcontext=context;
        setData(contacts);
        PHOTO_TEXT_BACKGROUND_COLORS=context.getResources().getIntArray(R.array.contacts_text_background_colors);
        CONTACT_PHOTO_IMAGE_SIZE=context.getResources().getDimensionPixelSize(
                R.dimen.list_item__contact_imageview_size);
    }

    public void setData(final ArrayList<SingleContact> contacts)
    {
        this.mContacts=contacts;
        oldContacts = new ArrayList<>();
        oldContacts.addAll(contacts);
        final String[] generatedContactNames=generateContactNames(contacts);
        setSectionIndexer(new StringArrayAlphabetIndexer(generatedContactNames,true));
    }

    private String[] generateContactNames(final List<SingleContact> contacts)
    {
        final ArrayList<String> contactNames=new ArrayList<String>();
        if(contacts!=null)
            for(final SingleContact contactEntity : contacts)
                contactNames.add(contactEntity.name);
        return contactNames.toArray(new String[contactNames.size()]);
    }

    @Override
    public View getView(final int position,final View convertView,final ViewGroup parent)
    {
        final ViewHolder holder;
        final View rootView;
        if(convertView==null)
        {
            holder=new ViewHolder();
            rootView=mInflater.inflate(R.layout.listview_item,parent,false);
            holder.tv_person_name=(TextView)rootView.findViewById(R.id.tv_person_name);
            holder.iv_Selected = (ImageView)rootView.findViewById(R.id.iv_Selected);
            rootView.setTag(holder);
        }
        else
        {
            rootView=convertView;
            holder=(ViewHolder)rootView.getTag();
        }
        final SingleContact contact=getItem(position);

        final String displayName=contact.name;
        holder.tv_person_name.setText(displayName);

        if (contact.isSelected)
        {
            holder.iv_Selected.setImageResource(R.drawable.btn_checked);
        }
        else
        {
            holder.iv_Selected.setImageResource(R.drawable.btn_unchecked);
        }
        return rootView;
    }

    @Override
    public boolean doFilter(final SingleContact item, final CharSequence constraint)
    {
        if(TextUtils.isEmpty(constraint))
            return true;
        final String displayName=item.name;
        return !TextUtils.isEmpty(displayName)&&displayName.toLowerCase(Locale.getDefault())
                .contains(constraint.toString().toLowerCase(Locale.getDefault()));
    }

    @Override
    public ArrayList<SingleContact> getOriginalList()
    {
        return mContacts;
    }


    public void filter(String search_string) {

        mContacts.clear();
        Log.e(TAG, "filter search_string is " + search_string);

        if (search_string.equals(""))
        {
            mContacts.addAll(oldContacts);
        }
        else
        {
            for (SingleContact model:oldContacts)
            {
                if (model.name.toLowerCase(Locale.getDefault()).contains(search_string))
                {
                    mContacts.add(model);
                }
                else
                {
                    Log.e(TAG, "no filter matched");
                }

            }
        }
        notifyDataSetChanged();


    }
}

// /////////////////////////////////////////////////////////////////////////////////////
// ViewHolder //
// /////////////
 class ViewHolder
{
    TextView tv_person_name;
    ImageView iv_Selected;
}