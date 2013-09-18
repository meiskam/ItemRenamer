package org.shininet.bukkit.itemrenamer.api;

/**
 * Represents the different priority levels for the renamer listeners.
 * <p>
 * ItemRenamer is invoked in-between the central priorities, 
 * @author Kristian
 */
public enum RenamerPriority {
	/**
	 * The listener has the lowest possible priority and may be among the first executed. 
	 * This is suitable when other listeners, including ItemRenamer, may further customize 
	 * or override the behavior.
	 * <p>
	 * This event occurs before ItemRenamer.
	 */
	PRE_LOWEST,
	
	/**
	 * The listener is of low importance. 
	 * <p>
	 * This event occurs before ItemRenamer.
	 */
	PRE_LOW,
	
	/**
	 * The listener is of neither low nor high importance, but is guarenteed to be executed before ItemRenamer.
	 */
	PRE_NORMAL,
	
	/**
	 * The listener is of neither low nor high importance, but is guarenteed to be executed after ItemRenamer.
	 */
	POST_NORMAL,
	
	/**
	 * The listener is of high importance, and will be executed after ItemRenamer.
	 */
	POST_HIGH,
	
	/**
	 * The listener is extermely important, and should be among the last executed. 
	 * <p>
	 * This event occurs after ItemRenamer.
	 */
	POST_HIGHEST,
	
	/**
	 * The listener will monitor the outcome of the rename event.
	 * <p>
	 * It should <b>not</b> perform any modifications.
	 */
	POST_MONITOR;
}
