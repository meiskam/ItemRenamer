/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class ItemRenamerCommandExecutor implements CommandExecutor {
	// Different permissions
	private static final String PERM_GET = "itemrenamer.config.get";
	private static final String PERM_SET= "itemrenamer.config.set";
	
	// The super command
	private static final Object COMMAND_ITEMRENAMER = "itemrenamer";
	
	// Recognized sub-commands
	public enum Commands {
		GET_AUTO_UPDATE,
		GET_CREATIVE_DISABLE, 
		SET_AUTO_UPDATE,
		SET_CREATIVE_DISABLE,
		GET_WORLD_PACK, 
		SET_WORLD_PACK,
		DELETE_PACK,
		SET_NAME,
	}
	
	private ItemRenamer plugin;
	private ItemRenamerConfiguration config;
	
	private CommandMatcher<Commands> matcher;
	
	public ItemRenamerCommandExecutor(ItemRenamer plugin, ItemRenamerConfiguration config) {
		this.plugin = plugin;
		this.matcher = registerCommands();
		this.config = config;
	}
	
	private CommandMatcher<Commands> registerCommands() {
		CommandMatcher<Commands> output = new CommandMatcher<Commands>();
		output.registerCommand(Commands.GET_AUTO_UPDATE, PERM_GET, "get autoupdate");
		output.registerCommand(Commands.GET_CREATIVE_DISABLE, PERM_GET, "get creativedisable");
		output.registerCommand(Commands.SET_AUTO_UPDATE, PERM_SET, "set autoupdate");
		output.registerCommand(Commands.SET_CREATIVE_DISABLE, PERM_SET, "set creativedisable");
		output.registerCommand(Commands.GET_WORLD_PACK, PERM_GET, "get world");
		output.registerCommand(Commands.SET_WORLD_PACK, PERM_SET, "set world");
		output.registerCommand(Commands.DELETE_PACK, PERM_SET, "delete pack");
		output.registerCommand(Commands.SET_NAME, PERM_GET, "set name");
		
		return output;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] arguments) {
		if (cmd.getName().equals(COMMAND_ITEMRENAMER)) {
			LinkedList<String> input = Lists.newLinkedList(Arrays.asList(arguments));
			
			// See which node is closest
			CommandMatcher<Commands>.CommandNode node = matcher.matchClosest(input);
			
			if (node.isCommand()) {
				try {
					sender.sendMessage(ChatColor.GOLD + performCommand(node.getCommand(), input));
				} catch (CommandErrorException e) {
					sender.sendMessage(ChatColor.RED + e.getMessage());
				}
			} else {
				sender.sendMessage(ChatColor.RED + "Available sub commands are: " + 
									Joiner.on(", ").join(node.getChildren()));
			}
			
			// It's still somewhat correct
			return true;
		} else {
			return false;
		}
	}
	
	private String performCommand(Commands command, Deque<String> args) {
		switch (command) {
			case GET_AUTO_UPDATE: 
				expectCommandCount(args, 0);
				return formatBoolean("Auto update is %s.", config.isAutoUpdate()); 
			case GET_CREATIVE_DISABLE: 
				expectCommandCount(args, 0);
				return formatBoolean("Creative disable is %s.", config.isCreativeDisabled());
			case SET_AUTO_UPDATE:
				expectCommandCount(args, 1);
				config.setAutoUpdate(parseBoolean(args.poll()));
				return "Updated auto update.";
			case SET_CREATIVE_DISABLE:
				expectCommandCount(args, 1);
				config.setCreativeDisabled(parseBoolean(args.poll()));
				return "Updated creative disable.";
			case GET_WORLD_PACK: 
				expectCommandCount(args, 1);
				return getWorldPack(args);
			case SET_WORLD_PACK:
				expectCommandCount(args, 2);
				return setWorldPack(args);
			case DELETE_PACK:
				expectCommandCount(args, 1);
				return deleteWorldPack(args);
			case SET_NAME:
				expectCommandCount(args, 2);
				return setItemName(args);
			default:
				throw new CommandErrorException("Unrecognized sub command: " + command);
		}
	}
	
	private String setItemName(Deque<String> args) {

		
		
	}

	private String deleteWorldPack(Deque<String> args) {
		String pack = args.poll();
		
		config.getRenameConfig().removePack(pack);
		return "Deleted pack " + pack;
	}

	private void expectCommandCount(Deque<String> args, int expected) {
		if (expected == args.size())
			throw new CommandErrorException((args.size() - expected) + " too many arguments.");
	}
	
	private String getWorldPack(Deque<String> args) {
		String world = args.poll();
		
		// Retrieve world pack
		return "Item pack for " + world + ": " + config.getWorldPack(world);
	}

	public String setWorldPack(Deque<String> args) {
		String world = checkWorld(args.poll()), pack = args.poll();
		
		config.setWorldPack(world, pack);
		return "Set the item pack in world " + world + " to " + pack;
	}
	
	private String checkWorld(String world) {
		// Ensure the world exists
		if (plugin.getServer().getWorld(world) == null)
			throw new CommandErrorException("Cannot find world " + world);
		return world;
	}
	
	private String formatBoolean(String format, boolean value) {
		return String.format(format, value ? "enabled" : "disabled");
	}
	
	// Simple boolean parsing
	private boolean parseBoolean(String value) {
		if (Arrays.asList("true", "yes", "enabled", "on", "1").contains(value))
			return true;
		else if (Arrays.asList("false", "no", "disabled", "off", "0").contains(value)) {
			return false;
		} else {
			throw new CommandErrorException("Cannot parse " + value + " as a boolean (yes/no)");
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("ItemRenamer")) {
			return false;
		}
		if (args.length == 0) {
			sender.sendMessage("["+label+"] Subcommand: config");
			return true;
		}
		if (args[0].equalsIgnoreCase("config")) {
			if (args.length == 1) {
				sender.sendMessage("["+label+":config] Subcommands: get, set, reload");
				return true;
			}
			if (args[1].equalsIgnoreCase("get") || args[1].equalsIgnoreCase("view")) {
				if (!sender.hasPermission(PERM_GET)) {
					sender.sendMessage("["+label+":config:get] You don't have permission to use that command");
					return true;
				}
				if (args.length == 2) {
					sender.sendMessage("["+label+":config:get] Config variables: "+ItemRenamer.configKeysString);
				} else if (args.length == 3) {
					String key = args[2].toLowerCase();
					sender.sendMessage("["+label+":config:get] "+key+": "+plugin.configFile.get(key));
				} else {
					sender.sendMessage("["+label+":config:get] Syntax: "+label+" config get [variable]");
				}
				return true;
			} else if (args[1].equalsIgnoreCase("set")) {
				if (!sender.hasPermission("itemrenamer.config.set")) {
					sender.sendMessage("["+label+":config:set] You don't have permission to use that command");
					return true;
				}
				if (args.length == 2 || args.length == 3) {
					sender.sendMessage("["+label+":config:set] Config variables: "+ItemRenamer.configKeysString);
					return true;
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
									sender.sendMessage("["+label+":config:set] ERROR: Can not convert "+value+" to a number");
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
						value = "";
						for (int i = 3; i<args.length; i++) {
							value += args[i] + " ";
						}
						value = value.trim();
						if (key.endsWith(".lore")) {
							List<String> valueList;
							try {
								valueList = yaml.loadAs(value, List.class);
							} catch (YAMLException e) {
								sender.sendMessage("["+label+":config:set] Error setting "+key+", " +
										"be sure to surround lore in [square brackets] and [\"quotes\"] if you're using special characters");
								return true;
							}
							try {
								plugin.configFile.set(key, valueList);
							} catch (IllegalArgumentException e) {
								sender.sendMessage("["+label+":config:set] Error setting "+key);
								return true;
							}
						} else {
							try {
								plugin.configFile.set(key, value);
							} catch (IllegalArgumentException e) {
								sender.sendMessage("["+label+":config:set] Error setting "+key);
								return true;
							}
						}
					}
					plugin.saveConfig();
					sender.sendMessage("["+label+":config:set] "+key+": "+plugin.configFile.get(key));
					return true;
				} else {
					sender.sendMessage("["+label+":config:set] Syntax: "+label+" config set [variable] [value]");
					return true;
				}
			} else if (args[1].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("itemrenamer.config.set")) {
					sender.sendMessage("["+label+":config:reload] You don't have permission to use that command");
					return true;
				}
				plugin.reloadConfig();
				plugin.configFile = plugin.getConfig();
				sender.sendMessage("["+label+":config:reload] Config reloaded");
				return true;
			} else {
				sender.sendMessage("["+label+":config:??] Invalid subcommand");
				return true;
			}
/*		} else if (args[0].equalsIgnoreCase("somethingelse")) {
			sender.sendMessage("["+label+":??] moo");
			return true;
*/		} else {
			sender.sendMessage("["+label+":??] Invalid subcommand");
			return true;
		}
	}

}
