/*
    com.rivescript.RiveScript - The Official Java RiveScript Interpreter
    Copyright (C) 2010  Noah Petherbridge

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.engine.interpretation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A topic manager class for RiveScript.
 */

public class Topic {
	// Private variables.
	private HashMap<String, com.engine.interpretation.Trigger> triggers   = new HashMap<String, com.engine.interpretation.Trigger>();   // Topics contain triggers
	private boolean hasPrevious                                 = false;                                            // Has at least one %Previous
	private TreeMap<String, LinkedList<String> > previous       = new TreeMap<String, LinkedList<String> >();       // Mapping of %Previous's to their triggers
	private LinkedList<String> includes                         = new LinkedList<String>();                         // Included topics
	private LinkedList<String> inherits                         = new LinkedList<String>();                         // Inherited topics
	private LinkedList<String> sorted                           = null;                                             // Sorted trigger list

	// Currently selected topic.
	String name = "";
        
        public final static Logger LOG = Logger.getLogger(Topic.class .getName()); 

	/**
	 * Create a topic manager. Only one per RiveScript interpreter needed.
	 */
	public Topic (String name) {
		this.name = name;
	}

	/**
	 * Turn on or off debug mode statically. This debug mode is static so it will
	 * be shared among all RiveScript instances and all Topics.
	 *
	 * @param debug The new debug mode to set.
	 */
	public static void setDebug (Level debugLegel) {
            LOG.setLevel(debugLegel);
	}

	/**
	 * Fetch a Trigger object from the topic. If the trigger doesn't exist, it
	 * is created on the fly.
	 *
	 * @param pattern The pattern for the trigger.
	 */
	public com.engine.interpretation.Trigger trigger (String pattern) {
		// Is this a new trigger?
		if (triggers.containsKey(pattern) == false) {
			// Create a new Trigger object.
			com.engine.interpretation.Trigger newTrig = new com.engine.interpretation.Trigger(this.name,pattern);
			triggers.put(pattern, newTrig);
		}

		return triggers.get(pattern);
	}

	/**
	 * Test if a trigger exists.
	 *
	 * @param trigger The pattern for the trigger.
	 */
	public boolean triggerExists (String trigger) {
		if (triggers.containsKey(trigger) == false) {
			return false;
		}
		return true;
	}

	/**
	 * Fetch a sorted list of all triggers. Note that the results are only accurate if
	 * you called sortTriggers() for this topic after loading new replies into it (the
	 * sortReplies() in RiveScript automagically calls sortTriggers() for all topics,
	 * so just make sure you call sortReplies() after loading new replies).
	 */
	public Object[] listTriggers () {
		return listTriggers (false);
	}

	/**
	 * Fetch a list of all triggers. If you provide a true value to this method, it will
	 * return the UNSORTED list (getting the keys of the trigger hash directly). If you
	 * want a SORTED list (which you probably do), use listTriggers() instead, or explicitly
	 * provide a false value to this method.
	 *
	 * @param raw Get a raw unsorted list instead of a sorted one.
	 */
	public Object[] listTriggers (boolean raw) {
		// If raw, get the unsorted triggers directly from the hash.
		if (raw) {
			// Turn the trigger keys into a list.
			LinkedList<String> trigs = new LinkedList<String>();
			Iterator it = triggers.keySet().iterator();
			while (it.hasNext()) {
				String next = it.next().toString();
                                LOG.log(Level.INFO, "RAW TRIGGER: {0}", next);
				trigs.add (next);
			}

			// Return it.
			return trigs.toArray();
		}

		// Do we have a sort buffer?
		if (sorted == null) {
			// Um no, that's bad.
			System.err.println("You called listTriggers() for topic " + name + " before its replies have been sorted!");
			return new String [0];
		}
		return sorted.toArray();
	}

	/**
	 * (Re)create the internal sort cache for this topic's triggers.
	 */
	public void sortTriggers (Object[] alltrigs) {
		// Get our list of triggers.
		LinkedList<String> sortedList   = new LinkedList<String>();

		// Do multiple sorts, one for each inheritence level.
		TreeMap<Integer, LinkedList<String> > heritage = new TreeMap<Integer, LinkedList<String> >();
		heritage.put(-1, new LinkedList<String>());
		int highest = -1;
		Pattern reInherit = Pattern.compile("\\{inherits=(\\d+)\\}");
		for (int i = 0; i < alltrigs.length; i++) {
			int inheritsIndex = -1; // Default, when no {inherits} tag.

			// Does it have an inherit level?
			if (alltrigs[i].toString().indexOf("{inherits=") > -1) {
				Matcher m = reInherit.matcher(alltrigs[i].toString());
				while (m.find()) {
					inheritsIndex = Integer.parseInt(m.group(1));
					if (inheritsIndex > highest) {
						highest = inheritsIndex;
					}
					break;
				}
			}

			alltrigs[i] = alltrigs[i].toString().replaceAll("\\{inherits=\\d+\\}","");

			// Initialize this inherit group?
			if (heritage.containsKey(inheritsIndex) == false) {
				heritage.put(inheritsIndex, new LinkedList<String>() );
			}

			// Add it.
			heritage.get(inheritsIndex).add(alltrigs[i].toString());
		}

		// Go on and sort each heritage level. We want to loop from level 0 up,
		// and then do level -1 last.		
		for (int h = -1; h <= highest; h++) {
			if (heritage.containsKey(h) == false) {
				continue;
			}

			int inheritsIndex = h;
			LOG.log(Level.INFO, "Sorting triggers by heritage level {0}", inheritsIndex);
			Object[] triggersList = heritage.get(inheritsIndex).toArray();

			// Sort-priority maps.
			TreeMap<Integer, LinkedList<String> > prior = new TreeMap<Integer, LinkedList<String> >();

			// Assign each trigger to its priority level.
			LOG.log(Level.INFO, "BEGIN sortTriggers in topic {0}", this.name);
			Pattern rePrior = Pattern.compile("\\{weight=(\\d+?)\\}");
			for (int i = 0; i < triggersList.length; i++) {
				int priority = 0;

				// See if this trigger has a {weight}.
				if (triggersList[i].toString().indexOf("{weight") > -1) {
					// Try to match the regexp then.
					Matcher m = rePrior.matcher(triggersList[i].toString());
					while (m.find() == true) {
						priority = Integer.parseInt(m.group(1));
					}
				}

				// Initialize its priority group?
				if (prior.containsKey(priority) == false) {
					// Create it.
					prior.put(priority, new LinkedList<String>() );
				}

				// Add it.
				prior.get(priority).add(triggersList[i].toString());
			}

			/*
				Keep in mind here that there is a difference between includes and
				inherits -- topics that inherit other topics are able to OVERRIDE
				triggers that appear in the inherited topic. This means that if the
				top topic has a trigger of simply *, then NO triggers are capable of
				matching in ANY inherited topic, because even though * has the lowest
				sorting priority, it has an automatic priority over all inherited
				topics.

				The topicTriggers in TopicManager takes this into account. All topics
				that inherit other topics will have their local triggers prefixed
				with a fictional {inherits} tag, which will start at {inherits=0}
				and increment if the topic tree has other inheriting topics. So
				we can use this tag to make sure topics that inherit things will
				have their triggers always be on the top of the stack, from
				inherits=0 to inherits=n.
			*/

			// Sort the priority lists numerically from highest to lowest.
			 NavigableSet<Integer> set = prior.descendingKeySet();
                        
                         for (Iterator<Integer> iter=set.iterator();iter.hasNext();) {  
                           Integer key = (Integer) iter.next();  
                           LinkedList<String> p_list = prior.get(key);

				/*
					So, some of these triggers may include {inherits} tags, if
					they came from a topic which inherits another topic. Lower
					inherits values mean higher priority on the stack. Keep this
					in mind when keeping track of how to sort these things.
				*/

				int highest_inherits = inheritsIndex; // highest {inherits} we've seen

				// Initialize a sort bucket that will keep inheritance levels'
				// triggers in separate places.
				//com.rivescript.InheritanceManager bucket = new com.rivescript.InheritanceManager();
				com.engine.interpretation.Inheritance bucket = new com.engine.interpretation.Inheritance();

				// Loop through the triggers and sort them into their buckets.
				for (Iterator<String> iter3=p_list.iterator();iter3.hasNext();) {  
                                        String trigger = (String) iter3.next();  
					//String trigger = e.nextElement().toString();

					// Count the number of whole words it has.
					String[] words = trigger.split("[ |\\*|\\#|\\_]");
					int wc = 0;
					for (int w = 0; w < words.length; w++) {
						if (words[w].length() > 0) {
							wc++;
						}
					}

					LOG.log(Level.INFO, "On trigger: {0} (it has {1} words) - inherit level: {2}", new Object[]{trigger, wc, inheritsIndex});

					// Profile it.
					if (trigger.indexOf("_") > -1) {
						// It has the alpha wildcard, _.
						if (wc > 0) {
							bucket.addAlpha(wc, trigger);
						}
						else {
							bucket.addUnder(trigger);
						}
					}
					else if (trigger.indexOf("#") > -1) {
						// It has the numeric wildcard, #.
						if (wc > 0) {
							bucket.addNumber(wc, trigger);
						}
						else {
							bucket.addPound(trigger);
						}
					}
					else if (trigger.indexOf("*") > -1) {
						// It has the global wildcard, *.
						if (wc > 0) {
							bucket.addWild(wc, trigger);
						}
						else {
							bucket.addStar(trigger);
						}
					}
					else if (trigger.indexOf("[") > -1) {
						// It has optional parts.
						bucket.addOption(wc, trigger);
					}
					else {
						// Totally atomic.
						bucket.addAtomic(wc, trigger);
					}
				}

				// Sort each inheritence level individually.
				LOG.log(Level.INFO, "Dumping sort bucket !");
				Collection<String> subsort = bucket.dump(new LinkedList<String>());
				for (Iterator<String> iter3=subsort.iterator();iter3.hasNext();) {  
                                    String item = (String) iter3.next(); 
                                    LOG.log(Level.INFO, "ADD TO SORT: {0}", item);
                                    sortedList.add(item);
                                }
			}
		}

		// Turn the running sort buffer into a string array and store it.
		this.sorted = sortedList;
	}

	/**
	 * Add a mapping between a trigger and a %Previous that follows it.
	 *
	 * @param pattern  The trigger pattern.
	 * @param previous The pattern in the %Previous.
	 */
	public void addPrevious (String pattern, String previous) {
		// Add it to the vector.
		if (this.previous.containsKey(previous) == false) {
			this.previous.put(previous, new LinkedList<String>());
		}
		this.previous.get(previous).add(pattern);
	}

	/**
	 * Check if any trigger in the topic has a %Previous (only good after
	 * sortPrevious, from RiveScript.sortReplies is called).
	 */
	public boolean hasPrevious () {
		return this.hasPrevious;
	}

	/**
	 * Get a list of all the %Previous keys.
	 */
	public Object[] listPrevious () {
		LinkedList<String> vector = new LinkedList<String>();
		Iterator sit = previous.keySet().iterator();
		while (sit.hasNext()) {
			vector.add((String) sit.next());
		}
		return vector.toArray();
	}

	/**
	 * List the triggers associated with a %Previous.
	 *
	 * @param previous The %Previous pattern.
	 */
	public Object[] listPreviousTriggers (String previous) {
		// TODO return sorted list
		if (this.previous.containsKey(previous)) {
			return this.previous.get(previous).toArray();
		}
		return null;
	}

	/**
	 * Sort the %Previous buffer.
	 */
	public void sortPrevious () {
		// Keep track if ANYTHING has a %Previous.
		this.hasPrevious = false;

		// Find all the triggers that have a %Previous. This hash maps a %Previous
		// label to the list of triggers that are associated with it.
		TreeMap<String, LinkedList<String> > prev2trig = new TreeMap<String, LinkedList<String> >();

		// Loop through the triggers to find those with a %Previous.
		Object[] triggersList = this.listTriggers(true);
		for (int i = 0; i < triggersList.length; i++) {
			String pattern = triggersList[i].toString();
			if (pattern.indexOf("{previous}") > -1) {
				// This one has it.
				this.hasPrevious = true;
				String[] parts = pattern.split("\\{previous\\}", 2);
				String previousItem = parts[1];

				// Keep it under the %Previous.
				if (prev2trig.containsKey(previousItem) == false) {
					prev2trig.put(previousItem, new LinkedList<String>());
				}
				prev2trig.get(previousItem).add(parts[0]);
			}
		}

		// TODO: we need to sort the triggers but ah well
		this.previous = prev2trig;
	}

	/**
	 * Query whether a %Previous is registered with this topic.
	 *
	 * @param previous The pattern in the %Previous.
	 */
	public boolean previousExists (String previous) {
		if (this.previous.containsKey(previous)) {
			return true;
		}
		return false;
	}

	/**
	 * Retrieve a string array of the +Triggers that are associated with a %Previous.
	 *
	 * @param previous The pattern in the %Previous.
	 */
	public Object[] listPrevious (String previous) {
		if (this.previous.containsKey(previous)) {
			return this.previous.get(previous).toArray();
		}
		else {
			return null;
		}
	}

	/**
	 * Add a topic that this one includes.
	 *
	 * @param topic The included topic's name.
	 */
	public void includes (String topic) {
		this.includes.add(topic);
	}

	/**
	 * Add a topic that this one inherits.
	 *
	 * @param topic The inherited topic's name.
	 */
	public void inherits (String topic) {
		this.inherits.add(topic);
	}

	/**
	 * Retrieve a list of included topics.
	 */
	public Object[] includes () {
		return this.includes.toArray();
	}

	/**
	 * Retrieve a list of inherited topics.
	 */
	public Object[] inherits () {
		return this.inherits.toArray();
	}
}
