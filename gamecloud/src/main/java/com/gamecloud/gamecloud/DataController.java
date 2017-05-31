package com.gamecloud.gamecloud;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Lluís Montabes on 31/05/17.
 */

public class DataController {

    /*
     * HOST
     */
    private final String hostUrl;

    /*
     * CONNECTION
     */
    private int frequency;
    private boolean connectionActive;
    private boolean unlimitedCollection;

    /*
     * SYNCHRONIZATION
     */
    private boolean shouldUpdate = false;
    private boolean updateComplete = true;
    private boolean hasNewData = false;

    /*
     * Stats
     */
    private int updates = 0;

    /*
     * MATCH
     */
    // Match id
    private int matchId;

    // Player assignation
    private int assignedPlayer;
    private int oppositePlayer;

    // Player readyness
    private boolean localPlayerReady = false;
    private boolean remotePlayerReady = false;

    /*
     * PARAMETERS
     */
    private ArrayList<Parameter> parameters;
    private int nextParamKey = 0;

    /*
     * EVENTS
     */
    private ArrayList<Event> localUnsentEvents;
    private int lastSentLocalEventIndex;

    private int remoteEventIndex;
    private Event remoteActiveEvent;

    /*
     * SERVER SCRIPTS
     */
    private Script connectionScript = new Script(Script.Type.CONNECTION, "connection.php");
    private Script checkConnectionScript = new Script(Script.Type.CHECK_CONNECTION, "check_connection.php");
    private Script sendLocalEventScript = new Script(Script.Type.SEND_LOCAL_EVENT, "send_local_event.php");
    private Script retrieveRemoteEventScript = new Script(Script.Type.RETRIEVE_REMOTE_EVENT, "retrieve_remote_event.php");
    private Script sendLocalParamScript = new Script(Script.Type.SEND_PARAM, "write_param.php"); // TODO
    private Script retrieveRemoteParamScript = new Script(Script.Type.RETRIEVE_PARAM, "read_param.php");

    /*
     * DEPENDENCIES
     */
    private Gson gson = new Gson();

    /**
     * Default constructor for DataController.
     * @param assignedPlayer        Assigned player number in the server.
     * @param hostUrl               Full url of the server host (http://www.example.com/).
     * @param f                     Frequency (in frames) at which to attempt to access the server.
     * @param connectionActive      Whether to activate the connection right away or not.
     * @param unlimitedConnection   Whether or not to attempt to connect to the host independently of FPS. WARNING: Setting this to true will be very host-consuming.
     */
    public DataController(int matchId, int assignedPlayer, String hostUrl, boolean connectionActive, int f, boolean unlimitedConnection){
        this.matchId = matchId;
        this.assignedPlayer = assignedPlayer;
        this.hostUrl = hostUrl;
        this.connectionActive = connectionActive;
        this.frequency = f;
        this.unlimitedCollection = unlimitedConnection;
    }

    // SETTERS

    /**
     * Set frequency (in frames) at which to attempt to access the server..
     * @param f Frequency (in frames)
     */
    public void setFrequency(int f){
        this.frequency = f;
    }

    /**
     * Set whether the DataController will connect to the host or not.
     * @param connectionActive  Boolean connectionActive
     */
    public void setConnectionActive(boolean connectionActive){
        this.connectionActive = connectionActive;
    }

    // GETTERS

    /**
     * Returns the current frequency (in frames)
     * @return Frequency (in frames)
     */
    public int getFrequency(){
        return this.frequency;
    }

    /**
     * Returns whether or not connection is active.
     * @return Boolean connectionActive
     */
    public boolean isConnectionActive(){
        return this.connectionActive;
    }

    /**
     * Returns true if the last requested update has been completed.
     * @return  Boolean updateComplete
     */
    public boolean updateComplete() {
        return this.updateComplete;
    }

    /**
     * Return true if the DataController has gathered new data since the last time this method
     * was called.
     * @return  Boolean hasNewData
     */
    public boolean hasNewData() {
        if (hasNewData) {
            hasNewData = false;
            return true;
        }else{
            return false;
        }
    }

    // PARAMETERS

    /**
     * Create a new Gamecloud parameter.
     * @param name  Name of the parameter (e.g. playerScore)
     * @param value Initial value of the parameter (e.g. 0)
     * @return      Key of the parameter.
     */
    public int createParameter(String name, String value){
        Parameter newParameter = new Parameter(nextParamKey, name, value);
        nextParamKey++;

        parameters.add(newParameter);

        return newParameter.getKey();
    }

    /**
     * Remove Parameter with given key.
     * @param key  Key of the parameter to remove.
     */
    public void removeParameter(int key){

        int i = 0;
        boolean found = false;
        while (i < parameters.size() && !found){
            if (parameters.get(i).getKey() == key) {
                parameters.remove(i);
                found = true;
            }
        }

    }

    /**
     * Get the value of the Parameter with the specified key.
     * @param key   Key of the Parameter whose value to retrieve.
     * @return      Value of the specified Parameter.
     */
    public String getParameterValue(int key){
        return parameters.get(key).value;
    }

    // COMMANDS

    /**
     * Update DataController to send and retrieve the latest data.
     */
    public void requestUpdate(){
        shouldUpdate = true;
    }

    // DATA GATHERING

    /**
     * Get JSON response as String from URL.
     *
     * @param url     URL to retrieve JSON from.
     * @param timeout Time available to establish connection.
     * @return JSON response as String.
     */
    @Nullable
    private String getJSON(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    private class EventIndexPair{

        public EventIndexPair(Event e, int i){
            this.event = e;
            this.eventIndex = i;
        }

        int eventIndex;
        Event event;
    }

    private class PointAnglePair{
        int x;
        int y;
        float angle;
    }

    private class ScorePair{
        int score1;
        int score2;
    }

    public class RemoteDataTask extends AsyncTask<String, String, Void> {

        // OVERRIDEN METHODS

        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Void doInBackground(String... params) {

            // Notify the server that the local player is ready.
            notifyLocalPlayerReady();

            while (connectionActive) {

                if (!remotePlayerReady){

                    // Ask the server if the remote player is ready yet.
                    checkRemotePlayerReady();

                }else {

                    if (unlimitedCollection || shouldUpdate) {

                        updates++;
                        updateComplete = false;

                        sendLocalParamData();
                        retrieveRemoteParamData();
                        sendLocalEventData();
                        retrieveRemoteEventData();

                        shouldUpdate = false;
                        updateComplete = true;
                        hasNewData = true;

                    }

                }

            }

            return null;

        }

        // PLAYER READYNESS

        private void notifyLocalPlayerReady(){

            // Set the playerX_ready column to 1 on the server database.
            getJSON(hostUrl + "send_local_ready.php?matchId=" + matchId
                    + "&player=" + assignedPlayer
                    + "&ready=" + 1, 2000);

        }

        private void checkRemotePlayerReady(){

            // This returns a string containing either 1 or 0, representing a boolean value.
            String data = getJSON(hostUrl + "retrieve_remote_ready.php?matchId=" + matchId
                    + "&player=" + oppositePlayer, 2000);

            System.out.println("Ready: " + data);

            // Get first character, as, for some reason, it returns one extra character.
            int ready = Integer.parseInt(data.substring(0, 1));

            if (ready == 1){
                remotePlayerReady = true;
            }

        }

        // DATA GATHERING

        /**
         * Send local parameters to be stored in the database.
         */
        private void sendLocalParamData(){
            String json = gson.toJson(parameters);
            getJSON(hostUrl + sendLocalParamScript.getFilename() + "?matchId=" + matchId + "&param=" + json, 2000);
        }

        /**
         * Retrieve remote parameters from the database.
         */
        private void retrieveRemoteParamData(){
            String json = getJSON(hostUrl + retrieveRemoteParamScript.getFilename() + "?matchId=" + matchId, 2000);
            Type collectionType = new TypeToken<ArrayList<Parameter>>(){}.getType();
            parameters = gson.fromJson(json, collectionType);
        }

        /**
         * Send local event data to database.
         */
        private void sendLocalEventData() {

            if(!localUnsentEvents.isEmpty()){

                Event event = localUnsentEvents.get(0);
                int eventIndex = event.getIndex();
                int eventNumber = event.getType();

                System.out.println("EVENT: " + event);
                System.out.println("INDEX: " + eventIndex);

                getJSON(hostUrl + sendLocalEventScript.getFilename()
                                + "?matchId=" + matchId
                                + "&player=" + assignedPlayer
                                + "&event=" + eventNumber
                                + "&eventIndex=" + eventIndex,
                        2000);

                lastSentLocalEventIndex = eventIndex;
                localUnsentEvents.remove(0);

            }

        }

        /**
         * Retrieve remote event data from database.
         */
        private void retrieveRemoteEventData(){

            //This returns a JSON object with a {"eventIndex": int, "event": int} pattern.
            String data = getJSON(hostUrl + "retrieve_remote_event.php?matchId=" + matchId
                    + "&player=" + oppositePlayer, 2000);

            // Parse the JSON information into an EventIndexPair object.
            Event e = new Gson().fromJson(data, Event.class);

            // Set event and eventIndex variables retrieved from JSON to the remoteActiveEvent and
            // remoteEventIndex global variables.
            // These variables will be used to process events locally on the next frame.
            if (e != null){
                if (e.getIndex() > remoteEventIndex){

                    remoteActiveEvent = e;
                    remoteEventIndex = e.getIndex();

                }
            }

        }

    }

    private class Parameter {

        private int key;
        private String name;
        private String value;

        /**
         * Parameter constructor with null value.
         * @param key      Parameter key.
         * @param name     Parameter name.
         * @param value    Value of the parameter.
         */
        private Parameter(int key, String name, String value){
            this.key = key;
            this.name = name;
            this.value = value;
        }

        public int getKey() { return this.key; }
        public String getName() {
            return this.name;
        }
        public String getValue() { return this.value; }

    }

}
