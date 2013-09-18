package org.shininet.bukkit.itemrenamer.utils;

import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

public class StackUtils {
	private StackUtils() {
		// Do not create
	}
	
	/**
	 * Search for the value of a key in the given NBT compound.
	 * @param parent - the parent compound.
	 * @param path - the relative path to follow.
	 * @return The value, or NULL if not found.
	 */
	public static Object getNbtTag(NbtCompound parent, String... path) {
		Object current = parent;
		
		for (String element : path) {
			// Only compounds have children NBT tags with names
			if (current instanceof NbtCompound) {
				current = ((NbtCompound) current).getObject(element);
			} else {
				return false;
			}
			
			// Unable to find this key
			if (current == null) {
				return null;
			}
		}
		return current;
	}
	
	/**
	 * Retrieve the corresponding CraftItemStack.
	 * @param stack - a Bukkit item stack, or a CraftItemStack.
	 * @return A CraftItemStack.
	 */
	public static ItemStack getCraftItemStack(ItemStack stack) {
		if (!MinecraftReflection.isCraftItemStack(stack))
			return MinecraftReflection.getBukkitItemStack(stack);
		else
			return stack;
	}
}
