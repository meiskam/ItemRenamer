package org.shininet.bukkit.itemrenamer.configuration;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.shininet.bukkit.itemrenamer.serialization.DamageSerializer;
import org.shininet.bukkit.itemrenamer.serialization.ExactSerializer;
import org.shininet.bukkit.itemrenamer.utils.ConfigurationUtils;

import com.google.common.collect.Maps;

/**
 * Represents a rule pack in memory.
 * 
 * @author Kristian
 */
class RulePack {
	public static final String EXACT = "exact";
	
	private final String name;
	
	// The two different lookups
	private Map<Integer, DamageLookup> rangeLookup;
	private ExactLookup exactLookup;
	
	// Number of extra modifications
	private int modCount; 
	
	public RulePack(String name) {
		this.name = name;
		this.rangeLookup = Maps.newHashMap();
		this.exactLookup = new MemoryExactLookup();
	}

	/**
	 * Construct a rule pack from a given configuration section.
	 * @param name - the name of the pack.
	 * @param parent - the parent configuration.
	 */
	public RulePack(String name, ConfigurationSection parent) {
		this.name = name;
		load(parent);
	}

	/**
	 * Load the content of a serialized configuration.
	 * @param parent - the parent configuration.
	 */
	public void load(ConfigurationSection parent) {
		ConfigurationSection items = ConfigurationUtils.getSection(parent, name);
		
		// Reset current values
		rangeLookup.clear();
		exactLookup.clear();
		
		if (items != null) {	
			for (String key : items.getKeys(false)) {
				if (EXACT.equals(key)) {
					ExactSerializer serializer = new ExactSerializer(
							ConfigurationUtils.getSection(items, EXACT));
					serializer.readLookup(exactLookup);
				} else {
					parseRange(items, key);
				}
			}
		}
	}

	/**
	 * Parse the given item section.
	 * @param items - the item section.
	 * @param key - the item ID.
	 */
	private void parseRange(ConfigurationSection items, String key) {
		Integer id = Integer.parseInt(key);
		DamageSerializer serializer = new DamageSerializer(ConfigurationUtils.getSection(items, key));
		DamageLookup damage = new MemoryDamageLookup();
		
		// Load and save
		serializer.readLookup(damage);
		rangeLookup.put(id, damage);
	}
	
	/**
	 * Save the content of this rule pack to the given configuration section.
	 * @param section - the section to save to.
	 */
	public void save(ConfigurationSection section) {
		// Write all the stored damage lookups
		for (Entry<Integer, DamageLookup> entry : rangeLookup.entrySet()) {
			DamageSerializer serializer = new DamageSerializer(section.createSection(name + "." + entry.getKey()));
			serializer.writeLookup(entry.getValue());
		}
		
		// Save the exact item stacks
		ExactSerializer serializer = new ExactSerializer(
				section.createSection(name + "." + EXACT));
		serializer.writeLookup(exactLookup);
	}
	
	/**
	 * Retrieve or create the range damage lookup associated with a given item ID.
	 * @param id - the item ID to search for.
	 * @return A damage lookup.
	 */
	public DamageLookup getOrCreateLookup(int id) {
		DamageLookup lookup = rangeLookup.get(id);
		
		// Create it if it doesn't exist yet
		if (lookup == null) {
			modCount++;
			rangeLookup.put(id, lookup = new MemoryDamageLookup());
		}
		return lookup;
	}
	
	/**
	 * Retrieve the current range (for both item ID and damage value) lookup.
	 * @return The range lookup.
	 */
	public Map<Integer, DamageLookup> getRangeLookup() {
		return rangeLookup;
	}
	
	/**
	 * Retrieve the exact lookup.
	 * @return The exact lookup.
	 */
	public ExactLookup getExactLookup() {
		return exactLookup;
	}
	
	/**
	 * Retrieve the name of this rule pack.
	 * @return The name of the rule pack.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Retrieve the total modification count used to track changes across saves and loads.
	 * @return The total modification count.
	 */
	public int getModificationCount() {
		int totalCount = modCount;
		
		for (DamageLookup lookup : rangeLookup.values()) {
			if (lookup.getModificationCount() > 0) {
				totalCount += lookup.getModificationCount();
			}
		}
		return exactLookup.getModificationCount() + totalCount;
	}
}
