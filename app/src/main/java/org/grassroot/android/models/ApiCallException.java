package org.grassroot.android.models;

/**
 * Created by luke on 2016/08/02.
 */
public class ApiCallException extends RuntimeException {

	public static final String PERMISSION_ERROR = "permission_error";

	public final String errorTag;

	public ApiCallException(String message) {
		super(message);
		errorTag = null;
	}

	public ApiCallException(String message, String errorTag) {
		super(message);
		this.errorTag = errorTag;
	}

}