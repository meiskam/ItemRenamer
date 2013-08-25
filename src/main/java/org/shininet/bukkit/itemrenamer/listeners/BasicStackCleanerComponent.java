package org.shininet.bukkit.itemrenamer.listeners;

import javax.annotation.Nonnull;

import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.RenameProcessor;
import org.shininet.bukkit.itemrenamer.component.AbstractComponent;

import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.google.common.base.Preconditions;

/**
 * Represents an ItemStack unprocessor 
 * @author Kristian
 */
class BasicStackCleanerComponent extends AbstractComponent {
	private final RenameProcessor processor;
	private final ProtocolManager protocolManager;
	
	private PacketListener listener;
	
	public BasicStackCleanerComponent(@Nonnull RenameProcessor processor, @Nonnull ProtocolManager protocolManager) {
		this.processor = Preconditions.checkNotNull(processor, "processor cannot be NULL");
		this.protocolManager = Preconditions.checkNotNull(protocolManager, "protocolManager cannot be NULL");
	}
	
	@Override
	protected void onRegistered(@Nonnull Plugin plugin) {
		protocolManager.addPacketListener(listener = 
				new PacketAdapter(plugin, ConnectionSide.CLIENT_SIDE, ListenerPriority.HIGH, Packets.Client.SET_CREATIVE_SLOT) {			
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
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		protocolManager.removePacketListener(listener);
		listener = null;
	}
}
