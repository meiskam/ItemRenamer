package org.shininet.bukkit.itemrenamer.enchants;

import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.utils.StackUtils;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

/**
 * Represents an enchanter that modifies the underlying NBT tag information.
 * 
 * @author Kristian
 */
public abstract class NbtEnchanter implements Enchanter {
	/**
	 * Retrieve the underlying NBT compound from a given CraftItemStack.
	 * @param stack - the CraftItemStack to look into.
	 * @return The NBT compound stored in the ItemStack.
	 */
	protected NbtCompound getCompound(ItemStack stack) {
		return (NbtCompound) NbtFactory.fromItemTag(stack);
	}
	
	/**
	 * Ensure the given stack is a CraftItemStack.
	 * @param stack - the stack to check.
	 * @return A CraftItemStack.
	 */
	protected ItemStack preprocess(ItemStack stack) {
		return StackUtils.getCraftItemStack(stack);
	}
}
