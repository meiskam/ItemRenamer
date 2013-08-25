package org.shininet.bukkit.itemrenamer.component;

import org.bukkit.plugin.Plugin;

/**
 * Represents a component that can be registered with a plugin.
 * @author Kristian
 */
public interface Component {
	/**
	 * Attempt to register the current component with the given plugin.
	 * <p>
	 * This will throw an exception if the component has already been registered.
	 * @param plugin - the plugin. Cannot be NULL.
	 * @return This registered component.
	 */
	public Component register(Plugin plugin);
	
	/**
	 * Attempt to unregister the current component.
	 * @param plugin - the expected plugin. Cannot be NULL.
	 * @return TRUE if the plugin matched and the component has been unregistered, FALSE otherwise.
	 */
	public boolean unregister(Plugin plugin);
	
	/**
	 * Determine if this component has been registered.
	 * @return TRUE if it has, FALSE otherwise.
	 */
	public boolean isRegistered();
}
