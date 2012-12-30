package com.engine.persistence;

import com.engine.analysis.AnalysisManager;
import com.engine.analysis.IAnalysis;
import com.engine.property.IProperty;
import com.engine.property.PropertyManager;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * This class handles the persistence against a MongoDB engine
 *
 * @author Giank
 */
public class PersistenceMongoDB implements IPersistence {

    //Singleton object instance
    private static PersistenceMongoDB instance = null;
    //Defines the type of engine this class is running for storage
    private static PersistenceManager.Source source;
    //Collection handlers
    private DBCollection knowledge;
    private MongoClient client;
    //Log
    private static final Logger LOG = Logger.getLogger(PersistenceMongoDB.class.getName());

    /**
     * Initializes MongoDB connection and collection loading for knowledge
     */
    protected PersistenceMongoDB() {

        source = PersistenceManager.Source.MONGODB;
        String address = null;
        String login = null;
        String password = null;
        String database = null;
        String collection = null;
        Integer port = null;

        try {
            //Load properties
            IProperty properties = PropertyManager.getInstance();
            address = properties.getProperty("mongo_url");
            login = properties.getProperty("mongo_login");
            password = properties.getProperty("mongo_pass");
            database = properties.getProperty("mongo_database");
            collection = properties.getProperty("mongo_collection");
            port = Integer.valueOf(properties.getProperty("mongo_port"));
            //Initialize connection
            client = new MongoClient(address, port);
            DB db = client.getDB(database);
            db.authenticate(login, password.toCharArray());
            knowledge = db.getCollection(collection);

        } catch (UnknownHostException ex) {
            LOG.log(Level.SEVERE, "UnknownHostException - PersistenceMongoDB constructor could not initialize the internal knowledge base"
                    + " .ip:" + address
                    + " .port:" + port
                    + " .user:" + login
                    + " .password:" + password
                    + " .database:" + database
                    + " .collection:" + collection, ex);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception - PersistenceMongoDB constructor could not initialize the internal knowledge base"
                    + " .ip:" + address
                    + " .port:" + port
                    + " .user:" + login
                    + " .password:" + password
                    + " .database:" + database
                    + " .collection:" + collection, ex);
        }
    }

    /**
     * Singleton class getter
     *
     * @return an instance of PersistenceManager
     */
    public static PersistenceMongoDB getInstance() {
        if (instance == null) {
            instance = new PersistenceMongoDB();
        }
        return instance;
    }

    /**
     * Find within the internal knowledge storage a category given its pattern,
     * the logic behind the find will be 1. Find a document with _id strictly
     * equal to "input", if hits return else 2. Find a document with _id
     * strictly equal to any of the elements in "tags", if hits returns else 3.
     * Find by best matching tags
     *
     * @param input Category's pattern
     * @param tags Labels gotten as wildcards from the sentence
     * @return Category's template
     */
    public String find(String input, LinkedList<String> tags) {
        String result = null;
        try {

            //1. Find by strict input
            BasicDBObject target = new BasicDBObject();
            target.append("_id", input);

            DBObject findOne = knowledge.findOne(target);

            if (findOne != null) {
                LOG.log(Level.INFO, "MongoDB Storage hit against _id:{0}", input);
                result = (String) findOne.get("content");
                if (!StringUtils.isEmpty(result)) {
                    LOG.log(Level.INFO, "MongoDB Storage valid result extracted content:{0}", result);
                    return result;
                } else {
                    LOG.log(Level.INFO, "MongoDB Storage invalid result, trying to find now by tags one by one");
                }
            } //2. Keep looking, make a search for every tag found against strict input
            else {
                if (tags != null) {
                    LOG.log(Level.INFO, "MongoDB Storage iterating through all tags");
                    for (Iterator<String> iter = tags.iterator(); iter.hasNext();) {
                        String tag = iter.next().trim();
                        BasicDBObject tagSearch = new BasicDBObject();
                        tagSearch.append("_id", tag);
                        DBObject findings = knowledge.findOne(tagSearch);
                        if (findings != null) {
                            result = (String) findings.get("content");
                            if (!StringUtils.isEmpty(result)) {
                                LOG.log(Level.INFO, "MongoDB Storage tag hit against _id:{0}", tag);
                                return result;
                            }
                            break;
                        }

                    }
                }
            }

            //3. If strict input did not find any matches get best match by tag. This best match
            //tag method is powered by the Aggregation Framework of MongoDB, a better performance
            //alternative than using the mapreduce technique
            if (StringUtils.isEmpty(result)) {

                // create our pipeline operations, first with the $match
                DBObject match = new BasicDBObject("$match", new BasicDBObject("tags", new BasicDBObject("$in", tags)));

                // Con el unwind me aparecen "tags" numero de veces el trigger sino solo aparece 1 vez
                DBObject unwind = new BasicDBObject("$unwind", "$tags");

                // build the $projection operation
                DBObject fields = new BasicDBObject("content", 1).append("tags", 1);
                DBObject project = new BasicDBObject("$project", fields);

                DBObject tagSearch = getTagSearchConditions(tags, 0);

                // Now the $group operation
                DBObject groupFields = new BasicDBObject("_id", "$content");
                groupFields.put("matches", new BasicDBObject("$sum", tagSearch));

                DBObject group = new BasicDBObject("$group", groupFields);

                DBObject sorting = new BasicDBObject("matches", -1);
                DBObject sort = new BasicDBObject("$sort", sorting);

                // run aggregation
                AggregationOutput output = knowledge.aggregate(match, unwind, project, group, sort);
                if (output != null) {
                    for (Iterator<DBObject> iter = output.results().iterator(); iter.hasNext();) {
                        DBObject next = iter.next();
                        result = (String) next.get("_id");
                        return result;
                    }
                }


                //4. We havent found a match yet, now try to do a Strict search of input against the content
                //if we have a match, return the id as response                 
                BasicDBObject reverse = new BasicDBObject();
                reverse.append("content", input);

                DBObject findReverse = knowledge.findOne(reverse);
                if (findReverse != null) {
                    LOG.log(Level.INFO, "MongoDB Storage hit against content:{0}", input);
                    result = (String) findReverse.get("_id");
                    if (!StringUtils.isEmpty(result)) {
                        LOG.log(Level.INFO, "MongoDB Storage valid result extracted _id:{0}", result);
                        return result;
                    }
                }

                //5. We havent found a match still, not match every tag against the Strict content, if
                //we get a hit return
                if (tags != null) {
                    LOG.log(Level.INFO, "MongoDB Storage iterating through all tags");
                    for (Iterator<String> iter = tags.iterator(); iter.hasNext();) {
                        String tag = iter.next().trim();
                        BasicDBObject reverseTagged = new BasicDBObject();
                        reverseTagged.append("content", tag);
                        DBObject findReverseTag = knowledge.findOne(reverseTagged);
                        if (findReverseTag != null) {
                            LOG.log(Level.INFO, "MongoDB Storage tag hit against content:{0}", input);
                            result = (String) findReverseTag.get("_id");
                            if (!StringUtils.isEmpty(result)) {
                                LOG.log(Level.INFO, "MongoDB Storage valid result extracted _id:{0}", result);
                                return result;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception - PersistenceMongoDB find could not retrieve any data for"
                    + " .input:" + input
                    + " .tags:" + tags.toString(), ex);
        }

        //No hit
        return result;
    }

    /**
     * Given a current iteration index and the list of tags, builds the nested
     * $cond operator needed for the aggregation command.
     *
     * @param tags List of tags
     * @param index Current iteration index
     * @return DBObject with the current $cond
     */
    private DBObject getTagSearchConditions(LinkedList<String> tags, int index) {

        //Get tag
        DBObject result = null;
        String tag = tags.get(index).trim();

        //Assemble the object with the 2 items to compare, here we build the $eq (equality condition) params.
        Object[] items = new Object[2];
        items[0] = "$tags";
        items[1] = tag;

        //Assemble the equality condition for give tag against the tags in database
        DBObject equality = new BasicDBObject("$eq", items);

        //Assemble the objects for equality condition
        Object[] operands = new Object[3];
        operands[0] = equality;
        operands[1] = 1;

        //There are futher conditions
        int nextIndex = index + 1;
        int size = tags.size();
        if (nextIndex < size) {
            operands[2] = getTagSearchConditions(tags, nextIndex);
        } //No more conditions, finalize
        else {
            operands[2] = 0;
        }

        //Assemble equality condition
        result = new BasicDBObject("$cond", operands);

        return result;
    }

    /**
     * Store within the internal knowledge a category given its pattern and
     * template. Before persisting it, uses the Analysis engine to identify
     * possible tags and add them to the document being saved for later on
     * search purposes.
     *
     * @param pattern Category's pattern
     * @param template Category's template
     */
    public void save(String pattern, String template) {

        try {

            //Obtain tags
            IAnalysis analysis = AnalysisManager.getInstance();
            String tagsString = pattern + " " + template;
            List<String> allTags = analysis.getKeywords(tagsString);

            //If tags are empty at least include the pattern as a tag
            if (allTags.isEmpty()) {
                allTags.add(pattern);
            }

            //Assemble object to save
            BasicDBObject doc = new BasicDBObject();
            doc.append("_id", pattern);
            doc.append("content", template);
            doc.append("tags", allTags);

            //Save object
            knowledge.save(doc);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception - PersistenceMongoDB.save could not store information in the internal knowledge base"
                    + " .pattern:" + pattern
                    + " .template:" + template, ex);
        }
    }

    /**
     * @return the engine
     */
    public PersistenceManager.Source getEngine() {
        return source;
    }

    /**
     * Close all storage elements
     */
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}