IRIS-ENGINE
===========

IRIS-ENGINE is an enhanced chatbot engine developed upon the work done by Noah Petherbridge with RiveScript. I've followed by quite a while many developments related to chatbot engines, some more amazing than others and in this ProgramJ version
I add my contribution to the cause. What does IRIS-ENGINE has that other engines do not offer and why?

**ECLIPSE BASED PROJECT**

Features
========

1. **MULTILINGUAL**. Most chatbot engines make extremely complicated the programming of multilingual chatbots. RiveScript stepped forward by defining substitutions and other language depending properties for each bot. Being multilingual and having web search capabilities for information requires that the knowledge search and data interpretation must be done with the same language but sometimes the internet source information languages defer from one to another and sometimes do not even match the end user language. That is why ProgramJ implements a translation layer that becomes the communication bridge bewteen the information sources and the end user.
2. **KNOWLEDGE BASED**. Powered by WolframAlpha and MIT Start search engines ProgramJ is able to acquire knowledge in realtime during a conversation with anyone by providing the programmer with a new set of tags that allow knowledge base searching.
3. **PERSISTENT**. What good is a bot that is not able to memorize new information?. Not good at all. That is why ProgramJ is enhanced with a persistence layer that is able to store units of knowledge (categories). This layer is powered by MongoDB.
4. **SMART TAGGING**. AlchemyAPI services provide the smart tagging capabilities and MongoDB provides the tag search functionalities with the Aggregation Framework (a better performance alternative to map reduce technique).

Current Status (TO_DO)
======================

1. Translation to bot language after calling a natural language processing service in English isn't done

Prerequisites
==============

1. Install MongoDB locally create a new database, a new collection and add authentication for a specific user with password (this information will be used later on during the installation process to fill the configuration.properties file). After installing the database engine, start the service.
2. Set your Windows Azure Marketplace client info - See http://msdn.microsoft.com/en-us/library/hh454950.aspx and enable the use of the Bing Translator API. Store your clientId and secret.
3. Get your WolframAlpha ApiKey for developers.
4. Obtain a set of RiveScript brain files.
5. Obtain the rsp4j.pl file from the RiveScript engine
6. In the utils folder there are some subfolders that will help you. You will find .rs sets to load as brains (this path is configured in configuration.properties)
7. The WolframAlpha library isnt available in Maven repositories so locally install it using the following properties

```
//Dependency information for local installation
groupId:     com.wolfram.alpha
artifactId:  wolfram
version:     1.1
type:        jar
```

     
8. Start installing the project

Installation
============

1. Download the source and open it in your favorite Java IDE (Netbeans is recommended)
2. Open the configuration.properties file and edit the following entries with the suggested information:

```
# Basic engine properties
! paths and variables

perl                    = (Path to rsp4j.pl)
files                   = (Path to .rs set)

# External Knowledge base (WolframAlpha)
wolfram_language        = ENGLISH
wolfram_key             = (your WolframAlpha key)

# External Knowledge base (MIT Start)
start_language          = ENGLISH
start_url               = http://start.csail.mit.edu/startfarm.cgi

# Internal Knowledge base (MongoDB)
mongo_url               = (server address)
mongo_login             = (database login credentials for authentication)
mongo_pass              = (database password credentials for authentication)
mongo_database          = (database name)
mongo_collection        = (collection name)
mongo_port              = 27017

# Translator Services Bing
bing_translator_id      = (azure application id)
bing_translator_secret  = (azure application secret)
```

3. Run the Console.java and chat demo with your bot
