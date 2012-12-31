package com.engine.property;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * This class loads the engine properties from the configuration.properties file in the resources folder
 * @author Giank
 */
public class PropertyFile implements IProperty {
    
    //Singleton instance
    private static PropertyFile instance    = null;
    private Properties properties           = null;
    
    //Log
    private static final Logger LOG = Logger.getLogger(PropertyManager.class .getName()); 
    
    /**
     * Singleton class constructor, initializes all needed variables and load
     * all needed properties
     */
    protected PropertyFile() {
        //Properties read from configuration.properties file. In case to be running on Glassfish
        //Server for example, create resources and modify this class to extract from resources
        FileInputStream fileInputStream = null;
        try {
            // Read properties for persistence, knowledge translation and remaining modules.
            properties = new Properties();
            
            fileInputStream = new FileInputStream("/iris/configuration.properties");
            properties.load(fileInputStream);
            LOG.log(Level.FINE, "PropertyFile successfully loaded");
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IOException reading the configuration.properties file");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception reading the configuration.properties file");
        } finally {
            if (fileInputStream!=null){
                try {
                    fileInputStream.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, "PropertyFile could not close input stream from properties", ex);
                }
            }
        }
    }
    
    /**
     * Singleton getter method for the class
     * @return an instance of PropertyManager class
     */
    public static PropertyFile getInstance() {
        if (instance == null) {
            instance = new PropertyFile();
        }
        return instance;
    }
    
    /**
     * Get property from this singleton managed class
     * @param name  Name of the property
     * @return      Value of the property
     */
    public String getProperty(String name) throws NoSuchFieldException{
        String property = null;
        try{
            property = properties.getProperty(name);
        } catch (Exception ex){
            LOG.log(Level.SEVERE, "Unexpected Exception PropertyFile.getProperty .name:{0} hard to know what went wrong", name);
        } finally {
            //Property looked for could not be retrieved
            if (StringUtils.isEmpty(property)){
                LOG.log(Level.WARNING, "Exception PropertyFile.getProperty .name:{0} does not exist", name);
                throw new NoSuchFieldException(name);
            }
        }
        //Property was successfully found, return the value
        return property;
    }
}