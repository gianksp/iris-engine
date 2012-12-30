/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.engine.client;

import com.engine.interpretation.Interpreter;
import com.engine.interpretation.ObjectHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 *
 * @author Giank
 */
public class Console {

      public static void main(String[] args) throws Exception
      {
          
        // Create a new Interpreter interpreter.
        System.out.println(":: Creating RS Object");
        Interpreter rs = new Interpreter();
        rs.setLogLevel(Level.OFF);

        // Create a handler for Perl objects.
        rs.setHandler(ObjectHandler.Handler.JAVASCRIPT);

        // Load and sort replies
        System.out.println(":: Loading replies");
        rs.loadDefaultDirectory();
        rs.sortReplies();

            System.out.print("You: " );   
            InputStreamReader r = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(r); 
            String s = br.readLine(); 
            while (!"exit".equals(s)){
                String reply = rs.reply("localuser", s);
                System.out.println("Bot: "+reply);
                System.out.print("You: ");
                s = br.readLine(); 
            }     
      }

}
