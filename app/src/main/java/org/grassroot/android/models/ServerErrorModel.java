package org.grassroot.android.models;

/**
 * Created by luke on 2016/08/07.
 * note : be very careful including this in inheritance chain as with rest of responses, given the custom adapter it uses and
 * its importance for digesting and relaying API errors
 */
public class ServerErrorModel {

	private String status;
	private Integer code;
	private String message;
	private Object data;

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

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "ServerErrorModel{" +
			"status='" + status + '\'' +
			", code=" + code +
			", message='" + message + '\'' +
			", data=" + data +
			'}';
	}
}
