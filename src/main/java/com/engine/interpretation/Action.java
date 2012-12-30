/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.interpretation;

/**
 *
 * @author Giank
 */
public class Action {
    
    //Variables
    public static enum type {KNOWLEDGE_SEARCH}
    private Action.type type;
    private String content;
    
    /**
     * Constructor for action types
     * @param type      Type of action the interpreter can perform
     * @param content   Value to process during action
     */
    public Action(Action.type type, String content){
        this.type=type;
        this.content=content;
    }
    
    /**
     * @return the type
     */
    public Action.type getType() {
        return type;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }
}
