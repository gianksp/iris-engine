package com.engine.interpretation;

/**
 * Interface for object handlers.
 */

public interface ObjectHandler {
    
        public enum Handler{PERL, JAVASCRIPT};
	/**
	 * Handler for when object code is read (loaded) by ProgramJ.
	 * Should return true for success or false to indicate error.
	 *
	 * @param name The name of the object.
	 * @param code The source code inside the object.
	 */
	public boolean onLoad (String name, Object[] code);

	/**
	 * Handler for when a user invokes the object. Should return the text
	 * reply from the object.
	 *
	 * @param name The name of the object being called.
	 * @param user The user's ID.
	 * @param args The argument list from the call tag.
	 */
	public String onCall (String name, String user, Object[] args);
}
