package org.grassroot.android.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Contact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by luke on 2016/06/07.
 */
public class ContactsAdapter2 extends ArrayAdapter<Contact> implements SectionIndexer{

    private static final String TAG = ContactsAdapter2.class.getCanonicalName();

    private final Context mContext;

    private List<Contact> contactsToDisplay;
    private StringArrayAlphabetIndexer indexer;

    public ContactsAdapter2(Context context, int resource) {
        super(context, resource);
        mContext = context;
    }

    public void setContactsToDisplay(List<Contact> contacts) {
        Log.e(TAG, "setting contacts to display, they look like: " + contacts.toString());
        contactsToDisplay = contacts;
        indexer = new StringArrayAlphabetIndexer(extractContactNames(), true);
        notifyDataSetChanged();
    }

    private String[] extractContactNames() {
        final ArrayList<String> contactNames = new ArrayList<>();
        if(contactsToDisplay != null)
            for (final Contact contactEntity : contactsToDisplay)
                contactNames.add(contactEntity.name);
        return contactNames.toArray(new String[contactNames.size()]);
    }

    @Override
    public View getView(final int position,final View convertView,final ViewGroup parent) {

        final View viewToReturn;
        final ContactViewHolder viewHolder;

        if (convertView != null) {
            try {
                viewToReturn = convertView;
                viewHolder = (ContactViewHolder) convertView.getTag();
            } catch (ClassCastException e) {
                throw new UnsupportedOperationException("Error! Contact list adapter passed wrong kind of view");
            }
        } else {
            viewToReturn = LayoutInflater.from(mContext).inflate(R.layout.listview_item, parent, false);
            viewHolder = new ContactViewHolder(viewToReturn);
            viewHolder.tvContactName = (TextView) viewToReturn.findViewById(R.id.tv_person_name);
            viewHolder.ivContactSelected = (ImageView) viewToReturn.findViewById(R.id.iv_Selected);
        }

        final Contact contact = contactsToDisplay.get(position);
        viewHolder.tvContactName.setText(contact.name);
        viewHolder.ivContactSelected.setImageResource(contact.isSelected ? R.drawable.btn_checked : R.drawable.btn_unchecked);

        // todo : add item click listener here

        return viewToReturn;
    }

    public void toggleSelected(int position, View view) {
        final Contact contact = contactsToDisplay.get(position);
        contact.isSelected = !contact.isSelected;
        ContactViewHolder holder = (ContactViewHolder) view.getTag();
        holder.ivContactSelected.setImageResource(contact.isSelected ? R.drawable.btn_checked : R.drawable.btn_unchecked);
    }

    public void setSelected(int position, View view, CharSequence selectedNumber) {
        final Contact contact = contactsToDisplay.get(position);
        contact.selectedNumber = selectedNumber.toString();
        contact.isSelected = true;
        ContactViewHolder holder = (ContactViewHolder) view.getTag();
        holder.ivContactSelected.setImageResource(R.drawable.btn_checked);
    }

    public List<Contact> getSelectedContacts() {
        List<Contact> toReturn = new ArrayList<>();
        for (Contact c : contactsToDisplay) { // hmm, once do filters, may need to use whole list
            if (c.isSelected)
                toReturn.add(c);
        }
        return toReturn;
    }

    @Override
    public Contact getItem(int position) {
        return contactsToDisplay == null ? null : contactsToDisplay.get(position);
    }

    @Override
    public int getCount() {
        return (contactsToDisplay == null) ? 0 : contactsToDisplay.size();
    }

    @Override
    public Object[] getSections() {
        return (indexer == null) ? null : indexer.getSections();
    }

    @Override
    public int getPositionForSection(int i) {
        return (indexer == null) ? -1 : indexer.getPositionForSection(i);
    }

    @Override
    public int getSectionForPosition(int i) {
        return (indexer == null) ? -1 : indexer.getSectionForPosition(i);
    }

    public static class ContactViewHolder {

        public final View contactView;

        TextView tvContactName;
        ImageView ivContactSelected;

        public ContactViewHolder(View rootView) {
            contactView = rootView;
            contactView.setTag(this);
        }

    }
}
