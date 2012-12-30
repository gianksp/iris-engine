/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.persistence;

import com.engine.interpretation.Trigger;
import java.util.LinkedList;

/**
 * Interface to manage internal knowledge engines, defaulted we start with
 * MongoDB but later on that can become many more engines
 * @author Giank
 */
public interface IPersistence {
    
    /**
     * Store into knowledge base
     * @param pattern   Input
     * @param template  Output
     */
    public void save(String pattern, String template);
    
    /**
     * Find output given input
     * @param input Input
     * @return Output
     */
    public String find(String input, LinkedList<String> tags);

    /**
     * Close connection to storage engine
     */
    public void close();
    
    /**
     * Get database engine
     * @return Database Engine
     */
    public PersistenceManager.Source getEngine();
}
