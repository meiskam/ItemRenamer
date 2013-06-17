package org.shininet.bukkit.itemrenamer.serialization;

import java.util.Collection;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.shininet.bukkit.itemrenamer.utils.ConfigurationUtils;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;

/**
 * Serialize and deserialize a collection of enchantments.
 * 
 * @author Kristian
 */
public class EnchantmentSerializer {
	/**
	 * Read the content of the current configuration section and store it in the given collection.
	 * @param section - the section to read from.
	 * @param destination - the destination collection.
	 */
	public void readEnchantments(ConfigurationSection section, Collection<LeveledEnchantment> destination) {
		for (Entry<String, Object> entry : section.getValues(false).entrySet()) {
			Enchantment enchantment = Enchantment.getByName(entry.getKey());
		
			if (entry.getValue() instanceof Number && enchantment != null) {
				Number number = (Number) entry.getValue();
				LeveledEnchantment output = new LeveledEnchantment(enchantment, number.intValue());
				
				// Store the parsed enchantment
				destination.add(output);
			} else {
				// Try to be a bit informative
				Bukkit.getLogger().warning("[ItemRenamer] [" + section.getCurrentPath() + "] Invalid value " + 
						entry.getValue() + " for key " + enchantment);
			}
		}
	}
	
	/**
	 * Read the content of the given packaged byte array and store it in the given collection.
	 * @param packaged
	 * @param destination
	 */
	public void readEnchantments(byte[] packaged, Collection<LeveledEnchantment> destination) {
		
	}
	
	/**
	 * Write all the enchantments in the given source to the configuration section.
	 * @param section - the section to write to.
	 * @param source - source of enchantments to write.
	 */
	public void writeEnchantments(ConfigurationSection section, Collection<LeveledEnchantment> source) {
		section = ConfigurationUtils.resetSection(section);
		
		// Write all the enchantments
		for (LeveledEnchantment leveled : source) {
			section.set(leveled.getEnchantment().getName(), leveled.getLevel());
		}
	}
}
