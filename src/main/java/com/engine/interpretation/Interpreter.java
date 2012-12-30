package com.engine.interpretation;

import com.engine.knowledge.IKnowledge;
import com.engine.knowledge.KnowledgeManager;
import com.engine.lang.Javascript;
import com.engine.lang.Perl;
import com.engine.persistence.IPersistence;
import com.engine.persistence.PersistenceManager;
import com.engine.translate.TranslationManager;
import com.engine.property.IProperty;
import com.engine.property.PropertyManager;
import com.engine.translate.ITranslation;
import com.engine.util.Parser;
import com.engine.util.Utils;
import com.memetix.mst.language.Language;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 * A Interpreter interpreter written in Java.<p>
 * SYNOPSIS<p>
 * import com.engine.core.Interpreter;<p>
 * // Create a new interpreter<br> Interpreter interpreter = new Interpreter();<p>
 * // Load a directory full of replies in *.rs files<br>
 * interpreter.loadDirectory("./replies");<p>
 * // Sort replies<br> rs.sortReplies();<p>
 * // Get a reply for the user<br> String reply = interpreter.reply("user", "Hello
 * bot!");
 */
public class Interpreter {
    
    // Private class variables.
    public int depth = 50;                     // Recursion depth limit
    public static Random rand = new Random();  // A random number generator
    
    // Module version
    public static final double VERSION  = 0.02;
    public Language language           = Language.ENGLISH; //Default language
    
    // Constant Interpreter command symbols.
    public static final double RS_VERSION      = 2.0; // This implements Interpreter 2.0
    public static final String CMD_DEFINE      = "!";
    public static final String CMD_TRIGGER     = "+";
    public static final String CMD_PREVIOUS    = "%";
    public static final String CMD_REPLY       = "-";
    public static final String CMD_CONTINUE    = "^";
    public static final String CMD_REDIRECT    = "@";
    public static final String CMD_CONDITION   = "*";
    public static final String CMD_LABEL       = ">";
    public static final String CMD_ENDLABEL    = "<";
    public static final String CMD_ACTION      = "&";
    public static final String CMD_LEARN       = "=";
    
    // The topic data structure, and the "thats" data structure.
    public TopicManager topics = null;
    
    // Bot's users' data structure.
    public ClientManager clients = null;
    
    // Object handlers
    public HashMap<String, ObjectHandler> handlers = null;
    public HashMap<String, String> objects         = null; // name->language mappers
    public String filesPath                        = null; // Path to .rs files
    public String perlPath                         = null; // Path to perl
   
    // Simpler internal data structures.
    public LinkedList<String> vTopics                  = null; // vector containing topic list (for quicker lookups)
    public HashMap<String, String> globals             = null; // ! global
    public HashMap<String, String> vars                = null; // ! var
    public HashMap<String, LinkedList<String>> arrays  = null; // ! array
    public HashMap<String, String> subs                = null; // ! sub
    public String[] subs_s                             = null; // sorted subs
    public HashMap<String, String> person              = null; // ! person
    public String[] person_s                           = null; // sorted persons
    
    //Log
    private final static Logger LOG = Logger.getLogger(Interpreter.class .getName()); 

    //Knowledge base objects
    public IKnowledge knowledgeManager;
    
    //Translator objects
    public ITranslation translatorManager;
    
    //Internal storage objects
    public IPersistence persistenceManager;
    
    //Property retrieval object
    public IProperty propertyManager;
    
    /**
     * Create a new Interpreter interpreter object, specifying the debug mode.
     * @param debug Enable debug mode (a *lot* of stuff is printed to the
     * terminal)
     */
    public Interpreter() throws NoSuchFieldException {

        //Knowledge base objects
        this.knowledgeManager   = KnowledgeManager.getInstance();
        this.translatorManager  = TranslationManager.getInstance();
        this.persistenceManager = PersistenceManager.getInstance();
        this.propertyManager    = PropertyManager.getInstance();
        
        //Set Interpreter properties
        this.perlPath  = propertyManager.getProperty("perl");      
        this.filesPath = propertyManager.getProperty("files");   
        
        //Initialize remaining variables
        this.topics     = new com.engine.interpretation.TopicManager();
        this.clients    = new com.engine.interpretation.ClientManager();
        this.handlers   = new HashMap<String, com.engine.interpretation.ObjectHandler>();
        this.objects    = new HashMap<String, String>();
        this.vTopics    = new LinkedList<String>(); 
        this.globals    = new HashMap<String, String>();        
        this.vars       = new HashMap<String, String>();        
        this.arrays     = new HashMap<String, LinkedList<String>>(); 
        this.subs       = new HashMap<String, String>();         
        this.subs_s     = null;                                 
        this.person     = new HashMap<String, String>();         
        this.person_s   = null;     

        setLogLevel(Level.ALL);
    }
    
    /**
     * Define logging level for the Interpreter
     * @param debugLevel 
     */
    public final void setLogLevel(Level debugLevel){
        Parser.LOG.setLevel(debugLevel);
        Interpreter.LOG.setLevel(debugLevel);
        Topic.LOG.setLevel(debugLevel);
        Client.LOG.setLevel(debugLevel);
    }

    /**
     * Load a directory full of Interpreter documents, specifying a custom list
     * of valid file extensions.
     * @param path The path to the directory containing Interpreter documents.
     * @param exts A string array containing file extensions to look for.
     */
    public boolean loadDirectory(String path, String[] exts) {
        
        //LOG.log(Level.INFO, );
        LOG.log(Level.INFO, "Load directory: {0}", path);
        // Get a directory handle.
        File dh = new File(path);

        // Search it for files.
        for (int i = 0; i < exts.length; i++) {
            // Search the directory for files of this type.
            //LOG.log(Level.INFO, "Searching for files of type: " + exts[i]);
            LOG.log(Level.INFO, "Searching for files of type: {0}", exts[i]);
            final String type = exts[i];
            String[] files = dh.list(new FilenameFilter() {
                public boolean accept(File d, String name) {
                    return name.endsWith(type);
                }
            });

            // No results?
            if (files == null) {
                //return error("Couldn't read any files from directory " + path);
                LOG.log(Level.SEVERE, "Couldn't read any files from directory {0}", path);
            }

            // Parse each file.
            for (int j = 0; j < files.length; j++) {
                Parser.loadFileRS(path + "/" + files[j], this);
            }
        }

        return true;
    }

    /**
     * Load a directory full of Interpreter documents (.rs files). If path is null
     * obtain the files from the path specified in the configuration.properties file
     * @param path The path to the directory containing Interpreter documents.
     */
    public boolean loadDirectory(String path) {
        String[] exts = {".rs"};
        return this.loadDirectory(path, exts);
    }
    
    /**
     * Load a default directory full of Interpreter documents (.rs files). If path is null
     * obtain the files from the path specified in the configuration.properties file
     * @param path The path to the directory containing Interpreter documents.
     */
    public boolean loadDefaultDirectory() {
        String[] exts = {".rs"};
        return this.loadDirectory(this.filesPath, exts);
    }

    /**
     * Stream some Interpreter code directly into the interpreter (as a single
     * string containing newlines in it).
     * @param code A string containing all the Interpreter code.
     */
    public boolean stream(String code) {
        
        // Split the given code up into lines.
        LinkedList lines = new LinkedList(Arrays.asList(code.split("\n")));

        // Send the lines to the parser.
        return Parser.parseRS(this, "(streamed)", lines);
    }

    /**
     * Stream some Interpreter code directly into the interpreter (as a string
     * array, one line per item).
     *
     * @param code A string array containing all the lines of code.
     */
    public boolean stream(String[] code) {
        
        // Split the given code up into lines.
        LinkedList lines = new LinkedList(Arrays.asList(code));
        
        // The coder has already broken the lines for us!
        return Parser.parseRS(this,"(streamed)", lines);
        //return parse("(streamed)", lines);
    }

    /**
     * Add a handler for a programming language to be used with Interpreter
     * object calls.
     * @param name The name of the programming language.
     */
    public void setHandler(ObjectHandler.Handler name) {
        
        ObjectHandler handler = null;
        switch (name){
            case PERL:
                handler = new Perl(this, this.perlPath);
                break;
            case JAVASCRIPT:
                handler = new Javascript(this);
                break;
            default:
                break;
        }
        this.handlers.put(name.name(), handler);
    }

    /**
     * Set a global variable for the interpreter (equivalent to ! global). Set
     * the value to null to delete the variable.<p>
     * There are two special globals that require certain data types:<p>
     * debug is boolean-like and its value must be a string value containing
     * "true", "yes", "1", "false", "no" or "0".<p>
     * depth is integer-like and its value must be a quoted integer like "50".
     * The "depth" variable controls how many levels deep Interpreter will go
     * when following reply redirections.<p>
     * Returns true on success, false on error.
     * @param name The variable name.
     * @param value The variable's value.
     */
    public boolean setGlobal(String name, String value) {
        
        boolean delete = false;
        
        if (value == null || "<undef>".equals(value)) {
            delete = true;
        }

        // Special globals
        if (name.equals("debug")) {
            // Debug is a boolean.
            if (value.equals("true") || value.equals("1") || value.equals("yes")) {
                setLogLevel(Level.ALL);
            } else if (value.equals("false") || value.equals("0") || value.equals("no") || delete) {
                setLogLevel(Level.OFF);
            } else {
                return false;
            }
        } else if (name.equals("depth")) {
            // Depth is an integer.
            try {
                this.depth = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // It's a user-defined global. OK.
        if (delete) {
            globals.remove(name);
        } else {
            globals.put(name, value);
        }

        return true;
    }

    /**
     * Set a bot variable for the interpreter (equivalent to ! var). A bot
     * variable is data about the chatbot, like its name or favorite color.<p>
     * A null value will delete the variable.
     * @param name The variable name.
     * @param value The variable's value.
     */
    public boolean setVariable(String name, String value) {
        if (value == null || "<undef>".equals(value)) {
            vars.remove(name);
        } else {
            vars.put(name, value);
        }

        return true;
    }

    /**
     * Set a substitution pattern (equivalent to ! sub). The user's input (and
     * the bot's reply, in %Previous) get substituted using these rules.<p>
     * A null value for the output will delete the substitution.
     * @param pattern The pattern to match in the message.
     * @param output The text to replace it with (must be lowercase, no special
     * characters).
     */
    public boolean setSubstitution(String pattern, String output) {
        if (output == null || "<undef>".equals(output)) {
            subs.remove(pattern);
        } else {
            subs.put(pattern, output);
        }

        return true;
    }

    /**
     * Set a person substitution pattern (equivalent to ! person). Person
     * substitutions swap first- and second-person pronouns, so the bot can
     * safely echo the user without sounding too mechanical.<p>
     * A null value for the output will delete the substitution.
     * @param pattern The pattern to match in the message.
     * @param output The text to replace it with (must be lowercase, no special
     * characters).
     */
    public boolean setPersonSubstitution(String pattern, String output) {
        if (output == null || "<undef>".equals(output)) {
            person.remove(pattern);
        } else {
            person.put(pattern, output);
        }

        return true;
    }

    /**
     * Set a variable for one of the bot's users. A null value will delete a
     * variable.
     * @param user The user's ID.
     * @param name The name of the variable to set.
     * @param value The value to set.
     */
    public boolean setUservar(String user, String name, String value) {
        if (value == null || "<undef>".equals(value)) {
            clients.client(user).delete(name);
        } else {
            clients.client(user).set(name, value);
        }

        return true;
    }

    /**
     * Set -all- user vars for a user. This will replace the internal hash for
     * the user. So your hash should at least contain a key/value pair for the
     * user's current "topic". This could be useful if you used getUservars to
     * store their entire profile somewhere and want to restore it later.
     * @param user The user's ID.
     * @param data The full hash of the user's data.
     */
    public boolean setUservars(String user, HashMap<String, String> data) {
        // TODO: this should be handled more sanely. ;)
        clients.client(user).setData(data);
        return true;
    }

    /**
     * Get a list of all the user IDs the bot knows about.
     * @return Collection of users
     */
    public Collection<String> getUsers() {
        // Get the user list from the clients object.
        return clients.listClients();
    }

    /**
     * Retrieve a listing of all the uservars for a user as a HashMap. Returns
     * null if the user doesn't exist.
     * @param user The user ID to get the vars for.
     */
    public HashMap<String, String> getUservars(String user) {
        if (clients.clientExists(user)) {
            return clients.client(user).getData();
        } else {
            return null;
        }
    }

    /**
     * Retrieve a single variable from a user's profile.
     * Returns null if the user doesn't exist. Returns the string "undefined" if
     * the variable doesn't exist.
     * @param user The user ID to get data from.
     * @param name The name of the variable to get.
     */
    public String getUservar(String user, String name) {
        if (clients.clientExists(user)) {
            return clients.client(user).get(name);
        } else {
            return null;
        }
    }

    /**
     * After loading replies into memory, call this method to (re)initialize
     * internal sort buffers. This is necessary for accurate trigger matching.
     */
    public void sortReplies() {
        // We need to make sort buffers under each topic.
        Object[] topicsListed = this.topics.listTopics();
        LOG.log(Level.INFO, "There are {0} topics to sort replies for.", topicsListed.length);

        // Tell the topic manager to sort its topics' replies.
        this.topics.sortReplies();

        // Sort the substitutions.
        subs_s = com.engine.interpretation.Util.sortByLength(com.engine.interpretation.Util.SSh2s(subs));
        person_s = com.engine.interpretation.Util.sortByLength(com.engine.interpretation.Util.SSh2s(person));
    }

    /**
     * Get a reply from the Interpreter interpreter.
     * @param username A unique user ID for the user chatting with the bot.
     * @param message The user's message to the bot.
     */
    public String reply(String username, String message) {
        LOG.log(Level.INFO, "Get reply to [{0}] {1}", new Object[]{username, message});

        // Format their message first.
        message = formatMessage(message);

        // This will hold the final reply.
        String reply = "";

        // If the BEGIN statement exists, consult it first.
        if (topics.exists("__begin__")) {
            String begin = this.reply(username, "request", true, 0);

            // OK to continue?
            if (begin.indexOf("{ok}") > -1) {
                // Get a reply then.
                reply = this.reply(username, message, false, 0);
                try{
                    begin = begin.replaceAll("\\{ok\\}", reply);
                }
                catch(Exception ex){
                    begin=reply;
                }
                reply = begin;
            }

            // Run final substitutions.
            reply = processTags(username, clients.client(username), message, reply,
                    new LinkedList<String>(), new LinkedList<String>(),
                    0);
        } else {
            // No BEGIN, just continue.
            reply = this.reply(username, message, false, 0);
        }

        //Execute actions and special commands if necessary
        //reply = this.executeTasksIfAny(reply);
        
        // Save their chat history.
        clients.client(username).addInput(message);
        clients.client(username).addReply(reply);

        // Return their reply.
        return reply;
    }
    
    /**
     * Internal method for getting a reply.
     * @param user The username of the calling user.
     * @param message The (formatted!) message sent by the user.
     * @param begin Whether the context is that we're in the BEGIN statement or
     * not.
     * @param step The recursion depth that we're at so far.
     */
    private String reply(String user, String message, boolean begin, int step) {

        String topic = "random";                                // Default topic = random
        LinkedList<String> stars = new LinkedList<String>();    // Wildcard matches
        LinkedList<String> botstars = new LinkedList<String>(); // Wildcards in %Previous
        String reply = "";                                      // The eventual reply
        com.engine.interpretation.Client profile;                         // The user's profile object

        // Get the user's profile.
        profile = clients.client(user);

        // Update their topic.
        topic = profile.get("topic");

        // Avoid letting the user fall into a missing topic.
        if (topics.exists(topic) == false) {
            LOG.log(Level.SEVERE, "User {0} was in a missing topic named {1}", new Object[]{user, topic});
            topic = "random";
            profile.set("topic", "random");
        }

        // Avoid deep recursion.
        if (step > depth) {
            reply = "ERR: Deep Recursion Detected!";
            LOG.log(Level.SEVERE, reply);
            return reply;
        }

        // Are we in the BEGIN statement?
        if (begin) {
            // This implies the begin topic.
            topic = "__begin__";
        }

        // Create a pointer for the matched data.
        com.engine.interpretation.Trigger matched = null;
        boolean foundMatch = false;
        String matchedTrigger = "";

        // See if there are any %previous's in this topic, or any topic related to it. This
        // should only be done the first time -- not during a recursive redirection.
        if (step == 0) {
            LOG.log(Level.INFO, "Looking for a %Previous");
            Object[] allTopics = {topic};
            //		if (this.topics.topic(topic).includes() || this.topics.topic(topic).inherits()) {
            // We need to walk the topic tree.
            allTopics = this.topics.getTopicTree(topic, 0);
            //		}
            for (int i = 0; i < allTopics.length; i++) {
                // Does this topic have a %Previous anywhere?
                LOG.log(Level.INFO, "Seeing if {0} has a %Previous", allTopics[i]);
                if (this.topics.topic(allTopics[i].toString()).hasPrevious()) {
                    LOG.log(Level.INFO, "Topic {0} has at least one %Previous", allTopics[i]);

                    // Get them.
                    Object[] previous = this.topics.topic(allTopics[i].toString()).listPrevious();
                    for (int j = 0; j < previous.length; j++) {
                        LOG.log(Level.INFO, "Candidate: {0}", previous[j].toString());

                        // Try to match the bot's last reply against this.
                        String lastReply = formatMessage(profile.getReply(1));
                        String regexp = triggerRegexp(user, profile, previous[j].toString());
                        LOG.log(Level.INFO, "Compare {0} <=> {1} ({2})", new Object[]{lastReply, previous[j].toString(), regexp});

                        // Does it match?
                        Pattern re = Pattern.compile("^" + regexp + "$");
                        Matcher m = re.matcher(lastReply.trim());
                        while (m.find() == true) {
                            LOG.log(Level.INFO, "OMFG the lastReply matches!");

                            // Harvest the botstars.
                            for (int s = 1; s <= m.groupCount(); s++) {
                                LOG.log(Level.INFO, "Add botstar: {0}", m.group(s));
                                botstars.add(m.group(s));
                            }

                            // Now see if the user matched this trigger too!
                            Object[] candidates = this.topics.topic(allTopics[i].toString()).listPreviousTriggers(previous[j].toString());
                            for (int k = 0; k < candidates.length; k++) {
                                LOG.log(Level.INFO, "Does the user''s message match {0}?", candidates[k]);
                                String humanside = triggerRegexp(user, profile, candidates[k].toString());
                                LOG.log(Level.INFO, "Compare {0} <=> {1} ({2})", new Object[]{message, candidates[k].toString(), humanside});

                                Pattern reH = Pattern.compile("^" + humanside + "$");
                                Matcher mH = reH.matcher(message);
                                while (mH.find() == true) {
                                    LOG.log(Level.INFO, "It's a match!!!");

                                    // Make sure it's all valid.
                                    String realTrigger = candidates[k].toString() + "{previous}" + previous[j].toString();
                                    if (this.topics.topic(allTopics[i].toString()).triggerExists(realTrigger)) {
                                        // Seems to be! Collect the stars.
                                        for (int s = 1; s <= mH.groupCount(); s++) {
                                            LOG.log(Level.INFO, "Add star: {0}", mH.group(s));
                                            stars.add(mH.group(s));
                                        }

                                        foundMatch = true;
                                        matchedTrigger = candidates[k].toString();
                                        matched = this.topics.topic(allTopics[i].toString()).trigger(realTrigger);
                                    }

                                    break;
                                }

                                if (foundMatch) {
                                    break;
                                }
                            }
                            if (foundMatch) {
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Search their topic for a match to their trigger.
        if (foundMatch == false) {
            // Go through the sort buffer for their topic.
            Object[] triggers = topics.topic(topic).listTriggers();
            for (int a = 0; a < triggers.length; a++) {
                String trigger = triggers[a].toString();

                // Prepare the trigger for the regular expression engine.
                String regexp = triggerRegexp(user, profile, trigger);
                LOG.log(Level.INFO, "Try to match \"{0}\" against \"{1}\" ({2})", new Object[]{message, trigger, regexp});

                // Is it a match?
                Pattern re = Pattern.compile("^" + regexp + "$");
                Matcher m = re.matcher(message);
                if (m.find() == true) {
                    LOG.log(Level.INFO, "The trigger matches! Star count: {0}", m.groupCount());

                    // Harvest the stars.
                    int starcount = m.groupCount();
                    for (int s = 1; s <= starcount; s++) {
                        LOG.log(Level.INFO, "Add star: {0}", m.group(s));
                        stars.add(m.group(s));
                    }

                    // We found a match, but what if the trigger we matched belongs to
                    // an inherited topic? Check for that.
                    if (this.topics.topic(topic).triggerExists(trigger)) {
                        // No, the trigger does belong to us.
                        matched = this.topics.topic(topic).trigger(trigger);
                    } else {
                        LOG.log(Level.INFO, "Trigger doesn't exist under this topic, trying to find it!");
                        matched = this.topics.findTriggerByInheritance(topic, trigger, 0);
                    }

                    foundMatch = true;
                    matchedTrigger = trigger;
                    break;
                }
            }
        }

        // Store what trigger they matched on (matchedTrigger can be blank if they didn't match).
        profile.set("__lastmatch__", matchedTrigger);

        // Did they match anything?
        if (foundMatch) {
            LOG.log(Level.INFO, "They were successfully matched to a trigger!");

            // Make a dummy once loop so we can break out anytime.
            for (int n = 0; n < 1; n++) {
                // Exists?
                if (matched == null) {
                    LOG.log(Level.SEVERE, "Unknown error: they matched trigger {0}, but it doesn''t exist?", matchedTrigger);
                    foundMatch = false;
                    break;
                }

                // Get the trigger object.
                Trigger trigger = matched;
                LOG.log(Level.INFO, "The trigger matched belongs to topic {0}", trigger.topic());

                // Check for actions
                if (trigger.hasAction() && trigger.isPersistent()){
                    for ( Iterator<Action> actionsIt = trigger.listActions().iterator(); actionsIt.hasNext(); ) {
                       Action action = actionsIt.next();
                       //Search for knowledge
                       if (action.getType().equals(Action.type.KNOWLEDGE_SEARCH)){
                            
                            String localMessage = null;
                            
                            //Try to find the whole message
                            localMessage = persistenceManager.find(message, stars);
                            
                            //We havent found the full message, try with the wildcards
                            /*if (StringUtils.isEmpty(localMessage)){
                                localMessage = persistenceManager.find(stars.getFirst());
                            }*/
                            
                            //Data was found in local repository
                            if (!StringUtils.isEmpty(localMessage)){
                                return localMessage;
                            } 
                            //Data must be found using knowledge master
                            else {
                                String translatedMessage;
                                //Translation needed for knowledge search
                                if (!knowledgeManager.getDefaultLanguage().equals(this.language)){
                                    translatedMessage = translatorManager.translate(message, knowledgeManager.getDefaultLanguage());
                                } else {
                                    translatedMessage = message;
                                }
                                
                                String rpl = knowledgeManager.answer(translatedMessage);
                                
                                if (!knowledgeManager.getDefaultLanguage().equals(this.language)){
                                    reply = translatorManager.translate(rpl, this.language);
                                } else {
                                    reply = rpl;
                                }
                                //If result was found and trigger reply is persistent, which 
                                //means it does not need to be updated everytime the question is asked
                                if (reply!= null && trigger.isPersistent()) {
                                    persistenceManager.save(message, reply);
                                    return reply;
                                }
                            }
                            break;
                       }
                    }
                }
                
                // Check for non persistence (learning content)
                if (!trigger.isPersistent()){
                    String temp = trigger.listReplies().iterator().next();
                    temp = replaceWildcardsWithValues(trigger.listReplies().iterator().next(),stars);
                    
                    // <set> tag
                    if (temp.indexOf("<set") > -1) {
                        Pattern reSet = Pattern.compile("<set (.+?)=(.+?)>");
                        Matcher mSet = reSet.matcher(temp);
                        while (mSet.find()) {
                            String tag = mSet.group(0);
                            String var = mSet.group(1);
                            String value = mSet.group(2);

                            // Set the uservar.
                            vars.put(var, value);
                            LOG.log(Level.INFO, "Set user var {0}={1}", new Object[]{var, value});
                        }
                    }
                    
                    persistenceManager.save(vars.get("subject"), vars.get("definition"));
                }
                
                // Check for conditions.
                Object[] conditions = trigger.listConditions().toArray();
                if (conditions.length > 0) {
                    LOG.log(Level.INFO, "This trigger has some conditions!");

                    // See if any conditions are true.
                    boolean truth = false;
                    for (int c = 0; c < conditions.length; c++) {
                        // Separate the condition from the potential reply.
                        String[] halves = conditions[c].toString().split("\\s*=>\\s*");
                        String condition = halves[0].trim();
                        String potreply = halves[1].trim();

                        // Split up the condition.
                        Pattern reCond = Pattern.compile("^(.+?)\\s+(==|eq|startsWith|\\!=|ne|<>|<|<=|>|>=)\\s+(.+?)$");
                        Matcher mCond = reCond.matcher(condition);
                        while (mCond.find()) {
                            String left = mCond.group(1).trim();
                            String eq = mCond.group(2).trim();
                            String right = mCond.group(3).trim();

                            // Process tags on both halves.
                            left = processTags(user, profile, message, left, stars, botstars, step + 1);
                            right = processTags(user, profile, message, right, stars, botstars, step + 1);
                            LOG.log(Level.INFO, "Compare: {0} {1} {2}", new Object[]{left, eq, right});

                            // Defaults
                            if (left.length() == 0) {
                                left = "undefined";
                            }
                            if (right.length() == 0) {
                                right = "undefined";
                            }

                            // Validate the expression.
                            if (eq.equals("eq") || eq.equals("ne") || eq.equals("==") || eq.equals("!=") || eq.equals("<>")) {
                                // String equality comparing.
                                if ((eq.equals("eq") || eq.equals("==")) && left.equals(right)) {
                                    truth = true;
                                    break;
                                } else if ((eq.equals("ne") || eq.equals("!=") || eq.equals("<>")) && !left.equals(right)) {
                                    truth = true;
                                    break;
                                }
                            } 
                            //Validate String related equalities
                            else if (eq.equals("startsWith") && left.startsWith(right)){
                                truth = true;
                                break;
                            }

                            // Numeric comparing.
                            int lt = 0;
                            int rt = 0;

                            // Turn the two sides into numbers.
                            try {
                                lt = Integer.parseInt(left);
                                rt = Integer.parseInt(right);
                            } catch (NumberFormatException e) {
                                // Oh well!
                                break;
                            }

                            // Run the remaining equality checks.
                            if (eq.equals("==") || eq.equals("!=") || eq.equals("<>")) {
                                // Equality checks.
                                if (eq.equals("==") && lt == rt) {
                                    truth = true;
                                    break;
                                } else if ((eq.equals("!=") || eq.equals("<>")) && lt != rt) {
                                    truth = true;
                                    break;
                                }
                            } else if (eq.equals("<") && lt < rt) {
                                truth = true;
                                break;
                            } else if (eq.equals("<=") && lt <= rt) {
                                truth = true;
                                break;
                            } else if (eq.equals(">") && lt > rt) {
                                truth = true;
                                break;
                            } else if (eq.equals(">=") && lt >= rt) {
                                truth = true;
                                break;
                            }
                        }

                        // True condition?
                        if (truth) {
                            reply = potreply;
                            break;
                        }
                    }
                }

                // Break if we got a reply from the conditions.
                if (reply.length() > 0) {
                    break;
                }

                // Return one of the replies at random. We lump any redirects in as well.
                Object[] redirects = trigger.listRedirects().toArray();
                Object[] replies = trigger.listReplies().toArray();

                // Take into account their weights.
                LinkedList<Integer> bucket = new LinkedList<Integer>();
                Pattern reWeight = Pattern.compile("\\{weight=(\\d+?)\\}");

                // Look at weights on redirects.
                for (int i = 0; i < redirects.length; i++) {
                    if (redirects[i].toString().indexOf("{weight=") > -1) {
                        Matcher mWeight = reWeight.matcher(redirects[i].toString());
                        while (mWeight.find()) {
                            int weight = Integer.parseInt(mWeight.group(1));

                            // Add to the bucket this many times.
                            if (weight > 1) {
                                for (int j = 0; j < weight; j++) {
                                    LOG.log(Level.INFO, "Trigger has a redirect (weight {0}): {1}", new Object[]{weight, redirects[i]});
                                    bucket.add(i);
                                }
                            } else {
                                LOG.log(Level.INFO, "Trigger has a redirect (weight {0}): {1}", new Object[]{weight, redirects[i]});
                                bucket.add(i);
                            }

                            // Only one weight is supported.
                            break;
                        }
                    } else {
                        LOG.log(Level.INFO, "Trigger has a redirect: {0}", redirects[i]);
                        bucket.add(i);
                    }
                }

                // Look at weights on replies.
                for (int i = 0; i < replies.length; i++) {
                    if (replies[i].toString().indexOf("{weight=") > -1) {
                        Matcher mWeight = reWeight.matcher(replies[i].toString());
                        while (mWeight.find()) {
                            int weight = Integer.parseInt(mWeight.group(1));

                            // Add to the bucket this many times.
                            if (weight > 1) {
                                for (int j = 0; j < weight; j++) {
                                    LOG.log(Level.INFO, "Trigger has a reply (weight {0}): {1}", new Object[]{weight, replies[i]});
                                    bucket.add(redirects.length + i);
                                }
                            } else {
                                LOG.log(Level.INFO, "Trigger has a reply (weight {0}): {1}", new Object[]{weight, replies[i]});
                                bucket.add(redirects.length + i);
                            }

                            // Only one weight is supported.
                            break;
                        }
                    } else {
                        LOG.log(Level.INFO, "Trigger has a reply: {0}", replies[i]);
                        bucket.add(redirects.length + i);
                    }
                }

                // Pull a random value out.
                Object[] choices = bucket.toArray();
                if (choices.length > 0) {
                    int choice = (Integer)choices[ rand.nextInt(choices.length)];
                    LOG.log(Level.INFO, "Possible choices: {0}; chosen: {1}", new Object[]{choices.length, choice});
                    if (choice < redirects.length) {
                        // The choice was a redirect!
                        String redirect = redirects[choice].toString().replaceAll("\\{weight=\\d+\\}", "");
                        LOG.log(Level.INFO, "Chosen a redirect to {0}!", redirect);
                        reply = reply(user, redirect, begin, step + 1);
                    } else {
                        // The choice was a reply!
                        choice -= redirects.length;
                        if (choice < replies.length) {
                            LOG.log(Level.INFO, "Chosen a reply: {0}", replies[choice]);
                            reply = replies[choice].toString();
                        }
                    }
                }
            }
        }

        // Still no reply?
        if (!foundMatch) {
            reply = "ERR: No Reply Matched";
        } else if (reply.length() == 0) {
            reply = "ERR: No Reply Found";
        }

        LOG.log(Level.INFO, "Final reply: {0}", reply);

        // Special tag processing for the BEGIN statement.
        if (begin) {
            // The BEGIN block may have {topic} or <set> tags and that's all.
            // <set> tag
            if (reply.indexOf("<set") > -1) {
                Pattern reSet = Pattern.compile("<set (.+?)=(.+?)>");
                Matcher mSet = reSet.matcher(reply);
                while (mSet.find()) {
                    String tag = mSet.group(0);
                    String var = mSet.group(1);
                    String value = mSet.group(2);

                    // Set the uservar.
                    profile.set(var, value);
                    reply = reply.replace(tag, "");
                }
            }

            // {topic} tag
            if (reply.indexOf("{topic=") > -1) {
                Pattern reTopic = Pattern.compile("\\{topic=(.+?)\\}");
                Matcher mTopic = reTopic.matcher(reply);
                while (mTopic.find()) {
                    String tag = mTopic.group(0);
                    topic = mTopic.group(1);
                    LOG.log(Level.INFO, "Set user''s topic to: {0}", topic);
                    profile.set("topic", topic);
                    reply = reply.replace(tag, "");
                }
            }
        } else {
            // Process tags.
            reply = processTags(user, profile, message, reply, stars, botstars, step);
        }

        return reply;
    }
    
    private String replaceWildcardsWithValues(String text, LinkedList<String> stars){
        text = text.replaceAll("<star>", stars.get(0).toString());
        for (int i = 1; i < stars.size(); i++) {
            int starIndex = i+1;
            text = text.replaceAll("<star" + starIndex + ">", stars.get(i).toString());
        }

        return text;
    }
    
    /**
     * Formats a trigger for the regular expression engine.
     * @param user The user ID of the caller.
     * @param trigger The raw trigger text.
     */
    private String triggerRegexp(String user, com.engine.interpretation.Client profile, String trigger) {
        // If the trigger is simply '*', it needs to become (.*?) so it catches the empty string.
        String regexp = trigger.replaceAll("^\\*$", "<zerowidthstar>");

        // Simple regexps are simple.
        regexp = regexp.replaceAll("\\*", "(.+?)");             // *  ->  (.+?)
        regexp = regexp.replaceAll("#", "(\\\\d+?)");         // #  ->  (\d+?)
        regexp = regexp.replaceAll("_", "(\\\\w+?)");     // _  ->  ([A-Za-z ]+?)
        regexp = regexp.replaceAll("\\{weight=\\d+\\}", "");    // Remove {weight} tags
        regexp = regexp.replaceAll("<zerowidthstar>", "(.*?)"); // *  ->  (.*?)

        // Handle optionals.
        if (regexp.indexOf("[") > -1) {
            Pattern reOpts = Pattern.compile("\\s*\\[(.+?)\\]\\s*");
            Matcher mOpts = reOpts.matcher(regexp);
            while (mOpts.find() == true) {
                String optional = mOpts.group(0);
                String contents = mOpts.group(1);

                // Split them at the pipes.
                String[] parts = contents.split("\\|");

                // Construct a regexp part.
                StringBuilder re = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    // We want: \s*part\s*
                    re.append("\\s*").append(parts[i]).append("\\s*");
                    if (i < parts.length - 1) {
                        re.append("|");
                    }
                }
                String pipes = re.toString();

                // If this optional had a star or anything in it, e.g. [*],
                // make it non-matching.
                pipes = pipes.replaceAll("\\(.+?\\)", "(?:.+?)");
                pipes = pipes.replaceAll("\\(\\d+?\\)", "(?:\\\\d+?");
                pipes = pipes.replaceAll("\\(\\w+?\\)", "(?:\\\\w+?)");

                // Put the new text in.
                pipes = "(?:" + pipes + "|\\s*)";
                regexp = regexp.replace(optional, pipes);
            }
        }

        // Make \w more accurate for our purposes.
        regexp = regexp.replaceAll("\\\\w", "[a-z ]");

        // Filter in arrays.
        if (regexp.indexOf("@") > -1) {
            // Match the array's name.
            Pattern reArray = Pattern.compile("\\@(.+?)\\b");
            Matcher mArray = reArray.matcher(regexp);
            while (mArray.find() == true) {
                String array = mArray.group(0);
                String name = mArray.group(1);

                // Do we have an array by this name?
                if (arrays.containsKey(name)) {
                    Object[] values = arrays.get(name).toArray();
                    StringBuilder joined = new StringBuilder();

                    // Join the array.
                    for (int i = 0; i < values.length; i++) {
                        joined.append(values[i].toString());
                        if (i < values.length - 1) {
                            joined.append("|");
                        }
                    }

                    // Final contents...
                    String rep = "(?:" + joined.toString() + ")";
                    regexp = regexp.replace(array, rep);
                } else {
                    // No array by this name.
                    regexp = regexp.replace(array, "");
                }
            }
        }

        // Filter in bot variables.
        if (regexp.indexOf("<bot") > -1) {
            Pattern reBot = Pattern.compile("<bot (.+?)>");
            Matcher mBot = reBot.matcher(regexp);
            while (mBot.find()) {
                String tag = mBot.group(0);
                String var = mBot.group(1);
                String value = vars.get(var).toLowerCase();//.replace("[^a-z0-9 ]+", "");

                // Have this?
                if (vars.containsKey(var)) {
                    regexp = regexp.replace(tag, value);
                } else {
                    regexp = regexp.replace(tag, "undefined");
                }
            }
        }

        // Filter in user variables.
        if (regexp.indexOf("<get") > -1) {
            Pattern reGet = Pattern.compile("<get (.+?)>");
            Matcher mGet = reGet.matcher(regexp);
            while (mGet.find()) {
                String tag = mGet.group(0);
                String var = mGet.group(1);
                String value = profile.get(var).toLowerCase();//.replaceAll("[^a-z0-9 ]+", "");

                // Have this?
                regexp = regexp.replace(tag, value);
            }
        }

        // Input and reply tags.
        regexp = regexp.replaceAll("<input>", "<input1>");
        regexp = regexp.replaceAll("<reply>", "<reply1>");
        if (regexp.indexOf("<input") > -1) {
            Pattern reInput = Pattern.compile("<input([0-9])>");
            Matcher mInput = reInput.matcher(regexp);
            while (mInput.find()) {
                String tag = mInput.group(0);
                int index = Integer.parseInt(mInput.group(1));
                String text = profile.getInput(index).toLowerCase();//.replaceAll("[^a-z0-9 ]+", "");
                regexp = regexp.replace(tag, text);
            }
        }
        if (regexp.indexOf("<reply") > -1) {
            Pattern reReply = Pattern.compile("<reply([0-9])>");
            Matcher mReply = reReply.matcher(regexp);
            while (mReply.find()) {
                String tag = mReply.group(0);
                int index = Integer.parseInt(mReply.group(1));
                String text = profile.getReply(index).toLowerCase();//.replaceAll("[^a-z0-9 ]+", "");
                regexp = regexp.replace(tag, text);
            }
        }

        return regexp;
    }

    /**
     * Process reply tags.
     * @param user The name of the end user.
     * @param profile The Interpreter client object holding the user's profile
     * @param message The message sent by the user.
     * @param reply The bot's original reply including tags.
     * @param stars The vector of wildcards the user's message matched.
     * @param botstars The vector of wildcards in any %Previous.
     * @param step The current recursion depth limit.
     */
    private String processTags(String user, com.engine.interpretation.Client profile,
            String message, String reply, LinkedList<String> vstars, LinkedList<String> vbotstars, int step) {
        // Pad the stars.
        vstars.add(0, "");
        vbotstars.add(0, "");

        // Set a default first star.
        if (vstars.size() == 1) {
            vstars.add("undefined");
        }
        if (vbotstars.size() == 1) {
            vbotstars.add("undefined");
        }

        // Convert the stars into simple arrays.
        Object[] stars = vstars.toArray();
        Object[] botstars = vbotstars.toArray();

        // Shortcut tags.
        reply = reply.replaceAll("<person>", "{person}<star>{/person}");
        reply = reply.replaceAll("<@>", "{@<star>}");
        reply = reply.replaceAll("<formal>", "{formal}<star>{/formal}");
        reply = reply.replaceAll("<sentence>", "{sentence}<star>{/sentence}");
        reply = reply.replaceAll("<uppercase>", "{uppercase}<star>{/uppercase}");
        reply = reply.replaceAll("<lowercase>", "{lowercase}<star>{/lowercase}");

        // Quick tags.
        reply = reply.replaceAll("\\{weight=\\d+\\}", ""); // Remove {weight}s
        reply = reply.replaceAll("<input>", "<input1>");
        reply = reply.replaceAll("<reply>", "<reply1>");
        reply = reply.replaceAll("<id>", user);
        reply = reply.replaceAll("\\\\s", " ");
        reply = reply.replaceAll("\\\\n", "\n");
        reply = reply.replaceAll("\\\\", "\\");
        reply = reply.replaceAll("\\#", "#");

        // Stars
        reply = reply.replaceAll("<star>", stars[1].toString());
        reply = reply.replaceAll("<botstar>", botstars[1].toString());
        for (int i = 1; i < stars.length; i++) {
            reply = reply.replaceAll("<star" + i + ">", stars[i].toString());
        }
        for (int i = 1; i < botstars.length; i++) {
            reply = reply.replaceAll("<botstar" + i + ">", botstars[i].toString());
        }
        reply = reply.replaceAll("<(star|botstar)\\d+>", "");

        // Input and reply tags.
        if (reply.indexOf("<input") > -1) {
            Pattern reInput = Pattern.compile("<input([0-9])>");
            Matcher mInput = reInput.matcher(reply);
            while (mInput.find()) {
                String tag = mInput.group(0);
                int index = Integer.parseInt(mInput.group(1));
                String text = profile.getInput(index).toLowerCase();//.replaceAll("[^a-z0-9 ]+", "");
                reply = reply.replace(tag, text);
            }
        }
        if (reply.indexOf("<reply") > -1) {
            Pattern reReply = Pattern.compile("<reply([0-9])>");
            Matcher mReply = reReply.matcher(reply);
            while (mReply.find()) {
                String tag = mReply.group(0);
                int index = Integer.parseInt(mReply.group(1));
                String text = profile.getReply(index).toLowerCase();//.replaceAll("[^a-z0-9 ]+", "");
                reply = reply.replace(tag, text);
            }
        }

        // {random} tag
        if (reply.indexOf("{random}") > -1) {
            Pattern reRandom = Pattern.compile("\\{random\\}(.+?)\\{\\/random\\}");
            Matcher mRandom = reRandom.matcher(reply);
            while (mRandom.find()) {
                String tag = mRandom.group(0);
                String[] candidates = mRandom.group(1).split("\\|");
                String chosen = candidates[ rand.nextInt(candidates.length)];
                reply = reply.replace(tag, chosen);
            }
        }

        // <bot> tag
        if (reply.indexOf("<bot") > -1) {
            Pattern reBot = Pattern.compile("<bot (.+?)>");
            Matcher mBot = reBot.matcher(reply);
            while (mBot.find()) {
                String tag = mBot.group(0);
                String var = mBot.group(1);

                // Have this?
                if (vars.containsKey(var)) {
                    reply = reply.replace(tag, vars.get(var));
                } else {
                    reply = reply.replace(tag, "undefined");
                }
            }
        }

        // <env> tag
        if (reply.indexOf("<env") > -1) {
            Pattern reEnv = Pattern.compile("<env (.+?)>");
            Matcher mEnv = reEnv.matcher(reply);
            while (mEnv.find()) {
                String tag = mEnv.group(0);
                String var = mEnv.group(1);

                // Have this?
                if (globals.containsKey(var)) {
                    reply = reply.replace(tag, globals.get(var));
                } else {
                    reply = reply.replace(tag, "undefined");
                }
            }
        }

        // {!stream} tag
        if (reply.indexOf("{!") > -1) {
            Pattern reStream = Pattern.compile("\\{\\!(.+?)\\}");
            Matcher mStream = reStream.matcher(reply);
            while (mStream.find()) {
                String tag = mStream.group(0);
                String code = mStream.group(1);
                LOG.log(Level.INFO, "Stream new code in: {0}", code);

                // Stream it.
                this.stream(code);
                reply = reply.replace(tag, "");
            }
        }

        // {person}
        if (reply.indexOf("{person}") > -1) {
            Pattern rePerson = Pattern.compile("\\{person\\}(.+?)\\{\\/person\\}");
            Matcher mPerson = rePerson.matcher(reply);
            while (mPerson.find()) {
                String tag = mPerson.group(0);
                String text = mPerson.group(1);

                // Run person substitutions.
                LOG.log(Level.INFO, "Run person substitutions: before: {0}", text);
                text = com.engine.interpretation.Util.substitute(person_s, person, text);
                LOG.log(Level.INFO, "After: {0}", text);
                reply = reply.replace(tag, text);
            }
        }

        // {formal,uppercase,lowercase,sentence} tags
        if (reply.indexOf("{formal}") > -1 || reply.indexOf("{sentence}") > -1
                || reply.indexOf("{uppercase}") > -1 || reply.indexOf("{lowercase}") > -1) {
            String[] tags = {"formal", "sentence", "uppercase", "lowercase"};
            for (int i = 0; i < tags.length; i++) {
                Pattern reTag = Pattern.compile("\\{" + tags[i] + "\\}(.+?)\\{\\/" + tags[i] + "\\}");
                Matcher mTag = reTag.matcher(reply);
                while (mTag.find()) {
                    String tag = mTag.group(0);
                    String text = mTag.group(1);

                    // String transform.
                    text = stringTransform(tags[i], text);
                    reply = reply.replace(tag, text);
                }
            }
        }

        // <set> tag
        if (reply.indexOf("<set") > -1) {
            Pattern reSet = Pattern.compile("<set (.+?)=(.+?)>");
            Matcher mSet = reSet.matcher(reply);
            while (mSet.find()) {
                String tag = mSet.group(0);
                String var = mSet.group(1);
                String value = mSet.group(2);

                // Set the uservar.
                profile.set(var, value);
                reply = reply.replace(tag, "");
                LOG.log(Level.INFO, "Set user var {0}={1}", new Object[]{var, value});
            }
        }

        // <add, sub, mult, div> tags
        if (reply.indexOf("<add") > -1 || reply.indexOf("<sub") > -1
                || reply.indexOf("<mult") > -1 || reply.indexOf("<div") > -1) {
            String[] tags = {"add", "sub", "mult", "div"};
            for (int i = 0; i < tags.length; i++) {
                Pattern reTag = Pattern.compile("<" + tags[i] + " (.+?)=(.+?)>");
                Matcher mTag = reTag.matcher(reply);
                while (mTag.find()) {
                    String tag = mTag.group(0);
                    String var = mTag.group(1);
                    String value = mTag.group(2);

                    // Get the user var.
                    String curvalue = profile.get(var);
                    int current = 0;
                    if (!curvalue.equals("undefined")) {
                        // Convert it to a int.
                        try {
                            current = Integer.parseInt(curvalue);
                        } catch (NumberFormatException e) {
                            // Current value isn't a number!
                            reply = reply.replace(tag, "[ERR: Can't \"" + tags[i] + "\" non-numeric variable " + var + "]");
                            continue;
                        }
                    }

                    // Value must be a number too.
                    int modifier = 0;
                    try {
                        modifier = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        reply = reply.replace(tag, "[ERR: Can't \"" + tags[i] + "\" non-numeric value " + value + "]");
                        continue;
                    }

                    // Run the operation.
                    if (tags[i].equals("add")) {
                        current += modifier;
                    } else if (tags[i].equals("sub")) {
                        current -= modifier;
                    } else if (tags[i].equals("mult")) {
                        current *= modifier;
                    } else {
                        // Don't divide by zero.
                        if (modifier == 0) {
                            reply = reply.replace(tag, "[ERR: Can't divide by zero!]");
                            continue;
                        }
                        current /= modifier;
                    }

                    // Store the new value.
                    profile.set(var, Integer.toString(current));
                    reply = reply.replace(tag, "");
                }
            }
        }

        // <get> tag
        if (reply.indexOf("<get") > -1) {
            Pattern reGet = Pattern.compile("<get (.+?)>");
            Matcher mGet = reGet.matcher(reply);
            while (mGet.find()) {
                String tag = mGet.group(0);
                String var = mGet.group(1);

                // Get the user var.
                reply = reply.replace(tag, profile.get(var));
            }
        }

        // {topic} tag
        if (reply.indexOf("{topic=") > -1) {
            Pattern reTopic = Pattern.compile("\\{topic=(.+?)\\}");
            Matcher mTopic = reTopic.matcher(reply);
            while (mTopic.find()) {
                String tag = mTopic.group(0);
                String topic = mTopic.group(1);
                LOG.log(Level.INFO, "Set user''s topic to: {0}", topic);
                profile.set("topic", topic);
                reply = reply.replace(tag, "");
            }
        }

        // {@redirect} tag
        if (reply.indexOf("{@") > -1) {
            Pattern reRed = Pattern.compile("\\{@(.+?)\\}");
            Matcher mRed = reRed.matcher(reply);
            while (mRed.find()) {
                String tag = mRed.group(0);
                String target = mRed.group(1).trim();

                // Do the reply redirect.
                String subreply = this.reply(user, target, false, step + 1);
                reply = reply.replace(tag, subreply);
            }
        }

        // <call> tag
        if (reply.indexOf("<call>") > -1) {
            Pattern reCall = Pattern.compile("<call>(.+?)<\\/call>");
            Matcher mCall = reCall.matcher(reply);
            while (mCall.find()) {
                String tag = mCall.group(0);
                String data = mCall.group(1);
                String[] parts = data.split(" ");
                String name = parts[0];
                LinkedList<String> args = new LinkedList<String>();
                for (int i = 1; i < parts.length; i++) {
                    args.add(parts[i]);
                }

                // See if we know of this object.
                if (objects.containsKey(name)) {
                    // What language handles it?
                    String lang = objects.get(name);
                    String result = handlers.get(lang).onCall(name, user, args.toArray());
                    reply = reply.replace(tag, result);
                } else {
                    reply = reply.replace(tag, "[ERR: Object Not Found]");
                }
            }
        }

        return reply;
    }

    /**
     * Reformats a string in a certain way: formal, uppercase, lowercase,
     * sentence.
     * @param format The format you want the string in.
     * @param text The text to format.
     */
    private String stringTransform(String format, String text) {
        if (format.equals("uppercase")) {
            return text.toUpperCase();
        } else if (format.equals("lowercase")) {
            return text.toLowerCase();
        } else if (format.equals("formal")) {
            // Capitalize Each First Letter
            String[] words = text.split(" ");
            LOG.log(Level.INFO, "wc: {0}", words.length);
            for (int i = 0; i < words.length; i++) {
                LOG.log(Level.INFO, "word: {0}", words[i]);
                String[] letters = words[i].split("");
                LOG.log(Level.INFO, "cc: {0}", letters.length);
                if (letters.length > 1) {
                    LOG.log(Level.INFO, "letter 1: {0}", letters[1]);
                    letters[1] = letters[1].toUpperCase();
                    LOG.log(Level.INFO, "new letter 1: {0}", letters[1]);
                    words[i] = com.engine.interpretation.Util.join(letters, "");
                    LOG.log(Level.INFO, "new word: {0}", words[i]);
                }
            }
            return com.engine.interpretation.Util.join(words, " ");
        } else if (format.equals("sentence")) {
            // Uppercase the first letter of the first word.
            String[] letters = text.split("");
            if (letters.length > 1) {
                letters[1] = letters[1].toUpperCase();
            }
            return com.engine.interpretation.Util.join(letters, "");
        } else {
            return "[ERR: Unknown String Transform " + format + "]";
        }
    }

    /**
     * Format the user's message to begin reply matching. Lowercases it, runs
     * substitutions, and neutralizes what's left.
     * @param message The input message to format.
     */
    private String formatMessage(String message) {
        // Lowercase it first.
        message = message.toLowerCase();

        message = Utils.removeAccents(message);
        // Run substitutions.
        message = com.engine.interpretation.Util.substitute(subs_s, subs, message);

        // Sanitize what's left.
        //message = message.replaceAll("[^a-z0-9 ]", "");
        return message;
    }

    /**
     * DEVELOPER: Dump the trigger sort buffers to the terminal.
     */
    public void dumpSorted() {
        Object[] topicsListed = this.topics.listTopics();
        for (int t = 0; t < topicsListed.length; t++) {
            String topic = topicsListed[t].toString();
            Object[] triggers = this.topics.topic(topic).listTriggers();

            // Dump.
            System.out.println("Topic: " + topic);
            for (int i = 0; i < triggers.length; i++) {
                System.out.println("       " + triggers[i].toString());
            }
        }
    }

    /**
     * DEVELOPER: Dump the entire topic/trigger/reply structure to the terminal.
     */
    public void dumpTopics() {
        // Dump the topic list.
        System.out.println("{");
        Object[] topicList = topics.listTopics();
        for (int t = 0; t < topicList.length; t++) {
            String topic = topicList[t].toString();
            String extra = "";

            // Includes? Inherits?
            Object[] includes = topics.topic(topic).includes();
            Object[] inherits = topics.topic(topic).inherits();
            if (includes.length > 0) {
                extra = "includes ";
                for (int i = 0; i < includes.length; i++) {
                    extra += includes[i].toString() + " ";
                }
            }
            if (inherits.length > 0) {
                extra += "inherits ";
                for (int i = 0; i < inherits.length; i++) {
                    extra += inherits[i].toString() + " ";
                }
            }
            System.out.println("  '" + topic + "' " + extra + " => {");

            // Dump the trigger list.
            Object[] trigList = topics.topic(topic).listTriggers();
            for (int i = 0; i < trigList.length; i++) {
                String trig = trigList[i].toString();
                System.out.println("    '" + trig + "' => {");

                // Dump the replies.
                Object[] reply = topics.topic(topic).trigger(trig).listReplies().toArray();
                if (reply.length > 0) {
                    System.out.println("      'reply' => [");
                    for (int r = 0; r < reply.length; r++) {
                        System.out.println("        '" + reply[r].toString() + "',");
                    }
                    System.out.println("      ],");
                }

                // Dump the conditions.
                Object[] cond = topics.topic(topic).trigger(trig).listConditions().toArray();
                if (cond.length > 0) {
                    System.out.println("      'condition' => [");
                    for (int r = 0; r < cond.length; r++) {
                        System.out.println("        '" + cond[r].toString() + "',");
                    }
                    System.out.println("      ],");
                }

                // Dump the redirects.
                Object[] red = topics.topic(topic).trigger(trig).listRedirects().toArray();
                if (red.length > 0) {
                    System.out.println("      'redirect' => [");
                    for (int r = 0; r < red.length; r++) {
                        System.out.println("        '" + red[r].toString() + "',");
                    }
                    System.out.println("      ],");
                }

                System.out.println("    },");
            }

            System.out.println("  },");
        }
    }
}