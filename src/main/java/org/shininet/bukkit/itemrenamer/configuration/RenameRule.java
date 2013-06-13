package org.shininet.bukkit.itemrenamer.configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.shininet.bukkit.itemrenamer.utils.CollectionsUtil;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
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
	
	public RenameRule() {
		this(null, null);
	}
	
	/**
	 * Construct a new immutable item rename rule.
	 * @param name - the new name.
	 * @param loreSections - list of lore sections.
	 */
	public RenameRule(String name, List<String> loreSections) {
		this.name = name;

		if (loreSections != null)
			// If it's already an immutable list, no copy will be made
			this.loreSections = ImmutableList.copyOf(loreSections);
		else
			// Lore sections should never be NULL
			this.loreSections = ImmutableList.of();
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
			if (CollectionsUtil.isEmpty(loreSections) ^ CollectionsUtil.isEmpty(other.getLoreSections()))
				return false;
			if (!loreSections.containsAll(other.getLoreSections()))
				return false;
			return true;
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
		
		return new RenameRule(name, newLore);
	}
	
	/**
	 * Construct a rename rule with a different name and the same lore.
	 * @param newName - new name.
	 * @return New item rename rule.
	 */
	public RenameRule withName(String newName) {
		return new RenameRule(newName, loreSections);
	}
	
	/**
	 * Determine if this is an identity rule.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public boolean isIdentity() {
		return name == null && CollectionsUtil.isEmpty(loreSections);
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
			RenameRule rule = fallback.withAdditionalLore(priority.getLoreSections());
			
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
			return new RenameRule(newName, null);
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
			return new RenameRule("", Arrays.asList(newLore));
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
