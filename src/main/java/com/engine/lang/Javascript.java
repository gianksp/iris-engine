/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.lang;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *
 * @author Giank
 */
public class Javascript implements com.engine.interpretation.ObjectHandler {

    private com.engine.interpretation.Interpreter parent;  // Parent RS object
    private HashMap<String, String> codes = new HashMap<String, String>();       // Object codes
    private ScriptEngine engine;

    /**
     * Create a Perl handler. Must take the path to the rsp4j script as its
     * argument.
     *
     * @param rivescript Instance of your Interpreter object.
     * @param rsp4j Path to the rsp4j script (either in .pl or .exe format).
     */
    public Javascript(com.engine.interpretation.Interpreter rivescript) {
        this.parent = rivescript;
        ScriptEngineManager sem = new ScriptEngineManager();
        engine = sem.getEngineByName("javascript");
    }

    public boolean onLoad(String name, Object[] code) {
        codes.put(name, com.engine.interpretation.Util.join(code, "\n"));
        return true;
    }

    public String onCall(String name, String user, Object[] args) {
        String result = null;
        try {
            
            for (int i = 0; i < args.length; i++) {
                engine.put("param"+i, args[i]);
            }
            String expression = codes.get(name);
            engine.eval(expression);
            result = (String) engine.get("result");
       
        } catch (Exception ex) {
            Logger.getLogger(Javascript.class.getName()).log(Level.SEVERE, null, ex);
        }
        
             return result;
    }
}
