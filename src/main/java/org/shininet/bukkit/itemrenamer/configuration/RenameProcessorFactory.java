package org.shininet.bukkit.itemrenamer.configuration;

import com.google.common.base.Function;

/**
 * Represents a factory for producing rename processors.
 * 
 * @author Kristian
 */
public abstract class RenameProcessorFactory  {
	/**
	 * Represents a processor (select) for RenameRule.
	 * 
	 * @author Kristian
	 */
	public interface RenameFunction extends Function<RenameRule, RenameRule> {
		// That's basically it
	}
	
	/**
	 * Construct a new rename processor.
	 * @return The new rename processor.
	 */
	public abstract RenameFunction create();
}
