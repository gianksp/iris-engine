package com.engine.interpretation;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A trigger class for Engine.
 */
public class Trigger {

    // A trigger is a parent of everything that comes after it (redirect, reply and conditions).
    private String pattern                  = null;
    private String inTopic                  = null;
    private Collection<String> redirect     = new ArrayList<String>();  // @Redirect
    private Collection<String> reply        = new ArrayList<String>();  // -Reply
    private Collection<String> condition    = new ArrayList<String>();  // *Condition
    private Collection<Action> action       = new ArrayList<Action>();  // &Action
    private boolean previous                = false;                    // has previous
    private boolean persistent              = true;                     // trigger must be refreshed on every query, for example "today's date"

    /**
     * Getter for pattern
     * @return String pattern
     */
    public String getPattern() {
        return this.pattern;
    }

    /**
     * Create a new trigger object.
     * @param pattern The match pattern for the trigger.
     */
    public Trigger(String topic, String pattern) {
        this.inTopic = topic;   // And then it's read-only! Triggers can't be moved to other topics
        this.pattern = pattern;
        //By default triggers are always set to persitent true, which means that once a query is
        //performed the data will be saved locally for further requests but if this happens while
        //querying for example "what is today's date" it will always bring the same date because
        //it is stored locally. The idea is to set persistent false to force queries everytime against
        //source
    }

    /**
     * If you have the trigger object, this will tell you what topic it belongs
     * to.
     * @return String topic
     */
    public String topic() {
        return this.inTopic;
    }
    
    /**
     * Flag that this trigger is paired with a %Previous (and shouldn't be
     * sorted with the other triggers for reply matching purposes).
     * @param paired Whether the trigger has a %Previous.
     */
    public void hasPrevious(boolean paired) {
        this.previous = true;
    }

    /**
     * Test whether the trigger has a %Previous.
     */
    public boolean hasPrevious() {
        return this.previous;
    }

    /**
     * Add a new reply to a trigger.
     * @param reply The reply text.
     */
    public void addReply(String reply) {
        this.reply.add(reply);
    }

    /**
     * Add a new action to a trigger.
     * @param action The action text.
     */
    public void addAction(Action action) {
        this.action.add(action);
    }
    
    /**
     * Get a list of actions linked to this trigger
     * @return  List of actions
     */
    public Collection<Action> listActions(){
        return this.action;
    }
    
    /**
     * If this Trigger has any action associated return true
     * @return true if Trigger has actions
     */
    public boolean hasAction(){
        return !this.action.isEmpty();
    }
    
    /**
     * List replies under this trigger.
     */
    public Collection<String> listReplies() {
        return reply;
    }

    /**
     * Add a new redirection to a trigger.
     *
     * @param meant What the user "meant" to say.
     */
    public void addRedirect(String meant) {
        this.redirect.add(meant);
    }

    /**
     * List redirections under this trigger.
     */
    public Collection<String> listRedirects() {
        return redirect;
    }

    /**
     * Add a new condition to a trigger.
     * @param condition The conditional line.
     */
    public void addCondition(String condition) {
        this.condition.add(condition);
    }

    /**
     * List conditions under this trigger.
     */
    public Collection<String> listConditions() {
        return condition;
    }

    /**
     * @return the persistent
     */
    public boolean isPersistent() {
        return persistent;
    }
    
    /**
     * This method sets the trigger as non persistent which means that is handling
     * a "LEARN" function for whatever the user inputs
     * @return the persistent
     */
    public void setPersistent(boolean value) {
        this.persistent=value;
    }
}