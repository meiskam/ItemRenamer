/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerPacket;
import org.shininet.bukkit.itemrenamer.listeners.ItemRenamerPlayerJoin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class ItemRenamer extends JavaPlugin {
	public Logger logger;
	public FileConfiguration configFile;
	private static boolean updateReady = false;
	private static String updateName = "";
	private static long updateSize = 0;
	private static final String updateSlug = "itemrenamer";
	private ItemRenamerCommandExecutor commandExecutor;
	private CommandExecutor oldCommandExecutor;
	private ItemRenamerPlayerJoin listenerPlayerJoin;
	private ItemRenamerPacket listenerPacket;
	private ProtocolManager protocolManager;
	public static enum configType {DOUBLE, BOOLEAN};
	@SuppressWarnings("serial")
	public static final Map<String, configType> configKeys = new HashMap<String, configType>(){
		{
			put("autoupdate", configType.BOOLEAN);
		}
	};
	public static final String configKeysString = implode(configKeys.keySet(), ", ");
	
	@Override
	public void onEnable(){
		logger = getLogger();
		configFile = getConfig();
		configFile.options().copyDefaults(true);
		this.saveDefaultConfig();
		this.saveResource("config.example.yml", true);
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (Exception e) {
			logger.warning("Failed to start Metrics");
		}
		try {
			if (configFile.getBoolean("autoupdate") && !(updateReady)) {
				Updater updater = new Updater(this, updateSlug, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false); // Start Updater but just do a version check
				updateReady = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE; // Determine if there is an update ready for us
				updateName = updater.getLatestVersionString(); // Get the latest version
				updateSize = updater.getFileSize(); // Get latest size
			}
		} catch (Exception e) {
			logger.warning("Failed to start Updater");
		}
		
		protocolManager = ProtocolLibrary.getProtocolManager();
		listenerPacket = new ItemRenamerPacket(this, protocolManager, logger);
		
		listenerPlayerJoin = new ItemRenamerPlayerJoin(this);
		getServer().getPluginManager().registerEvents(listenerPlayerJoin, this);
		
		oldCommandExecutor = getCommand("ItemRenamer").getExecutor();
		commandExecutor = new ItemRenamerCommandExecutor(this);
		getCommand("ItemRenamer").setExecutor(commandExecutor);
	}

	@Override
	public void onDisable() {
		listenerPacket.unregister();
		listenerPlayerJoin.unregister();
		if (oldCommandExecutor != null) {
			getCommand("ItemRenamer").setExecutor(oldCommandExecutor);
		}
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

	public void update() {
		new Updater(this, updateSlug, getFile(), Updater.UpdateType.NO_VERSION_CHECK, true);
	}
	
	public static String implode(Set<String> input, String glue) {
		int i = 0;
		StringBuilder output = new StringBuilder();
		for (String key : input) {
			if (i++ != 0) output.append(glue);
			output.append(key);
		}
		return output.toString();
	}

	private void packName(ItemMeta itemMeta, int id, int damage) {
		String output;
		if (((output = configFile.getString("pack."+id+".all.name")) == null) &&
				((output = configFile.getString("pack."+id+"."+damage+".name")) == null) &&
				((output = configFile.getString("pack."+id+".other.name")) == null)) {
			return;
		}
		itemMeta.setDisplayName(ChatColor.RESET+ChatColor.translateAlternateColorCodes('&', output)+ChatColor.RESET);
	}
	
	private void packLore(ItemMeta itemMeta, int id, int damage) {
		List<String> output;
		if (((output = configFile.getStringList("pack."+id+".all.lore")).isEmpty()) &&
				((output = configFile.getStringList("pack."+id+"."+damage+".lore")).isEmpty()) &&
				((output = configFile.getStringList("pack."+id+".other.lore")).isEmpty())) {
			return;
		}
		for (int i = 0; i < output.size(); i++) {
			output.set(i, ChatColor.translateAlternateColorCodes('&', output.get(i))+ChatColor.RESET);
		}
		itemMeta.setLore(output);
	}

	public ItemStack process(ItemStack input) {
		if (input == null) {
			return input;
		}
		ItemMeta itemMeta = input.getItemMeta();
		packName(itemMeta, input.getTypeId(), input.getDurability());
		packLore(itemMeta, input.getTypeId(), input.getDurability());
		input.setItemMeta(itemMeta);
		return input;
	}
	
	public ItemStack[] process(ItemStack[] input) {
		if (input == null) {
			return input;
		}
		for (int i = 0; i < input.length; i++) {
			input[i] = process(input[i]);
		}
		return input;
	}
}
