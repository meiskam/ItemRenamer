package org.shininet.bukkit.itemrenamer.serialization;

import org.bukkit.configuration.ConfigurationSection;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;

/**
 * A serializer and deserializer for RenameRule.
 * 
 * @author Kristian
 */
public class RuleSerializer {
	private static final String RULE_NAME = "name";
	private static final String RULE_LORE = "lore";
	
	private final ConfigurationSection section;

	/**
	 * Construct a rule serializer with the given parent section.
	 * @param section - the parent section.
	 */
	public RuleSerializer(ConfigurationSection section) {
		this.section = section;
	}

	/**
	 * Retrieve the parent configuration section.
	 * @return Parent configuration section.
	 */
	public ConfigurationSection getSection() {
		return section;
	}
	
	/**
	 * Read a rule from a given key.
	 * @param key - the key to read.
	 * @return The read rename rule.
	 */
	public RenameRule readRule(String key) {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be NULL.");
		
		return readRule(getSection(key));
	}
		
	/**
	 * Read a rule from a given configuration section. 
	 * @param ruleSection - the section to read from.
	 * @return The read rename rule.
	 */
	public RenameRule readRule(ConfigurationSection ruleSection) {
		if (ruleSection == null)
			return null;
		
		// Any of these may fail too
		return new RenameRule(ruleSection.getString(RULE_NAME), 
							  ruleSection.getStringList(RULE_LORE));
	}
	
	/**
	 * Write a given rename rule to a given key.
	 * @param key - the key.
	 * @param rule - the rule to write.
	 */
	public void writeRule(String key, RenameRule rule) {
		if (rule != null) {
			ConfigurationSection ruleSection = section.createSection(key);
			
			// Serialize the rename rule
			if (rule.getName() != null)
				ruleSection.set(RULE_NAME, rule.getName());
			if (rule.getLoreSections().size() > 0)
				ruleSection.set(RULE_LORE, rule.getLoreSections());
		} else {
			// Delete it
			section.set(key, null);
		}
	}
	
	/**
	 * Read a configuration section at a given location.
	 * @param key - the key of this section.
	 * @return The configuration section.
	 * @IllegalArgumentException If a value at this section is not a configuration section.
	 */
	private ConfigurationSection getSection(String key) {
		Object ruleSection = section.get(key.toLowerCase());
		
		// Attempt to get a configuration section
		if (ruleSection instanceof ConfigurationSection) {
			return (ConfigurationSection) ruleSection;
		} else if (ruleSection != null) {
			// Warn the user about this corrupt file
			throw new IllegalArgumentException(String.format("Expected a configuration section at %s.%s. Got %s.",
						section.getCurrentPath(), key, ruleSection));
		}
		// Not defined
		return null;
	}
}
