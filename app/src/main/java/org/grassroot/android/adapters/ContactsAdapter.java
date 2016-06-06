package org.grassroot.android.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.grassroot.android.ContactLib.BaseSectionedListViewAdapter;
import org.grassroot.android.ContactLib.StringArrayAlphabetIndexer;
import org.grassroot.android.R;
import org.grassroot.android.models.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by admin on 04-Apr-16.
 */
public class ContactsAdapter extends BaseSectionedListViewAdapter implements Filterable {

    public  Context mcontext;
    private LayoutInflater mInflater;
    private ArrayList<Contact> mContacts;

    private String TAG= ContactsAdapter.class.getSimpleName();
    ArrayList<Contact> oldContacts;

    private ArrayList<Contact> mFilterListCopy;
    private final Filter mFilter;

    @Override
    public CharSequence getSectionTitle(int sectionIndex) {
        return ((StringArrayAlphabetIndexer.AlphaBetSection) getSections()[sectionIndex]).getName();
    }

    public ContactsAdapter(final ArrayList<Contact> contacts, Context context) {
        mInflater=LayoutInflater.from(context);
        this.mcontext=context;
        setData(contacts);

        mFilter = new Filter() {
            CharSequence lastConstraint = null;

            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                if (constraint == null || constraint.length() == 0)
                    return null;
                final ArrayList<Contact> newFilterArray = new ArrayList<Contact>();
                final FilterResults results = new FilterResults();
                for (final Contact item : getOriginalList())
                    if (doFilter(item, constraint))
                        newFilterArray.add(item);
                results.values = newFilterArray;
                results.count = newFilterArray.size();
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults results) {
                mFilterListCopy = results == null ? null : (ArrayList<Contact>) results.values;
                final boolean needRefresh = !TextUtils.equals(constraint, lastConstraint);
                lastConstraint = constraint == null ? null : constraint;
                if (needRefresh)
                    notifyDataSetChanged();
            }
        };
    }

    public void setData(final ArrayList<Contact> contacts) {
        this.mContacts=contacts;
        oldContacts = new ArrayList<>();
        oldContacts.addAll(contacts);
        final String[] generatedContactNames=generateContactNames(contacts);
        setSectionIndexer(new StringArrayAlphabetIndexer(generatedContactNames,true));
    }

    private String[] generateContactNames(final List<Contact> contacts)
    {
        final ArrayList<String> contactNames=new ArrayList<>();
        if(contacts!=null)
            for(final Contact contactEntity : contacts)
                contactNames.add(contactEntity.name);
        return contactNames.toArray(new String[contactNames.size()]);
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    @Override
    public int getCount() {
        return mContacts.size();
    }

    @Override
    public Contact getItem(int i) {
        return mContacts.get(i);
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
        final Contact contact=getItem(position);

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

    public boolean doFilter(final Contact item, final CharSequence constraint) {
        if(TextUtils.isEmpty(constraint))
            return true;
        final String displayName=item.name;
        return !TextUtils.isEmpty(displayName)&&displayName.toLowerCase(Locale.getDefault())
                .contains(constraint.toString().toLowerCase(Locale.getDefault()));
    }

    public ArrayList<Contact> getOriginalList()
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
            for (Contact model:oldContacts)
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

    // /////////////////////////////////////////////////////////////////////////////////////
    // ViewHolder //
    // /////////////
    class ViewHolder
    {
        TextView tv_person_name;
        ImageView iv_Selected;
    }
}