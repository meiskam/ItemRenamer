package org.shininet.bukkit.itemrenamer.utils;

import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.utility.MinecraftReflection;

public class StackUtils {
	private StackUtils() {
		// Do not create
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
