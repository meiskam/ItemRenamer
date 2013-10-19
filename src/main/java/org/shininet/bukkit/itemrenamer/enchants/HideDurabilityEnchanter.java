package org.shininet.bukkit.itemrenamer.enchants;

import org.bukkit.inventory.ItemStack;

public class HideDurabilityEnchanter implements Enchanter {
	private final int newDurability;
	
	/**
	 * Hide the real durability of an item by replacing it with the given durability,
	 * @param newDurability - the new durability.
	 */
	public HideDurabilityEnchanter(int newDurability) {
		this.newDurability = newDurability;
	}

	@Override
	public ItemStack enchant(ItemStack stack) {
		stack.setDurability((short) newDurability);
		return stack;
	}

	@Override
	public ItemStack disenchant(ItemStack stack) {
		// Doesn't really make any sense
		return stack;
	}
}
