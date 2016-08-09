package org.grassroot.android.models.exceptions;

import android.util.Log;

/**
 * Created by luke on 2016/08/02.
 */
public class ApiCallException extends RuntimeException {

	public static final String PERMISSION_ERROR = "permission_error";

	public final String errorTag;
	public final Object data;

	public ApiCallException(String message) {
		super(message);
		errorTag = null;
		data = null;
	}

	public ApiCallException(String message, String errorTag) {
		super(message);
		this.errorTag = errorTag;
		this.data = null;
	}

	public ApiCallException(String message, String errorTag, Object data) {
		super(message);
		Log.e("ApiCallException", "exception, with errorTag = " + errorTag);
		this.errorTag = errorTag;
		this.data = data;
	}

}