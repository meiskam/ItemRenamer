package org.shininet.bukkit.wrappers;

import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

/**
 * Represents an item stack that takes enchantment into account when doing comparisions.
 * @author Kristian
 */
public class SpecificItemStack {
	private final ItemStack stack;
	
	// Don't use EnumSet - may not work that well with MCPC+
	private static Set<Material> armor = Sets.newHashSet();
	private static Set<Material> tool = Sets.newHashSet();
	
	// Invoked to initialize the armor and weapon lookups
	static {
		for (Material material : Material.values()) {
			String name = material.name();
			
			if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGING") || name.contains("BOOTS")) {
				armor.add(material);
			} else if (name.contains("AXE") || name.contains("HOE") || name.contains("PICKAXE") || name.contains("SPADE") || name.contains("SWORD")) {
				tool.add(material);
			}
		}
	}
	
	/**
	 * Construct a new specific item stack wrapper.
	 * @param stack - an item stack to wrap.
	 */
	public SpecificItemStack(ItemStack stack) {
		if (stack == null)
			throw new IllegalArgumentException("Cannot wrap a NULL stack.");
		this.stack = stack;
		
		// Ignore durability for armor
		if (isArmorTool()) {
			stack = stack.clone();
			stack.setDurability((short) 0);
		}
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
	
	/**
	 * Determine if the current item stack is an armor or a tool
	 * @return TRUE it if is either, FALSE otherwise.
	 */
	private boolean isArmorTool() {
		return armor.contains(stack.getType()) || tool.contains(stack.getType());
	}
	
	@Override
	public String toString() {
		return "Specific[" + stack + "]";
	}
}
