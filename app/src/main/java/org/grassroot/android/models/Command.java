package org.grassroot.android.models;

/**
 * Created by paballo on 2016/09/20.
 */
public class Command {

    private String command;
    private String description;
    private String hint;

    public Command(String command, String hint, String description){
        this.command = command;
        this.hint = hint;
        this.description = description;
    }

    public String getCommand() {
        return command;
    }

    public String getDescrption() {
        return description;
    }

    public String getHint() {
        return hint;
    }

    @Override
    public String toString() {
        return command; //must return only the command
    }
}
