package com.xianhai.newsflash;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

/**
 * Created by xianhai on 2/3/18.
 */

public class StringRequestRetry extends StringRequest {

    public StringRequestRetry(int get, String url, Response.Listener<String> listener,
                              Response.ErrorListener errorListener) {
        super(url, listener, errorListener);
        setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                20, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }
}