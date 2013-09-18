package org.shininet.bukkit.itemrenamer.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginDisableEvent;

import org.shininet.bukkit.itemrenamer.ItemRenamerPlugin;
import org.shininet.bukkit.itemrenamer.component.AbstractBukkitComponent;

/**
 * Represents a bukkit component that will automatically clean up all API listeners of a plugin.
 * @author Kristian
 */
public class ListenerCleanupComponent extends AbstractBukkitComponent {
	private final ItemRenamerPlugin owner;
	
	public ListenerCleanupComponent(ItemRenamerPlugin owner) {
		this.owner = owner;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPluginDisable(PluginDisableEvent event) {
		if (owner.equals(event.getPlugin())) {
			owner.cleanupPlugin(event.getPlugin());
		}
	}
}
