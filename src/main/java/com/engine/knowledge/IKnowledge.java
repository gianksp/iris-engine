/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.knowledge;

import com.memetix.mst.language.Language;

/**
 *
 * @author Giank
 */
public interface IKnowledge {
    public String answer(String message);
    public Language getDefaultLanguage();
}
