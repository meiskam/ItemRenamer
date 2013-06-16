package org.shininet.bukkit.itemrenamer.configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.shininet.bukkit.itemrenamer.utils.CollectionsUtil;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Represents a new immutable name and lore for a given item.
 * 
 * @author Kristian
 */
public class RenameRule {
	/**
	 * Represents a rename rule that makes no modifications to a given item stack (identity rule).
	 */
	public static final RenameRule IDENTITY = new RenameRule();
	
	private final String name;
	private final ImmutableList<String> loreSections;
	
	// Enchantments to add or remove
	private final ImmutableSet<LeveledEnchantment> addedEnchantments;
	private final ImmutableSet<LeveledEnchantment> removedEnchantments;
	
	public RenameRule() {
		this(null, null, null, null);
	}
	
	/**
	 * Construct a new immutable item rename rule.
	 * @param name - the new name.
	 * @param loreSections - list of lore sections.
	 * @param added - set of enchantments to add.
	 * @param removoed - set of enchantments to remove.
	 */
	public RenameRule(String name, List<String> loreSections, 
					  Set<LeveledEnchantment> added, Set<LeveledEnchantment> removed) {
		this.name = name;
		this.loreSections = safeList(loreSections);
		this.addedEnchantments = safeSet(added);
		this.removedEnchantments = safeSet(removed);
	}
	
	/**
	 * Construct an immutable copy of a given list.
	 * @param list - the list to copy.
	 * @return The copied immutable list.
	 */
	private <T> ImmutableList<T> safeList(List<T> list) {
		if (list != null)
			return ImmutableList.copyOf(list);
		else
			return ImmutableList.of();
	}
	
	/**
	 * Construct an immutable copy of a given set.
	 * @param list - the set to copy.
	 * @return The copied immutable set.
	 */
	private <T> ImmutableSet<T> safeSet(Set<T> set) {
		if (set != null)
			return ImmutableSet.copyOf(set);
		else
			return ImmutableSet.of();
	}

	/**
	 * Retrieve the new name of a given item.
	 * @return The new name, or NULL if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieve a list of all the new lore sections to add.
	 * @return New lore sections.
	 */
	@Nonnull
	public ImmutableList<String> getLoreSections() {
		return loreSections;
	}
	
	/**
	 * Retrieve a list of every enchantment that will be added to the item stack.
	 * @return Every enchantment to be added.
	 */
	@Nonnull
	public ImmutableSet<LeveledEnchantment> getAddedEnchantments() {
		return addedEnchantments;
	}
	
	/**
	 * Retrieve a list of every enchantment (or dechantment) that will be removed from the item stack.
	 * @return Every enchantment to be removed.
	 */
	public ImmutableSet<LeveledEnchantment> getRemovedEnchantments() {
		return removedEnchantments;
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(name, loreSections);
	}
	
	@Override
	public boolean equals(Object obj) {
		// Shortcut
		if (this == obj)
			return true;
		
		if (obj instanceof RenameRule) {
			RenameRule other = (RenameRule) obj;
			
			if (!Objects.equal(name, other.getName()))
				return false;
			return CollectionsUtil.equalsMany(loreSections, other.getLoreSections()) &&
				   CollectionsUtil.equalsMany(addedEnchantments, other.getAddedEnchantments()) &&
				   CollectionsUtil.equalsMany(removedEnchantments, other.getRemovedEnchantments());
		}
		return false;
	}
	
	@Override
	public String toString() {
		if (loreSections != null && loreSections.size() > 0)
			return String.format("[name: %s, lore: \n%s\n]]", name, Joiner.on("\n  -").join(loreSections));
		else
			return "[name: " + name + "]";
	}
	
	/**
	 * Construct a new rename rule with an additional line of lore.
	 * @param lore - new line of lore.
	 * @return The updated rename rule.
	 */
	public RenameRule withAdditionalLore(Collection<String> lore) {
		List<String> newLore = Lists.newArrayList(loreSections);
		newLore.addAll(lore);
		
		return new RenameRule(name, newLore, addedEnchantments, removedEnchantments);
	}
	
	/**
	 * Construct a rename rule with a different name and the same lore.
	 * @param newName - new name.
	 * @return New item rename rule.
	 */
	public RenameRule withName(String newName) {
		return new RenameRule(newName, loreSections, addedEnchantments, removedEnchantments);
	}
	
	/**
	 * Construct a new rename rule with an additional added enchantment.
	 * @param enchantments - new enchantments to add.
	 * @return The updated rename rule.
	 */
	public RenameRule withAddedEnchantment(Collection<LeveledEnchantment> enchantments) {
		Set<LeveledEnchantment> copy = ImmutableSet.<LeveledEnchantment>builder().
				addAll(addedEnchantments).
				addAll(enchantments).
				build();
		return new RenameRule(name, loreSections, copy, removedEnchantments);
	}
	
	/**
	 * Construct a new rename rule with a removed enchantment.
	 * @param enchantments - new enchantments to remove.
	 * @return The updated rename rule.
	 */
	public RenameRule withRemovedEnchantment(Collection<LeveledEnchantment> enchantments) {
		Set<LeveledEnchantment> copy = ImmutableSet.<LeveledEnchantment>builder().
				addAll(removedEnchantments).
				addAll(enchantments).
				build();
		return new RenameRule(name, loreSections, addedEnchantments, copy);
	}
	
	/**
	 * Determine if this is an identity rule.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public boolean isIdentity() {
		return name == null && 
				CollectionsUtil.isEmpty(loreSections) &&
				CollectionsUtil.isEmpty(addedEnchantments) &&
				CollectionsUtil.isEmpty(removedEnchantments);
	}
	
	/**
	 * Merge two item rename rules such that the priority rule overrides the fallback in name. 
	 * <p>
	 * The lore is combined from both. Priority is last.
	 * <p>
	 * Also note that null names will not override set names.
	 * @param priority - priority rule.
	 * @param fallback - fallback role.
	 * @return Merged rename rule, or NULL if both are NULL.
	 */
	public static RenameRule merge(RenameRule priority, RenameRule fallback) {
		if (fallback == null || priority == null) {
			return priority != null ? priority : fallback;
		} else {
			// Priority takes, well, priority
			RenameRule rule = fallback.
					withAdditionalLore(priority.getLoreSections()).
					withAddedEnchantment(priority.getAddedEnchantments()).
					withRemovedEnchantment(priority.getRemovedEnchantments());
			
			// Don't merge nulls
			if (priority.getName() != null) {
				rule = rule.withName(priority.getName());
			}
			return rule;
		}
	}
	
	/**
	 * Modify the given rename rule by setting a new name.
	 * @param original - the original rename rule.
	 * @param newName - the new name.
	 * @return The modified rename rule.
	 */
	public static RenameRule withName(RenameRule original, String newName) {
		if (original == null)
			return new RenameRule(newName, null, null, null);
		else
			return original.withName(newName);
	}
	
	/**
	 * Modify the given rename rule by setting a new name.
	 * @param original - the original rename rule.
	 * @param newLore - the new lore.
	 * @return The modified rename rule.
	 */
	public static RenameRule withAdditionalLore(RenameRule original, String newLore) {
		if (original == null)
			return new RenameRule("", Arrays.asList(newLore), null, null);
		else
			return original.withAdditionalLore(Arrays.asList(newLore));
	}
	
	/**
	 * Determine if a given rename rule is the identity rule. 
	 * <p>
	 * That is, it preserves both the name and lore section of any item stack given to it.
	 * @param rule - the rule to check.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public static boolean isIdentity(RenameRule rule) {
		return rule == null || rule == IDENTITY || rule.isIdentity();
	}
}
