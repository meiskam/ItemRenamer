/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.shininet.bukkit.itemrenamer.configuration.DamageLookup;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.comphenix.itemrenamer.command.AnnotationCommandExecutor;
import com.comphenix.itemrenamer.command.CommandCall;
import com.comphenix.itemrenamer.command.RestrictArray;
import com.comphenix.itemrenamer.command.RestrictString;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class ItemRenamerCommands {
	private ItemRenamer plugin;
	private ItemRenamerConfiguration config;
	
	private enum BoolSettings {
		CREATIVE_WORLD,
		AUTO_UPDATE
	}
	
	private enum DamageMatcher {
		ALL,
		OTHER
	}
	
	public ItemRenamerCommands(ItemRenamer plugin, ItemRenamerConfiguration config) {
		this.plugin = plugin;
		this.config = config;
	}
	
	@CommandCall(name = "itemrenamer")
	public void printMainCommands(CommandSender sender) {
		sender.sendMessage("Subcommands: set get reload");
	}
	
	@CommandCall(name = "itemrenamer get")
	public void getBooleanSetting(CommandSender sender, BoolSettings setting) {
		boolean value = false;
		
		// Easily extendable
		switch (setting) {
			case AUTO_UPDATE: value = config.isAutoUpdate();
			case CREATIVE_WORLD: value = config.isCreativeDisabled();
		}
		sender.sendMessage(ChatColor.GOLD + setting.name() + " is " + (value ? "disabled" : "enabled"));
	}
	
	@CommandCall(name = "itemrenamer set")
	public void setBooleanSetting(CommandSender sender, BoolSettings setting, boolean value) {
		switch (setting) {
			case AUTO_UPDATE: config.setAutoUpdate(value);
			case CREATIVE_WORLD: config.setCreativeDisabled(value);
		}
		sender.sendMessage(ChatColor.GOLD + "Updated " + setting.name() + " to " + value);
	}
	
	@CommandCall(name = "itemrenamer get world")
	public void getWorldPack(CommandSender sender, String worldName) {
		String pack = config.getWorldPack(worldName);
		
		// You could also define your own parser
		if (pack != null)
			sender.sendMessage(String.format("World pack for %s is %s.", worldName, pack));
		else
			sender.sendMessage(ChatColor.RED + "World " + worldName + " is not recognized.");
	}
	
	@CommandCall(name = "itemrenamer set world")
	public void setWorldPack(CommandSender sender, String worldName, String packName) {
		// Will always succeed
		config.setWorldPack(worldName, packName);
		sender.sendMessage("Set world " + worldName + " item pack to " + packName);
	}
	
	@CommandCall(name = "itemrenamer get name")
	public void getItemPackName(CommandSender sender, String pack, int itemID, DamageMatcher matcher) {
		RenameRule rule = getRule(pack, itemID, matcher);

		if (rule != null) {
			sender.sendMessage(ChatColor.GOLD + String.format(
				"Item %s:%s in pack %s - %s", itemID, matcher.name(), pack, rule.getName()));
		} else {
			sender.sendMessage(ChatColor.RED + String.format(
				"Cannot find %s:%s for pack %s.", itemID, matcher.name(), pack));
		}
	}
	
	@CommandCall(name = "itemrenamer set name")
	public void setItemPackName(CommandSender sender, String pack, int itemID, DamageMatcher matcher, String name) {
		DamageLookup lookup = config.getRenameConfig().createLookup(pack, itemID);
		
		switch (matcher) {
			case ALL: lookup.setAllRule(RenameRule.withName(lookup.getAllRule(), name)); break;
			case OTHER: lookup.setOtherRule(RenameRule.withName(lookup.getOtherRule(), name)); break;
		}
		sender.sendMessage(ChatColor.GOLD + String.format(
			"Set name of %s:%s in pack %s to %s.", itemID, matcher.name(), pack, name));
	}
	
	private RenameRule getRule(String pack, int itemID, DamageMatcher matcher) {
		DamageLookup lookup = config.getRenameConfig().getLookup(pack, itemID);
		
		switch (matcher) {
			case ALL: return lookup.getAllRule();
			case OTHER: return lookup.getOtherRule();
		}
	}
	
	// Order matter - place your error handlers at the end
	@CommandCall(name = "itemrenamer config")
	public void printNoSetting(CommandSender sender, @RestrictString(pattern = "set|get") String sub, 
							   String incorrectSetting, @RestrictArray(maxCount = 1) String[] argument) {
		sender.sendMessage(ChatColor.RED + "Cannot " + sub + " setting. " + incorrectSetting + " does not exist.");
	}
	
	@SuppressWarnings("unchecked")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("ItemRenamer")) {
			return false;
		}
		if (args.length == 0) {
			sender.sendMessage("["+label+"] Subcommand: config");
			return true;
		}
		final String command = args[0];
		
		if (command.equalsIgnoreCase("config")) {
			if (args.length == 1) {
				sender.sendMessage("["+label+":config] Subcommands: get, set, reload");
				return true;
			}
			final String subcommand = args[1];
			
			if (subcommand.equalsIgnoreCase("get") || subcommand.equalsIgnoreCase("view")) {
				if (!checkPermission(sender, subcommand, "itemrenamer.config.get"))
					return true;
				
				cmdGetConfig(sender, label + ":config:get", args);
				return true;

			} else if (subcommand.equalsIgnoreCase("set")) {
				if (!checkPermission(sender, subcommand, "itemrenamer.config.set"))
					return true;
				
				cmdSetConfig(sender, label + ":config:set", args);
				
			} else if (subcommand.equalsIgnoreCase("reload")) {
				if (!checkPermission(sender, subcommand, "itemrenamer.config.reload"))
					return true;
				
				plugin.reloadConfig();
				plugin.configFile = plugin.getConfig();
				sender.sendMessage("["+label+":config:reload] Config reloaded");
				return true;
				
			} else {
				sender.sendMessage("["+label+":config:??] Invalid subcommand");
				return true;
			}
		} else {
			sender.sendMessage("["+label+":??] Invalid subcommand");
			return true;
		}
		return false;
	}

	private void cmdSetConfig(CommandSender sender, String label, String[] args) {
		if (args.length == 2 || args.length == 3) {
			sender.sendMessage("["+label+"] Config variables: "+ItemRenamer.configKeysString);

		} else if (args.length >= 4) {
			String key = args[2].toLowerCase();
			String value = args[3].toLowerCase();
			boolean keyFound = false;
			
			for (String keySet : ItemRenamer.configKeys.keySet()) {
				if (key.equals(keySet.toLowerCase())) {
					keyFound = true;
					switch (ItemRenamer.configKeys.get(keySet.toLowerCase())) {
					case BOOLEAN:
						if (value.equals("false") || value.equals("no") || value.equals("0")) {
							plugin.configFile.set(key, false);
						} else {
							plugin.configFile.set(key, true);
						}
						break;
					case DOUBLE:
						try {
							plugin.configFile.set(key, Double.parseDouble(value));
						} catch (NumberFormatException e) {
							sender.sendMessage("["+label+"] ERROR: Can not convert "+value+" to a number");
						}
						break;
					default:
						plugin.logger.warning("configType \""+ItemRenamer.configKeys.get(keySet.toLowerCase())+"\" unrecognised - this is a bug");
						break;
					}
					break;
				}
			}
			if (!keyFound) {
				value = StringUtils.join(args, ' ', 3, args.length).trim();

				if (key.endsWith(".lore")) {
					List<String> valueList;
					try {
						//valueList = yaml.loadAs(value, List.class);
					} catch (YAMLException e) {
						sender.sendMessage("["+label+"] Error setting "+key+", " +
								"be sure to surround lore in [square brackets] and [\"quotes\"] if you're using special characters");
						return;
					}
					
					try {
						//plugin.configFile.set(key, valueList);
					} catch (IllegalArgumentException e) {
						sender.sendMessage("["+label+"] Error setting " + key);
						return;
					}
					
				} else {
					try {
						plugin.configFile.set(key, value);
					} catch (IllegalArgumentException e) {
						sender.sendMessage("["+label+"] Error setting " + key);
						return;
					}
				}
			}
			plugin.saveConfig();
			sender.sendMessage("["+label+"] "+ key + ": " + plugin.configFile.get(key));
		} else {
			sender.sendMessage("["+label+"] Syntax: " + label + " config set [variable] [value]");
		}
	}
	
	private void cmdGetConfig(CommandSender sender, String label, String[] args) {
		if (args.length == 2) {
			sender.sendMessage("["+label+"] Config variables: " + ItemRenamer.configKeysString);
		} else if (args.length == 3) {
			String key = args[2].toLowerCase();
			sender.sendMessage("["+label+"] " + key + ": "+plugin.configFile.get(key));
		} else {
			sender.sendMessage("["+label+"] Syntax: " + label + " config get [variable]");
		}
	}
	
	private boolean checkPermission(CommandSender sender, String commandName, String permission) {
		if (!sender.hasPermission(permission)) {
			sender.sendMessage("[config:" + commandName + "] You don't have permission to use that command");
			return false;
		}
		
		// The sender has permission!
		return true;
	}
}
