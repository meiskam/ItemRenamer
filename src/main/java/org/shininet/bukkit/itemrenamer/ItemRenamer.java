/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer;

import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerGameModeChange;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerPacket;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerPlayerJoin;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerStackRestrictor;
import org.shininet.bukkit.itemrenamer.metrics.BukkitMetrics;
import org.shininet.bukkit.itemrenamer.metrics.Updater;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class ItemRenamer extends JavaPlugin {
	public Logger logger;
	public FileConfiguration configFile;
	
	private static boolean updateReady = false;
	private static String updateName = "";
	private static long updateSize = 0;

	public static final String updateSlug = "itemrenamer";

	private ItemRenamerCommands commandExecutor;
	private ItemRenamerConfiguration configuration;
	private RenameProcessor processor;
	
	private ItemRenamerPlayerJoin listenerPlayerJoin;
	private ItemRenamerGameModeChange listenerGameModeChange;
	
	private ItemRenamerPacket listenerPacket;
	private ItemRenamerStackRestrictor stackRestrictor;
	
	private ProtocolManager protocolManager;
	
	@Override
	public void onEnable() {
		logger = getLogger();
		configuration = new ItemRenamerConfiguration(this);
		processor = new RenameProcessor(configuration);
		
		startMetrics();
		startUpdater();
		
		// Managers
		PluginManager plugins = getServer().getPluginManager();
		protocolManager = ProtocolLibrary.getProtocolManager();
		
		listenerPacket = new ItemRenamerPacket(this, configuration, processor, protocolManager, logger);
		listenerPlayerJoin = new ItemRenamerPlayerJoin(this);
		listenerGameModeChange = new ItemRenamerGameModeChange(this);
		stackRestrictor = new ItemRenamerStackRestrictor(processor);
		
		plugins.registerEvents(listenerPlayerJoin, this);
		plugins.registerEvents(listenerGameModeChange, this);
		plugins.registerEvents(stackRestrictor, this);
		
		commandExecutor = new ItemRenamerCommands(this, configuration);
		getCommand("ItemRenamer").setExecutor(commandExecutor);
	}

	private void startUpdater() {
		try {
			if (configFile.getBoolean("autoupdate") && !(updateReady)) {
				
				// Start Updater but just do a version check
				Updater updater = new Updater(this, updateSlug, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false); 
				
				// Determine if there is an update ready for us
				updateReady = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE; 
				updateName = updater.getLatestVersionString(); // Get the latest version
				updateSize = updater.getFileSize(); // Get latest size
			}
		} catch (Exception e) {
			logger.warning("Failed to start Updater");
		}
	}

	private void startMetrics() {
		try {
		    BukkitMetrics metrics = new BukkitMetrics(this);
		    metrics.start();
		} catch (Exception e) {
			logger.warning("Failed to start Metrics");
		}
	}

	// TODO: Determine if this is necessary
	public void performConversion() {
		if ((configFile.contains("pack")) && (!configFile.contains("packs.converted"))) { //conversion ftw
			configFile.set("packs.converted", configFile.getConfigurationSection("pack"));
			configFile.set("pack", null);
			saveConfig();
		}
	}
	
	@Override
	public void onDisable() {
		listenerPacket.unregister();
		listenerPlayerJoin.unregister();
		listenerGameModeChange.unregister();
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
