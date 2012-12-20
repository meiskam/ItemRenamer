/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer.listeners;

import java.util.logging.Level;
import java.util.logging.Logger;

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
				
				try {
					switch (event.getPacketID()) {
					case 0x67:
						StructureModifier<ItemStack> sm = packet.getSpecificModifier(ItemStack.class);
						sm.write(0, myPlugin.process(sm.read(0).clone()));
						break;

					case 0x68:
						StructureModifier<ItemStack[]> smArray = packet.getSpecificModifier(ItemStack[].class);
						smArray.write(0, myPlugin.process(smArray.read(0).clone()));
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
