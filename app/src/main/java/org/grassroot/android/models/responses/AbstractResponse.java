package org.grassroot.android.models.responses;

/**
 * Created by luke on 2016/09/01.
 */
public class AbstractResponse {

	protected String status;
	protected Integer code;
	protected String message;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "AbstractResponse{" +
				"status='" + status + '\'' +
				", code=" + code +
				", message='" + message + '\'' +
				'}';
	}
}
