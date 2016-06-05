package org.grassroot.android.services;

/**
 * Created by luke on 2016/05/15.
 */
public class NoConnectivityException extends RuntimeException {

    private String uriAttempted;

    public NoConnectivityException(String s, String uriAttempted) {
        super(s);
        this.uriAttempted = uriAttempted;
    }

    public String getUriAttempted() { return uriAttempted; }
}
