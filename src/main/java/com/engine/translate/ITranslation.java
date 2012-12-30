/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.translate;

import com.memetix.mst.language.Language;

/**
 * Interface for handling translation classes
 * @author Giank
 */
public interface ITranslation {
    
    /**
     * Translate input to target language
     * @param input         Message
     * @param destination   Target language
     * @return Translated input into target language
     */
    public String translate(String input, Language destination);
}
