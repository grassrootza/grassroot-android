package org.grassroot.android.models.exceptions;

import org.grassroot.android.utils.ErrorUtils;
import org.grassroot.android.utils.NetworkUtils;

/**
 * Created by luke on 2016/08/09.
 */
public class InvalidNumberException extends ApiCallException {

	/*
	Pass this a list of the numbers, separated by a space ... Rx Java's onError message is wiping
	enriched info from exceptions, hence have to do this ...
	 */
	public InvalidNumberException(final String incorrectNumbers) {
		super(NetworkUtils.SERVER_ERROR, ErrorUtils.GROUP_MEMBER_INVALID_PHONE, incorrectNumbers);
	}

}
