package org.shininet.bukkit.itemrenamer.configuration;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.shininet.bukkit.itemrenamer.ItemRenamer;

/**
 * Represents a item renamer configuration file.
 * 
 * @author Kristian
 */
public class ItemRenamerConfiguration {
	// Locations in the configuration file
	private static final String AUTO_UPDATE = "autoupdate";
	private static final String WORLD_PACKS = "worlds";
	
	private RenameConfiguration renameConfig;
	private FileConfiguration config;
	private ItemRenamer plugin;
	
	// Path to the configuration file
	private String path;
	
	// Number of times it has changed
	private int modCount;
	
	public ItemRenamerConfiguration(ItemRenamer plugin, String path) {
		this.plugin = plugin;
		this.path = path;
		
		// Copy over default values if it doesn't already exist
		if (new File(path).exists()) {
			plugin.getConfig().options().copyDefaults(true);
			plugin.saveDefaultConfig();
		}
		initializeConfig();
	}
	
	public void save() {
		// Backup the current configuration
		File currentFile = new File(path);
		File backupFolder = new File(currentFile.getParentFile(), "backup");
		File backupFile = new File(backupFolder, "config" + System.currentTimeMillis() + ".yml");
		
		// Don't overwrite - rename
		if ((backupFolder.exists() || backupFolder.mkdirs()) && currentFile.renameTo(backupFile)) {
			// Save and reload
			modCount = 0;
			onSynchronized();
			
			renameConfig.saveAll();
			plugin.saveConfig();
			config = plugin.getConfig();
		} else {
			throw new RuntimeException("Cannot rename " + currentFile + " to " + backupFile + " for backup.");
		}
	}
	
	public void reload() {		
		plugin.reloadConfig();
		initializeConfig();
		
		modCount = 0;
		onSynchronized();
	}
	
	/**
	 * Invoked when the configuration has been synchronized with the underlying file system.
	 */
	protected void onSynchronized() {
		// Do nothing
	}
	
	private void initializeConfig() {
		config = plugin.getConfig();
		renameConfig = new RenameConfiguration(config.getConfigurationSection("packs"));
	}
	
	public boolean isAutoUpdate() {
		return config.getBoolean(AUTO_UPDATE);
	}
	
	public void setAutoUpdate(boolean value) {
		modCount++;
		config.set(AUTO_UPDATE, value);
	}
	
	/**
	 * Set the default item name pack for a given world.
	 * @param world - the world name.
	 * @param pack - the pack name.
	 */
	public void setWorldPack(String world, String pack) {
		modCount++;
		config.set(WORLD_PACKS + "." + world, pack);
	}
	
	/**
	 * Retrieve the default item name pack for a given world.
	 * @param world - the name of the world.
	 * @return The default item name pack.
	 */
	public String getWorldPack(String world) {
		return config.getString(WORLD_PACKS + "." + world);
	}
	
	/**
	 * Retrieve the number of modications to the configuration since last reload.
	 * @return The number of modifications.
	 */
	public int getModificationCount() {
		if (modCount > 0) {
			System.out.println("[ItemRenamer] Main config has changed");
		}
		return modCount + renameConfig.getModificationCount();
	}
	
	/**
	 * Access the current rename rules for every pack.
	 * @return Rename rules for every pack.
	 */
	public RenameConfiguration getRenameConfig() {
		return renameConfig;
	}
}
