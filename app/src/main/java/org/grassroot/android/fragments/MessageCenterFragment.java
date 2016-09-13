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
import org.grassroot.android.adapters.GroupChatCenterAdapter;
import org.grassroot.android.adapters.RecyclerTouchListener;
import org.grassroot.android.interfaces.ClickListener;
import org.grassroot.android.models.Message;
import org.grassroot.android.services.GcmListenerService;
import org.grassroot.android.utils.RealmUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.functions.Action1;

/**
 * Created by paballo on 2016/09/12.
 */
public class MessageCenterFragment extends Fragment {
    private GroupChatCenterAdapter groupChatCenterAdapter;
    private LinearLayoutManager viewLayoutManager;

    Unbinder unbinder;
    @BindView(R.id.message_recycler_view)
    RecyclerView recyclerView;


    public static MessageCenterFragment newInstance() {
        MessageCenterFragment fragment = new MessageCenterFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View viewToReturn = inflater.inflate(R.layout.fragment_message_center, container, false);
        unbinder = ButterKnife.bind(this, viewToReturn);
        setHasOptionsMenu(true);
//        GcmListenerService.clearChatNotifications(getContext());
        loadMessages();
        return viewToReturn;
    }

    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }


    public void loadMessages() {
        RealmUtils.loadDistinctMessages().subscribe(new Action1<List<Message>>() {
            @Override
            public void call(List<Message> msgs) {
                if (groupChatCenterAdapter == null) {
                     setUpListAndAdapter(msgs);
                } else {
                  groupChatCenterAdapter.reloadFromDb();
                }
            }
        });
    }

    private void setUpListAndAdapter(List<Message> messages) {
        groupChatCenterAdapter = new GroupChatCenterAdapter(messages);
        recyclerView.setAdapter(groupChatCenterAdapter);
        recyclerView.setHasFixedSize(false);
        viewLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(viewLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());


        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView,
                new ClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        String groupUid = groupChatCenterAdapter.getChatList().get(position).getGroupUid();
                        GroupChatFragment groupChatFragment = GroupChatFragment.newInstance(groupUid);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.gca_fragment_holder, groupChatFragment).commitAllowingStateLoss();

                    }

                    @Override
                    public void onLongClick(View view, int position) {

                    }
                })
        );
    }

}
