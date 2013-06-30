package org.shininet.bukkit.itemrenamer.configuration;

import java.io.File;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;
import org.shininet.bukkit.itemrenamer.ItemRenamerPlugin;

/**
 * Represents a item renamer configuration file.
 * 
 * @author Kristian
 */
public class ItemRenamerConfiguration {
	// Locations in the configuration file
	private static final String AUTO_UPDATE = "autoupdate";
	private static final String STACK_RESTRICTOR = "stackrestrictor";
	private static final String WORLD_PACKS = "worlds";
	
	private RenameConfiguration renameConfig;
	private FileConfiguration config;
	private final ItemRenamerPlugin plugin;
	
	// Path to the configuration file
	private final String path;
	
	// Number of times it has changed
	private int modCount;
	
	public ItemRenamerConfiguration(ItemRenamerPlugin plugin, String path) {
		this.plugin = plugin;
		this.path = path;
		
		// Copy over default values if it doesn't already exist
		if (!getFile().exists()) {
			plugin.getConfig().options().copyDefaults(true);
			plugin.saveDefaultConfig();
		}
		initializeConfig();
	}
	
	private File getFile() {
		return new File(path);
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
	 * @return The default item name pack, or NULL if not found.
	 */
	public String getWorldPack(String world) {
		return config.getString(WORLD_PACKS + "." + world);
	}
	
	public boolean hasStackRestrictor() {
		return config.getBoolean(STACK_RESTRICTOR, true);
	}
	
	public void setStackRestrictor(boolean value) {
		config.set(STACK_RESTRICTOR, value);
	}
	
	/**
	 * Retrieve a list of the worlds that have been defined in the configuration section.
	 * @return List of defined worlds in the config.
	 */
	public Set<String> getWorldKeys() {
		return config.getConfigurationSection(WORLD_PACKS).getKeys(false);
	}
	
	/**
	 * Retrieve the number of modications to the configuration since last reload.
	 * @return The number of modifications.
	 */
	public int getModificationCount() {
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
