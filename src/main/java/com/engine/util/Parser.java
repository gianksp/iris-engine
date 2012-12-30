package com.engine.util;

import com.engine.interpretation.Action;
import com.engine.interpretation.Interpreter;
import com.engine.interpretation.ObjectHandler;
import com.memetix.mst.language.Language;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class in the one loading the brain data into the program structure for question - answer
 * interpretations. The source could be AIML files, RiveScript files or any other format supported.
 * Each source format has a different parser method
 * @author Giank
 */
public class Parser {
    
    //Log instance
    public final static Logger LOG = Logger.getLogger(Parser.class .getName()); 

    /**
     * Parser for RiveScript files
     * @param interpreter   Instance of the interpreter target
     * @param filename      File source
     * @param code          List of lines
     * @return              Operation status
     */
    public static boolean parseRS(Interpreter interpreter, String filename, LinkedList<String> code) {
        
        // Track some state variables for this parsing round.
        String topic                = "random"; // Default topic = random
        int lineno                  = 0;        // Current line number (index)
        boolean comment             = false;    // In a multi-line comment
        boolean inobj               = false;    // In an object
        String objName              = "";       // Name of the current object
        String objLang              = "";       // Programming language of the object
        LinkedList<String> objBuff  = null;     // Buffer for the current object
        String onTrig               = "";       // Trigger we're on
        String isThat               = "";       // Is a %Previous trigger

        // The given "code" is an array of lines, so jump right in.
        for (int i = 0; i < code.size(); i++) {
            lineno++; // Increment the line counter.
            String line = code.get(i);
            LOG.log(Level.INFO, "Line: {0}", line);

            // Trim the line of whitespaces.
            line = line.trim();

            // Are we inside an object?
            if (inobj) {
                if (line.startsWith("<object") || line.startsWith("< object")) { // TODO regexp
                    // End of the object. Did we have a handler?
                    if (interpreter.handlers.containsKey(objLang.toUpperCase())) {
                        // Yes, call the handler's onLoad function.
                        ObjectHandler objHandler = interpreter.handlers.get(objLang.toUpperCase());
                        objHandler.onLoad(objName, objBuff.toArray());

                        // Map the name to the language.
                        interpreter.objects.put(objName, objLang.toUpperCase());
                    }

                    objName = "";
                    objLang = "";
                    objBuff = null;
                    inobj = false;
                    continue;
                }

                // Collect the code.
                objBuff.add(line);
                continue;
            }

            // Look for comments.
            if (line.startsWith("/*")) {
                // Beginning a multi-line comment.
                if (line.indexOf("*/") > -1) {
                    // It ends on the same line.
                    continue;
                }
                comment = true;
            } else if (line.startsWith("/")) {
                // A single line comment.
                continue;
            } else if (line.indexOf("*/") > -1) {
                // End a multi-line comment.
                comment = false;
                continue;
            }
            if (comment) {
                continue;
            }

            // Skip any blank lines.
            if (line.length() < 2) {
                continue;
            }

            // Separate the command from the rest of the line.
            String cmd = line.substring(0, 1);
            line = line.substring(1).trim();
            LOG.log(Level.INFO, "\tCmd: {0}", cmd);

            // Ignore inline comments.
            if (line.indexOf(" // ") > -1) {
                String[] split = line.split(" // ");
                line = split[0];
            }

            // Reset the %Previous if this is a new +Trigger.
            if (cmd.equals(Interpreter.CMD_TRIGGER)) {
                isThat = "";
            }

            // Do a look-ahead to see ^Continue and %Previous.
            for (int j = (i + 1); j < code.size(); j++) {
                // Peek ahead.
                String peek = code.get(j).trim();

                // Skip blank.
                if (peek.length() == 0) {
                    continue;
                }

                // Get the command.
                String peekCmd = peek.substring(0, 1);
                peek = peek.substring(1).trim();

                // Only continue if the lookahead line has any data.
                if (peek.length() > 0) {
                    // The lookahead command has to be a % or a ^
                    if (peekCmd.equals(Interpreter.CMD_CONTINUE) == false && peekCmd.equals(Interpreter.CMD_PREVIOUS) == false) {
                        break;
                    }

                    // If the current command is a +, see if the following is a %.
                    if (cmd.equals(Interpreter.CMD_TRIGGER)) {
                        if (peekCmd.equals(Interpreter.CMD_PREVIOUS)) {
                            // It has a %Previous!
                            isThat = peek;
                            break;
                        } else {
                            isThat = "";
                        }
                    }

                    // If the current command is a ! and the next command(s) are
                    // ^, we'll tack each extension on as a "line break".
                    if (cmd.equals(Interpreter.CMD_DEFINE)) {
                        if (peekCmd.equals(Interpreter.CMD_CONTINUE)) {
                            line += "<crlf>" + peek;
                        }
                    }

                    // If the current command is not a ^ and the line after is
                    // not a %, but the line after IS a ^, then tack it onto the
                    // end of the current line.
                    if (cmd.equals(Interpreter.CMD_CONTINUE) == false && cmd.equals(Interpreter.CMD_PREVIOUS) == false && cmd.equals(Interpreter.CMD_DEFINE) == false) {
                        if (peekCmd.equals(Interpreter.CMD_CONTINUE)) {
                            line += peek;
                        } else {
                            break;
                        }
                    }
                }
            }

            // Start handling command types.
            if (cmd.equals(Interpreter.CMD_DEFINE)) {
                LOG.log(Level.INFO, "\t! DEFINE");
                String[] whatis = line.split("\\s*=\\s*", 2);
                String[] left = whatis[0].split("\\s+", 2);
                String type = left[0];
                String var = "";
                String value = "";
                boolean delete = false;
                if (left.length == 2) {
                    var = left[1].trim().toLowerCase();
                }
                if (whatis.length == 2) {
                    value = whatis[1].trim();
                }

                // Remove line breaks unless this is an array.
                if (!type.equals("array")) {
                    value = value.replaceAll("<crlf>", "");
                }

                // Version is the only type that doesn't have a var.
                if (type.equals("version")) {
                    LOG.log(Level.INFO, "\tUsing ProgramJ version {0}", value);

                    // Convert the value into a double, catch exceptions.
                    double version = 0;
                    try {
                        version = Double.valueOf(value).doubleValue();
                    } catch (NumberFormatException e) {
                        LOG.log(Level.SEVERE, "ProgramJ version {0} not a valid floating number in {1} at {2}", new Object[]{value, filename, lineno});
                        continue;
                    }

                    if (version > Interpreter.RS_VERSION) {
                        LOG.log(Level.SEVERE, "We can''t parse ProgramJ v{0} documents in {1} at {2}", new Object[]{value, filename, lineno});
                        return false;
                    }

                    continue;
                } 
                //Obtain bot language
                if (type.equals("language")) {
                    LOG.log(Level.INFO, "\tUsing ProgramJ language {0}", value);
                    interpreter.language = Language.valueOf(value.toUpperCase());

                    continue;
                }
                else {
                    // All the other types require a variable and value.
                    if (var.equals("")) {
                        LOG.log(Level.SEVERE, "Missing a {0} variable name in {1} at {2}", new Object[]{type, filename, lineno});
                        continue;
                    }
                    if (value.equals("")) {
                        LOG.log(Level.SEVERE, "Missing a {0} value in {1} at {2}", new Object[]{type, filename, lineno});
                        continue;
                    }
                    if (value.equals("<undef>")) {
                        // Deleting its value.
                        delete = true;
                    }
                }

                // Handle the variable set types.
                if (type.equals("global")) {
                    // Is it a special global? (debug or depth or etc).
                    LOG.log(Level.INFO, "\tSet global {0} = {1}", new Object[]{var, value});
                    interpreter.setGlobal(var, value);
                } else if (type.equals("var")) {
                    // Set a bot variable.
                    LOG.log(Level.INFO, "\tSet bot variable {0} = {1}", new Object[]{var, value});
                    interpreter.setVariable(var, value);
                } else if (type.equals("array")) {
                    // Set an array.
                    LOG.log(Level.INFO, "\tSet array {0}", var);

                    // Deleting it?
                    if (delete) {
                        interpreter.arrays.remove(var);
                        continue;
                    }

                    // Did the array have multiple lines?
                    String[] parts = value.split("<crlf>");
                    LinkedList<String> items = new LinkedList<String>();
                    for (int a = 0; a < parts.length; a++) {
                        // Split at pipes or spaces?
                        String[] pieces;
                        if (parts[a].indexOf("|") > -1) {
                            pieces = parts[a].split("\\|");
                        } else {
                            pieces = parts[a].split("\\s+");
                        }
                        items.addAll(Arrays.asList(pieces));
                    }

                    // Store this array.
                    interpreter.arrays.put(var, items);
                } else if (type.equals("sub")) {
                    // Set a substitution.
                    LOG.log(Level.INFO, "\tSubstitution {0} => {1}", new Object[]{var, value});
                    interpreter.setSubstitution(var, value);
                } else if (type.equals("person")) {
                    // Set a person substitution.
                    LOG.log(Level.INFO, "\tPerson substitution {0} => {1}", new Object[]{var, value});
                    interpreter.setPersonSubstitution(var, value);
                } else {
                    LOG.log(Level.SEVERE, "Unknown definition type {0} in {1} at {2}", new Object[]{type, filename, lineno});
                    continue;
                }
            } else if (cmd.equals(Interpreter.CMD_LABEL)) {
                // > LABEL
                LOG.log(Level.INFO, "\t> LABEL");
                String label[] = line.split("\\s+");
                String type = "";
                String name = "";
                if (label.length >= 1) {
                    type = label[0].trim().toLowerCase();
                }
                if (label.length >= 2) {
                    name = label[1].trim();
                }

                // Handle the label types.
                if (type.equals("begin")) {
                    // The BEGIN statement.
                    LOG.log(Level.INFO, "\tFound the BEGIN Statement.");

                    // A BEGIN is just a special topic.
                    type = "topic";
                    name = "__begin__";
                }
                if (type.equals("topic")) {
                    // Starting a new topic.
                    LOG.log(Level.INFO, "\tSet topic to {0}", name);
                    onTrig = "";
                    topic = name;

                    // Does this topic include or inherit another one?
                    if (label.length >= 3) {
                        final int mode_includes = 1;
                        final int mode_inherits = 2;
                        int mode = 0;
                        for (int a = 2; a < label.length; a++) {
                            if (label[a].toLowerCase().equals("includes")) {
                                mode = mode_includes;
                            } else if (label[a].toLowerCase().equals("inherits")) {
                                mode = mode_inherits;
                            } else if (mode > 0) {
                                // This topic is either inherited or included.
                                if (mode == mode_includes) {
                                    interpreter.topics.topic(topic).includes(label[a]);
                                } else if (mode == mode_inherits) {
                                    interpreter.topics.topic(topic).inherits(label[a]);
                                }
                            }
                        }
                    }
                }
                if (type.equals("object")) {
                    // If a field was provided, it should be the programming language.
                    String lang = "";
                    if (label.length >= 3) {
                        lang = label[2].toLowerCase();
                    }

                    // Only try to parse a language we support.
                    onTrig = "";
                    if (lang.length() == 0) {
                        LOG.log(Level.SEVERE, "Trying to parse unknown programming language (assuming it''s JavaScript) in {0} at {1}", new Object[]{filename, lineno});
                        lang = "javascript"; // Assume it's JavaScript
                    }
                    if (!interpreter.handlers.containsKey(lang.toUpperCase())) {
                        // We don't have a handler for this language.
                        LOG.log(Level.INFO, "We can''t handle {0} object code!", lang);
                        continue;
                    }

                    // Start collecting its code!
                    objName = name;
                    objLang = lang;
                    objBuff = new LinkedList<String>();
                    inobj = true;
                }
            } else if (cmd.equals(Interpreter.CMD_ENDLABEL)) {
                // < ENDLABEL
                LOG.log(Level.INFO, "\t< ENDLABEL");
                String type = line.trim().toLowerCase();

                if (type.equals("begin") || type.equals("topic")) {
                    LOG.log(Level.INFO, "\t\tEnd topic label.");
                    topic = "random";
                } else if (type.equals("object")) {
                    LOG.log(Level.INFO, "\t\tEnd object label.");
                    inobj = false;
                } else {
                    LOG.log(Level.SEVERE, "Unknown end topic type {0} in {1} at {2}", new Object[]{type, filename, lineno});
                }
            } else if (cmd.equals(Interpreter.CMD_TRIGGER)) {
                // + TRIGGER
                LOG.log(Level.INFO, "\t+ TRIGGER: {0}", line);

                if (isThat.length() > 0) {
                    // This trigger had a %Previous. To prevent conflict, tag the
                    // trigger with the "that" text.
                    onTrig = line + "{previous}" + isThat;
                    interpreter.topics.topic(topic).trigger(line).hasPrevious(true);
                    interpreter.topics.topic(topic).addPrevious(line, isThat);
                } else {
                    // Set the current trigger to this.
                    onTrig = line;
                }
            } else if (cmd.equals(Interpreter.CMD_REPLY)) {
                // - REPLY
                LOG.log(Level.INFO, "\t- REPLY: {0}", line);

                // This can't come before a trigger!
                if (onTrig.length() == 0) {
                    LOG.log(Level.SEVERE, "Reply found before trigger in {0} at {1}", new Object[]{filename, lineno});
                    continue;
                }

                // Add the reply to the trigger.
                interpreter.topics.topic(topic).trigger(onTrig).addReply(line);
            } else if (cmd.equals(Interpreter.CMD_ACTION)) {
                // & ACTION
                LOG.log(Level.INFO, "\t- ACTION: {0}", line);

                // This can't come before a trigger!
                if (onTrig.length() == 0) {
                    LOG.log(Level.SEVERE, "Action found before trigger in {0} at {1}", new Object[]{filename, lineno});
                    continue;
                }

                // Add the action to the trigger for now just knowledge search.
                Action action = new Action(Action.type.KNOWLEDGE_SEARCH,line);
                interpreter.topics.topic(topic).trigger(onTrig).addAction(action);
            } else if (cmd.equals(Interpreter.CMD_LEARN)) {
                // = LEARN
                LOG.log(Level.INFO, "\t- LEARN: {0}", line);

                // This can't come before a trigger!
                if (onTrig.length() == 0) {
                    LOG.log(Level.SEVERE, "Learn found before trigger in {0} at {1}", new Object[]{filename, lineno});
                    continue;
                }

                // Add the action to the trigger for now just knowledge search.
                interpreter.topics.topic(topic).trigger(onTrig).setPersistent(false);
                interpreter.topics.topic(topic).trigger(onTrig).addReply(line);
            }else if (cmd.equals(Interpreter.CMD_PREVIOUS)) {
                // % PREVIOUS
                // This was handled above.
            } else if (cmd.equals(Interpreter.CMD_CONTINUE)) {
                // ^ CONTINUE
                // This was handled above.
            } else if (cmd.equals(Interpreter.CMD_REDIRECT)) {
                // @ REDIRECT
                LOG.log(Level.INFO, "\t@ REDIRECT: {0}", line);

                // This can't come before a trigger!
                if (onTrig.length() == 0) {
                    LOG.log(Level.SEVERE, "Redirect found before trigger in {0} at {1}", new Object[]{filename, lineno});
                    continue;
                }

                // Add the redirect to the trigger.
                // TODO: this extends Interpreter, not compat w/ Perl yet
                interpreter.topics.topic(topic).trigger(onTrig).addRedirect(line);
            } else if (cmd.equals(Interpreter.CMD_CONDITION)) {
                // * CONDITION
                LOG.log(Level.INFO, "\t* CONDITION: {0}", line);

                // This can't come before a trigger!
                if (onTrig.length() == 0) {
                    LOG.log(Level.SEVERE, "Redirect found before trigger in {0} at {1}", new Object[]{filename, lineno});
                    continue;
                }

                // Add the condition to the trigger.
                interpreter.topics.topic(topic).trigger(onTrig).addCondition(line);
            } else {
                LOG.log(Level.SEVERE, "Unrecognized command {0} in {1} at {2}", new Object[]{cmd, filename, lineno});
            }
        }

        return true;
    }
    
       /**
     * Load a single Interpreter document.
     * @param file Path to a Interpreter document.
     */
    public static boolean loadFileRS(String file, Interpreter interpreter) {
        
        //Log
        LOG.log(Level.INFO, "Load file: {0}", file);
        
        // Create a file handle.
        File fh = null;
        DataInputStream dis = null;
        BufferedReader br = null;
        FileInputStream fis = null;
        // Slurp the file's contents.
        LinkedList<String> lines = new LinkedList<String>();

        try {
            fh = new File(file);
            fis = new FileInputStream(fh);

            // Using buffered input stream for fast reading.
            dis = new DataInputStream(fis);
            br = new BufferedReader(new InputStreamReader(dis));

            // Read all the lines.
            String line;
            while ((line = br.readLine()) != null) {
                lines.add((String) line);
            }   
        } catch (FileNotFoundException e) {
            LOG.log(Level.SEVERE, "{0}: file not found exception.", file);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "{0}: IOException while reading.", file);
        } finally {
            try{
            // Dispose of the resources we don't need anymore.
            if (fis != null){
                fis.close();
            }
            if (br != null){
                br.close();
            }
            if (dis != null){
                dis.close();
            }
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "IOException while closing streams during file upload.", ex);
            }

        }
        // Send the code to the parser.
        return Parser.parseRS(interpreter, file, lines);
    }
}
