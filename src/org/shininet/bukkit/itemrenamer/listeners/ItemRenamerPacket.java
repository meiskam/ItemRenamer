/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer.listeners;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.ItemRenamer;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.StructureModifier;

public class ItemRenamerPacket {

	private ItemRenamer myPlugin;
	private ProtocolManager protocolManager;
	private final Logger logger;
	private PacketAdapter packetAdapter;

	public ItemRenamerPacket(ItemRenamer myPlugin, ProtocolManager protocolManager, Logger logger) {
		this.myPlugin = myPlugin;
		this.protocolManager = protocolManager;
		this.logger = logger;
		addListener();
	}
	
	public void addListener() {

		protocolManager.addPacketListener(packetAdapter = new PacketAdapter(myPlugin, ConnectionSide.SERVER_SIDE, 0x67, 0x68) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				if ((myPlugin.configFile.getBoolean("creativedisable")) && (event.getPlayer().getGameMode() == GameMode.CREATIVE)) {
					return;
				}
				try {
					switch (event.getPacketID()) {
					case 0x67:
						StructureModifier<ItemStack> sm = packet.getItemModifier();
						for (int i = 0; i < sm.size(); i++) {
							sm.write(i, myPlugin.process(event.getPlayer().getWorld().getName(), sm.read(i)));
						}
						break;

					case 0x68:
						StructureModifier<ItemStack[]> smArray = packet.getItemArrayModifier();
						for (int i = 0; i < smArray.size(); i++) {
							smArray.write(i, myPlugin.process(event.getPlayer().getWorld().getName(), smArray.read(i)));
						}
						break;
				
					}
				} catch (FieldAccessException e) {
					logger.log(Level.WARNING, "Couldn't access field.", e);
				}
			}
		});
	}
	
	public void unregister() {
		protocolManager.removePacketListener(packetAdapter);
	}

}
