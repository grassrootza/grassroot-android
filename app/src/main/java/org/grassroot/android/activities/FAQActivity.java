package org.grassroot.android.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import org.grassroot.android.R;
import org.grassroot.android.adapters.FAQAdapter;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by karthik on 16-09-2015.
 */
public class FAQActivity extends PortraitActivity{

    @BindView(R.id.listview_faq)
    ExpandableListView faqListview;
    @BindView(R.id.rl_root_faq)
    LinearLayout rlRootFaq;
    @BindView(R.id.fq_toolbar)
    Toolbar fqToolbar;
    private List<String> listDataHeader;
    private List<String> listDataChild;
    private FAQAdapter faqAdapter;

    private int lastExpandedPosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);
        ButterKnife.bind(this);
        setUpToolbar();
        init();;

    }

    private void setUpToolbar() {
        fqToolbar.setNavigationIcon(R.drawable.btn_back_wt);
        fqToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public void onBackPressed() {
        finish();
    }

    public void init() {
        generateData();
        faqListview.setGroupIndicator(null);
        faqAdapter = new FAQAdapter(getApplicationContext(), listDataHeader, listDataChild);
        faqListview.setAdapter(faqAdapter);

    }

    private void generateData() {
        String[] fq_qstn = getResources().getStringArray(R.array.fq_Header);
        listDataHeader = Arrays.asList(fq_qstn);

        // Answers
        String[] fq_ans = getResources().getStringArray(R.array.fq_Answer);
        listDataChild = Arrays.asList(fq_ans);
        faqListview.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (lastExpandedPosition != -1
                        && groupPosition != lastExpandedPosition) {
                    faqListview.collapseGroup(lastExpandedPosition);
                }
                lastExpandedPosition = groupPosition;
            }
        });

    }

}
