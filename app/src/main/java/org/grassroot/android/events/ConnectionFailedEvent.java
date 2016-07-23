package org.grassroot.android.events;

/**
 * Created by luke on 2016/07/23.
 */
public class ConnectionFailedEvent {

    final String typeOfFailure;

    public ConnectionFailedEvent(String typeOfFailure) {
        this.typeOfFailure = typeOfFailure;
    }

}
