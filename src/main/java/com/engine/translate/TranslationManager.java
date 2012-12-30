package com.engine.translate;

import com.memetix.mst.language.Language;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles translation services. If bots and information sources do
 * not match the same language then a work must be done on the output or the
 * input, sometimes both, to normalize knowledge search and display
 *
 * @author Giank
 */
public class TranslationManager implements ITranslation {

    //Available translation engines
    public enum Source {
        BING
    };
    
    //Singleton instance
    private static ITranslation instance = null;
    
    //Current translation engine
    private static TranslationManager.Source source = Source.BING;
    
    //Log instance
    private final static Logger LOG = Logger.getLogger(TranslationManager.class.getName());

    /**
     * Protected constructor
     */
    protected TranslationManager(){
    
    }
    
    /**
     * Singleton class getter method
     * @return an instance of TranslationManager class
     */
    public static ITranslation getInstance() {

        //Get the translator from appropriate source
        switch (source) {
            case BING:
                instance = TranslationBing.getInstance();
                LOG.log(Level.INFO, "TranslationManager TranslationBing instance initialized (Using Microsoft Bing Translation Services)");
                break;
        }

        return instance;
    }

    /**
     * Translates a given input from origin language to destination language
     * @param input Message
     * @param origin Language origin
     * @param destination Target language
     * @return Message translated to target language
     */
    public String translate(String input, Language destination) {
        return instance.translate(input, destination);
    }
}
