package org.grassroot.android.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by paballo on 2016/05/05.
 */
public class GroupSearchResponse extends RealmObject {


private String status;
private Integer code;
private String message;
@SerializedName("data")
private RealmList<PublicGroupModel> data = new RealmList<>();

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
 * The data
 */
public List<PublicGroupModel> getGroups() {
        return data;
        }

/**
 *
 * @param data
 * The data
 */
public void setData(RealmList<PublicGroupModel> data) {
        this.data = data;
        }

        }
