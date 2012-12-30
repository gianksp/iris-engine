package com.engine.interpretation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A manager for all the Bot users.
 */
public class ClientManager {
	
    //Variables
    private HashMap<String, Client> clients = null;     // List of users

    /**
     * Create a client manager. Only one needed per bot.
     */
    public ClientManager () {
        this.clients = new HashMap<String, Client>();
    }

    /**
     * Get a Client object for a given user ID.
     * @param username The user ID you want to work with.
     * @return Client
     */
    public com.engine.interpretation.Client client (String username) {
        // Is this a new topic? then create a new User
        if (!clients.containsKey(username)) {
            clients.put(username, new Client(username));
        }
        return clients.get(username);
    }

    /**
     * Get a list of the clients managed.
     * @return Collection of clients
     */
    public Collection<String> listClients () {
        Collection<String> result = new ArrayList<String>();
        Iterator it = clients.keySet().iterator();
        while (it.hasNext()) {
            result.add(it.next().toString());
        }
        return result;
    }

    /**
     * Query whether a given client exists
     * @param user The user ID.
     * @return true if client exists
     */
    public boolean clientExists (String user) {
        boolean result = false;
        if (clients.containsKey(user)) {
            result = true;
        }
        return result;
    }
}