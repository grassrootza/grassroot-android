package org.grassroot.android.services.model;


public class Token {

    private String code;
    private Long createdDateTime;
    private Long expiryDateTime;

    /**
     *
     * @return
     * The code
     */
    public String getCode() {
        return code;
    }

    /**
     *
     * @param code
     * The code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     *
     * @return
     * The createdDateTime
     */
    public Long getCreatedDateTime() {
        return createdDateTime;
    }

    /**
     *
     * @param createdDateTime
     * The createdDateTime
     */
    public void setCreatedDateTime(Long createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    /**
     *
     * @return
     * The expiryDateTime
     */
    public Long getExpiryDateTime() {
        return expiryDateTime;
    }

    /**
     *
     * @param expiryDateTime
     * The expiryDateTime
     */
    public void setExpiryDateTime(Long expiryDateTime) {
        this.expiryDateTime = expiryDateTime;
    }

}
