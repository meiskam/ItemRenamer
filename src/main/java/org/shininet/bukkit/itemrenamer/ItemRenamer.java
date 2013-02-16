/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerPacket;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerPlayerJoin;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerStackRestrictor;
import org.shininet.bukkit.itemrenamer.metrics.BukkitMetrics;
import org.shininet.bukkit.itemrenamer.metrics.Updater;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class ItemRenamer extends JavaPlugin {
	public Logger logger;

	private static boolean updateReady = false;
	private static String updateName = "";
	private static long updateSize = 0;

	public static final String updateSlug = "itemrenamer";

	private ItemRenamerCommands commandExecutor;
	private ItemRenamerConfiguration config;
	private RenameProcessor processor;
	
	private ItemRenamerPlayerJoin listenerPlayerJoin;
	private ItemRenamerPacket listenerPacket;
	private ItemRenamerStackRestrictor stackRestrictor;
	private RefreshInventoryTask refreshTask;
	
	private ProtocolManager protocolManager;
	private int lastSaveCount;
	
	@Override
	public void onEnable() {
		logger = getLogger();
		config = new ItemRenamerConfiguration(this, new File(getDataFolder(), "config.yml").getAbsolutePath()) {
			protected void onSynchronized() {
				lastSaveCount = getModificationCount();
				refreshTask.forceRefresh();
			};
		};
		processor = new RenameProcessor(config);
		
		startMetrics();
		startUpdater();
		
		// Managers
		PluginManager plugins = getServer().getPluginManager();
		protocolManager = ProtocolLibrary.getProtocolManager();
		
		listenerPacket = new ItemRenamerPacket(this, processor, protocolManager, logger);
		listenerPlayerJoin = new ItemRenamerPlayerJoin(this);
		stackRestrictor = new ItemRenamerStackRestrictor(processor);
		
		plugins.registerEvents(listenerPlayerJoin, this);
		plugins.registerEvents(stackRestrictor, this);
		
		commandExecutor = new ItemRenamerCommands(this, config);
		getCommand("ItemRenamer").setExecutor(commandExecutor);
		
		// Tasks
		refreshTask = new RefreshInventoryTask(getServer().getScheduler(), this, config);
		refreshTask.start();
	}

	private void startUpdater() {
		try {
			if (config.isAutoUpdate() && !(updateReady)) {
				
				// Start Updater but just do a version check
				Updater updater = new Updater(this, updateSlug, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false); 
				
				// Determine if there is an update ready for us
				updateReady = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE; 
				updateName = updater.getLatestVersionString(); // Get the latest version
				updateSize = updater.getFileSize(); // Get latest size
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to start Updater", e);
		}
	}

	private void startMetrics() {
		try {
		    BukkitMetrics metrics = new BukkitMetrics(this);
		    metrics.start();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to start Metrics", e);
		}
	}

	// TODO: Determine if this is necessary
	public void performConversion() {
		//if ((configFile.contains("pack")) && (!configFile.contains("packs.converted"))) { //conversion ftw
		//	configFile.set("packs.converted", configFile.getConfigurationSection("pack"));
		//	configFile.set("pack", null);
		//	saveConfig();
		//}
	}
	
	@Override
	public void onDisable() {
		// Save all changes if anything has changed
		if (config.getModificationCount() != lastSaveCount) {
			config.save();
			logger.info("Saving configuration.");
		}
		
		listenerPacket.unregister(this);
		listenerPlayerJoin.unregister();
		refreshTask.stop();
	}

	public boolean getUpdateReady() {
		return updateReady;
	}

	public String getUpdateName() {
		return updateName;
	}

	public long getUpdateSize() {
		return updateSize;
	}
}
