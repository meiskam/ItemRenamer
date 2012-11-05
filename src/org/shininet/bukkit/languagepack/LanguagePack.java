/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.languagepack;

import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class LanguagePack extends JavaPlugin {
	public Logger logger;
	public FileConfiguration configFile;
	private static boolean updateReady = false;
	private static String updateName = "";
	private static long updateSize = 0;
	private static final String updateSlug = "languagepack";
	private LanguagePackCommandExecutor commandExecutor;
	private LanguagePackListener listener;
	
	@Override
	public void onEnable(){
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
		listener = new LanguagePackListener(this);
		commandExecutor = new LanguagePackCommandExecutor(this);
		getServer().getPluginManager().registerEvents(listener, this);
		getCommand("LanguagePack").setExecutor(commandExecutor);
	}

	@Override
	public void onDisable() {
		listener.unregister();
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
	
}
