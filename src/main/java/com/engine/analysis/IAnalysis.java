/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.analysis;

import java.util.List;

/**
 *
 * @author Giank
 */
public interface IAnalysis {
    
    public List<String> getConceptTags(String text);
    public List<String> getEntityTags(String text);
    public List<String> getAllTags(String text);
    public List<String> getKeywords(String text);
}
