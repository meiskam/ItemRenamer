package org.shininet.bukkit.itemrenamer.enchants;

import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

public class GlowEnchanter implements Enchanter {
	@Override
	public ItemStack enchant(ItemStack stack) {
		if (isApplicable(stack)) {
			NbtCompound compound = (NbtCompound) NbtFactory.fromItemTag(stack = preprocess(stack));

			compound.put(NbtFactory.ofList("ench"));
		}
		return stack;
	}

	@Override
	public ItemStack disenchant(ItemStack stack) {
		if (isApplicable(stack)) {
			NbtCompound compound = (NbtCompound) NbtFactory.fromItemTag(stack = preprocess(stack));
			compound.getValue().remove("ench");
		}
		return stack;
	}
	
	/**
	 * Ensure the given stack is a CraftItemStack.
	 * @param stack - the stack to check.
	 * @return A CraftItemStack.
	 */
	private ItemStack preprocess(ItemStack stack) {
		if (!MinecraftReflection.isCraftItemStack(stack)) 
			return MinecraftReflection.getBukkitItemStack(stack);
		else
			return stack;
	}
	
	/**
	 * Determine if this effect that be applied to a given item.
	 * @param stack - the stack to check.
	 * @return TRUE if it can, FALSE otherwise.
	 */
	private boolean isApplicable(ItemStack stack) {
		if (stack == null)
			throw new IllegalArgumentException("stack cannot be NULL.");
		
		return stack.getEnchantments() == null || stack.getEnchantments().size() == 0;
	}
}
