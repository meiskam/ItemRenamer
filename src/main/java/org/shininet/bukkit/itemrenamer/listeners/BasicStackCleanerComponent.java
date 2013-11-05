package org.shininet.bukkit.itemrenamer.listeners;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.AbstractRenameProcessor;
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

/**
 * Represents an ItemStack unprocessor 
 * @author Kristian
 */
class BasicStackCleanerComponent extends AbstractComponent {
	protected final AbstractRenameProcessor processor;
	protected final ProtocolManager protocolManager;
	
	protected StreamSerializer serializer = new StreamSerializer();
	protected PacketListener listener;
	
	public BasicStackCleanerComponent(@Nonnull AbstractRenameProcessor processor, @Nonnull ProtocolManager protocolManager) {
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
							
							// Handle signing books
							try {
								packet.getByteArrays().write(0, unprocessBook(event, data));
							} catch (Exception e) {
								throw new RuntimeException("Unable to process the incoming book change.", e);
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
	 * Unprocess a book transmitted as an ItemStack.
	 * @param data - the data.
	 * @return The unprocessed book.
	 * @throws IOException If anything went wrong.
	 */
	protected byte[] unprocessBook(PacketEvent event, byte[] data) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ItemStack original = event.getPlayer().getItemInHand().clone();
		ItemStack altered = serializer.deserializeItemStack(
			new DataInputStream(new ByteArrayInputStream(data))
		);
		
		if (original == null)
			throw new IllegalStateException("Empty hand. Cannot deduce book changes.");
		
		// Use the original item to process the new
		BookMeta originalMeta = (BookMeta) original.getItemMeta();
		BookMeta alteredMeta = (BookMeta) altered.getItemMeta();
		
		originalMeta.setPages(alteredMeta.getPages());
		originalMeta.setTitle(alteredMeta.getTitle());
		originalMeta.setAuthor(alteredMeta.getAuthor());
		original.setItemMeta(originalMeta);
		original.setType(altered.getType());

		serializer.serializeItemStack(new DataOutputStream(output), original);
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
