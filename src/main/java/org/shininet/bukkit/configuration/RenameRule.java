package org.shininet.bukkit.configuration;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

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
	private final String name;
	private final ImmutableList<String> loreSections;
	
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
			this.loreSections = ImmutableList.<String>of();
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
			
			return Objects.equal(name, other.getName()) &&
				   Objects.equal(getLoreSections(), other.getLoreSections());
		}
		return false;
	}
	
	@Override
	public String toString() {
		return String.format("[name: %s, lore: \n%s\n]]", Joiner.on("\n  -").join(loreSections));
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
}
