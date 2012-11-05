/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.languagepack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.shininet.bukkit.languagepack.listeners.LanguagePackPacket;
import org.shininet.bukkit.languagepack.listeners.LanguagePackPlayerJoin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class LanguagePack extends JavaPlugin {
	public Logger logger;
	public FileConfiguration configFile;
	private static boolean updateReady = false;
	private static String updateName = "";
	private static long updateSize = 0;
	private static final String updateSlug = "languagepack";
	private LanguagePackCommandExecutor commandExecutor;
	private LanguagePackPlayerJoin listenerPlayerJoin;
	private LanguagePackPacket listenerPacket;
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
		saveConfig();
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (Exception e) {
			logger.warning("Failed to start Metrics");
		}
		if (configFile.getBoolean("autoupdate") && !(updateReady)) {
			Updater updater = new Updater(this, updateSlug, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, false); // Start Updater but just do a version check
			updateReady = updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE; // Determine if there is an update ready for us
			updateName = updater.getLatestVersionString(); // Get the latest version
			updateSize = updater.getFileSize(); // Get latest size
		}
		
		protocolManager = ProtocolLibrary.getProtocolManager();
		listenerPacket = new LanguagePackPacket(this, protocolManager, logger);
		
		listenerPlayerJoin = new LanguagePackPlayerJoin(this);
		getServer().getPluginManager().registerEvents(listenerPlayerJoin, this);
		
		commandExecutor = new LanguagePackCommandExecutor(this);
		getCommand("LanguagePack").setExecutor(commandExecutor);
	}

	@Override
	public void onDisable() {
		listenerPacket.unregister();
		listenerPlayerJoin.unregister();
		//getCommand("LanguagePack").setExecutor(null);
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
}
