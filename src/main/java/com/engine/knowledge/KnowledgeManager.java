package com.engine.knowledge;

import com.memetix.mst.language.Language;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the main class of knowledge manipulation. Do not get confused between
 * interpretation and knowledge interpretation is done in other modules, here
 * the real knowledge is handled. The first step would be to check the internal
 * repository (document database) to check if the searched knowledge can be
 * found there, if not then use whatever external knowledge base we have
 * registered to find the information, store it in our local repository and then
 * return the answer.
 *
 * @author Giank
 */
public class KnowledgeManager implements IKnowledge {

    //External knowledge repositories
    public enum Source {
        WOLFRAMALPHA,MITSTART
    };
    
    //Sigleton instance class
    private static IKnowledge instance = null;
    
    //Current knowledge base source
    private static KnowledgeManager.Source source = Source.WOLFRAMALPHA;    //Default external knowledge source
    
    //Log instance
    private final static Logger LOG = Logger.getLogger(KnowledgeManager.class.getName());

    /**
     * Singleton class getter
     * @return an instance of KnowledgeManager class
     */
    public static IKnowledge getInstance() {

        //Get the database from appropriate source
        switch (source) {
            case WOLFRAMALPHA:
                    instance = KnowledgeWolframAlpha.getInstance();
                    LOG.log(Level.INFO, "KnowledgeManager WolframAplha instance initialized (Using Wolfram Alpha knowledge database services)");
                break;
            case MITSTART:
                    instance = KnowledgeMITStart.getInstance();
                    LOG.log(Level.INFO, "KnowledgeManager MIT Start search instance initialized (Using MIT Start knowledge database services)");
                break;
        }

        return instance;
    }

    /**
     * Given an input searches the external knowledge repository for the needed
     * message
     * @param message Knowledge to seek
     * @return Answer to knowledge
     */
    public String answer(String message) {
        return instance.answer(message);
    }

    /**
     * @return the defaultLanguage
     */
    public Language getDefaultLanguage() {
        return instance.getDefaultLanguage();
    }
}