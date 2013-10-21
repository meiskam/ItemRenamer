package org.shininet.bukkit.itemrenamer.listeners;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.RenameProcessor;
import org.shininet.bukkit.itemrenamer.component.AbstractComponent;

import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketAdapter.AdapterParameteters;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.utility.StreamSerializer;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;

/**
 * Represents an ItemStack unprocessor 
 * @author Kristian
 */
class BasicStackCleanerComponent extends AbstractComponent {
	protected final RenameProcessor processor;
	protected final ProtocolManager protocolManager;
	
	protected StreamSerializer serializer = new StreamSerializer();
	protected PacketListener listener;
	
	public BasicStackCleanerComponent(@Nonnull RenameProcessor processor, @Nonnull ProtocolManager protocolManager) {
		this.processor = Preconditions.checkNotNull(processor, "processor cannot be NULL");
		this.protocolManager = Preconditions.checkNotNull(protocolManager, "protocolManager cannot be NULL");
	}
	
	@Override
	protected void onRegistered(@Nonnull Plugin plugin) {
		protocolManager.addPacketListener(listener = new PacketAdapter(adapterBuilder(plugin)) {			
			@Override
			public void onPacketReceiving(PacketEvent event) {
				final PacketContainer packet = event.getPacket();
				
				switch (event.getPacketID()) {
					case Packets.Client.PLACE:
					case Packets.Client.SET_CREATIVE_SLOT:
						// Do the opposite
						unprocessFieldStack(event, packet.getItemModifier().read(0));
						break;

					case Packets.Client.CUSTOM_PAYLOAD:
						String channel = event.getPacket().getStrings().read(0);
						
						if ("MC|BEdit".equals(channel) || "MC|BSign".equals(channel)) {
							byte[] data = packet.getByteArrays().read(0);
							
							// Attempt to unprocess this item stack
							try {
								packet.getByteArrays().write(0, unprocessByteStack(data));
							} catch (Exception e) {
								throw new RuntimeException("Unable to handle byte array: " + Bytes.asList(data));
							}
						}
						break;
					default :
						throw new IllegalArgumentException("Unrecognized packet: " + event.getPacketID());
				}
				// Thread safe too!
				if (event.getPacketID() == Packets.Client.SET_CREATIVE_SLOT) {

				}
			}
		});
	}
	
	/**
	 * Unprocess an item stack in a packet field.
	 * @param stack - the item stack to process.
	 */
	protected void unprocessFieldStack(PacketEvent event, ItemStack stack) {
		processor.unprocess(stack);
	}
	
	/**
	 * Unprocess an item stack transmitted as a byte array in the custom data payload packet.
	 * @param data - the data.
	 * @return The unprocessed item stack.
	 * @throws IOException If anything went wrong.
	 */
	protected byte[] unprocessByteStack(byte[] data) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ItemStack stack = serializer.deserializeItemStack(
			new DataInputStream(new ByteArrayInputStream(data))
		);
		
		processor.unprocess(stack);
		serializer.serializeItemStack(new DataOutputStream(output), stack);
		return output.toByteArray();
	}
	
	/**
	 * Initialize the parameters used to construct the packet listener.
	 * @param plugin - the current plugin.
	 * @return The parameters for the packet listener.
	 */
	protected AdapterParameteters adapterBuilder(Plugin plugin) {
		return PacketAdapter.params(plugin, 
			Packets.Client.PLACE, Packets.Client.SET_CREATIVE_SLOT, Packets.Client.CUSTOM_PAYLOAD).clientSide();
	}
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		protocolManager.removePacketListener(listener);
		listener = null;
	}
}
