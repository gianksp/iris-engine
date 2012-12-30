package com.engine.knowledge;

import com.engine.property.IProperty;
import com.engine.property.PropertyManager;
import com.memetix.mst.language.Language;
import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles all WolframAlpha as external knowledge source
 * @author Giank
 */
public class KnowledgeWolframAlpha implements IKnowledge {
    
    //Sigleton instance class
    private static KnowledgeWolframAlpha instance    = null;
    
    //Class variables
    private WAEngine engine                     = null;
    private Language defaultLanguage            = Language.ENGLISH;
    private WAQuery query                       = null;
    private String lineSeparator                = System.getProperty("line.separator");
    
    //Log instance
    private final static Logger LOG = Logger.getLogger(KnowledgeManager.class .getName()); 
    
    /**
     * Class constructor and variable initializer
     */
    protected KnowledgeWolframAlpha(){

        String apiKey   = null;
        String language = null;
        
        try{
            //Load properties
            IProperty properties = PropertyManager.getInstance();
            apiKey      = properties.getProperty("wolfram_key");
            language    = properties.getProperty("wolfram_language");
            this.engine = new WAEngine();
            this.engine.setAppID(apiKey);
            this.engine.addFormat("plaintext");
            this.defaultLanguage = Language.valueOf(language);
            this.query = this.engine.createQuery();
            LOG.log(Level.FINE, "KnowledgeWolframAlpha successfully loaded");
        } catch (NoSuchFieldException ex) { 
            LOG.log(Level.SEVERE, "KnowledgeWolframAlpha No fields found for WolframAlpha API .key:"+apiKey+" .language:"+language.toString(), ex);
        }   
    }

    /**
     * Singleton class getter
     * @return an instance of KnowledgeManager class
     */
    public static KnowledgeWolframAlpha getInstance() {
        if (instance == null) {
            instance = new KnowledgeWolframAlpha();
        }
        return instance;
    }

    /**
     * Given an input searches the external knowledge repository for the needed message
     * @param message   Knowledge to seek
     * @return          Answer to knowledge
     */
    public String answer(String message){

        String result = "";
        WAQueryResult queryResult = null;
        try {
            this.query.setInput(message);
            queryResult = this.engine.performQuery(query);

            if (queryResult.isError()) {
                LOG.log(Level.SEVERE,   "WAExpection - KnowledgeWolframAlpha.answer query error " + " .code: {0} .message: {1}", new Object[]{queryResult.getErrorCode(), queryResult.getErrorMessage()});
            } else if (!queryResult.isSuccess()) {
                LOG.log(Level.INFO, "WAWarning - KnowledgeWolframAlpha.answer Query was not understood; no results available. Given .message:{0}", message);
            } else {
                // Got a result.
                for (WAPod pod : queryResult.getPods()) {
                    if (!pod.isError()) {
                        for (WASubpod subpod : pod.getSubpods()) {
                            for (Object element : subpod.getContents()) {
                                if (element instanceof WAPlainText) {
                                    result+=((WAPlainText) element).getText()+lineSeparator;
                                }
                            }
                        }
                    }
                }
                // We ignored many other types of Wolfram|Alpha output, such as warnings, assumptions, etc.
                // These can be obtained by methods of WAQueryResult or objects deeper in the hierarchy.
            }
        } catch (WAException ex) {
            LOG.log(Level.SEVERE, "WAExpection - KnowledgeWolframAlpha.answer for .input: "+message, ex);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Unexpected exception - KnowledgeWolframAlpha.answer for .input: "+message, ex);
        } finally {
            if (queryResult!=null){
                queryResult.release();
            }
        }

        return result;
    } 

    /**
     * @return the defaultLanguage
     */
    public Language getDefaultLanguage() {
        return this.defaultLanguage;
    }
}
