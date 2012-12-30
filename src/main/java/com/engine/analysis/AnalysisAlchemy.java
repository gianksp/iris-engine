/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.analysis;

import com.engine.persistence.PersistenceMongoDB;
import com.engine.property.IProperty;
import com.engine.property.PropertyManager;
import com.engine.util.PostRequestHelper;
import com.memetix.mst.language.Language;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.NameValuePair;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 *
 * @author Giank
 */
public class AnalysisAlchemy implements IAnalysis{

    //Singleton object instance
    private static AnalysisAlchemy instance = null;
        //Singleton object instance

    private static String conceptTagsUrl = null;
    private static String entityTagsUrl = null;
        private static String keywordsUrl = null;
    private static String key = null;
    private static Language language = Language.ENGLISH;
    //Log
    private static final Logger LOG = Logger.getLogger(AnalysisAlchemy.class.getName());
    
        /**
     * Initializes MongoDB connection and collection loading for knowledge
     */
    protected AnalysisAlchemy() {
        try {
            //Load properties
            IProperty properties = PropertyManager.getInstance();
            conceptTagsUrl = properties.getProperty("alchemy_concept_tags_url");
            entityTagsUrl = properties.getProperty("alchemy_entity_tags_url");
            keywordsUrl = properties.getProperty("alchemy_keyword_url");
            key = properties.getProperty("alchemy_key");
            language = Language.valueOf(properties.getProperty("alchemy_language"));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception - AnalysisAlchemy constructor could not initialize the analysis service", ex);
        }
    }

    /**
     * Singleton class getter
     *
     * @return an instance of PersistenceManager
     */
    public static AnalysisAlchemy getInstance() {
        if (instance == null) {
            instance = new AnalysisAlchemy();
        }
        return instance;
    }
    
    /**
     * Get concept tags
     * @param text  Source
     * @return      List with concept tags
     */
    public List<String> getConceptTags(String text) {

        //text = TranslationManager.getInstance().translate(text, language);
        List<String> result = new ArrayList<String>();
        List<String> conceptTags = new ArrayList<String>();

        NameValuePair[] params = new NameValuePair[3];
        params[0] = new NameValuePair("apikey", key);
        params[1] = new NameValuePair("showSourceText", "0");
        params[2] = new NameValuePair("text", text);

        try {
            String data = PostRequestHelper.post(conceptTagsUrl, params);

            conceptTags = this.extractTagsFromXML(data, "concepts", "concept", "text");

            //String translatedTags = TranslationManager.getInstance().translate(conceptTags.toString(), Language.SPANISH);

            for (int index = 0; index < conceptTags.size(); index++) {
                result.add(conceptTags.get(index));
            }
            
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException - Analysis.getConceptTags could not be retrieved ", ex);
        } finally {

        }

        return result;
    }

    private List<String> extractTagsFromXML(String data, String family, String subfamily, String target) {

        List<String> tags = new ArrayList<String>();
        try {
            SAXBuilder builder = new SAXBuilder();
            org.jdom.Document document;

            document = builder.build(new ByteArrayInputStream(data.getBytes()));

            org.jdom.Element root = document.getRootElement().getChild(family);
            List row = root.getChildren(subfamily);

            //Iterate through each Concept
            for (int i = 0; i < row.size(); i++) {

                org.jdom.Element child = (org.jdom.Element) row.get(i);
                org.jdom.Element item = child.getChild(target);
                String name = item.getValue().toLowerCase();

                tags.add(name);
            }

        } catch (JDOMException ex) {
            LOG.log(Level.SEVERE, "JDOMException -  Analysis.extractTagsFromXML tags could not be parsed", ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException - Analysis.extractTagsFromXML could not be retrieved ", ex);
        }

        return tags;
    }
    
    /**
     * Get entity tags
     * @param text  Source
     * @return      List with entity tags
     */
    public List<String> getEntityTags(String text) {

        //text = TranslationManager.getInstance().translate(text, language);

        List<String> entityTags = new ArrayList<String>();
        List<String> result = new ArrayList<String>();

        NameValuePair[] params = new NameValuePair[2];
        params[0] = new NameValuePair("apikey", key);
        params[1] = new NameValuePair("text", text);

        try {
            String data = PostRequestHelper.post(entityTagsUrl, params);
            entityTags = this.extractTagsFromXML(data, "entities", "entity", "text");

            //String translatedTags = TranslationManager.getInstance().translate(entityTags.toString(), Language.SPANISH);
            //String[] split = translatedTags.split(",");
            for (int index = 0; index < entityTags.size(); index++) {
                result.add(entityTags.get(index));
            }
            
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException - Analysis.getConceptTags could not be retrieved ", ex);
        } finally {
        }

        return result;
    }
    
    /**
     * Get all tags
     * @param text  Source
     * @return      List of all tags
     */
    public List<String> getAllTags(String text) {

        List<String> tags = new ArrayList<String>();
        List<String> concepts = this.getConceptTags(text);
        List<String> entities = this.getEntityTags(text);

        //Get concept tags
        for (Iterator<String> conceptsIter = concepts.iterator(); conceptsIter.hasNext();) {
            String tag = conceptsIter.next();
            tags.add(tag);
        }
        
        //Get entity tags
        for (Iterator<String> entitiesIter = entities.iterator(); entitiesIter.hasNext();) {
            String tag = entitiesIter.next();
            tags.add(tag);
        }

        return tags;
    }
    
    public List<String> getKeywords(String text){

        //text = TranslationManager.getInstance().translate(text, language);
        List<String> result = new ArrayList<String>();
        List<String> keywords = new ArrayList<String>();

        NameValuePair[] params = new NameValuePair[3];
        params[0] = new NameValuePair("apikey", key);
        params[1] = new NameValuePair("showSourceText", "0");
        params[2] = new NameValuePair("text", text);

        try {
            String data = PostRequestHelper.post(keywordsUrl, params);

            keywords = this.extractTagsFromXML(data, "keywords", "keyword", "text");

            //String translatedTags = TranslationManager.getInstance().translate(conceptTags.toString(), Language.SPANISH);

            for (int index = 0; index < keywords.size(); index++) {
                result.add(keywords.get(index));
            }
            
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "IOException - Analysis.getKeywords could not be retrieved ", ex);
        } finally {

        }

        return result;
    }
}
