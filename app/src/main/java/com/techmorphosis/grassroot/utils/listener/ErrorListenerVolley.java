package com.techmorphosis.grassroot.utils.listener;

import com.android.volley.VolleyError;

public interface ErrorListenerVolley {
    void onError(VolleyError volleyError);
}
