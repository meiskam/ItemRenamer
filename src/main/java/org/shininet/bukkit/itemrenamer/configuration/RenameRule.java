package org.shininet.bukkit.itemrenamer.configuration;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

import org.shininet.bukkit.itemrenamer.utils.CollectionsUtil;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
	private final boolean skipCustomNamed;
	private final ImmutableList<String> loreSections;

	// Enchantments to add or remove
	private final ImmutableSet<LeveledEnchantment> enchantments;
	private final ImmutableSet<LeveledEnchantment> dechantments;

	/**
	 * Construct the identity rule.
	 */
	private RenameRule() {
		this(new Builder());
	}

	/**
	 * Construct a new rename rule from a builder.
	 * @param builder - the builder to construct from.
	 */
	private RenameRule(Builder builder) {
		this.name = builder.name;
		this.skipCustomNamed = builder.skipCustomNamed;
		this.loreSections = safeList(builder.loreSections);
		this.enchantments = safeSet(builder.enchantments);
		this.dechantments = safeSet(builder.dechantments);
	}

	/**
	 * Construct an immutable copy of a given list.
	 * @param list - the list to copy.
	 * @return The copied immutable list.
	 */
	private static <T> ImmutableList<T> safeList(Collection<T> list) {
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
	private static <T> ImmutableSet<T> safeSet(Collection<T> set) {
		if (set != null)
			return ImmutableSet.copyOf(set);
		else
			return ImmutableSet.of();
	}
	
	/**
	 * Represents a rename rule builder.
	 * @author Kristian
	 */
	public static class Builder {
		private String name;
		private boolean skipCustomNamed;
		private Collection<String> loreSections;
		private Set<LeveledEnchantment> enchantments;
		private Set<LeveledEnchantment> dechantments;
		
		private Builder() {
			// Initialize to nothing
		}
		
		private Builder(RenameRule template) {
			// Note that the enchantments themselves are never NULL
			name(template.name);
			skipCustomNamed(template.skipCustomNamed);
			loreSections(template.loreSections);
			enchantments(template.enchantments);
			dechantments(template.dechantments);
		}
		
		/**
		 * Set the new name of items that are applied to this rule.
		 * @param name - new name, or NULL to keep the old.
		 * @return This builder, for chaining.
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		/**
		 * Set whether or not to skip items that already have a custom name or lore lines.
		 * <p>
		 * This is implicitly set by the damage lookup (TRUE) and explicit lookup (FALSE), and will 
		 * not persist through serialization.
		 * @param skipNamed - TRUE to skip items with custom name or lore, FALSE otherwise.
		 * @return This builder, for chaining. 
		 */
		public Builder skipCustomNamed(boolean skipNamed) {
			this.skipCustomNamed = skipNamed;
			return this;
		}
		
		/**
		 * Set the list of lore sections to add to the items that are applied.
		 * @param loreSections - list of new lore sections, or NULL for none at all.
		 * @return This builder, for chaining.
		 */
		public Builder loreSections(Collection<String> loreSections) {
			this.loreSections = loreSections != null ? Lists.newArrayList(loreSections) : null;
			return this;
		}
		
		/**
		 * Merge in the given lore sections.
		 * @param loreSections - the lore sections to merge.
		 * @return This builder, for chaining.
		 */
		public Builder mergeLoreSections(@Nonnull Collection<String> loreSections) {
			Preconditions.checkNotNull(loreSections, "loreSections cannot be NULL.");
			
			// Construct a new list if needed
			if (this.loreSections != null)
				this.loreSections.addAll(loreSections);
			else
				loreSections(loreSections);
			return this;
		}
		
		/**
		 * Set which enchantments to add to the item stack.
		 * @param enchantments - collection of new lore sections.
		 * @return This builder, for chaining.
		 */
		public Builder enchantments(Collection<LeveledEnchantment> enchantments) {
			this.enchantments = enchantments != null ? Sets.newHashSet(enchantments) : null;
			return this;
		}

		/**
		 * Merge in the given enchantments.
		 * @param enchantments - the enchantments to merge.
		 * @return This builder, for chaining.
		 */
		public Builder mergeEnchantments(@Nonnull Collection<LeveledEnchantment> enchantments) {
			Preconditions.checkNotNull(enchantments, "enchantments cannot be NULL.");
			
			if (this.enchantments != null)
				this.enchantments.addAll(enchantments);
			else
				enchantments(enchantments);
			return this;
		}
		
		/**
		 * Set which dechantments to remove from the item stack.
		 * @param dechantments - enchantments to remove from the stack.
		 * @return This builder, for chaining.
		 */
		public Builder dechantments(Collection<LeveledEnchantment> dechantments) {
			this.dechantments = dechantments != null ? Sets.newHashSet(dechantments) : null;
			return this;
		}
		
		/**
		 * Merge in the given dechantments.
		 * @param dechantments - the enchantments to merge.
		 * @return This builder, for chaining.
		 */
		public Builder mergeDechantments(@Nonnull Collection<LeveledEnchantment> dechantments) {
			Preconditions.checkNotNull(dechantments, "dechantments cannot be NULL.");

			if (this.dechantments != null)
				this.dechantments.addAll(dechantments);
			else
				dechantments(dechantments);
			return this;
		}
		
		/**
		 * Remove enchantments and dechantments that cancel each other.
		 */
		private void simplify() {
			// No cancellation is possible
			if (enchantments == null || dechantments == null)
				return;
			
			Set<LeveledEnchantment> intersection = Sets.intersection(enchantments, dechantments);
			
			if (intersection.size() > 0) {
				intersection = Sets.newHashSet(intersection);
				enchantments.removeAll(intersection);
				dechantments.removeAll(intersection);
			}
		}
		
		/**
		 * Construct a new rename rule based on the current parameters.
		 * @return The new rename rule.
		 */
		public RenameRule build() {
			simplify();
			return new RenameRule(this);
		}
	}

	/**
	 * Construct a new rename rule builder that is initialized for identity rules.
	 * @return Rename rule builder.
	 */
	public static Builder newBuilder() {
		return new Builder();
	}
	
	/**
	 * Construct a new rename rule builder initialized to the given template.
	 * <p>
	 * Note that NULL is treated as the IDENTITY template.
	 * @param template - rule template.
	 * @return Rename rule builder.
	 */
	public static Builder newBuilder(RenameRule template) {
		// Handle NULL too
		if (template != null)
			return new Builder(template);
		else
			return newBuilder();
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
	public ImmutableSet<LeveledEnchantment> getEnchantments() {
		return enchantments;
	}

	/**
	 * Retrieve a list of every enchantment (or dechantment) that will be removed from the item stack.
	 * @return Every enchantment to be removed.
	 */
	public ImmutableSet<LeveledEnchantment> getDechantments() {
		return dechantments;
	}
	
	/**
	 * Determine if this rename rule will skip items with custom name or lore.
	 * @return TRUE it the rule will skip those items, FALSE otherwise.
	 */
	public boolean isSkippingCustomNamed() {
		return skipCustomNamed;
	}

	/**
	 * Retrieve a rename rule with the given skip rule.
	 * @param skipCustomNamed - whether or not to skip names that have a custom name or lore.
	 * @return The same rename rule with this skip rule.
	 */
	public RenameRule withSkipRule(boolean skipCustomNamed) {
		if (this.skipCustomNamed == skipCustomNamed)
			return this;
		else
			return newBuilder(this).skipCustomNamed(skipCustomNamed).build();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(name, skipCustomNamed, loreSections, enchantments, dechantments);
	}

	@Override
	public boolean equals(Object obj) {
		// Shortcut
		if (this == obj)
			return true;

		if (obj instanceof RenameRule) {
			RenameRule other = (RenameRule) obj;

			if (!Objects.equal(name, other.getName()) || skipCustomNamed != other.isSkippingCustomNamed())
				return false;
			return CollectionsUtil.equalsMany(loreSections, other.getLoreSections())
					&& CollectionsUtil.equalsMany(enchantments, other.getEnchantments())
					&& CollectionsUtil.equalsMany(dechantments, other.getDechantments());
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		// For constructing the extra sections
		Collection<?>[] collections = new Collection<?>[] { loreSections, enchantments, dechantments };
		String[] names = new String[] { "lore", "ench", "dech" };
		
		// The extra sections
		for (int i = 0; i < collections.length; i++) {
			if (!CollectionsUtil.isEmpty(collections[i])) {
				result.append(", ");
				result.append(names[i] + ": " + collections[i]);
			}
		}
		return "[name: " + name + result.toString() + "]";
	}

	/**
	 * Retrieve a builder initialized to this rename rule.
	 * @return A builder that will build copies of this rename rule.
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * Determine if this is an identity rule.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public boolean isIdentity() {
		return this == IDENTITY || 
				(name == null && CollectionsUtil.isEmpty(loreSections)
				&& CollectionsUtil.isEmpty(enchantments)
				&& CollectionsUtil.isEmpty(dechantments));
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
			Builder builder = fallback.toBuilder().
					mergeLoreSections(priority.getLoreSections()).
					mergeEnchantments(priority.getEnchantments()).
					mergeDechantments(priority.getDechantments());

			// Don't merge nulls
			if (priority.getName() != null) {
				builder.name(priority.getName());
			}
			return builder.build();
		}
	}

	/**
	 * Determine if a given rename rule is the identity rule. 
	 * <p>
	 * That is, it preserves both the name and lore section of any item stack given to it.
	 * @param rule - the rule to check.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public static boolean isIdentity(RenameRule rule) {
		return rule == null || rule.isIdentity();
	}
}
