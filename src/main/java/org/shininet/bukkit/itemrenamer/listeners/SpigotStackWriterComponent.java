package org.shininet.bukkit.itemrenamer.listeners;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.component.AbstractComponent;

import static com.comphenix.protocol.PacketType.Play.Server.*;

import com.comphenix.net.sf.cglib.proxy.Factory;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.events.PacketOutputAdapter;
import com.comphenix.protocol.events.PacketOutputHandler;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.utility.StreamSerializer;
import com.google.common.eventbus.EventBus;

/**
 * Represents a stack writer that is required in Spigot #1521 and above.
 * @author Kristian
 */
public class SpigotStackWriterComponent extends AbstractComponent {	
	private StreamSerializer serializer = SpigotSafeSerializer.getDefault();
	
	private ProtocolManager protocolManager;
	private PacketListener listener;
	
	// Possibly change to a builder
	public SpigotStackWriterComponent(ProtocolManager protocolManager) {
		this.protocolManager = protocolManager;
	}
	
	/**
	 * Determine if this workaround is required.
	 * @return TRUE if we must use this workaround, FALSE otherwise.
	 */
	public static boolean isRequired() {
		return SpigotSafeSerializer.isRequired();
	}
	
	@Override
	protected void onRegistered(Plugin plugin, EventBus bus) {
		protocolManager.addPacketListener(
				listener = new PacketAdapter(plugin, ListenerPriority.MONITOR, 
					SET_SLOT, WINDOW_ITEMS, ENTITY_EQUIPMENT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				// Skip temporary players
				if (event.getPlayer() instanceof Factory)
					return;

				// Register the workaround if we have to
				if (!hasSpigotWorkaround(event.getNetworkMarker().getOutputHandlers())) {
					event.getNetworkMarker().addOutputHandler(
							new SafeItemOutputAdapter(getPlugin(), ListenerPriority.NORMAL));
				}
			}
		});
		
		// Notify the user
		System.out.println("[ItemRenamer] Enabling Spigot packet scrubbing workaround.");
	}
	
	/**
	 * Determine if there is already a Spigot safe workaround handler registered.
	 * @param handlers - the handlers.
	 * @return TRUE if there are, FALSE otherwise.
	 */
	private boolean hasSpigotWorkaround(Collection<PacketOutputHandler> handlers) {
		String name = SpigotWorkaroundAdapter.class.getSimpleName();
		
		for (PacketOutputHandler handler : handlers) {
			for (Class<?> interfaze : handler.getClass().getInterfaces()) {
				if (name.equals(interfaze.getSimpleName())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Calculate the byte length of a variable integer that contains the given value.
	 * @param value - the value.
	 * @return The length in bytes.
	 */
	private int getVarIntLength(int value) {
		int length = 1; // Minimum length
		
		while ((value & 0xFFFFFF80) != 0) {
			length++;
			value >>>= 7;
		}
		return length;
	}
	
	private byte[] writeItemPacket(PacketContainer packet, int headerLength, byte[] prefix) throws FieldAccessException, IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		output.write(prefix, 0, headerLength);
		serializer.serializeItemStack(new DataOutputStream(output), packet.getItemModifier().read(0));
		return output.toByteArray();
	}
	
	private byte[] writeItemArrayPacket(PacketContainer packet, int headerLength, byte[] prefix) throws FieldAccessException, IOException {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		DataOutputStream dataOutput = new DataOutputStream(byteOutput);
		
		dataOutput.write(prefix, 0, headerLength);
		
		// Safely write each item stack
		for (ItemStack stack : packet.getItemArrayModifier().read(0)) {
			serializer.serializeItemStack(dataOutput, stack);
		}
		return byteOutput.toByteArray();
	}
	
	@Override
	protected void onUnregistered(Plugin plugin) {
		protocolManager.removePacketListener(listener);
	}
	
	/**
	 * Represents a packet output adapter that implements the Spigot scrubber workaround.
	 * @author Kristian
	 */
	private final class SafeItemOutputAdapter extends PacketOutputAdapter implements SpigotWorkaroundAdapter {
		private SafeItemOutputAdapter(Plugin plugin, ListenerPriority priority) {
			super(plugin, priority);
		}
		@Override
		public byte[] handle(PacketEvent event, byte[] buffer) {
			try {
				// Modify the byte array here
				PacketType type = event.getPacketType();
				int idSize = event.getNetworkMarker().requireOutputHeader() ? 
					getVarIntLength(type.getCurrentId()) : 0;
				
				if (type == SET_SLOT) {
					return writeItemPacket(event.getPacket(), 3 + idSize, buffer);
				} else if (type == ENTITY_EQUIPMENT) {
					return writeItemPacket(event.getPacket(), 6 + idSize, buffer);
				} else if (type == WINDOW_ITEMS) {
					return writeItemArrayPacket(event.getPacket(), 3 + idSize, buffer);	
				}
				return buffer;
			} catch (IOException e) {
				throw new RuntimeException("Unable to apply workaround.", e);
			}
		}
	}
	
	/**
	 * Marker interface used to indicate that an output adapter is a Spigot workaround adapter.
	 * <p >
	 * This is used to avoid adding multiple workaround adapters by multiple plugins.
	 * @author Kristian
	 */
	private interface SpigotWorkaroundAdapter {
		
	}
}
