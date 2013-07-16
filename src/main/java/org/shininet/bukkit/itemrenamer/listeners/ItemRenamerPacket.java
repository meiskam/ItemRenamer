/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer.listeners;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.RenameProcessor;
import org.shininet.bukkit.itemrenamer.merchant.MerchantRecipe;
import org.shininet.bukkit.itemrenamer.merchant.MerchantRecipeList;
import org.shininet.bukkit.itemrenamer.meta.CharCodeStore;
import org.shininet.bukkit.itemrenamer.meta.CompoundStore;

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
import com.google.common.io.ByteStreams;

public class ItemRenamerPacket {
	private final RenameProcessor processor;
	private final ProtocolManager protocolManager;

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
					Player player = event.getPlayer();
					
					switch (event.getPacketID()) {
					case 0x67:
						StructureModifier<ItemStack> sm = packet.getItemModifier();
						for (int i = 0; i < sm.size(); i++) {
							processor.process(player, sm.read(i));
						}
						break;

					case 0x68:
						StructureModifier<ItemStack[]> smArray = packet.getItemArrayModifier();
						for (int i = 0; i < smArray.size(); i++) {
							processor.process(player, smArray.read(i));
						}
						break;
				
					case 0xFA:
						String packetName = packet.getStrings().read(0);
						
						// Make sure this is a merchant list
						if (packetName.equals("MC|TrList")) {	
							try {
								byte[] result = processMerchantList(player, packet.getByteArrays().read(0));
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
					// Process the slot data
					processor.process(event.getPlayer(), event.getPacket().getItemModifier().read(0));
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
		
		
		protocolManager.addPacketListener(new PacketAdapter(
				plugin, ConnectionSide.CLIENT_SIDE, ListenerPriority.LOW, 
				Packets.Client.WINDOW_CLICK, Packets.Client.CUSTOM_PAYLOAD) {
			
			@Override
			public void onPacketReceiving(PacketEvent event) {
				switch (event.getPacketID()) {
					case Packets.Client.WINDOW_CLICK :
						// Do the opposite
						processor.unprocess(event.getPacket().getItemModifier().read(0));
						break;

					case Packets.Client.CUSTOM_PAYLOAD:
						PacketContainer packet = event.getPacket();
						String name = packet.getStrings().read(0);
						
						if ("MC|ItemName".equals(name)) {
							byte[] data = packet.getByteArrays().read(0);
							
							// No need to modify NULL arrays
							if (data == null) {
								return;
							}

							// Read each segment without decompressing any data
							CharCodeStore store = new CharCodeStore(CompoundStore.PLUGIN_ID) {
								protected byte[] getPayload(int uncompressedSize, DataInputStream input) throws IOException {
									byte[] output = new byte[uncompressedSize];
									
									// Only read as much as we can
									ByteStreams.readFully(input, output, 0, Math.min(uncompressedSize, input.available()));
									return output;
								}
								
								@Override
								protected OutputStream getPayloadOutputStream(OutputStream storage) {
									return storage;
								}
							};
						
							store.parse(new String(data));

							// Remove any stored information by our plugin
							if (store.hasData()) {
								String newData = store.toString();
								packet.getByteArrays().write(0, newData.getBytes());
							}
						}
						
					default :
						break;
				}
			}
		});
	}
	
	private byte[] processMerchantList(Player player, byte[] data) throws IOException {
		ByteArrayInputStream source = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(source);
		
		int containerCounter = input.readInt();
		MerchantRecipeList list = MerchantRecipeList.readRecipiesFromStream(input);
		
		// Process each and every item stack
		for (MerchantRecipe recipe : list) {
			recipe.setItemToBuy(processor.process(player, recipe.getItemToBuy()) );
			recipe.setSecondItemToBuy(processor.process(player, recipe.getSecondItemToBuy()) );
			recipe.setItemToSell(processor.process(player, recipe.getItemToSell()) );
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
