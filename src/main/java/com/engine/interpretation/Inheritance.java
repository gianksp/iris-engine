package com.engine.interpretation;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.TreeMap;

/**
 * An inheritance tracker to aid in sorting replies.
 */
public class Inheritance {
	
    // Private variables.
    private TreeMap<Integer, Collection<String> > atomic = null;    // Whole words, no wildcards
    private TreeMap<Integer, Collection<String> > option = null;    // With [optional] parts
    private TreeMap<Integer, Collection<String> > alpha  = null;    // With _alpha_ wildcards
    private TreeMap<Integer, Collection<String> > number = null;    // With #number# wildcards
    private TreeMap<Integer, Collection<String> > wild   = null;    // With *star* wildcards
    private LinkedList<String> pound = null;                        // With only # in them
    private LinkedList<String> under = null;                        // With only _ in them
    private LinkedList<String> star  = null;                        // With only * in them

    /**
     * Constructor, initialize variables
     */
    public Inheritance () {
        this.atomic = new TreeMap<Integer, Collection<String> >();
        this.option = new TreeMap<Integer, Collection<String> >();
        this.alpha  = new TreeMap<Integer, Collection<String> >();
        this.number = new TreeMap<Integer, Collection<String> >(); 
        this.wild   = new TreeMap<Integer, Collection<String> >();
        this.pound  = new LinkedList<String>();
        this.under  = new LinkedList<String>();
        this.star   = new LinkedList<String>();
    }

    /**
     * Dump the buckets out and add them to the given vector.
     * @param sorted    Collection of items
     * @return Collection of sorted items
     */
    public Collection<String> dump (LinkedList<String> sorted) {
        
        // Sort each sort-category by the number of words they have, in descending order.
        sorted = addSortedList(sorted, atomic);
        sorted = addSortedList(sorted, option);
        sorted = addSortedList(sorted, alpha);
        sorted = addSortedList(sorted, number);
        sorted = addSortedList(sorted, wild);

        // Add the singleton wildcards too.
        sorted = addSortedList(sorted, under);
        sorted = addSortedList(sorted, pound);
        sorted = addSortedList(sorted, star);

        return sorted;
    }

    /**
     * A helper function for sortReplies, adds a hash of (word count -> triggers vector) to the
     * running sort buffer.
     * @param vector The running sort buffer vector
     * @param hash   The hash of word count -> triggers vector
     * @return  Collection of sorted list
     */
    private LinkedList<String> addSortedList (LinkedList<String> target, TreeMap<Integer, Collection<String> > map) {
        // We've been given a hash where the keys are integers (word counts) and
        // the values are all triggers with that number of words in them (where
        // words are things that aren't wildcards).

        // Sort the hash by its number of words, descending.
        NavigableSet<Integer> navig = ((TreeMap)map ).descendingKeySet();  
  
        for (Iterator<Integer> iter=navig.iterator();iter.hasNext();) {  
            Integer key = (Integer) iter.next();  
            Collection<String> collection = map.get(key);  
            for (Iterator colIter = collection.iterator(); colIter.hasNext();) {
                String val = (String) colIter.next();
                target.add(val);
            }
        }  

        // Return the new vector.
        return target;
    }

    /**
     * A helper function for sortReplies, adds a vector of wildcard triggers to the running sort buffer.
     * @param target    The running sort buffer list
     * @param list      The list of wildcard triggers
     */
    private LinkedList<String> addSortedList (LinkedList<String> target, LinkedList<String> list) {
        
        for (Iterator<String> iter=list.descendingIterator(); iter.hasNext();) {  
            String trigger = (String) iter.next(); 
            target.add(trigger);
        }
        return target;
    }

    /**
     * Add items to atomic.
     * @param wc        Word count
     * @param trigger   Item to add
     */
    public void addAtomic (int wc, String trigger) {
        //If an instance of the list for the given number of element does not exist, create a new one
        if (!atomic.containsKey(wc)) {
            atomic.put(wc, new LinkedList<String>());
        }
        atomic.get(wc).add(trigger);
    }
    
     /**
     * Add items to option.
     * @param wc        Word count
     * @param trigger   Item to add
     */
    public void addOption (int wc, String trigger) {
        //If an instance of the list for the given number of element does not exist, create a new one
        if (!option.containsKey(wc)) {
            option.put(wc, new LinkedList<String>());
        }
        option.get(wc).add(trigger);
    }
    
    /**
     * Add items to alpha.
     * @param wc        Word count
     * @param trigger   Item to add
     */
    public void addAlpha (int wc, String trigger) {
        //If an instance of the list for the given number of element does not exist, create a new one
        if (!alpha.containsKey(wc)) {
            alpha.put(wc, new LinkedList<String>());
        }
        alpha.get(wc).add(trigger);
    }
    
    /**
     * Add items to number.
     * @param wc        Word count
     * @param trigger   Item to add
     */    
    public void addNumber (int wc, String trigger) {
        //If an instance of the list for the given number of element does not exist, create a new one
        if (!number.containsKey(wc)) {
            number.put(wc, new LinkedList<String>());
        }
        number.get(wc).add(trigger);
    }
    
    /**
     * Add items to wild.
     * @param wc        Word count
     * @param trigger   Item to add
     */
    public void addWild (int wc, String trigger) {
        //If an instance of the list for the given number of element does not exist, create a new one
        if (!wild.containsKey(wc)) {
            wild.put(wc, new LinkedList<String>());
        }
        wild.get(wc).add(trigger);
    }
    
    /**
     * Add items to pound
     * @param trigger Item to add
     */
    public void addPound (String trigger) {
        pound.add(trigger);
    }
    
    /**
     * Add items to under
     * @param trigger Item to add
     */
    public void addUnder (String trigger) {
        under.add(trigger);
    }
        
    /**
     * Add items to star
     * @param trigger Item to add
     */
    public void addStar (String trigger) {
        star.add(trigger);
    }
}