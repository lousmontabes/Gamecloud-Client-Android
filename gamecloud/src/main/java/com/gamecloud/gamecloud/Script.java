package com.gamecloud.gamecloud;

/**
 * Created by lmontaga7.alumnes on 31/05/17.
 */

public class Script {

    private Script.Type type;
    private String filename;

    public static enum Type{
        CHECK_CONNECTION,
        CONNECTION,
        SEND_LOCAL_EVENT,
        RETRIEVE_REMOTE_EVENT,
        SEND_PARAM, // TODO: Implementar al server
        RETRIEVE_PARAM // TODO: Implementar al server
    }

    public Script(Script.Type type, String filename){
        this.type = type;
        this.filename = filename;
    }

    public Script.Type getType(){
        return this.type;
    }

    public String getFilename(){
        return this.filename;
    }

}
