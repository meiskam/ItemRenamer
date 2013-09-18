/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.shininet.bukkit.itemrenamer.ItemRenamerPlugin;
import org.shininet.bukkit.itemrenamer.component.AbstractBukkitComponent;

/**
 * Represents a Bukkit listener component that notifies players of an available update.
 * @author Kristian
 */
public class UpdateNotifierComponent extends AbstractBukkitComponent {
	private final ItemRenamerPlugin plugin;
	
	public UpdateNotifierComponent(ItemRenamerPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		if (player.hasPermission("itemrenamer.update") && plugin.getUpdateReady()) {
			player.sendMessage("[ItemRenamer] An update is available: " + plugin.getUpdateName() + "(" + plugin.getUpdateSize() + " bytes)");
			player.sendMessage("[ItemRenamer] http://curse.com/server-mods/minecraft/" + ItemRenamerPlugin.updateSlug);
		}
	}
}