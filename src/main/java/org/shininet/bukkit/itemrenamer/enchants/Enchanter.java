package org.shininet.bukkit.itemrenamer.enchants;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a custom enchantment.
 * @author Kristian
 */
public interface Enchanter {
	/**
	 * Apply the current enchantment to a given item stack.
	 * <p>
	 * The given stack may be modified or cloned, depending on what was necessary.
	 * @param stack - the stack to apply to.
	 * @return The resulting item stack.
	 */
	public ItemStack enchant(ItemStack stack);

	/**
	 * Remove the current enchantment from an item stack, if present.
	 * @param stack - the stack to disenchant.
	 * @return The resulting item stack.
	 */
	public ItemStack disenchant(ItemStack stack);
}
