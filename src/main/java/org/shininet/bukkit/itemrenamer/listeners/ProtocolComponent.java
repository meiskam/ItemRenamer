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

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.AbstractRenameProcessor;
import org.shininet.bukkit.itemrenamer.component.AbstractComponent;
import org.shininet.bukkit.itemrenamer.component.Component;
import org.shininet.bukkit.itemrenamer.merchant.MerchantRecipe;
import org.shininet.bukkit.itemrenamer.merchant.MerchantRecipeList;
import org.shininet.bukkit.itemrenamer.meta.CharCodeStore;
import org.shininet.bukkit.itemrenamer.meta.CompoundStore;

import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.reflect.StructureModifier;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * Represents a component that handles all the ProtocolLib interfacing.
 * @author Kristian
 */
public class ProtocolComponent extends AbstractComponent {
	/**
	 * Represents an event bus object for determining the entity of an entity ID.
	 * <p>
	 * This is used by DisguiseComponent.
	 * @author Kristian
	 */
	public class LookupEntity {
		private final int entityId;
		private Entity entity;
		
		private LookupEntity(World world, int entityId) {
			this.entityId = entityId;
			this.entity = protocolManager.getEntityFromID(world, entityId);
		}

		public Entity getEntity() {
			return entity;
		}
		
		public void setEntity(Entity entity) {
			this.entity = entity;
		}
		
		public int getEntityId() {
			return entityId;
		}
	}
	
	private final AbstractRenameProcessor processor;
	private final ProtocolManager protocolManager;

	private static final int MERCHANT_CRAFT_1 = 0;
	private static final int MERCHANT_CRAFT_2 = 1;
	private static final int MERCHANT_SELL_1 = 2;
	
	private final Logger logger;
	
	// Current listeners
	private List<PacketListener> listeners = Lists.newArrayList();

	// Scrubbing creative override
	private Component stackCleaner;
	
	// Spigot workaround
	private Component spigotWorkaround;
	
	// Possibly change to a builder
	public ProtocolComponent(AbstractRenameProcessor processor, ProtocolManager protocolManager, Logger logger) {
		this.processor = Preconditions.checkNotNull(processor, "processor cannot be NULL.");
		this.protocolManager = Preconditions.checkNotNull(protocolManager, "protocolManager cannot be NULL");
		this.logger = Preconditions.checkNotNull(logger, "logger cannot be NULL");
	}
	
	@Override
	protected void onRegistered(@Nonnull Plugin plugin, EventBus bus) {
		listeners.add(registerCommonListeners(plugin));
		
		// Prevent creative from overwriting the item stacks
		if (PacketType.Legacy.Server.SET_CREATIVE_SLOT.isSupported()) {
			listeners.add(registerCreative(plugin));
		}

		// Remove data stored in the display name of items
		listeners.add(registerClearCharStore(plugin));
		stackCleaner = registerStackCleaner(plugin);
		
		if (SpigotStackWriterComponent.isRequired()) {
			spigotWorkaround = new SpigotStackWriterComponent(protocolManager);
			spigotWorkaround.register(plugin, bus);
		}
	}
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		for (PacketListener listener : listeners) {
			protocolManager.removePacketListener(listener);
		}
		listeners.clear();
		stackCleaner.unregister(plugin);
		spigotWorkaround.unregister(plugin);
	}

	private PacketListener registerCommonListeners(Plugin plugin) {
		return addListener(
				new PacketAdapter(plugin, ListenerPriority.HIGH, 
					PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.WINDOW_ITEMS, 
					PacketType.Play.Server.CUSTOM_PAYLOAD, PacketType.Play.Server.ENTITY_EQUIPMENT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				
				// Skip temporary players
				if (event.getPlayer() instanceof Factory)
					return;
				Player player = event.getPlayer();
				PacketType type = event.getPacketType();
				
				if (type == PacketType.Play.Server.SET_SLOT) {
					handleSlot(packet, player);
				} else if (type == PacketType.Play.Server.WINDOW_ITEMS) {
					handleWindowItems(packet, player);
				} else if (type == PacketType.Play.Server.CUSTOM_PAYLOAD) {
					handleCustomPayload(packet, player);
				} else if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
					handleEntityEquipment(event, packet, player);
				}	
			}
		});
	}

	private void handleEntityEquipment(PacketEvent event, PacketContainer packet, Player player) {
		ItemStack stack = packet.getItemModifier().read(0);
		LookupEntity lookup = new LookupEntity(event.getPlayer().getWorld(), 
				packet.getIntegers().read(0));
		int equipmentSlot = packet.getIntegers().read(1);
		
		// Use EventBus, as we don't want plugins to interfer with this
		bus.post(lookup);
		
		// The custom inventory view we will pass on in the API
		Inventory inventory = null;
		
		if (lookup.getEntity() instanceof LivingEntity) {
			LivingEntity targetLiving = (LivingEntity) lookup.getEntity();
			inventory = new EquipmentAdapter(targetLiving.getEquipment(), targetLiving, player);
		} else {
			throw new IllegalArgumentException("Unexpected entity sent equipment: " + lookup.getEntity());
		}
		
		// Now we're ready to process the item stack
		processor.process(player, 
			new EquipmentInventoryView(inventory, player.getInventory(), player), 
			stack, equipmentSlot);
	}

	private void handleCustomPayload(PacketContainer packet, Player player) {
		String packetName = packet.getStrings().read(0);
		
		// Make sure this is a merchant list
		if (packetName.equals("MC|TrList")) {	
			try {
				byte[] result = processMerchantList(player, packet.getByteArrays().read(0));
				packet.getByteArrays().write(0, result);
				
				// Not needed in 1.7.2
				if (packet.getIntegers().size() > 0) {
					packet.getIntegers().write(0, result.length);
				}
			} catch (IOException e) {
				logger.log(Level.WARNING, "Cannot read merchant list!", e);
			}
		}
	}

	private void handleWindowItems(PacketContainer packet, Player player) {
		StructureModifier<ItemStack[]> smArray = packet.getItemArrayModifier();
		
		for (int i = 0; i < smArray.size(); i++) {
			processor.process(player, smArray.read(i));
		}
	}

	private void handleSlot(PacketContainer packet, Player player) {
		StructureModifier<ItemStack> sm = packet.getItemModifier();
		int slot = packet.getIntegers().read(1);
		
		for (int i = 0; i < sm.size(); i++) {
			processor.process(player, sm.read(i), slot);
		}
	}
	
	private PacketListener registerCreative(Plugin plugin) {
		return addListener(
				new PacketAdapter(plugin, ListenerPriority.HIGH, PacketType.Legacy.Server.SET_CREATIVE_SLOT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if (event.getPacketType() == PacketType.Legacy.Server.SET_CREATIVE_SLOT) {
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
				plugin, ListenerPriority.LOW, 
				PacketType.Play.Client.WINDOW_CLICK, PacketType.Play.Client.CUSTOM_PAYLOAD) {
			
			@Override
			public void onPacketReceiving(PacketEvent event) {
				PacketType type = event.getPacketType();
				
				if (type == PacketType.Play.Client.WINDOW_CLICK) {
					// Do the opposite
					processor.unprocess(event.getPacket().getItemModifier().read(0));
					
				} else if (type == PacketType.Play.Client.CUSTOM_PAYLOAD) {
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
				}
			}
		});
	}
	
	private Component registerStackCleaner(Plugin plugin) {
		// Use the intercept buffer scrubber if necessary
		Component component = AdvancedStackCleanerComponent.isRequired() ? 
			new AdvancedStackCleanerComponent(processor, protocolManager) : 
			new BasicStackCleanerComponent(processor, protocolManager);
		
		component.register(plugin, bus);
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
