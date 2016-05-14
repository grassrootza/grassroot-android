package com.techmorphosis.grassroot.Interface;

/**
 * Created by ravi on 15/4/16.
 */
public interface FilterInterface {

    void vote(boolean vote, boolean meeting, boolean todo,boolean clear);

    void meeting(boolean vote, boolean meeting, boolean todo,boolean clear);

    void todo(boolean vote, boolean meeting, boolean todo,boolean clear);

    void clear(boolean vote, boolean meeting, boolean todo,boolean clear);

}
