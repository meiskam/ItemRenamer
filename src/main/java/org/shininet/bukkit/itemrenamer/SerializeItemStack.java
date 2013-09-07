package org.shininet.bukkit.itemrenamer;

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
	 * Save the given item stack as a NBT compound.
	 * @param stack - the item stack.
	 * @return The NBT compound.
	 */
	public NbtCompound save(ItemStack stack) {
		NbtCompound result = NbtFactory.ofCompound("");
		NbtCompound data = (NbtCompound) NbtFactory.fromItemTag(StackUtils.getCraftItemStack(stack));
		
		result.put("id", (short) stack.getTypeId());
		result.put("count", (byte) stack.getAmount());
		result.put("damage", (short) stack.getDurability());
		
		if (data != null) {
			result.put("tag", data);
		}
		return result;
	}
	
	/**
	 * Load the given saved item stack.
	 * @param savedStack - the saved item stack.
	 * @return The item stack.
	 */
	public ItemStack load(NbtCompound savedStack) {
		ItemStack destination = MinecraftReflection.getBukkitItemStack(new ItemStack(Material.STONE));
		loadInto(destination, savedStack);
		return destination;
	}
	
	/**
	 * Load the given saved item stack into another stack.
	 * @param stack - the destination CraftItemStack.
	 * @param savedStack - the saved item stack.
	 */
	public void loadInto(ItemStack stack, NbtCompound savedStack) {
		if (!MinecraftReflection.isCraftItemStack(stack))
			throw new IllegalArgumentException("Stack must be a CraftItemStack, but was " + stack);

		stack.setTypeId(savedStack.getShort("id"));
		stack.setAmount(savedStack.getByte("count"));
		stack.setDurability(savedStack.getShort("damage"));
		
		if (savedStack.containsKey("tag")) {
			NbtFactory.setItemTag(stack, savedStack.getCompound("tag"));
		} else {
			NbtFactory.setItemTag(stack, null);
		}
	}
}
