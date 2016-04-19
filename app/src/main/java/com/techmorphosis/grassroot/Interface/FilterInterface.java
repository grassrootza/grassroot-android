package com.techmorphosis.grassroot.Interface;

/**
 * Created by ravi on 15/4/16.
 */
public interface FilterInterface {

    public void vote(boolean vote, boolean meeting, boolean todo,boolean clear);

    public void meeting(boolean vote, boolean meeting, boolean todo,boolean clear);

    public void todo(boolean vote, boolean meeting, boolean todo,boolean clear);

    public void clear(boolean vote, boolean meeting, boolean todo,boolean clear);

}
