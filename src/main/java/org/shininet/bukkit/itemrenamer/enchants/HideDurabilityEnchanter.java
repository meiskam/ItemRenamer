package org.shininet.bukkit.itemrenamer.enchants;

import org.bukkit.inventory.ItemStack;

public class HideDurabilityEnchanter implements Enchanter {
	@Override
	public ItemStack enchant(ItemStack stack) {
		stack.setDurability((short) 0);
		return stack;
	}

	@Override
	public ItemStack disenchant(ItemStack stack) {
		// Doesn't really make any sense
		return stack;
	}
}
