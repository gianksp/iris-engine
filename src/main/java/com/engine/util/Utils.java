/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.util;

import java.util.LinkedList;

/**
 *
 * @author Giank
 */
public class Utils {
    
    public static String tagsToContent(LinkedList<String> tags){
        String result= "";
        if (tags != null) {
            result= tags.toString().replaceAll(",", " ");
        }
        
        return result;
    }
    
    /**
 * Función que elimina acentos y caracteres especiales de
 * una cadena de texto.
 * @param input
 * @return cadena de texto limpia de acentos y caracteres especiales.
 */
public static String removeAccents(String input) {
    // Cadena de caracteres original a sustituir.
    String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
    // Cadena de caracteres ASCII que reemplazarán los originales.
    String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
    String output = input;
    for (int i=0; i<original.length(); i++) {
        // Reemplazamos los caracteres especiales.
        output = output.replace(original.charAt(i), ascii.charAt(i));
    }//for i
    return output;
}//remove1
}
