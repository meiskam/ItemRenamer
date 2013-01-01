package org.shininet.bukkit.itemrenamer.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.shininet.bukkit.itemrenamer.ItemRenamer;

public class ItemRenamerGameModeChange implements Listener {

	private ItemRenamer plugin;
	
	public ItemRenamerGameModeChange(ItemRenamer plugin) {
		this.plugin = plugin;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		if (plugin.configFile.getBoolean("creativedisable")) {
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
