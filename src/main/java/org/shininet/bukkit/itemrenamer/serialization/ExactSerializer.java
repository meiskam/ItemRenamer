package org.shininet.bukkit.itemrenamer.serialization;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.configuration.ExactLookup;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;
import org.shininet.bukkit.itemrenamer.utils.ConfigurationUtils;

import com.google.common.collect.Sets;

/**
 * Serializes and deserializes rules pertaining to specific item stacks.
 * @author Kristian
 */
public class ExactSerializer {
	private static final String KEYS = "keys";
	private static final String VALUES = "values";
	
	private ConfigurationSection section;

	/**
	 * Initialize a new exact lookup from a configuration section.
	 * @param section - the configuration section.
	 */
	public ExactSerializer(ConfigurationSection section) {
		setSection(section);
	}
	
	/**
	 * Retrieve the associated configuration section.
	 * @return The associated configuration section.
	 */
	public ConfigurationSection getSection() {
		return section;
	}
	/**
	 * Set the associated configuration section.
	 * @param section - the associated configuration section.
	 */
	private void setSection(ConfigurationSection section) {
		this.section = section;
	}
	
	/**
	 * Deserialize the content of the configuration section to the given exact lookup.
	 * @param destination - the output lookup.
	 */
	public void readLookup(ExactLookup destination) {
		int oldModCount = destination.getModificationCount();
		String currentPath = section.getCurrentPath();
		ConfigurationSection keys = ConfigurationUtils.getSection(section, KEYS);
		ConfigurationSection values = ConfigurationUtils.getSection(section, VALUES);
	
		// Don't permit nulls
		if (keys == null || values == null) {
			// Unless they both are (initial value)
			if (keys != values)
				throw new IllegalStateException(
						"Section " + section + " must have both a keys and a values sub-section.");
			else
				return;
		}
		
		RuleSerializer serializer = new RuleSerializer(values);
		
		// First, parse all the keys and associate with values
		for (String key : keys.getKeys(false)) {
			ItemStack parsed = ItemStack.deserialize(
					ConfigurationUtils.getSection(keys, key).getValues(false));
			
			// Ensure that we succeeded
			if (parsed != null) {
				RenameRule rule = serializer.readRule(key);
				destination.setRule(parsed, rule);
			}
		}
		
		// This is probably wrong
		for (String missing : Sets.symmetricDifference(keys.getKeys(false), values.getKeys(false))) {
			Bukkit.getLogger().warning("[ItemRenamer] [" + currentPath + "] Missing key or value: " + missing);
		}
		destination.setModificationCount(oldModCount);
	}
	
	/**
	 * Write the content of the given lookup to the configuration file.
	 * @param source - the exact lookup to write.
	 */
	public void writeLookup(ExactLookup source) {
		// Reset section
		section = ConfigurationUtils.resetSection(section);
		
		ConfigurationSection keys = section.createSection(KEYS);
		ConfigurationSection values = section.createSection(VALUES);
		RuleSerializer serializer = new RuleSerializer(values);
		
		// The index is the common key used in the one-to-one relation
		Map<ItemStack, RenameRule> lookup = source.toLookup();
		int index = 0;
		
		for (Entry<ItemStack, RenameRule> entry : lookup.entrySet()) {
			String key = Integer.toString(index++);
			
			// Save this mapping
			keys.createSection(key, entry.getKey().serialize());
			serializer.writeRule(key, entry.getValue());
		}
	}
}
