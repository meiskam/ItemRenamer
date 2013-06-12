package org.shininet.bukkit.itemrenamer.configuration;

import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.google.common.base.Function;

/**
 * Represents a lookup for specific item stacks.
 * @author Kristian
 */
public interface ExactLookup extends Modifiable {
	/**
	 * Get the defined rename rule for a given item stack.
	 * @param stack - the item stack.
	 * @return A rule, or NULL if not found.
	 */
	public abstract RenameRule getRule(ItemStack stack);
	
	/**
	 * Set the defined rename rule for a given item stack.
	 * @param stack - the item stack to update.
	 * @param rule - the new rule, or NULL to delete the rule.
	 */
	public abstract void setRule(ItemStack stack, RenameRule rule);
	
	/**
	 * Transform the rename rule associated with the given item stack.
	 * <p>
	 * If no rule exists, an identity rule will be created before invoking the function. 
	 * If the function returns NULL, or the identity rename rule, then the rule will be deleted.
	 * @param stack - the item stack that will be updated.
	 * @param function - the rule transformation. 
	 */
	public void setTransform(ItemStack stack, Function<RenameRule, RenameRule> function);
	
	/**
	 * Retrieve every defined rule in the lookup tree.
	 * @return Every defined rule.
	 */
	public abstract Map<ItemStack, RenameRule> toLookup();

	/**
	 * Reset the content of this exact lookup.
	 */
	public abstract void clear();
}
