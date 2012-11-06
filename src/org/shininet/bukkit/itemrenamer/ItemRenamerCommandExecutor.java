package org.shininet.bukkit.itemrenamer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ItemRenamerCommandExecutor implements CommandExecutor {
	
	private ItemRenamer plugin;
	
	public ItemRenamerCommandExecutor(ItemRenamer plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("ItemRenamer")) {
			return false;
		}
		if (args.length == 0) {
			sender.sendMessage("["+label+"] Subcommand: config, update");
			return true;
		}
		if (args[0].equalsIgnoreCase("config")) {
			if (args.length == 1) {
				sender.sendMessage("["+label+":config] Subcommands: get, set, reload");
				return true;
			}
			if (args[1].equalsIgnoreCase("get") || args[1].equalsIgnoreCase("view")) {
				if (!sender.hasPermission("itemrenamer.config.get")) {
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
				} else if (args.length == 4) {
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
						plugin.configFile.set(key, value);
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
		} else if (args[0].equalsIgnoreCase("update")) {
			if (!sender.hasPermission("itemrenamer.update")) {
				sender.sendMessage("["+label+":update] You don't have permission to use that command");
				return true;
			}
			if (!plugin.configFile.getBoolean("autoupdate")) {
				sender.sendMessage("["+label+":update] Updater is disabled");
				return true;
			}
			if (!plugin.getUpdateReady()) {
				sender.sendMessage("["+label+":update] There is no update available");
				return true;
			}
			plugin.update();
			sender.sendMessage("["+label+":update] Update started, check console for info");
			return true;
/*		} else if (args[0].equalsIgnoreCase("somethingelse")) {
			sender.sendMessage("["+label+":??] moo");
			return true;
*/		} else {
			sender.sendMessage("["+label+":??] Invalid subcommand");
			return true;
		}
	}

}
