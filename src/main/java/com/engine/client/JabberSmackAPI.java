package com.engine.client;

import com.engine.interpretation.Interpreter;
import com.engine.interpretation.ObjectHandler;
import java.io.*;
import java.util.*; 
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
 
public class JabberSmackAPI implements MessageListener{
    static Interpreter rs;
    XMPPConnection connection;
    static String user;
 
    public void login(String userName, String password) throws XMPPException
    {
    ConnectionConfiguration config = new ConnectionConfiguration("192.168.1.102",5222);
    connection = new XMPPConnection(config);
 
    connection.connect();
    connection.login(userName, password);
    }
 
    public void sendMessage(String message, String to) throws XMPPException
    {
    
    Chat chat = connection.getChatManager().createChat(to, this);
    chat.sendMessage(message);
    }
 
    /*public void displayBuddyList()
    {
    Roster roster = connection.getRoster();
    Collection<RosterEntry> entries = roster.getEntries();
 
    System.out.println("\n\n" + entries.size() + " buddy(ies):");
    for(RosterEntry r:entries)
    {
    System.out.println(r.getUser());
    }
    }*/
 
    public void disconnect()
    {
    connection.disconnect();
    }
 
    public void processMessage(Chat chat, Message message)
    {
        if(message.getType() == Message.Type.chat){
            try {
                //System.out.println(chat.getParticipant() + " says: " + message.getBody());
                this.sendMessage(rs.reply(user, message.getBody()), "client");
            } catch (XMPPException ex) {
                Logger.getLogger(JabberSmackAPI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
    }
 
    public static void main(String args[]) throws XMPPException, IOException, NoSuchFieldException, InterruptedException
    {
        
                System.out.println(":: Creating RS Object");
        rs = new Interpreter();
        rs.setLogLevel(Level.OFF);

        // Create a handler for Perl objects.
        rs.setHandler(ObjectHandler.Handler.PERL);

        // Load and sort replies
        System.out.println(":: Loading replies");
        rs.loadDefaultDirectory();
        rs.sortReplies();
        
            ConnectionConfiguration config = new ConnectionConfiguration("192.168.1.102",5222);
         // connect to server
  XMPPConnection connection = new XMPPConnection(config);
  connection.connect();
  connection.login("iris", "123"); // TODO: change user and pass

  // register listeners
  ChatManager chatmanager = connection.getChatManager();
  connection.getChatManager().addChatListener(new ChatManagerListener()
  {
    public void chatCreated(final Chat chat, final boolean createdLocally)
    {
        
      chat.addMessageListener(new MessageListener()
      {
        public void processMessage(Chat chat, Message message)
        {
          System.out.println("Received message: " 
            + (message != null ? message.getBody() : "NULL"));
              try {
                  chat.sendMessage(rs.reply(user, message.getBody()));
              } catch (XMPPException ex) {
                  Logger.getLogger(JabberSmackAPI.class.getName()).log(Level.SEVERE, null, ex);
              }
        }
      });
    }
  });

  // idle for 20 seconds
  final long start = System.nanoTime();
  while ((System.nanoTime() - start) / 1000000 < 2000000) // do for 2000 seconds
  {
    Thread.sleep(500);
  }
  connection.disconnect();
        
   /* // declare variables
    JabberSmackAPI c = new JabberSmackAPI();
   // BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String msg;
 
 
    // turn on the enhanced debugger
    XMPPConnection.DEBUG_ENABLED = true;
 user="iris";
 
    // Enter your login information here
    c.login(user, "123");
*/
    
/*
    System.out.println("-----");
 
    System.out.println("Who do you want to talk to? - Type contacts full email address:");
    String talkTo = br.readLine();
 
    System.out.println("-----");
    System.out.println("All messages will be sent to " + talkTo);
    System.out.println("Enter your message in the console:");
    System.out.println("-----\n");*/
 
    
              
        // Create a new Interpreter interpreter.


     /*   c.sendMessage("Hola", "client");*/
    /*    
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
    
    */
  /*     while( !(msg=br.readLine()).equals("bye"))
    {
        c.sendMessage(msg, "client");
    }*/
   
  //  c.disconnect();
    }
 


}