package org.shininet.bukkit.itemrenamer.listeners;

import java.io.DataInputStream;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.utility.StreamSerializer;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.google.common.base.Preconditions;

public class BukkitNbtScrubberFix {
	/**
	 * Stores the version after which the NBT fix is mandatory.
	 */
	private static final MinecraftVersion REQUIRED_VERSION = new MinecraftVersion(1, 6, 1);
	
	private final Plugin plugin;
	private final ProtocolManager manager;
	
	// The registered listener
	private PacketListener listener;
	
	public BukkitNbtScrubberFix(@Nonnull Plugin plugin, @Nonnull ProtocolManager manager) {
		this.plugin = Preconditions.checkNotNull(plugin, "plugin cannot be NULL.");
		this.manager = Preconditions.checkNotNull(manager, "manager cannot be NULL.");
	}

	/**
	 * Determine if this fix is required.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public boolean isRequired() {
		MinecraftVersion current = new MinecraftVersion(Bukkit.getServer());
		
		// Only enable this fix for Minecraft 1.6.1 and later
		return REQUIRED_VERSION.compareTo(current) <= 0;
	}
	
	/**
	 * Determine if this fix has already been enabled.
	 * @return TRUE if it has, FALSE otherwise.
	 */
	public boolean isEnabled() {
		return listener != null;
	}
	
	/**
	 * Attempt to enable the fix.
	 * @throws IllegalStateException If it has already been enabled.
	 */
	public void enable() {
		if (isEnabled())
			throw new IllegalStateException("Fix has already been enabled.");
		
		manager.addPacketListener(
		  listener = new PacketAdapter(PacketAdapter.params(plugin, Packets.Client.SET_CREATIVE_SLOT).clientSide().optionIntercept()) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				DataInputStream input = event.getNetworkMarker().getInputStream();
				
				// Skip simulated packets
				if (input == null)
					return;

				try {
					// Read slot
					input.readShort();
					ItemStack stack = readItemStack(input, new StreamSerializer());
					
					// Undo the stupid "item meta" cleanup
					event.getPacket().getItemModifier().write(0, stack);
					
				} catch (IOException e) {
					// Just let ProtocolLib handle it
					throw new RuntimeException("Cannot undo NBT scrubber.", e);
				}
			}
		});
	}
	
	/**
	 * Read an ItemStack from a input stream without "scrubbing" the NBT content.
	 * @param input - the input stream.
	 * @param serializer - methods for serializing Minecraft object.
	 * @return The deserialized item stack.
	 * @throws IOException If anything went wrong.
	 */
	private ItemStack readItemStack(DataInputStream input, StreamSerializer serializer) throws IOException {
		ItemStack result = null;
		short type = input.readShort();

		if (type >= 0) {
			byte amount = input.readByte();
			short damage = input.readShort();

			result = new ItemStack(type, amount, damage);
			NbtCompound tag = serializer.deserializeCompound(input);

			if (tag != null) {
				result = MinecraftReflection.getBukkitItemStack(result);
				NbtFactory.setItemTag(result, tag);
			}
		}
		return result;
	}
	
	/**
	 * Disable the current fix.
	 * @return TRUE if it was disabled, FALSE if it has already been disabled.
	 */
	public boolean disable() {
		if (isEnabled()) {
			manager.removePacketListener(listener);
			
			listener = null;
			return true;
		}
		return false;
	}
}
