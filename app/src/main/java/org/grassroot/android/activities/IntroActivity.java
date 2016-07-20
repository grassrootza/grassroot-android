package org.grassroot.android.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.grassroot.android.BuildConfig;
import org.grassroot.android.R;
import org.grassroot.android.utils.Constant;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnPageChange;

/**
 * Created by luke on 2016/06/15.
 */
public class IntroActivity extends AppCompatActivity {

    private static final String TAG = IntroActivity.class.getSimpleName();

    IntroAdapter introAdapter;
    @BindView(R.id.intro_view_pager)
    ViewPager viewPager;

    @BindView(R.id.intro_iv_logo)
    ImageView logoImage;

    @BindView(R.id.intro_ll_buttons)
    LinearLayout buttonLayout;

    private int[] titles;
    private int[] messages;
    private boolean inCreation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_IntroScreen);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_intro);
        ButterKnife.bind(this);

        // todo : add tablet layout / options
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        titles = new int[] {
                R.string.intro_page0_title,
                R.string.intro_page1_title,
                R.string.intro_page2_title,
                R.string.intro_page3_title
        };

        messages = new int[] {
                R.string.intro_page0_msg,
                R.string.intro_page1_msg,
                R.string.intro_page2_msg,
                R.string.intro_page3_msg
        };

        introAdapter = new IntroAdapter();
        viewPager.setAdapter(introAdapter);
        viewPager.setPageMargin(0);
        viewPager.setOffscreenPageLimit(1);

        inCreation = true;
    }

    @OnPageChange(value = R.id.intro_view_pager, callback = OnPageChange.Callback.PAGE_SELECTED)
    public void onPageSelected(int position) {
        ViewGroup tracker = (ViewGroup) viewPager.findViewById(R.id.page_tracker);
        int counter = tracker.getChildCount();
        for (int i = 0; i < counter; i++) {
            tracker.getChildAt(i).setBackgroundResource(i == position ? R.color.primaryColor : R.color.text_grey);
        }
    }

    @OnClick(R.id.intro_bt_login)
    public void onLoginClicked() {
        Intent i = new Intent(this, LoginRegisterActivity.class);
        i.putExtra("default_to_login", true);
        startActivity(i);
    }

    @OnClick(R.id.intro_bt_register)
    public void onRegisterClicked() {
        Intent i = new Intent(this, LoginRegisterActivity.class);
        i.putExtra("default_to_login",false);
        startActivity(i);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (inCreation) {
            animateInto();
            inCreation = false;
        }
    }

    private void animateInto() {

        if (Build.VERSION.SDK_INT >= 14) {

            final int heightShift = -(getApplicationContext().getResources().getDisplayMetrics().heightPixels) / 3;

            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f);
            PropertyValuesHolder pushUp = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, heightShift);

            // create an animator even though not visible, so it moves with the logo (else lots of layout fiddly errors)
            // i do not have the faintest clue what's happening with the heights in here, so ...

            ObjectAnimator pagerAnim = ObjectAnimator.ofFloat(viewPager, "y", heightShift);
            ObjectAnimator logoAnim = ObjectAnimator.ofPropertyValuesHolder(logoImage, scaleX, scaleY, pushUp);

            logoAnim.setDuration(Constant.mediumDelay);
            logoAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    buttonLayout.setVisibility(View.VISIBLE);

                    viewPager.setVisibility(View.VISIBLE);
                    viewPager.setCurrentItem(0);
                }
            });

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(Constant.mediumDelay);
            animatorSet.playTogether(logoAnim, pagerAnim);
            animatorSet.start();

        } else {
            // todo : move the logo
            buttonLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            viewPager.setCurrentItem(0);
        }
    }

    public class IntroAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return titles.length;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = View.inflate(container.getContext(), R.layout.intro_text_layout, null);
            TextView title = (TextView) view.findViewById(R.id.header_text);
            TextView msg = (TextView) view.findViewById(R.id.message_text);
            container.addView(view, 0);

            title.setText(titles[position]);
            msg.setText(messages[position]);

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }
    }

}
