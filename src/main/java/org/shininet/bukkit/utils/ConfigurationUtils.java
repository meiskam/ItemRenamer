package org.shininet.bukkit.utils;

import java.lang.reflect.Field;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import com.comphenix.protocol.reflect.FieldUtils;

public class ConfigurationUtils {
	// Group Manager sucks
	private static Field internalMap;
	
	/**
	 * Retrieve a configuration section from a given parent section.
	 * @param parent - the parent section.
	 * @param key - the key of the element to retrieve.
	 * @return The configuration section, or NULL.
	 */
	public static ConfigurationSection getSection(ConfigurationSection parent, String key) {
		ConfigurationSection result = parent.getConfigurationSection(key);
		
		// What the hell?
		if (result == parent) {
			if (internalMap == null)
				internalMap = FieldUtils.getField(result.getClass(), "map", true);
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) internalMap.get(parent);
				Object raw = map != null ? map.get(key) : null;
				
				// Neat
				if (raw instanceof ConfigurationSection)
					return (ConfigurationSection) raw;
			} catch (Exception e) {
				throw new RuntimeException("GroupMananger hack failed!", e);
			}
			// Failure
			return null;
		}
		return result;
	}
}
