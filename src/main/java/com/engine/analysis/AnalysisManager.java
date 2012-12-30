/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.analysis;

import com.engine.persistence.PersistenceMongoDB;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Giank
 */
public class AnalysisManager implements IAnalysis{

    public List<String> getKeywords(String text) {
        return instance.getKeywords(text);
    }

        //Available engines for storage
    public enum Source{
        ALCHEMYAPI
    };
    
    //Singleton object instance
    private static IAnalysis instance = null;
    private static Source source = Source.ALCHEMYAPI;
    //Log
    private static final Logger LOG = Logger.getLogger(AnalysisManager.class.getName());

    /**
     * Initializes Analysis connection and collection loading for knowledge
     */
    protected AnalysisManager() {
    
    }

    /**
     * Singleton class getter
     *
     * @return an instance of Analysis
     */
    public static IAnalysis getInstance() {
         //Get the database from appropriate source
        switch (source){
            case ALCHEMYAPI:
                     instance = AnalysisAlchemy.getInstance();
                     LOG.log(Level.INFO,"AnalysisManager AnalysisAlchemy instance initialized (Using AlchemyAPI service)");
                 break;
        }

        return instance;
    }

    /**
     * Get concept tags
     * @param text  Source
     * @return      List with concept tags
     */
    public List<String> getConceptTags(String text) {
        return instance.getConceptTags(text);
    }

    /**
     * Get entity tags
     * @param text  Source
     * @return      List with entity tags
     */
    public List<String> getEntityTags(String text) {
        return instance.getEntityTags(text);
    }

    /**
     * Get all tags
     * @param text  Source
     * @return      List of all tags
     */
    public List<String> getAllTags(String text) {
        return instance.getAllTags(text);
    }
}
