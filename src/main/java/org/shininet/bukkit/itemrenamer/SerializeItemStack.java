package org.shininet.bukkit.itemrenamer;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.utils.StackUtils;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

/**
 * Represents a serialization method for storing ItemStacks in compounds.
 * 
 * @author Kristian
 *
 */
class SerializeItemStack {
	/**
	 * Represents the different fields that we may save.
	 * @author Kristian
	 */
	public enum StackField {
		ID("id"),
		COUNT("count"),
		DAMAGE("damage"),
		TAG("tag");
		
		/**
		 * The NBT key for this particular field.
		 */
		private final String nbt;
		
		private StackField(String nbt) {
			this.nbt = nbt;
		}
	}

	// Which fields will be saved
	private Set<StackField> fields = EnumSet.allOf(StackField.class);
	
	/**
	 * Save the given item stack as a NBT compound.
	 * <p>
	 * Only certain fields will be saved.
	 * @param stack - the item stack.
	 * @return The NBT compound.
	 */
	public NbtCompound save(ItemStack stack) {
		NbtCompound result = NbtFactory.ofCompound("");
		NbtCompound data = getData(stack);
	
		if (fields.contains(StackField.ID))
			result.put("id", (short) stack.getTypeId());
		if (fields.contains(StackField.COUNT))
			result.put("count", (byte) stack.getAmount());
		if (fields.contains(StackField.DAMAGE))
			result.put("damage", (short) stack.getDurability());
		
		if (fields.contains(StackField.TAG) && data != null) {
			result.put("tag", data);
		}
		return result;
	}
	
	private NbtCompound getData(ItemStack stack) {
		if (stack.getType() != Material.AIR) {
			return (NbtCompound) NbtFactory.fromItemTag(StackUtils.getCraftItemStack(stack));
		}
		return null;
	}
	
	/**
	 * Load the given saved item stack.
	 * @param savedStack - the saved item stack.
	 * @return The item stack.
	 */
	public ItemStack load(NbtCompound savedStack) {
		ItemStack destination = MinecraftReflection.getBukkitItemStack(new ItemStack(Material.STONE));
		loadInto(destination, savedStack, true);
		return destination;
	}
	
	/**
	 * Load the given saved item stack into another stack.
	 * <p>
	 * Only certain fields will be loaded.
	 * @param stack - the destination CraftItemStack.
	 * @param savedStack - the saved item stack.
	 */
	public void loadInto(ItemStack stack, NbtCompound savedStack) {
		loadInto(stack, savedStack, false);
	}
	
	/**
	 * Load the given saved item stack into another stack.
	 * <p>
	 * Only certain fields will be loaded.
	 * @param stack - the destination CraftItemStack.
	 * @param savedStack - the saved item stack.
	 * @param ignoreMissing - whether or not to ignore missing keys.
	 */
	public void loadInto(ItemStack stack, NbtCompound savedStack, boolean ignoreMissing) {
		if (!MinecraftReflection.isCraftItemStack(stack))
			throw new IllegalArgumentException("Stack must be a CraftItemStack, but was " + stack);

		if (loadKey(StackField.ID, savedStack, ignoreMissing))
			stack.setTypeId(savedStack.getShort("id"));
		if (loadKey(StackField.COUNT, savedStack, ignoreMissing))
			stack.setAmount(savedStack.getByte("count"));
		if (loadKey(StackField.DAMAGE, savedStack, ignoreMissing))
			stack.setDurability(savedStack.getShort("damage"));
		
		if (fields.contains(StackField.TAG) && savedStack.containsKey("tag")) {
			NbtFactory.setItemTag(stack, savedStack.getCompound("tag"));
		} else {
			NbtFactory.setItemTag(stack, null);
		}
	}

	private boolean loadKey(StackField field, NbtCompound savedStack, boolean ignoreMissing) {
		if (fields.contains(field)) {
			if (ignoreMissing)
				return savedStack.containsKey(field.nbt);
			return true;
		}
		return false;
	}
	
	/**
	 * Determine if the serializer is saving or loading a given field.
	 * @param field - the field to save or load.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public boolean hasField(StackField field) {
		return fields.contains(field);
	}
	
	/**
	 * Add a given field for saving or loading.
	 * @param field - the field to add.
	 * @return TRUE if the field was added, FALSE otherwise.
	 */
	public boolean addField(StackField field) {
		return fields.add(field);
	}
	
	/**
	 * Remove a given field from being saved or loaded.
	 * @param field - the field.
	 * @return TRUE if the field was removed, FALSE otherwise.
	 */
	public boolean removeField(StackField field) {
		return fields.remove(field);
	}
}
