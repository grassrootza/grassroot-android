package com.techmorphosis.grassroot.Network;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.techmorphosis.grassroot.utils.UtilClass;
import com.techmorphosis.grassroot.utils.listener.ErrorListenerVolley;
import com.techmorphosis.grassroot.utils.listener.ResponseListenerVolley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class NetworkCall {

    // Class variables
    private static final String TAG = "NetworkCall";
    private Context mContext;
    private ProgressDialog prgDialog;
    private ResponseListenerVolley responseListenerVolley;
    private ErrorListenerVolley errorListenerVolley;
    private String link;
    RequestQueue requestQueue;
    UtilClass utilClass;


    public NetworkCall(Context mContext, ResponseListenerVolley responseListenerVolley,
                       ErrorListenerVolley errorListenerVolley, String link, String prgMessage, boolean showProgress) {

        this.mContext = mContext;
        this.responseListenerVolley = responseListenerVolley;
        this.errorListenerVolley = errorListenerVolley;
        requestQueue = Volley.newRequestQueue(mContext);
        this.link = link;

        prgDialog = new ProgressDialog(mContext);
        prgDialog.setMessage(prgMessage);
        if (showProgress)
        {
            prgDialog.show();
        }
        prgDialog.setCancelable(false);
    }

    public void makeStringRequest_GET() {
        utilClass = new UtilClass();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, link, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, " makeRequest() onResponse " + response);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Log.e(TAG,"fine");
                    if (mContext!=null) {
                        responseListenerVolley.onSuccess(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "not fine");
                     VolleyError volleyError=new NoConnectionError();
                    if (mContext!=null) {
                        NetworkCall.this.errorListenerVolley.onError(volleyError);
                    }
                }

                prgDialog.dismiss();
                requestQueue.stop();

            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                Log.e(TAG,"b4 volleyError is " + volleyError);
                Log.e(TAG,"b4 volleyError message  is " + volleyError.toString());
                Log.e(TAG,"b4 volleyError getClass  is " + volleyError.getClass().getSimpleName());


                if (mContext!=null) {

                    if (volleyError instanceof NoConnectionError) {
                        if (volleyError.getMessage().contains("No authentication challenges found")) {//AuthFailureError
                            // This is the error.  You probably will want to handle any IOException, not just those with the same message.
                            volleyError = new AuthFailureError();
                            NetworkCall.this.errorListenerVolley.onError(volleyError);

                        } else {//NoConnectionError
                            NetworkCall.this.errorListenerVolley.onError(volleyError);
                        }
                    } else
                    {
                        NetworkCall.this.errorListenerVolley.onError(volleyError);
                    }

                }
                Log.e(TAG,"volleyError is " + volleyError);
                Log.e(TAG,"volleyError message  is " + volleyError.toString());
                Log.e(TAG,"volleyError getClass  is " + volleyError.getClass().getSimpleName());

                prgDialog.dismiss();
                requestQueue.stop();

            }
        }
        );

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 0));

        // Adding request to request queue
        requestQueue.add(stringRequest);
    }


    public void makeStringRequest_POST(final HashMap params) {
        utilClass = new UtilClass();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, link, new Response.Listener<String>()
        {
            @Override
            public void onResponse(String response) {

                Log.e(TAG, " makeRequest() onResponse " + response);

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Log.e(TAG,"fine");
                    if (mContext!=null) {
                        responseListenerVolley.onSuccess(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "not fine");
                    VolleyError volleyError=new NoConnectionError();
                    if (mContext!=null) {
                        NetworkCall.this.errorListenerVolley.onError(volleyError);
                    }
                }


                prgDialog.dismiss();
                requestQueue.stop();

            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                NetworkCall.this.errorListenerVolley.onError(volleyError);

                Log.e(TAG,"b4 volleyError is " + volleyError);
                Log.e(TAG,"b4 volleyError message  is " + volleyError.toString());
                Log.e(TAG, "b4 volleyError getClass  is " + volleyError.getClass().getSimpleName());

                if (mContext!=null) {

                    if (volleyError instanceof NoConnectionError) {
                        if (volleyError.getMessage().contains("No authentication challenges found")) {//AuthFailureError
                            // This is the error.  You probably will want to handle any IOException, not just those with the same message.
                            volleyError = new AuthFailureError();
                            NetworkCall.this.errorListenerVolley.onError(volleyError);

                        } else {//NoConnectionError
                            NetworkCall.this.errorListenerVolley.onError(volleyError);
                        }
                    }
                    else {

                        NetworkCall.this.errorListenerVolley.onError(volleyError);

                    }

                }
                Log.e(TAG,"volleyError is " + volleyError);
                Log.e(TAG,"volleyError message  is " + volleyError.toString());
                Log.e(TAG,"volleyError getClass  is " + volleyError.getClass().getSimpleName());


                prgDialog.dismiss();
                requestQueue.stop();

            }
        }
        )
        {



            protected Map getParams() throws AuthFailureError
            {

                return params;
            }


        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 0));

        // Adding request to request queue
        requestQueue.add(stringRequest);
    }



}
