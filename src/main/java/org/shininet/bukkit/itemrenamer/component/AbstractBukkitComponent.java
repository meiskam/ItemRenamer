package org.shininet.bukkit.itemrenamer.component;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Represents a Bukkit listener as a registerable component.
 * @author Kristian
 */
public class AbstractBukkitComponent extends AbstractComponent implements Listener {
	@Override
	protected void onRegistered(@Nonnull Plugin plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		HandlerList.unregisterAll(this);
	}
}
