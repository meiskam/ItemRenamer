package org.shininet.bukkit.itemrenamer.enchants;

import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

public class GlowEnchanter extends NbtEnchanter {
	@Override
	public ItemStack enchant(ItemStack stack) {
		if (isApplicable(stack)) {
			NbtCompound compound = getCompound(stack = preprocess(stack));
			compound.put(NbtFactory.ofList("ench"));
		}
		return stack;
	}

	@Override
	public ItemStack disenchant(ItemStack stack) {
		if (isApplicable(stack)) {
			NbtCompound compound = getCompound(stack = preprocess(stack));
			compound.getValue().remove("ench");
		}
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
