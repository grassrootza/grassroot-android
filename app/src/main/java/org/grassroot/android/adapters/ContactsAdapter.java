package org.grassroot.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Contact;
import org.grassroot.android.services.ContactService;

import java.util.List;

/**
 * Created by luke on 2016/06/07.
 */
public class ContactsAdapter extends ArrayAdapter<Contact> implements SectionIndexer{

    private static final String TAG = ContactsAdapter.class.getSimpleName();

    private final Context mContext;

    private List<Contact> contactsToDisplay;
    private StringArrayAlphabetIndexer indexer; // todo : move this to service

    public ContactsAdapter(Context context, int resource) {
        super(context, resource);
        mContext = context;
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

        final Contact contact = ContactService.getInstance().displayedContacts.get(position);
        viewHolder.tvContactName.setText(contact.getDisplayName());
        viewHolder.ivContactSelected.setImageResource(contact.isSelected ? R.drawable.btn_checked : R.drawable.btn_unchecked);

        return viewToReturn;
    }

    public void toggleSelected(int position, View view) {
        final Contact contact = ContactService.getInstance().displayedContacts.get(position);
        contact.isSelected = !contact.isSelected;
        ContactViewHolder holder = (ContactViewHolder) view.getTag();
        holder.ivContactSelected.setImageResource(contact.isSelected ? R.drawable.btn_checked : R.drawable.btn_unchecked);
    }

    public void setSelected(int contactPosition, int selectedNumberIndex,View viewHolder) {
        final Contact contact = ContactService.getInstance().displayedContacts.get(contactPosition);
        contact.selectedNumber = contact.numbers.get(selectedNumberIndex);
        contact.selectedMsisdn = contact.msisdns.get(selectedNumberIndex);
        contact.isSelected = true;
        ContactViewHolder holder = (ContactViewHolder) viewHolder.getTag(); // just do notify item changed ??
        holder.ivContactSelected.setImageResource(R.drawable.btn_checked);
    }

    @Override
    public Contact getItem(int position) {
        return ContactService.getInstance().displayedContacts.get(position);
    }

    @Override
    public int getCount() {
        if (ContactService.getInstance().displayedContacts == null) {
            return 0;
        } else {
            return ContactService.getInstance().displayedContacts.size();
        }
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
