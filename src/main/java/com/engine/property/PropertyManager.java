package com.engine.property;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles all parameter objects like database properties, external
 * services properties, IP, paths, locators, etc. At first the information is
 * loaded from a configuration.properties file but another option must be
 * implemented to load information from server resources using JNDI names, among
 * others
 *
 * @author Giank
 */
public class PropertyManager implements IProperty {

    //Enum with types of property sources
    public enum Source {
        FILE
    };
    
    //Singleton instance
    private static IProperty instance = null;
    
    //Current property source
    private static PropertyManager.Source source = Source.FILE;   //Default property source
    
    //Log
    private static final Logger LOG = Logger.getLogger(PropertyManager.class.getName());

    /**
     * Singleton getter method for the class
     * @return an instance of PropertyManager class
     */
    public static IProperty getInstance() throws IllegalArgumentException {

        //Get the properties from appropriate source
        switch (source) {
            case FILE:
                instance = PropertyFile.getInstance();
                LOG.log(Level.INFO, "PropertyManager PropertyFile instance initialized (Using configuration.properties file)");
                break;
        }

        return instance;
    }

    /**
     * Get property from this singleton managed class
     * @param name Name of the property
     * @return Value of the property
     */
    public String getProperty(String name) throws NoSuchFieldException {
        return instance.getProperty(name);
    }
}