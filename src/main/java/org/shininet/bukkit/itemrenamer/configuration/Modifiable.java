package org.shininet.bukkit.itemrenamer.configuration;

/**
 * Represents an object that keeps track of how many times it has been modified.
 * 
 * @author Kristian
 */
public interface Modifiable {
	/**
	 * Determine how many times this damage lookup has been modified.
	 * @return The number of times each individual field has been changed.
	 */
	public abstract int getModificationCount();
	
	/**
	 * Set how many times this damage lookup has been modified.
	 * @param value - the new number of times it has changed.
	 */
	public abstract void setModificationCount(int value);
}
