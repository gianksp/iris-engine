/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.translate;

import com.engine.property.IProperty;
import com.engine.property.PropertyManager;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Giank
 */
public class TranslationBing implements ITranslation {
        
    //Singleton instance
    private static TranslationBing instance = null;
    
    //Log instance
    private final static Logger LOG = Logger.getLogger(TranslationManager.class .getName()); 

    /**
     * Singleton class constructor, initializes all needed variables and load
     * all needed properties
     */
    protected TranslationBing(){
        String id = null;
        String secret = null;
        try{
            //Load properties manager
            IProperty properties = PropertyManager.getInstance();
            id       = properties.getProperty("bing_translator_id");
            secret   = properties.getProperty("bing_translator_secret");

            // Set your Windows Azure Marketplace client info - See http://msdn.microsoft.com/en-us/library/hh454950.aspx
            Translate.setClientId(id);
            Translate.setClientSecret(secret);
            LOG.log(Level.FINE, "TranslationManager successfully loaded");
        } catch (NoSuchFieldException ex){
            LOG.log(Level.SEVERE, "NoSuchFieldException - TranslationManager Constructor could not connect using"
                                + " .idClient:"+id
                                + " .secret:"+secret, ex);
        } catch (Exception ex){
            LOG.log(Level.SEVERE, "Exception - TranslationManager Constructor could not connect using"
                                + " .idClient:"+id
                                + " .secret:"+secret, ex);
        }
    }

    /**
     * Singleton class getter method
     * @return an instance of TranslationManager class
     */
     public static TranslationBing getInstance() {
        if (instance == null) {
            instance = new TranslationBing();
        }
        return instance;
    }
     
    /**
     * Translates a given input from origin language to destination language
     * @param input         Message
     * @param origin        Language origin
     * @param destination   Target language
     * @return              Message translated to target language
     */
    public String translate(String input, Language destination){
        String response = input;
        try {
            response = Translate.execute(input, Language.AUTO_DETECT, destination);
            if (response.contains("TranslateApiException")){
                LOG.log(Level.SEVERE, response);
                response=null;
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception Translator.translate .input:"+input, ex);
        }
        return response;
    }
}