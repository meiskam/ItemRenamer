package org.shininet.bukkit.itemrenamer;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;

class RefreshInventoryTask implements Runnable {
	private static final int TICKS_PER_SECOND = 20;
	private static final int DELAY = TICKS_PER_SECOND * 2;	
	
	private int lastModCount = -1;
	private int taskID;
	
	private final ItemRenamerConfiguration config;
	private final BukkitScheduler scheduler;
	private final Plugin plugin;
	
	/**
	 * Construct a new refresh inventory task.
	 * @param scheduler - the scheduler to use.
	 * @param plugin - the owner plugin.
	 * @param config - the configuration.
	 */
	public RefreshInventoryTask(BukkitScheduler scheduler, Plugin plugin, ItemRenamerConfiguration config) {
		this.config = config;
		this.scheduler = scheduler;
		this.plugin = plugin;
	}

	public void start() {
		taskID = scheduler.scheduleSyncRepeatingTask(plugin, this, DELAY, DELAY);
		
		if (taskID < 0)
			throw new IllegalStateException("Unable to start refresh inventory task.");
	}
	
	public void stop() {
		scheduler.cancelTask(taskID);
	}
	
	@SuppressWarnings("deprecation")
	public void forceRefresh() {
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			player.updateInventory();
		}
		
		// Don't refresh automatically again
		lastModCount = config.getModificationCount();
	}

	@Override
	public void run() {
		if (lastModCount != config.getModificationCount()) {
			forceRefresh();
		}		
	}
}
