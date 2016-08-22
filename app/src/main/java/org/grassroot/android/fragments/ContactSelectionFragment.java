package org.grassroot.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.adapters.ContactsAdapter;
import org.grassroot.android.events.PhoneContactsChanged;
import org.grassroot.android.fragments.dialogs.PickNumberDialogFragment;
import org.grassroot.android.models.Contact;
import org.grassroot.android.services.ContactService;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by luke on 2016/06/07.
 */
public class ContactSelectionFragment extends Fragment
    implements PickNumberDialogFragment.PickNumberListener, ContactsAdapter.ContactsAdapterListener {

  private static final String TAG = ContactSelectionFragment.class.getSimpleName();

  public interface ContactSelectionListener {
    void onContactSelectionComplete(List<Contact> contactsSelected);
  }

  private ContactsAdapter adapter;
  private ContactSelectionListener listener;

  @BindView(R.id.cs_list_view) RecyclerView contactListView;

  public ContactSelectionFragment() {
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    try {
      listener = (ContactSelectionListener) context;
      EventBus.getDefault().register(this);
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("Error! Activity must implement listener");
    }
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View viewToReturn = inflater.inflate(R.layout.fragment_contact_selection, container, false);
    ButterKnife.bind(this, viewToReturn);

    adapter = new ContactsAdapter(this.getContext(), this);
    contactListView.setAdapter(adapter);
    contactListView.setLayoutManager(new LinearLayoutManager(this.getContext()));
    contactListView.setHasFixedSize(true);
    contactListView.setItemAnimator(null); // otherwise annoying slow motion tick
    contactListView.setItemViewCacheSize(20);

    return viewToReturn;
  }

  @OnClick(R.id.cs_bt_save) public void saveAndFinish() {
    List<Contact> addedMembers = ContactService.getInstance().returnSelectedContacts();
    listener.onContactSelectionComplete(addedMembers);
  }

  @Override public void onDetach() {
    EventBus.getDefault().unregister(this);
    ContactService.getInstance().resetSelectedState(false); // should only get called on activity destroyed, but watch out and possibly move to activity itself
    super.onDetach();
  }

  /**
   * SECTION : handle clicking on member, including asking to pick one number if multiple
   * Note : we store view and position as seems more efficient than passing around to dialogs etc
   * which should only be concerned with the contact, but can revisit
   */

  private void pickNumberDialog(Contact contact, int position) {
    PickNumberDialogFragment dialog = PickNumberDialogFragment.newInstance(contact, position, this);
    dialog.show(getFragmentManager(), "PickNumberDialog");
  }

  @Override
  public void contactClicked(Contact contact, int position) {
    if (contact.isSelected) {
      adapter.toggleSelected(position);
    } else {
      if (contact.numbers.size() == 1) {
        if (TextUtils.isEmpty(contact.selectedNumber)) {
          contact.selectedNumber = contact.numbers.get(0);
          contact.selectedMsisdn = contact.msisdns.get(0);
        }
        adapter.toggleSelected(position);
      } else {
        pickNumberDialog(contact, position);
      }
    }
  }

  @Override public void onNumberPicked(final int contactPosition, final int numberIndex) {
    adapter.setSelected(contactPosition, numberIndex);
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onEvent(PhoneContactsChanged e) {
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
  }
}