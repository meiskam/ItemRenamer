package org.shininet.bukkit.itemrenamer.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.shininet.bukkit.itemrenamer.ItemRenamer;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;

public class ItemRenamerGameModeChange implements Listener {

	private ItemRenamer plugin;
	private ItemRenamerConfiguration config;
	
	public ItemRenamerGameModeChange(ItemRenamer plugin, ItemRenamerConfiguration config) {
		this.plugin = plugin;
		this.config = config;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		if (config.isCreativeDisabled()) {
			final Player player = event.getPlayer();
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	            public void run() {
	            	player.updateInventory();
	            }
	        });
		}
	}
	
	public void unregister() {
		PlayerGameModeChangeEvent.getHandlerList().unregister(this);
	}
}
