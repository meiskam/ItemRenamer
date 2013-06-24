package org.shininet.bukkit.itemrenamer.serialization;

import java.util.Collection;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.shininet.bukkit.itemrenamer.utils.ConfigurationUtils;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment.CustomEnchantment;

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
			CustomEnchantment custom = CustomEnchantment.parse(entry.getKey());
			
			if (entry.getValue() instanceof Number && (enchantment != null || custom != null)) {
				Number number = (Number) entry.getValue();;
				
				// Store the parsed enchantment
				if (enchantment != null)
					destination.add(new LeveledEnchantment(enchantment, number.intValue()));
				else
					destination.add(new LeveledEnchantment(custom, number.intValue()));		
			} else {
				// Try to be a bit informative
				Bukkit.getLogger().warning("[ItemRenamer] [" + section.getCurrentPath() + "] Invalid value " + 
						entry.getValue() + " for key " + enchantment);
			}
		}
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
			if (leveled.hasCustomEnchantment())
				section.set(leveled.getCustom().name(), leveled.getLevel());
			else
				section.set(leveled.getEnchantment().getName(), leveled.getLevel());
		}
	}
}
