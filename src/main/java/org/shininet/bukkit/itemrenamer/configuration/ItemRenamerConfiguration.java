package org.shininet.bukkit.itemrenamer.configuration;

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
	private static final String CREATIVE_DISABLED = "creativedisabled";
	private static final String WORLD_PACKS = "worlds";
	
	private FileConfiguration config;
	private ItemRenamer plugin;
	
	private RenameConfiguration renameConfig;
	
	public ItemRenamerConfiguration(ItemRenamer plugin) {
		this.plugin = plugin;
		
		// Copy over default values
		plugin.getConfig().options().copyDefaults(true);
		plugin.saveDefaultConfig();
		initializeConfig();
	}
	
	public void save() {
		// Save and reload
		plugin.saveConfig();
		config = plugin.getConfig();
	}
	
	public void reload() {
		plugin.reloadConfig();
		initializeConfig();
	}
	
	private void initializeConfig() {
		config = plugin.getConfig();
		renameConfig = new RenameConfiguration(config.getConfigurationSection("packs"));
	}
	
	public boolean isAutoUpdate() {
		return config.getBoolean(AUTO_UPDATE);
	}
	
	public void setAutoUpdate(boolean value) {
		config.set(AUTO_UPDATE, value);
	}
	
	public boolean isCreativeDisabled() {
		return config.getBoolean(CREATIVE_DISABLED);
	}
	
	public void setCreativeDisabled(boolean value) {
		config.set(CREATIVE_DISABLED, value);
	}
	
	/**
	 * Set the default item name pack for a given world.
	 * @param world - the world name.
	 * @param pack - the pack name.
	 */
	public void setWorldPack(String world, String pack) {
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
	 * Access the current rename rules for every pack.
	 * @return Rename rules for every pack.
	 */
	public RenameConfiguration getRenameConfig() {
		return renameConfig;
	}
}
