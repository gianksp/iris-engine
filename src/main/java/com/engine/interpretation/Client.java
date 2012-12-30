package com.engine.interpretation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An object to represent an individual user's data.
 */
public class Client {
	
    //Variables
    private String id                       = null;         // Client Identifier
    private HashMap<String, String> data    = null;         // User data
    private LinkedList<String> input        = null;         // User's inputs
    private LinkedList<String> reply        = null;         // Bot's replies
    private String defaultRandom            = "random";     // Default random value
    private String defaultTopic             = "topic";      // Default topic value
    private String undefined                = "undefined";  // Default undefined value
    
    //log
    public final static Logger LOG = Logger.getLogger(Client.class .getName()); 

    /**
     * Create a new client object.
     * @param id A unique ID for this client.
     */
    public Client (String id) {
        
        // Initialize varibales
        this.id = id;
        //Initialize client history
        this.input = new LinkedList<String>();
        this.reply = new LinkedList<String>();
        //Initialize data
        this.data = new HashMap<String, String>();
        this.data.put(defaultTopic, defaultRandom);
    }

    /**
     * Get client id
     * @return String client id
     */
    public String getId(){
        return this.id;
    }
    
    /**
     * Set a variable for the client.
     * @param name  The name of the variable.
     * @param value The value to set in the variable.
     */
    public void set (String name, String value) {
        data.put(name, value);
    }

    /**
     * Get a variable from the client. Returns the text "undefined" if it doesn't exist.
     * @param name The name of the variable.
     */
    public String get (String name) {
        String result = undefined;
        if (data.containsKey(name)) {
            result = data.get(name);
        }
        return result;
    }

    /**
     * Delete a variable for the client.
     * @param name The name of the variable.
     */
    public void delete (String name) {
        if (data.containsKey(name)) {
            data.remove(name);
        }
    }

    /**
     * Retrieve a HashMap of all the user's variables and values.
     * @return HashMap<String, String> User data key value
     */
    public HashMap<String, String> getData () {
        return data;
    }

    /**
     * Replace the internal HashMap with this new data (dangerous!).
     * @param newdata New data HashMap
     */
    public void setData (HashMap<String, String> newdata) {
        this.data = newdata;
    }

    /**
     * Add a line to the user's input history.
     * @param text New input
     */
    public void addInput (String text) {
        // Push this onto the front of the input array.
        input.addFirst(text);
    }

    /**
     * Add a line to the user's reply history.
     * @param text New bot reply
     */
    public void addReply (String text) {
        // Push this onto the front of the reply array.
        reply.addFirst(text);
    }

    /**
     * Get a specific input value by index.
     * @param index The index of the input value to get
     */
    public String getInput (int index){
        String result = undefined;
        int position = index-1;
        try{
            result = this.input.get(position);
        } catch (IndexOutOfBoundsException ex) {
            LOG.log(Level.INFO, "No item found in user input list at index: {0}, returning default value", position);
        }
        return result;
    }

    /**
     * Get a specific reply value by index.
     * @param index The index of the reply value to get (1-9).
     */
    public String getReply (int index){
        String result = undefined;
        int position = index-1;
        try{
            result = this.reply.get(index-1);
        } catch (IndexOutOfBoundsException ex) {
            LOG.log(Level.INFO, "No item found in bot reply list at index: {0}, returning default value", position);
        }
        return result;
    }
}