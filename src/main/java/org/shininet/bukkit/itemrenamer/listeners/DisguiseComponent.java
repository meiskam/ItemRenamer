package org.shininet.bukkit.itemrenamer.listeners;

import javax.annotation.Nonnull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.component.AbstractBukkitComponent;
import org.shininet.bukkit.itemrenamer.listeners.ProtocolComponent.LookupEntity;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import pgDev.bukkit.DisguiseCraft.DisguiseCraft;
import pgDev.bukkit.DisguiseCraft.api.DisguiseCraftAPI;
import pgDev.bukkit.DisguiseCraft.api.PlayerDisguiseEvent;
import pgDev.bukkit.DisguiseCraft.api.PlayerUndisguiseEvent;
import pgDev.bukkit.DisguiseCraft.disguise.Disguise;

public class DisguiseComponent extends AbstractBukkitComponent {
	private BiMap<Integer, Player> disguises = HashBiMap.create();
	
	// The current API
	private DisguiseCraftAPI api;
		
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerUndisguise(PlayerUndisguiseEvent e) {
		disguises.inverse().remove(e.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDisguise(PlayerDisguiseEvent e) {
		disguises.put(e.getDisguise().entityID, e.getPlayer());
	}
	
	@Subscribe
	public void onLookupEntity(LookupEntity e) {
		// Attempt to look it up with DisguiseCraft
		if (e.getEntity() == null) {
			e.setEntity(getPlayer(e.getEntityId()));
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		handlePlayer(e.getPlayer());
	}
	
	@Override
	protected void onRegistered(@Nonnull Plugin plugin, EventBus bus) {
		super.onRegistered(plugin, bus);
		this.api = DisguiseCraft.getAPI();

		System.out.println("Loading DisguiseCraft component.");
		
		// Prepare all existing disguises
		for (Player player : api.getOnlineDisguisedPlayers()) {
			handlePlayer(player);
		}
	}
	
	/**
	 * Ensure we have recorded the disguise of the given player.
	 * @param player - the player.
	 */
	private void handlePlayer(Player player) {
		Disguise disguise = api.getDisguise(player);
		
		if (disguise != null) {
			disguises.put(disguise.entityID, player);
		}
	}
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		super.onUnregistered(plugin);
		this.api = null;

		// Don't hold on to Player obj
		disguises.clear();
	}
	
	@Override
	protected boolean requireBukkit() {
		return true;
	}
	@Override
	protected boolean requireEventBus() {
		return true;
	}
	
	/**
	 * Retrieve the player from the given mob disguise.
	 * @param entityID - mob disguise.
	 * @return The player, or NULL.
	 */
	public Player getPlayer(int entityID) {
		return disguises.get(entityID);
	}
}
