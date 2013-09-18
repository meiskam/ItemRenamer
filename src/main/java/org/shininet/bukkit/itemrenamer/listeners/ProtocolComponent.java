/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer.listeners;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.RenameProcessor;
import org.shininet.bukkit.itemrenamer.component.AbstractComponent;
import org.shininet.bukkit.itemrenamer.component.Component;
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
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.StructureModifier;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Represents a component that handles all the ProtocolLib interfacing.
 * @author Kristian
 */
public class ProtocolComponent extends AbstractComponent {
	private final RenameProcessor processor;
	private final ProtocolManager protocolManager;

	private static final int MERCHANT_CRAFT_1 = 0;
	private static final int MERCHANT_CRAFT_2 = 1;
	private static final int MERCHANT_SELL_1 = 2;
	
	private final Logger logger;
	
	// Current listeners
	private List<PacketListener> listeners = Lists.newArrayList();

	// Scrubbing creative override
	private Component stackCleaner;
	
	// Possibly change to a builder
	public ProtocolComponent(RenameProcessor processor, ProtocolManager protocolManager, Logger logger) {
		this.processor = Preconditions.checkNotNull(processor, "processor cannot be NULL.");
		this.protocolManager = Preconditions.checkNotNull(protocolManager, "protocolManager cannot be NULL");
		this.logger = Preconditions.checkNotNull(logger, "logger cannot be NULL");
	}
	
	@Override
	protected void onRegistered(@Nonnull Plugin plugin) {
		listeners.add(registerCommonListeners(plugin));
		
		// Prevent creative from overwriting the item stacks
		listeners.add(registerCreative(plugin));

		// Remove data stored in the display name of items
		listeners.add(registerClearCharStore(plugin));
		stackCleaner = registerStackCleaner(plugin);
	}
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		for (PacketListener listener : listeners) {
			protocolManager.removePacketListener(listener);
		}
		listeners.clear();
		stackCleaner.unregister(plugin);
	}

	private PacketListener registerCommonListeners(Plugin plugin) {
		return addListener(
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
						int slot = packet.getIntegers().read(1);
						
						for (int i = 0; i < sm.size(); i++) {
							processor.process(player, sm.read(i), slot);
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
	}
	
	private PacketListener registerCreative(Plugin plugin) {
		return addListener(
				new PacketAdapter(plugin, ConnectionSide.SERVER_SIDE, ListenerPriority.HIGH, Packets.Client.SET_CREATIVE_SLOT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if (event.getPacketID() == Packets.Client.SET_CREATIVE_SLOT) {
					PacketContainer packet = event.getPacket();
					ItemStack stack = packet.getItemModifier().read(0);
					int slot = packet.getIntegers().read(0);
					
					// Process the slot data
					processor.process(event.getPlayer(), stack, slot);
				}
			}
		});
	}
	
	private PacketListener registerClearCharStore(Plugin plugin) {
		return addListener(new PacketAdapter(
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
							CharCodeStore store = new CharCodeStore(CompoundStore.PLUGIN_ID, CharCodeStore.RawPayloadStore.INSTANCE);
							store.parse(new String(data));

							// Remove any stored information by our plugin
							if (store.hasData()) {
								store.removeData(store.getPluginId());
								packet.getByteArrays().write(0, store.toString().getBytes());
							}
						}
						
					default :
						break;
				}
			}
		});
	}
	
	private Component registerStackCleaner(Plugin plugin) {
		// Use the intercept buffer scrubber if necessary
		Component component = AdvancedStackCleanerComponent.isRequired() ? 
			new AdvancedStackCleanerComponent(processor, protocolManager) : 
			new BasicStackCleanerComponent(processor, protocolManager);
		
		component.register(plugin);
		return component;
	}
	
	/**
	 * Add a particular packet listener to the manager.
	 * @param listener - new listener to add.
	 * @return The new listener.
	 */
	private PacketListener addListener(PacketListener listener) {
		protocolManager.addPacketListener(listener);
		return listener;
	}
	
	private byte[] processMerchantList(Player player, byte[] data) throws IOException {
		ByteArrayInputStream source = new ByteArrayInputStream(data);
		DataInputStream input = new DataInputStream(source);
		
		int containerCounter = input.readInt();
		MerchantRecipeList list = MerchantRecipeList.readRecipiesFromStream(input);
		
		// Process each and every item stack
		for (MerchantRecipe recipe : list) {
			recipe.setItemToBuy(processor.process(player, recipe.getItemToBuy(), MERCHANT_CRAFT_1) );
			recipe.setSecondItemToBuy(processor.process(player, recipe.getSecondItemToBuy(), MERCHANT_CRAFT_2) );
			recipe.setItemToSell(processor.process(player, recipe.getItemToSell(), MERCHANT_SELL_1) );
		}
		
		// Write the result back
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(buffer);
		
		output.writeInt(containerCounter);
		list.writeRecipiesToStream(output);
		return buffer.toByteArray();
	}
}
