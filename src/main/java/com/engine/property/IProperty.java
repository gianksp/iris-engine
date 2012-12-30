/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.property;

/**
 * This Interface defines the methods for resource acquisition across the application
 * @author Giank
 */
public interface IProperty {
    
    /**
     * Given a property name return its value either from configuration file,
     * server resources, etc.
     * @param name  Parameter name
     * @return      Parameter value
     * @throws NoSuchFieldException
     */
     public String getProperty(String name) throws NoSuchFieldException;
}