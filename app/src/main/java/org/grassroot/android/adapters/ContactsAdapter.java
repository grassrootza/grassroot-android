package org.grassroot.android.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContactsAdapter extends BaseAdapter implements Filterable, SectionIndexer {

    private static final String TAG = ContactsAdapter.class.getCanonicalName();

    private final Context mcontext;

    private ArrayList<Contact> displayedContacts;
    private ArrayList<Contact> allContacts;

    private StringArrayAlphabetIndexer mIndexer;
    private final Filter mFilter;

    public ContactsAdapter(final ArrayList<Contact> contacts, Context context) {

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

            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults results) {
                lastConstraint = constraint == null ? null : constraint;
                if (!TextUtils.equals(constraint, lastConstraint)) {
                    displayedContacts = results == null ? null : (ArrayList<Contact>) results.values;
                    notifyDataSetChanged();
                }
            }
        };
    }

    public void setData(final ArrayList<Contact> contacts) {
        this.displayedContacts = contacts;
        allContacts = new ArrayList<>();
        allContacts.addAll(contacts);
        final String[] generatedContactNames= generateContactNames(contacts);
        mIndexer = new StringArrayAlphabetIndexer(generatedContactNames, true);
    }

    private String[] generateContactNames(final List<Contact> contacts) {
        final ArrayList<String> contactNames=new ArrayList<>();
        if(contacts!=null)
            for(final Contact contactEntity : contacts)
                contactNames.add(contactEntity.name);
        return contactNames.toArray(new String[contactNames.size()]);
    }

    @Override
    public int getPositionForSection(final int sectionIndex) {
        return mIndexer.getPositionForSection(sectionIndex);
    }

    @Override
    public int getSectionForPosition(final int position) {
        return mIndexer.getSectionForPosition(position);
    }

    @Override
    public Object[] getSections() {
        return mIndexer.getSections();
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    @Override
    public int getCount() {
        return displayedContacts.size();
    }

    @Override
    public Contact getItem(int i) {
        return displayedContacts.get(i);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position,final View convertView,final ViewGroup parent) {

        final ViewHolder holder;
        final View rootView;
        final Contact contact=getItem(position);

        if (convertView != null) {
            rootView = convertView;
            holder = (ViewHolder) rootView.getTag();
        } else {
            holder = new ViewHolder();
            rootView = LayoutInflater.from(mcontext).inflate(R.layout.listview_item,parent,false);
            holder.tv_person_name = (TextView)rootView.findViewById(R.id.tv_person_name);
            holder.iv_Selected = (ImageView)rootView.findViewById(R.id.iv_Selected);
            rootView.setTag(holder);
        }

        holder.tv_person_name.setText(contact.name);
        holder.iv_Selected.setImageResource(contact.isSelected ? R.drawable.btn_checked : R.drawable.btn_unchecked);

        return rootView;
    }

    public boolean doFilter(final Contact item, final CharSequence constraint) {
        if(TextUtils.isEmpty(constraint))
            return true;
        final String displayName=item.name;
        return !TextUtils.isEmpty(displayName) &&
                displayName.toLowerCase(Locale.getDefault()).contains(constraint.toString().toLowerCase(Locale.getDefault()));
    }

    public ArrayList<Contact> getOriginalList()
    {
        return allContacts;
    }

    /*
    ViewHolder class : todo : see if a better way to do this ..
     */
    class ViewHolder {
        TextView tv_person_name;
        ImageView iv_Selected;
    }

    /*
    NOTE : this is only method preserved from prior cut-and-paste job, as may be useful if we decide
    to implement section headers in future (though, unlikely -- large A/B/ etc much better)

    public CharSequence getSectionTitle(int sectionIndex) {
        return ((StringArrayAlphabetIndexer.AlphaBetSection) getSections()[sectionIndex]).getName();
    }

    protected void bindSectionHeader(final TextView headerView, final View dividerView, final int position) {

        final int sectionIndex = getSectionForPosition(position);
        if (getPositionForSection(sectionIndex) == position) {
            final CharSequence title = getSectionTitle(sectionIndex);
            headerView.setText(title);
            headerView.setVisibility(View.VISIBLE);
            if (dividerView != null)
                dividerView.setVisibility(View.GONE);
        } else {
            headerView.setVisibility(View.GONE);
            if (dividerView != null)
                dividerView.setVisibility(View.VISIBLE);
        }
        // move the divider for the last item in a section
        if (dividerView != null)
            if (getPositionForSection(sectionIndex + 1) - 1 == position)
                dividerView.setVisibility(View.GONE);
            else
                dividerView.setVisibility(View.VISIBLE);
        if (!mHeaderViewVisible)
            headerView.setVisibility(View.GONE);
    }*/
}