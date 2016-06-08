package org.grassroot.android.models;

public class GenericResponse {

    private String status;
    private Integer code;
    private String message;
    private Object data;
    private Boolean hasGroups;
    private String displayName;

    /**
     * @return The status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status The status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    public boolean wasSuccessful() { return "SUCCESS".equals(status); } // todo: check constant/code instead

    /**
     * @return The code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * @param code The code
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message The message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return The data
     */
    public Object getData() {
        return data;
    }

    /**
     * @param data The data
     */
    public void setData(Object data) {
        this.data = data;
    }

    public Boolean getHasGroups() {
        return hasGroups;
    }

    public void setHasGroups(Boolean hasGroups) {
        this.hasGroups = hasGroups;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
