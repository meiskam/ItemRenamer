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
	private static final String DEFAULT_PACK = "default";
	
	private RenameConfiguration renameConfig;
	private FileConfiguration config;
	private final ItemRenamerPlugin plugin;
	
	// Path to the configuration file
	private final String path;
	
	// Number of times it has changed
	private int modCount;
	
	/**
	 * Construct a new renamer configuration.
	 * @param plugin - the renamer plugin.
	 * @param path - the path.
	 */
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
	
	/**
	 * Retrieve the underlying file.
	 * @return The underlying file.
	 */
	private File getFile() {
		return new File(path);
	}
	
	/**
	 * Save the current configuration to disk.
	 */
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
	
	/**
	 * Reload the configuration from disk.
	 */
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
	
	/**
	 * Determine if automatic updates are enabled.
	 * @return TRUE if they are, FALSE otherwise.
	 */
	public boolean isAutoUpdate() {
		return config.getBoolean(AUTO_UPDATE);
	}
	
	/**
	 * Set the default world pack.
	 * @param pack - the default world pack.
	 */
	public void setDefaultPack(String pack) {
		modCount++;
		config.set(DEFAULT_PACK, pack);
	}
	
	/**
	 * Retrieve the default world pack.
	 * @return Default pack, or NULL.
	 */
	public String getDefaultPack() {
		return config.getString(DEFAULT_PACK);
	}
	
	/**
	 * Set whether or not the plugin will automatically check for updates.
	 * @param value - TRUE if it should, FALSE otherwise.
	 */
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
	
	/**
	 * Retrieve the effective rename pack for a world.
	 * <p>
	 * If a specific world pack is missing, fall back to the default rename pack.
	 * @param world - the world to lookup.
	 * @return The effective world pack.
	 */
	public String getEffectiveWorldPack(String world) {
		String pack = getWorldPack(world);
		
		if (pack == null) {
			pack = getDefaultPack();
		}
		return pack;
	}
	
	/**
	 * Determine if the stack restrictor is enabled.
	 * @return - the stack restrictor.
	 */
	public boolean hasStackRestrictor() {
		return config.getBoolean(STACK_RESTRICTOR, true);
	}
	
	/**
	 * Set whether or not the stack restrictor is enabled.
	 * @param value - the stack restrictor.
	 */
	public void setStackRestrictor(boolean value) {
		config.set(STACK_RESTRICTOR, value);
		plugin.refreshStackRestrictor();
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
