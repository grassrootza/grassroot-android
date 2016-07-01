package org.grassroot.android.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import java.util.List;
import org.grassroot.android.R;
import org.grassroot.android.adapters.ContactsAdapter;
import org.grassroot.android.events.ContactsLoadedEvent;
import org.grassroot.android.fragments.dialogs.PickNumberDialogFragment;
import org.grassroot.android.models.Contact;
import org.grassroot.android.services.ContactService;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Created by luke on 2016/06/07.
 */
public class ContactSelectionFragment extends Fragment
    implements PickNumberDialogFragment.PickNumberListener {

  private static final String TAG = ContactSelectionFragment.class.getSimpleName();

  public interface ContactSelectionListener {
    void onContactSelectionComplete(List<Contact> contactsSelected);
  }

  private ContactsAdapter adapter;
  private ContactSelectionListener listener;

  @BindView(R.id.cs_list_view) ListView contactListView;

  private ProgressDialog progressDialog;

  public ContactSelectionFragment() {
  }

  public static ContactSelectionFragment newInstance(List<Contact> filter, boolean filterByID) {
    ContactSelectionFragment fragment = new ContactSelectionFragment();
    ContactService.getInstance()
        .syncContactList(filter, filterByID); // so it's ready to go when needed
    return fragment;
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    try {
      listener = (ContactSelectionListener) context;
      EventBus.getDefault().register(this); // might not be triggered, but no problem is so
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("Error! Activity must implement listener");
    }
  }

  @Subscribe public void onContactsLoaded(ContactsLoadedEvent event) {
    // note : contacts might finish loading before fragment is even attached, hence null checks
    if (progressDialog != null && progressDialog.isShowing()) {
      progressDialog.dismiss();
    }
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View viewToReturn = inflater.inflate(R.layout.fragment_contact_selection, container, false);
    ButterKnife.bind(this, viewToReturn);
    adapter = new ContactsAdapter(this.getContext(), R.id.tv_person_name);
    contactListView.setAdapter(adapter);
    return viewToReturn;
  }

  @Override public void onResume() {
    super.onResume();
    if (ContactService.getInstance().contactsLoading) {
      progressDialog = new ProgressDialog(getContext());
      progressDialog.setMessage(getString(R.string.cs_loading_contacts));
      progressDialog.show();
    } else if (ContactService.getInstance().contactsFinishedLoading) {
      if (adapter != null) {
        adapter.notifyDataSetChanged();
      }
    } else {
      throw new UnsupportedOperationException(
          "Error! Selection fragment called without contact service started " + "or finished");
    }
  }

  @OnClick(R.id.cs_bt_save) public void saveAndFinish() {
    List<Contact> addedMembers = ContactService.getInstance().returnSelectedContacts();
    listener.onContactSelectionComplete(addedMembers);
  }

  @Override public void onDetach() {
    EventBus.getDefault().unregister(this);
    ContactService.getInstance().resetSelectedState(false);
    super.onDetach();
  }

  /**
   * SECTION : handle clicking on member, including asking to pick one number if multiple
   * Note : we store view and position as seems more efficient than passing around to dialogs etc
   * which should only be concerned with the contact, but can revisit
   */

  private View temporaryViewHolder;

  private void pickNumberDialog(Contact contact, int position) {
    PickNumberDialogFragment dialog = PickNumberDialogFragment.newInstance(contact, position, this);
    dialog.show(getFragmentManager(), "PickNumberDialog");
  }

  @OnItemClick(R.id.cs_list_view) public void selectMember(View view, int position) {
    final Contact contact = adapter.getItem(position);
    if (contact.isSelected) {
      adapter.toggleSelected(position, view);
    } else {
      if (contact.numbers.size() == 1) {
        if (TextUtils.isEmpty(contact.selectedNumber)) {
          contact.selectedNumber = contact.numbers.get(0).toString();
          contact.selectedMsisdn = contact.msisdns.get(0).toString();
        }
        adapter.toggleSelected(position, view);
      } else {
        temporaryViewHolder = view;
        pickNumberDialog(contact, position);
      }
    }
  }

  @Override public void onNumberPicked(final int contactPosition, final int numberIndex) {
    adapter.setSelected(contactPosition, numberIndex, temporaryViewHolder);
    temporaryViewHolder = null;
  }
}