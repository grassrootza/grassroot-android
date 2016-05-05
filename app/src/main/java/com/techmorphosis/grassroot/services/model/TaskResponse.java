package com.techmorphosis.grassroot.services.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paballo on 2016/05/05.
 */
public class TaskResponse {

    private String status;
    private Integer code;
    private String message;
    @SerializedName("data")
    private List<TaskModel> tasks = new ArrayList<>();

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
    public List<TaskModel> getTasks() {
        return tasks;
    }

    /**
     *
     * @param tasks
     * The data
     */
    public void setTasks(List<TaskModel> tasks) {
        this.tasks = tasks;
    }

}