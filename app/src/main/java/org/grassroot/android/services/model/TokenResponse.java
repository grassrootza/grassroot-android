package org.grassroot.android.services.model;


import com.google.gson.annotations.SerializedName;

public class TokenResponse {

    private String status;
    private Integer code;
    private String message;
    @SerializedName("data")
    private Token token;
    private Boolean hasGroups;
    private String displayName;


    public TokenResponse() {

    }

    /**
     *
     * @return
     * The status
     */
    public String getStatus() {
        return status;
    }

    /**
     *
     * @param status
     * The status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     *
     * @return
     * The code
     */
    public Integer getCode() {
        return code;
    }

    /**
     *
     * @param code
     * The code
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     *
     * @return
     * The message
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     * @param message
     * The message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     *
     * @return
     * The token
     */
    public Token getToken() {
        return token;
    }

    /**
     *
     * @param token
     * The token
     */
    public void setToken(Token token) {
        this.token = token;
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


