package org.shininet.bukkit.configuration;

import java.util.Map;
import java.util.Map.Entry;

import com.comphenix.protocol.concurrency.AbstractIntervalTree;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;

/**
 * Represents a damage lookup interval tree.
 * @author Kristian
 */
class MemoryDamageLookup implements DamageLookup {
	/**
	 * Represents an integer interval tree.
	 * 
	 * @author Kristian
	 */
	private class IntegerInterval extends AbstractIntervalTree<Integer, RenameRule> {
		@Override
		protected Integer decrementKey(Integer key) {
			return key - 1;
		}

		@Override
		protected Integer incrementKey(Integer key) {
			return key + 1;
		}
	}
	
	// Used to store our data
	private AbstractIntervalTree<Integer, RenameRule> tree = new IntegerInterval();
	
	private RenameRule all;
	private RenameRule other;
	
	/**
	 * Construct a new memory damage lookup.
	 */
	public MemoryDamageLookup() {	
		// Use default values
	}
	
	/**
	 * Clone a memory damage lookup from a given lookup.
	 * @param lookup - the other lookup.
	 */
	public MemoryDamageLookup(DamageLookup other) {
		setAllRule(other.getAllRule());
		setOtherRule(other.getOtherRule());
		
		// Use the lookup method
		for (Entry<Range<Integer>, RenameRule> entry : other.toLookup().entrySet()) {
			Range<Integer> range = entry.getKey();
			RenameRule rule = entry.getValue();
			setRule(range.lowerEndpoint(), range.upperEndpoint(), rule);
		}
	}
	
	@Override
	public RenameRule getAllRule() {
		return all;
	}

	@Override
	public void setAllRule(RenameRule rule) {
		this.all = rule;
	}
	
	@Override
	public RenameRule getOtherRule() {
		return other;
	}
	
	@Override
	public void setOtherRule(RenameRule rule) {
		this.other = rule;
	}
	
	@Override
	public RenameRule getRule(int damage) {
		RenameRule value = getDefinedRule(damage);
		RenameRule all = getAllRule();
		
		// All is our fallback
		if (value == null) {
			return RenameRule.merge(getOtherRule(), all);
		} else {
			return RenameRule.merge(value, all);
		}
	}
	
	@Override
	public RenameRule getDefinedRule(int damage) {
		return tree.get(damage);
	}
	
	@Override
	public Map<Range<Integer>, RenameRule> toLookup() {
		Map<Range<Integer>, RenameRule> lookup = Maps.newLinkedHashMap();
		
		// Enumerate and create the resulting set
		for (IntegerInterval.Entry entry : tree.entrySet()) {
			lookup.put(entry.getKey(), entry.getValue());
		}
		return lookup;
	}
	
	@Override
	public void setRule(int damage, RenameRule rule) {
		if (damage < 0)
			throw new IllegalArgumentException("Damage cannot be less than zero.");
		tree.put(damage, damage, rule);
	}
	
	@Override
	public void setRule(int lowerDamage, int upperDamage, RenameRule rule) {
		if (lowerDamage < 0 || upperDamage < 0)
			throw new IllegalArgumentException("Damage cannot be less than zero.");
		if (lowerDamage > upperDamage)
			throw new IllegalArgumentException("Lower damage must be less than upper damage.");
		
		tree.put(lowerDamage, upperDamage, rule);
	}
}
