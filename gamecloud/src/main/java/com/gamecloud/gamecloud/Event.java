package com.gamecloud.gamecloud;

/**
 * Created by lmontaga7.alumnes on 31/05/17.
 */

public class Event {

    private int type, index;

    public Event(int type, int index){
        this.type = type;
        this.index = index;
    }

    public int getType(){
        return this.type;
    }

    public int getIndex(){
        return this.index;
    }

}
