package org.shininet.bukkit.itemrenamer.listeners;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.meta.CompoundStore;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.StreamSerializer;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.google.common.base.Objects;

/**
 * Represents a serializer that works around the Spigot patch for removing custom NBT data.
 * @author Kristian
 */
public class SpigotSafeSerializer extends StreamSerializer {
	private static final SpigotSafeSerializer DEFAULT = new SpigotSafeSerializer();
	
	// Cache the outcome of the following test
	private static volatile Boolean IS_REQUIRED;
	
	/**
	 * Retrieve the default Spigot safe serializer.
	 * @return The appropriate default serializer.
	 */
	public static SpigotSafeSerializer getDefault() {
		return DEFAULT;
	}
	
	/**
	 * Determine if this workaround is required.
	 * @return TRUE if we must use this workaround, FALSE otherwise.
	 */
	public static boolean isRequired() {
		Boolean result = IS_REQUIRED;
		
		if (result != null) {
			return result;
		}
		
		try {
			// Used to check if custom NBT data survives the round trip
			NbtCompound proof = NbtFactory.ofCompound("", 
					Collections.singletonList(NbtFactory.of("value", "TEST")));
			
			ItemStack source = new ItemStack(Material.IRON_AXE);
			ItemStack stored = CompoundStore.getNativeStore(source, "com.comphenix.test").saveCompound(proof);
			
			StreamSerializer rawSerializer = new StreamSerializer();
			SpigotSafeSerializer safeSerializer = new SpigotSafeSerializer();
			
			// We are testing the raw serializer here
			ItemStack roundTrip = safeSerializer.deserializeItemStack(rawSerializer.serializeItemStack(stored));
			NbtCompound extracted = CompoundStore.getNativeStore(roundTrip, "com.comphenix.test").loadCompound();
			
			// Did it survive unscathed?
			result = !Objects.equal(proof, extracted);
			IS_REQUIRED = result;
			return result;
			
		} catch (IOException e) {
			throw new RuntimeException("Unexpected error during round trip test.", e);
		}
	}
	
	/**
	 * Read an ItemStack from a input stream without "scrubbing" the NBT content.
	 * @param input - the input stream.
	 * @return The deserialized item stack.
	 * @throws IOException If anything went wrong.
	 */
	@Override
	public ItemStack deserializeItemStack(DataInputStream input) throws IOException {
		ItemStack result = null;
		short type = input.readShort();

		if (type >= 0) {
			byte amount = input.readByte();
			short damage = input.readShort();

			result = new ItemStack(type, amount, damage);
			NbtCompound tag = super.deserializeCompound(input);

			if (tag != null) {
				result = MinecraftReflection.getBukkitItemStack(result);
				NbtFactory.setItemTag(result, tag);
			}
		}
		return result;
	}
	
	@Override
	public void serializeItemStack(DataOutputStream output, ItemStack stack) throws IOException {
		// Speed things up if the workaround is not required
		if (!isRequired()) {
			super.serializeItemStack(output, stack);
			return;
		}
		NbtCompound tag = stack != null && stack.getType() != Material.AIR ? 
				NbtFactory.asCompound(NbtFactory.fromItemTag(stack)) : null;

		// Note that we can't write the stack data directly, as 1.8 switched to sending a 
		// item name instead of an item ID. 
		if (tag != null) {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream dataStream = new DataOutputStream(byteStream);
			ItemStack withoutTag = new ItemStack(stack.getType(), stack.getAmount(), stack.getDurability());
			
			// This will write the stack as normal, without its tag compound and the tag field length
			super.serializeItemStack(dataStream, withoutTag);
			output.write(byteStream.toByteArray(), 0, byteStream.size() - 2);
			serializeCompound(output, tag);

		} else {
			// Write the stack as normal
			super.serializeItemStack(output, stack);
		}
	}
}
