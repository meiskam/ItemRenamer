package org.shininet.bukkit.itemrenamer.listeners;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.meta.CompoundStore;

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
	 * Retrieve a SpigotSafeSerializer if it is required, otherwise use the default serializer.
	 * @return The appropriate default serializer.
	 */
	public static StreamSerializer getDefault() {
		return isRequired() ? DEFAULT : StreamSerializer.getDefault();
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
			
			StreamSerializer serializer = new StreamSerializer();
			ItemStack roundTrip = serializer.deserializeItemStack(serializer.serializeItemStack(stored));
				
			// Did it survive unscathed?
			result = !Objects.equal(proof, 
				CompoundStore.getNativeStore(roundTrip, "com.comphenix.test").loadCompound());
			IS_REQUIRED = result;
			return result;
			
		} catch (IOException e) {
			throw new RuntimeException("Unexpected error during round trip test.", e);
		}
	}
	
	@Override
	public void serializeItemStack(DataOutputStream output, ItemStack stack) throws IOException {
		NbtCompound tag = stack != null && stack.getType() != Material.AIR ? 
				NbtFactory.asCompound(NbtFactory.fromItemTag(stack)) : null;

		// Note that we can't write the stack data directly, as 1.8 switched to sending a 
		// item name instead of an item ID. 
		if (tag != null) {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream dataStream = new DataOutputStream(byteStream);
			NbtFactory.setItemTag(stack, null);
			
			// This will write the stack as normal, without its tag compound and the tag field length
			super.serializeItemStack(dataStream, stack);
			output.write(byteStream.toByteArray(), 0, byteStream.size() - 2);
			serializeCompound(output, tag);
			
			// Finally, revert the tag
			NbtFactory.setItemTag(stack, tag);
			
		} else {
			// Write the stack as normal
			super.serializeItemStack(output, stack);
		}
	}
}
