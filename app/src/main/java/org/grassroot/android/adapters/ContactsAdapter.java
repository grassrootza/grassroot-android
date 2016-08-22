package org.grassroot.android.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import java.util.List;
import org.grassroot.android.R;
import org.grassroot.android.models.Contact;
import org.grassroot.android.services.ContactService;

import rx.functions.Action1;

/**
 * Created by luke on 2016/06/07.
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

  private static final String TAG = ContactsAdapter.class.getSimpleName();

  private final Context mContext;
  private final ContactsAdapterListener listener;

  public interface ContactsAdapterListener {
    void contactClicked(Contact contact, int position);
  }

  public ContactsAdapter(Context context, ContactsAdapterListener listener) {
    mContext = context;
    this.listener = listener;
  }

  @Override
  public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View viewToReturn = LayoutInflater.from(mContext).inflate(R.layout.row_contact_list, parent, false);
    return new ContactViewHolder(viewToReturn);
  }

  @Override
  public void onBindViewHolder(final ContactViewHolder holder, int position) {

    final Contact contact = ContactService.getInstance().displayedContacts.get(position);
    holder.tvContactName.setText(contact.getDisplayName());
    holder.ivContactSelected.setImageResource(contact.isSelected ? R.drawable.btn_checked : R.drawable.btn_unchecked);

    holder.contactView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        listener.contactClicked(contact, holder.getAdapterPosition());
      }
    });
  }

  public void toggleSelected(final int position) {
    ContactService.getInstance().toggleContactSelected(position).subscribe(new Action1<Boolean>() {
      @Override
      public void call(Boolean aBoolean) {
        notifyItemChanged(position);
      }
    });
  }

  public void setSelected(final int contactPosition, int selectedNumberIndex) {
    ContactService.getInstance().setContactSelected(contactPosition, selectedNumberIndex)
        .subscribe(new Action1<Boolean>() {
          @Override
          public void call(Boolean aBoolean) {
            notifyItemChanged(contactPosition);
          }
        });
  }

  @Override
  public int getItemCount() {
    if (ContactService.getInstance().displayedContacts == null) {
      return 0;
    } else {
      return ContactService.getInstance().displayedContacts.size();
    }
  }

  public static class ContactViewHolder extends RecyclerView.ViewHolder {

    public final View contactView;

    TextView tvContactName;
    ImageView ivContactSelected;

    public ContactViewHolder(View rootView) {
      super(rootView);
      contactView = rootView;
      tvContactName = (TextView) rootView.findViewById(R.id.tv_person_name);
      ivContactSelected = (ImageView) rootView.findViewById(R.id.iv_Selected);

    }
  }
}
