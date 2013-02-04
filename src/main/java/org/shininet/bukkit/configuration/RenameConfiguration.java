package org.shininet.bukkit.configuration;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;

import com.google.common.collect.Maps;

/**
 * Store the rename pack from a configuration file.
 * 
 * @author Kristian
 */
public class RenameConfiguration {
	private ConfigurationSection section;
	
	// Store of every loaded lookup
	private Map<String, Map<Integer, DamageLookup>> memoryLookup = Maps.newHashMap();
	
	public RenameConfiguration(ConfigurationSection section) {
		this.section = section;
	}

	/**
	 * Retrieve a damage lookup object from a given name pack and item ID.
	 * <p>
	 * The pack must exist. 
	 * @param pack - the name pack. 
	 * @param itemID - the item ID.
	 * @return The damage lookup, or NULL if the item ID has not been registered.
	 */
	public DamageLookup getLookup(String pack, int itemID) {
		Map<Integer, DamageLookup> itemLookup = loadPack(pack);

		if (itemLookup != null) {
			return itemLookup.get(itemID);
		} else {
			throw new IllegalArgumentException("Pack " + pack + " doesn't exist.");
		}
	}
	
	/**
	 * Load a given item name pack from the configuration file.
	 * @param pack - the pack to load.
	 * @return A map representation of this pack.
	 */
	private Map<Integer, DamageLookup> loadPack(String pack) {
		Map<Integer, DamageLookup> itemLookup = memoryLookup.get(pack);

		// Initialize item lookup
		if (itemLookup == null) {
			ConfigurationSection items = section.getConfigurationSection(pack);
			
			if (items != null) {
				memoryLookup.put(pack, itemLookup = Maps.newHashMap());
				
				for (String key : items.getKeys(false)) {
					Integer id = Integer.parseInt(key);
					DamageSerializer serializer = new DamageSerializer(items.getConfigurationSection(key));	
					DamageLookup damage = new MemoryDamageLookup();
					
					// Load and save
					serializer.readLookup(damage);
					itemLookup.put(id, damage);
				}
			} else {
				return null;
			}
		}
		return itemLookup;
	}

	/**
	 * Create a new damage lookup, or load the existing damage value if it exists.
	 * @param pack - package it belongs to.
	 * @param itemID - item ID.
	 * @return Existing damage lookup, or a new one if it doesn't exist.
	 */
	public DamageLookup createLookup(String pack, int itemID) {
		Map<Integer, DamageLookup> itemLookup = loadPack(pack);
		
		// Create a new if we need to
		if (itemLookup == null) {
			memoryLookup.put(pack, itemLookup = Maps.newHashMap());
		}
		
		// Same thing for the lookup
		DamageLookup lookup = itemLookup.get(itemID);
		
		if (lookup == null) {
			itemLookup.put(itemID, lookup = new MemoryDamageLookup());
		}
		return lookup;
	}
	
	/**
	 * Save the given name pack to the configuration file.
	 * @param pack - name pack.
	 */
	public void saveLookup(String pack) {
		Map<Integer, DamageLookup> itemLookup = memoryLookup.get(pack);
		
		if (itemLookup != null) {
			// Write all the stored damage lookups
			for (Entry<Integer, DamageLookup> entry : itemLookup.entrySet()) {
				DamageSerializer serializer = new DamageSerializer(section.createSection(pack + "." + entry.getKey()));
				serializer.writeLookup(entry.getValue());
			}
			
		} else {
			throw new IllegalArgumentException("Cannot save " + pack + ": It doesn't exist.");
		}
	}
	
	/**
	 * Save every name pack and every setting.
	 */
	public void saveAll() {
		for (String pack : memoryLookup.keySet()) {
			saveLookup(pack);
		}
	}
}
