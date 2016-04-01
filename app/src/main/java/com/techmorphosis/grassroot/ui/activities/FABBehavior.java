package com.techmorphosis.grassroot.ui.activities;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;

/**
 * Created by admin on 31-Mar-16.
 */
public class FABBehavior extends FloatingActionButton.Behavior {

    public FABBehavior() {
    }

    public FABBehavior(Context context, AttributeSet attributeSet) {
    }

    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        if(dependency instanceof Snackbar.SnackbarLayout) {
//           / return super.onDependentViewChanged(parent, child, dependency);
        } else if(dependency instanceof AppBarLayout) {
            this.updateFabVisibility(parent, (AppBarLayout)dependency, child);
        }

        return false;
    }

    private boolean updateFabVisibility(CoordinatorLayout parent, AppBarLayout appBarLayout, FloatingActionButton child) {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)child.getLayoutParams();
        if(lp.getAnchorId() != appBarLayout.getId()) {
            return false;
        } else {

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
            int point = child.getTop() - params.topMargin;
            try {
                Method method = AppBarLayout.class.getDeclaredMethod("getMinimumHeightForVisibleOverlappingContent");
                method.setAccessible(true);
                if(point <= (int) method.invoke(appBarLayout)) {
                    child.hide();
                     //   Create_Group.menu2.setVisibility(View.INVISIBLE);
                } else {
                    child.show();
                    //if (child.isShown())
                   // Create_Group.menu2.setVisibility(View.VISIBLE);

                }
                return true;
            } catch (Exception e) {
                return true;
            }
        }
    }
}
