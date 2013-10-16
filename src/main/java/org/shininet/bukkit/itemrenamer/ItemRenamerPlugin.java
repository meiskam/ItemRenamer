/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.shininet.bukkit.itemrenamer.component.Component;
import org.shininet.bukkit.itemrenamer.component.Components;
import org.shininet.bukkit.itemrenamer.component.ToggleComponent;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.listeners.ListenerCleanupComponent;
import org.shininet.bukkit.itemrenamer.listeners.ProtocolComponent;
import org.shininet.bukkit.itemrenamer.listeners.UpdateNotifierComponent;
import org.shininet.bukkit.itemrenamer.listeners.StackRestrictorComponent;
import org.shininet.bukkit.itemrenamer.metrics.BukkitMetrics;
import org.shininet.bukkit.itemrenamer.metrics.Updater;
import org.shininet.bukkit.itemrenamer.metrics.Updater.UpdateResult;

import com.comphenix.protocol.ProtocolLibrary;

public class ItemRenamerPlugin extends JavaPlugin {
	/**
	 * The BukkitDev project ID for ItemRenamer.
	 * <p>
	 * This can be retrieved using the 
	 * <a href="https://api.curseforge.com/servermods/projects?search=itemrenamer">Curse API</a>
	 */
	public static final int BUKKIT_DEV_ID = 46933;

	/**
	 * The current BukkitDev slug. May change in the future.
	 */
	public static final String BUKKIT_DEV_SLUG = "itemrenamer";

	// The current API
	private static ItemRenamerAPI RENAMER_API;
	
	// The current updater
	private Updater updater;
	
	private Logger logger;
    private RefreshInventoryTask refreshTask;

    private ItemRenamerConfiguration config;
    private RenameProcessor processor;
    
    // Current registered components
    private UpdateNotifierComponent updateNotifyComponent;
    private Component compositeComponent;
    
    // For tracking the currently selected item
    private SelectedItemTracker selectedTracker;
    // For restricting the current stack
    private ToggleComponent toggleRestrictor;
    
    // Rename listeners
    private RenameListenerManager listenerManager;
    
    // Handling saving
    private int lastSaveCount;
	private Chat chat;
	
	/**
	 * Retrieve the renamer API.
	 * @return The renamer API.
	 */
	public static ItemRenamerAPI getRenamerAPI() {
		return RENAMER_API;
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
		listenerManager = new RenameListenerManager(this);
        processor = new RenameProcessor(listenerManager, config, chat);
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
		ListenerCleanupComponent cleanupComponent = new ListenerCleanupComponent(this);
        updateNotifyComponent = new UpdateNotifierComponent(this);
        
        // Every component
        compositeComponent = Components.asComposite(toggleRestrictor, listenerPacket, updateNotifyComponent, cleanupComponent);			
        compositeComponent.register(this);
        
        ItemRenamerCommands commandExecutor = new ItemRenamerCommands(this, config, selectedTracker);
		getCommand("ItemRenamer").setExecutor(commandExecutor);
		
		// Tasks
		refreshTask = new RefreshInventoryTask(getServer().getScheduler(), this, config);
		refreshTask.start();
		
		// Initialize the API
		RENAMER_API = new ItemRenamerAPI(config, processor);
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
			if (config.isAutoUpdate() && updater == null) {
				// Start Updater but just do a version check
				updater = new Updater(this, BUKKIT_DEV_ID, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false); 
				logger.info("Checking for updates ...");
				
				// Inform the console too
				updater.addListener(new Runnable() {
					@Override
					public void run() {
						updateNotifyComponent.notifySender(Bukkit.getConsoleSender());
					}
				});
				updater.start();
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
		RENAMER_API = null;
	}
	
	/**
	 * Clean up every listener associated with the given plugin.
	 * @param plugin - the plugin.
	 */
	public void cleanupPlugin(Plugin plugin) {
		listenerManager.removeListeners(plugin);
	}

	public boolean getUpdateReady() {
		return updater != null && updater.getResult() == UpdateResult.UPDATE_AVAILABLE;
	}

	public String getUpdateName() {
		return updater != null ? updater.getLatestName() : null;
	}
}
