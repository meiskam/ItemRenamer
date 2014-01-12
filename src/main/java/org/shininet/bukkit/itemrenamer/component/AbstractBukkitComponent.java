package org.shininet.bukkit.itemrenamer.component;

import javax.annotation.Nonnull;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.google.common.eventbus.EventBus;

/**
 * Represents a Bukkit listener as a registerable component.
 * @author Kristian
 */
public abstract class AbstractBukkitComponent extends AbstractComponent implements Listener {
	@Override
	protected void onRegistered(@Nonnull Plugin plugin, EventBus bus) {
		if (requireBukkit())
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
		if (requireEventBus())
			bus.register(this);
	}
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		if (requireBukkit())
			HandlerList.unregisterAll(this);
		if (requireEventBus())
			bus.unregister(this);
	}
	
	/**
	 * Determine if we require Bukkit events.
	 * @return TRUE if we do, FALSE otherwise.
	 */
	protected abstract boolean requireBukkit();
	
	/**
	 * Determine if we require event bus events.
	 * @return TRUE if we do, FALSE otherwise.
	 */
	protected abstract boolean requireEventBus();
}
