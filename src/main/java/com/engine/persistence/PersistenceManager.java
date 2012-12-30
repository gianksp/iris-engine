package com.engine.persistence;

import com.engine.interpretation.Trigger;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles all data within the internal knowledge base. The idea of
 * the internal knowledge base is to serve as a repository for information. When
 * a user inputs a sentence that requires knowledge search within knowledge base
 * it first hits the persistence manager to check if it has the category
 * information already. if not, queries the external knowledge base, obtains the
 * information and stores it here so that the next time the same knowledge is
 * required by the user through a sentence it comes directly from the internal
 * repository. In short, this serves as a bot memory
 *
 * @author Giank
 */
public class PersistenceManager implements IPersistence {

    //Available engines for storage
    public enum Source{
        MONGODB
    };
    
    //Singleton object instance
    private static IPersistence instance = null;
    
    //Current persistence engine
    private static PersistenceManager.Source source = Source.MONGODB;   //Default database
    
    //Log instance
    private final static Logger LOG = Logger.getLogger(PersistenceManager.class.getName());

    /**
     * Class constructor and variable initializer, default set to MONGODB
     */
    protected PersistenceManager() {

    }

    /**
     * Singleton class getter
     * @return an instance of PersistenceManager
     */
    public static IPersistence getInstance() {
                
         //Get the database from appropriate source
        switch (source){
            case MONGODB:
                    instance = PersistenceMongoDB.getInstance();
                    LOG.log(Level.INFO,"PersistenceManager MongoDB instance initialized (Using MongoDB storage)");
                 break;
        }

        return instance;
    }

    /**
     * Find within the internal knowledge storage a category given its pattern
     * @param input Category's pattern
     * @return Category's template
     */
    public String find(String input, LinkedList<String> tags) {
        return instance.find(input, tags);
    }

    /**
     * Store within the internal knowledge a category given its pattern and
     * template
     * @param pattern Category's pattern
     * @param template Category's template
     */
    public void save(String pattern, String template) {
        instance.save(pattern, template);
    }

    /**
     * Close all database connections
     */
    public void close(){
        instance.close();
    }
    
    /**
     * Get the current database engine
     * @return Source engine
     */
    public Source getEngine() {
        return instance.getEngine();
    }
}