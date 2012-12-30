/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.knowledge;

import com.engine.property.IProperty;
import com.engine.property.PropertyManager;
import com.engine.util.PostRequestHelper;
import com.memetix.mst.language.Language;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;

/**
 * This class handles all knowledge search  against the MIT START search engine which is opensource
 * and an alternative to WolframAlpha
 * @author Giank
 */
public class KnowledgeMITStart implements  IKnowledge {

    //Sigleton instance class
    private static KnowledgeMITStart instance    = null;
    
    //Service URL
    private String url;
    private Language defaultLanguage = Language.ENGLISH;
    
    //Log instance
    private final static Logger LOG = Logger.getLogger(KnowledgeMITStart.class .getName()); 
    
    /**
     * Class constructor and variable initializer
     */
    protected KnowledgeMITStart(){
        try{
            //Load properties
            IProperty properties = PropertyManager.getInstance();
            this.url             = properties.getProperty("start_url");
            this.defaultLanguage = Language.valueOf(properties.getProperty("start_language"));
            LOG.log(Level.FINE, "KnowledgeMITStart successfully loaded");
        } catch (NoSuchFieldException ex) { 
            LOG.log(Level.SEVERE, "KnowledgeMITStart No fields found for MIT Start at:"+url, ex);
        }   
    }

    /**
     * Singleton class getter
     * @return an instance of KnowledgeMITStart class
     */
    public static KnowledgeMITStart getInstance() {
        if (instance == null) {
            instance = new KnowledgeMITStart();
        }
        return instance;
    }
    
    /**
     * Process a message against MIT Start search engine and then strips it from useless
     * HTML code that is embedded in the response
     * @param message   Query
     * @return          Answer
     */
    public String answer(String message) {
        String answer = null;
        try {
            NameValuePair[] params = new NameValuePair[1];  
            params[0] = new NameValuePair("query",message);
                    
            //Obtain the info retrieved from MIT Start search engine but the data comes as
            //HTML with a lot of unnecessary headers and tags, strip them away
            //and return a clean result
            answer = PostRequestHelper.post(url, params);
            
            //Strip HTML tags
            if (!StringUtils.isEmpty(answer)){
                answer = Jsoup.parse(answer).text();
            }
            
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException KnowledgeMITStart.answer could not process .query:"+message, ex);
        }
        return answer;
    }

    /**
     * Return the current MIT Start engine language
     * @return Language
     */
    public Language getDefaultLanguage() {
        return this.defaultLanguage;
    }
    
}
