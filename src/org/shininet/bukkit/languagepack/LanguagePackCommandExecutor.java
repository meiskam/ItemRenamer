package org.shininet.bukkit.languagepack;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.shininet.bukkit.languagepack.LanguagePack;

public class LanguagePackCommandExecutor implements CommandExecutor {
	
	private LanguagePack plugin;
	
	public LanguagePackCommandExecutor(LanguagePack plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("PlayerHeads")) {
			return false;
		}
		if (args.length == 0) {
			sender.sendMessage("["+label+"] Subcommand: update");
			return true;
		}
		if (args[0].equalsIgnoreCase("update")) {
			if (!sender.hasPermission("languagepack.update")) {
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
