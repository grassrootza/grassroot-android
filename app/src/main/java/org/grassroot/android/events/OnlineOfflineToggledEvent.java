package org.grassroot.android.events;

/**
 * Created by luke on 2016/07/23.
 */
public class OnlineOfflineToggledEvent {

    public final boolean isOnline;

    public OnlineOfflineToggledEvent(boolean isOnline) {
        this.isOnline = isOnline;
    }

}
