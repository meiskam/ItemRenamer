package org.shininet.bukkit.itemrenamer.enchants;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class VanillaEnchanter implements Enchanter {
	private final Enchantment enchantment;
	private final int level;
	
	/**
	 * Construct a new enchanter.
	 * @param enchantment - the Bukkit enchantment to apply.
	 * @param level - the enchant level.
	 */
	public VanillaEnchanter(Enchantment enchantment, int level) {
		if (enchantment == null)
			throw new IllegalArgumentException("enchantment cannot be NULL.");
		
		this.enchantment = enchantment;
		this.level = level;
	}

	@Override
	public ItemStack enchant(ItemStack stack) {
		if (stack == null)
			throw new IllegalArgumentException("stack cannot be NULL.");
		
		stack.addUnsafeEnchantment(enchantment, level);
		return stack;
	}

	@Override
	public ItemStack disenchant(ItemStack stack) {
		if (stack == null)
			throw new IllegalArgumentException("stack cannot be NULL.");
		
		stack.removeEnchantment(enchantment);
		return stack;
	}
}
