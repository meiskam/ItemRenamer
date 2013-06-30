package org.shininet.bukkit.itemrenamer.configuration;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Store the rename packs from a configuration file.
 * 
 * @author Kristian
 */
public class RenameConfiguration {
	private final ConfigurationSection section;
	
	// Store of every loaded lookup
	private final Map<String, RulePack> memoryLookup = Maps.newHashMap();
	
	// How many times this configuration has changed
	private int modCount;
	
	/**
	 * Construct a new rename configuration from a given section.
	 * @param section - the underlying configuration section.
	 */
	public RenameConfiguration(ConfigurationSection section) {
		this.section = section;
		
		// Load all packs
		for (String pack : section.getKeys(false)) {
			loadPack(pack);
		}
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
		RulePack itemLookup = loadPack(pack);

		if (itemLookup != null) {
			return itemLookup.getRangeLookup().get(itemID);
		} else {
			throw new IllegalArgumentException("Pack " + pack + " doesn't exist.");
		}
	}
	
	/**
	 * Retrieve the exact lookup for a given pack.
	 * @param pack - the pack name.
	 * @return The exact lookup.
	 */
	public ExactLookup getExact(String pack) {
		RulePack itemLookup = loadPack(pack);
		
		if (itemLookup != null) {
			return itemLookup.getExactLookup();
		} else {
			throw new IllegalArgumentException("Pack " + pack + " doesn't exist.");	
		}
	}
	
	/**
	 * Retrieve an immutable view of the name of every registered pack.
	 * @return Name of every registered pack.
	 */
	public Set<String> getPacks() {
		return ImmutableSet.copyOf(memoryLookup.keySet());
	}
	
	/**
	 * Load a given item name pack from the configuration file.
	 * @param pack - the pack to load.
	 * @return A map representation of this pack.
	 */
	private RulePack loadPack(String pack) {
		RulePack itemLookup = memoryLookup.get(pack);
		
		// Initialize item lookup
		if (itemLookup == null) {
			itemLookup = new RulePack(pack);
			itemLookup.load(section);
			memoryLookup.put(pack, itemLookup);
		}
		return itemLookup;
	}
	
	/**
	 * Determine if the given pack exists.
	 * @param pack - the pack to lookup.
	 * @return TRUE if it does, FALSE otherwise.
	 */
	public boolean hasPack(String pack) {
		return loadPack(pack) != null;
	}

	/**
	 * Create a new damage lookup, or load the existing damage value if it exists.
	 * @param pack - package it belongs to.
	 * @param itemID - item ID.
	 * @return Existing damage lookup, or a new one if it doesn't exist.
	 */
	public DamageLookup createLookup(String pack, int itemID) {
		return loadOrCreatePack(pack).getOrCreateLookup(itemID);
	}
	
	/**
	 * Create a new exact lookup, or load the existing lookup if it exist.
	 * @param pack - package it belongs to.
	 * @return Existing exact lookup, or a new one if it doesn't exist.
	 */
	public ExactLookup createExact(String pack) {
		return loadOrCreatePack(pack).getExactLookup();
	}
	
	/**
	 * Delete the pack with the given name.
	 * @param pack - the pack to remove.
	 * @return TRUE if a pack was removed, FALSE otherwise.
	 */
	public boolean removePack(String pack) {
		modCount++;
		return memoryLookup.remove(pack) != null;
	}
	
	/**
	 * Construct a new item pack.
	 * @param pack - name of the pack to construct.
	 * @return TRUE if this pack was constructed, FALSE if a pack by this name already exists.
	 */
	public boolean createPack(String pack) {
		if (loadPack(pack) == null) {
			loadOrCreatePack(pack);
			return true;
		}
		return false;
	}
	
	/**
	 * Load or create a new pack.
	 * @param pack - name of the pack to create.
	 * @return The existing pack, or a new one.
	 */
	private RulePack loadOrCreatePack(String pack) {
		RulePack itemLookup = loadPack(pack);
		
		// Create a new if we need to
		if (itemLookup == null) {
			modCount++;
			memoryLookup.put(pack, itemLookup);
		}
		return itemLookup;
	}
	
	/**
	 * Save the given name pack to the configuration file.
	 * @param pack - name pack.
	 */
	public void saveLookup(String pack) {
		RulePack itemLookup = memoryLookup.get(pack);
		
		if (itemLookup != null) {
			itemLookup.save(section);
		} else {
			throw new IllegalArgumentException("Cannot save " + pack + ": It doesn't exist.");
		}
	}
	
	/**
	 * Determine if this configuration has changed.
	 * @return TRUE if it has, FALSE otherwise.
	 */
	public int getModificationCount() {
		int totalCount = modCount;
		
		for (Entry<String, RulePack> packEntry : memoryLookup.entrySet()) {
			totalCount += packEntry.getValue().getModificationCount();
		}
		return totalCount;
	}
	
	/**
	 * Save every name pack and every setting.
	 */
	public void saveAll() {
		// Reset everything first
		for (String key : Lists.newArrayList(section.getKeys(false))) {
			section.set(key, null);
		}
		
		for (String pack : memoryLookup.keySet()) {
			saveLookup(pack);
		}
	}
}
