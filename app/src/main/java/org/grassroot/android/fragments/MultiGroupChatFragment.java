package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.grassroot.android.R;
import org.grassroot.android.adapters.IncomingChatMessageAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.events.GroupChatEvent;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.Message;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.utils.RealmUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * Created by paballo on 2016/09/12.
 */
public class MultiGroupChatFragment extends Fragment {
    private static final String TAG = MultiGroupChatFragment.class.getSimpleName();

    private IncomingChatMessageAdapter incomingChatMessageAdapter;

    Unbinder unbinder;
    @BindView(R.id.message_recycler_view) RecyclerView recyclerView;


    public static MultiGroupChatFragment newInstance() {
        return new MultiGroupChatFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_message_center, container, false);
        unbinder = ButterKnife.bind(this, viewToReturn);
        setHasOptionsMenu(false);
        GcmListenerService.clearChatNotifications(getContext());
        return viewToReturn;
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle();
        loadMessages();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void loadMessages() {
        RealmUtils.loadDistinctMessages().subscribe(new Consumer<List<Message>>() {
            @Override
            public void accept(@NonNull List<Message> msgs) throws Exception {
                if (incomingChatMessageAdapter == null) {
                    incomingChatMessageAdapter = new IncomingChatMessageAdapter(getActivity(),msgs);
                } else {
                  incomingChatMessageAdapter.reloadFromDb();
                }
                setUpList();
            }
        });
    }

    private void setUpList() {
        recyclerView.setAdapter(incomingChatMessageAdapter);
        recyclerView.setHasFixedSize(false);

        LinearLayoutManager viewLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(viewLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView,
                new ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        String groupUid = incomingChatMessageAdapter.getChatList().get(position).getGroupUid();
                        String groupName = incomingChatMessageAdapter.getChatList().get(position).getGroupName();
                        GroupChatFragment groupChatFragment = GroupChatFragment.newInstance(groupUid,groupName );
                        getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .addToBackStack(TAG)
                            .replace(R.id.gca_fragment_holder, groupChatFragment).commit();
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                    }
                })
        );
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GroupChatEvent groupChatEvent){
        loadMessages();
        incomingChatMessageAdapter.notifyDataSetChanged();
    }

    private void setTitle(){
        getActivity().setTitle(getActivity().getString(R.string.chat_messages));
    }

}
