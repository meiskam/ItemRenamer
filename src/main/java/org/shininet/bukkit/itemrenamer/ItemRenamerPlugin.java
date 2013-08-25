/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.shininet.bukkit.itemrenamer.component.Component;
import org.shininet.bukkit.itemrenamer.component.Components;
import org.shininet.bukkit.itemrenamer.component.ToggleComponent;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.listeners.ProtocolComponent;
import org.shininet.bukkit.itemrenamer.listeners.UpdateNotifierComponent;
import org.shininet.bukkit.itemrenamer.listeners.StackRestrictorComponent;
import org.shininet.bukkit.itemrenamer.metrics.BukkitMetrics;
import org.shininet.bukkit.itemrenamer.metrics.Updater;

import com.comphenix.protocol.ProtocolLibrary;

public class ItemRenamerPlugin extends JavaPlugin {
	private static boolean updateReady = false;
	private static String updateName = "";
	private static long updateSize = 0;

	public static final String updateSlug = "itemrenamer";
	
	// The current API
	private static ItemRenamerAPI renamerAPI;
	
	private Logger logger;
    private RefreshInventoryTask refreshTask;

    private ItemRenamerConfiguration config;
    private RenameProcessor processor;
    
    // For tracking the currently selected item
    private SelectedItemTracker selectedTracker;
    
    // Current registered components
    private Component compositeComponent;
    
    // For restricting the current stack
    private ToggleComponent toggleRestrictor;
    
    private int lastSaveCount;
	private Chat chat;
	
	/**
	 * Retrieve the renamer API.
	 * @return The renamer API.
	 */
	public static ItemRenamerAPI getRenamerAPI() {
		return renamerAPI;
	}
	
	@Override
	public void onEnable() {
		logger = getLogger();
		config = new ItemRenamerConfiguration(this, new File(getDataFolder(), "config.yml").getAbsolutePath()) {
			protected void onSynchronized() {
				lastSaveCount = getModificationCount();
				refreshTask.forceRefresh();
			}
		};
		
		if (setupChat()) {
			logger.info("Found Vault!");
		}
		
		startMetrics();
		startUpdater();
		
		// Initialize helpers
        processor = new RenameProcessor(config, chat);
		selectedTracker = new SelectedItemTracker();
		
		// The stack restrictor that can be enabled or disabled
        toggleRestrictor = Components.asToggleable(new StackRestrictorComponent(processor));
		
		// Update stack restrictor
		if (config.hasStackRestrictor()) {
			logger.info("Starting stack restrictor.");
		} else {
			logger.warning("Stack restrictor has been disabled.");
		}
		refreshStackRestrictor();
        
		// Packet and Bukkit listeners
        ProtocolComponent listenerPacket = new ProtocolComponent(processor, ProtocolLibrary.getProtocolManager(), logger);
        UpdateNotifierComponent listenerPlayerJoin = new UpdateNotifierComponent(this);
        
        // Every component
        compositeComponent = Components.asComposite(toggleRestrictor, listenerPacket, listenerPlayerJoin);			
        compositeComponent.register(this);
        
        ItemRenamerCommands commandExecutor = new ItemRenamerCommands(this, config, selectedTracker);
		getCommand("ItemRenamer").setExecutor(commandExecutor);
		
		// Tasks
		refreshTask = new RefreshInventoryTask(getServer().getScheduler(), this, config);
		refreshTask.start();
		
		// Initialize the API
		renamerAPI = new ItemRenamerAPI(config, processor);
		checkWorlds();
	}
	
	private void checkWorlds() {
		Set<String> specifiedWorlds = config.getWorldKeys();
		
		// Warn if a world cannot be found
		for (String world : specifiedWorlds) {
			if (getServer().getWorld(world) == null) {
				logger.warning("Unable to find world " + world + ". Config may be invalid.");
			} else {
				// Is the pack valid
				String pack = config.getEffectiveWorldPack(world);
				
				if (config.getRenameConfig().hasPack(pack))
					logger.info("Item renaming enabled for world " + world);
				else
					logger.warning("Cannot find pack " + pack + " for world " + world);
			}
		}
		
		// "Load" default packs as well
		if (config.getDefaultPack() != null) {
			for (World world : getServer().getWorlds()) {
				if (!specifiedWorlds.contains(world.getName())) {
					logger.info("Item renaming enabled for world " + world.getName());
				}
			}
		}
	}
	
	/**
	 * Ensure that the stack restrictor is registered or not.
	 */
	public void refreshStackRestrictor() {
		toggleRestrictor.setEnabled(config.hasStackRestrictor());
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

	/**
	 * Initialize reference to Vault.
	 * @return TRUE if Vault was detected and loaded, FALSE otherwise.
	 */
    private boolean setupChat() {
    	try {
	        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(Chat.class);
	        
	        if (chatProvider != null) {
	            chat = chatProvider.getProvider();
	        }
	        return (chat != null);
    	} catch (NoClassDefFoundError e) {
    		// Nope
    		return false;
    	}
    }

	@Override
	public void onDisable() {
		// Save all changes if anything has changed
		if (config.getModificationCount() != lastSaveCount) {
			config.save();
			logger.info("Saving configuration.");
		}
		
		compositeComponent.unregister(this);
		refreshTask.stop();
		
		// Clear API
		renamerAPI = null;
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
