package org.shininet.bukkit.languagepack.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.shininet.bukkit.languagepack.LanguagePack;

public class LanguagePackPlayerJoin implements Listener {
	
	private LanguagePack plugin;
	
	public LanguagePackPlayerJoin(LanguagePack plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if(player.hasPermission("languagepack.update") && plugin.getUpdateReady())
		{
			player.sendMessage("[LanguagePack] An update is available: " + plugin.getUpdateName() + "(" + plugin.getUpdateSize() + " bytes");
			player.sendMessage("[LanguagePack] Type \"/LanguagePack update\" if you would like to update.");
		}
	}
	
	public void unregister() {
		PlayerJoinEvent.getHandlerList().unregister(this);
	}
}
