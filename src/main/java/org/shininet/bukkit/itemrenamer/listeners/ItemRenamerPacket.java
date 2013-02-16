/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer.listeners;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.RenameProcessor;
import org.shininet.bukkit.itemrenamer.merchant.MerchantRecipe;
import org.shininet.bukkit.itemrenamer.merchant.MerchantRecipeList;

import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.StructureModifier;

public class ItemRenamerPacket {
	private RenameProcessor processor;
	private ProtocolManager protocolManager;

	private final Logger logger;

	// Possibly change to a builder
	public ItemRenamerPacket(Plugin plugin, RenameProcessor processor, ProtocolManager protocolManager, Logger logger) {
		this.processor = processor;
		this.protocolManager = protocolManager;
		this.logger = logger;
		addListener(plugin);
	}
	
	public void addListener(final Plugin plugin) {
		protocolManager.addPacketListener(
				new PacketAdapter(plugin, ConnectionSide.SERVER_SIDE, ListenerPriority.HIGH, 0x67, 0x68, 0xFA) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				
				// Skip temporary players
				if (event.getPlayer() instanceof Factory)
					return;
				
				try {
					String world = event.getPlayer().getWorld().getName();
					
					switch (event.getPacketID()) {
					case 0x67:
						StructureModifier<ItemStack> sm = packet.getItemModifier();
						for (int i = 0; i < sm.size(); i++) {
							processor.process(world, sm.read(i));
						}
						break;

					case 0x68:
						StructureModifier<ItemStack[]> smArray = packet.getItemArrayModifier();
						for (int i = 0; i < smArray.size(); i++) {
							processor.process(world, smArray.read(i));
						}
						break;
				
					case 0xFA:
						// Make sure this is a merchant list
						if (packet.getStrings().read(0).equals("MC|TrList")) {
							
							try {
								byte[] result = processMerchantList(world, packet.getByteArrays().read(0));
								packet.getIntegers().write(0, result.length);
								packet.getByteArrays().write(0, result);
							} catch (IOException e) {
								logger.log(Level.WARNING, "Cannot read merchant list!", e);
							}
						}
						break;
					}
				} catch (FieldAccessException e) {
					logger.log(Level.WARNING, "Couldn't access field.", e);
				}
			}
		});
		
		// Prevent creative from overwriting the item stacks
		protocolManager.addPacketListener(
				new PacketAdapter(plugin, ConnectionSide.BOTH, ListenerPriority.HIGH, Packets.Client.SET_CREATIVE_SLOT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if (event.getPacketID() == Packets.Client.SET_CREATIVE_SLOT) {
					String worldName = event.getPlayer().getWorld().getName();
					
					// Process the slot data
					processor.process(worldName, event.getPacket().getItemModifier().read(0));
				}
			}
			
			@Override
			public void onPacketReceiving(PacketEvent event) {
				// Thread safe too!
				if (event.getPacketID() == Packets.Client.SET_CREATIVE_SLOT) {
					// Do the opposite
					processor.unprocess(event.getPacket().getItemModifier().read(0));
				}
			}
		});
	}
	
	private byte[] processMerchantList(String world, byte[] data) throws IOException {
		ByteArrayInputStream source = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(source);
		
		int containerCounter = input.readInt();
		MerchantRecipeList list = MerchantRecipeList.readRecipiesFromStream(input);
		
		// Process each and every item stack
		for (MerchantRecipe recipe : list) {
			recipe.setItemToBuy(processor.process(world, recipe.getItemToBuy()) );
			recipe.setSecondItemToBuy(processor.process(world, recipe.getSecondItemToBuy()) );
			recipe.setItemToSell(processor.process(world, recipe.getItemToSell()) );
		}
		
		// Write the result back
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(buffer);
		
		output.writeInt(containerCounter);
		list.writeRecipiesToStream(output);
		return buffer.toByteArray();
	}

	/**
	 * Unregisters every packet listener.
	 * @param plugin - our plugin reference.
	 */
	public void unregister(Plugin plugin) {
		protocolManager.removePacketListeners(plugin);
	}
}
