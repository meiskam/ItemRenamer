package org.shininet.bukkit.itemrenamer.wrappers;

import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.shininet.bukkit.itemrenamer.utils.MaterialUtils;

import com.google.common.base.Objects;

/**
 * Represents an item stack that takes enchantment into account when doing comparisions.
 * @author Kristian
 */
public class SpecificItemStack {
	private final ItemStack stack;
	
	/**
	 * Construct a new specific item stack wrapper.
	 * @param stack - an item stack to wrap.
	 */
	public SpecificItemStack(ItemStack stack) {
		if (stack == null)
			throw new IllegalArgumentException("Cannot wrap a NULL stack.");
		// Ensure that we are using a copy
		stack = stack.clone();
		
		// Ignore durability for armor
		if (MaterialUtils.isArmorTool(stack.getType())) {
			stack.setDurability((short) 0);
		}
		
		// Also remove repair cost
		if (stack.hasItemMeta() && stack.getItemMeta() instanceof Repairable) {
			Repairable repairable = (Repairable) stack.getItemMeta();

			repairable.setRepairCost(0);
			stack.setItemMeta((ItemMeta) repairable);
		}
		
		// Save the resulting stack
		this.stack = stack;
	}

	/**
	 * Retrieve the underlying wrapped item stack.
	 * @return The wrapped item stack.
	 */
	public ItemStack getStack() {
		return stack;
	}
	
	/**
	 * Deserialize the content of a specific item stack.
	 * @param data - the data.
	 * @return The deserialized item stack.
	 */
	public static SpecificItemStack deserialize(Map<String, Object> data) {
		return new SpecificItemStack(ItemStack.deserialize(data));
	}
	
	/**
	 * Serialize the content of the wrapped item stack.
	 * @return The content of the item stack.
	 */
	public Map<String, Object> serialise() {
		return stack.serialize();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;	
		if (obj instanceof SpecificItemStack) {
			SpecificItemStack specific = (SpecificItemStack) obj;
			
			if (!Objects.equal(stack, specific.getStack()))
				return false;
			if (specific.getStack() != null && !specific.getStack().getEnchantments().equals(stack.getEnchantments()))
				return false;
			return true;
		}
		// Must be of the same type
		return false;
	}
	
	@Override
	public int hashCode() {	
		// Special case
		if (stack == null)
			return 0;
		return Objects.hashCode(stack.hashCode(), stack.getEnchantments().hashCode());
	}
	
	@Override
	public String toString() {
		return "Specific[" + stack + "]";
	}
}
