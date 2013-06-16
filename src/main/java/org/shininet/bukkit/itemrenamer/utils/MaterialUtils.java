package org.shininet.bukkit.itemrenamer.utils;

import java.util.Set;

import org.bukkit.Material;
import com.google.common.collect.Sets;

public class MaterialUtils {
	// Don't use EnumSet - may not work that well with MCPC+
	private static Set<Material> ARMOR = Sets.newHashSet();
	private static Set<Material> TOOL = Sets.newHashSet();
	
	// Invoked to initialize the armor and weapon lookups
	static {
		for (Material material : Material.values()) {
			String name = material.name();
			
			if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGING") || name.contains("BOOTS")) {
				ARMOR.add(material);
			} else if (name.contains("AXE") || name.contains("HOE") || name.contains("PICKAXE") || name.contains("SPADE") || name.contains("SWORD")) {
				TOOL.add(material);
			}
		}
	}
	
	/**
	 * Determine if the given material represents armor.
	 * @param type - the type to check.
	 * @return TRUE it if is either, FALSE otherwise.
	 */
	public static boolean isArmor(Material type) {
		return ARMOR.contains(type);
	}
	
	/**
	 * Determine if the given material represents a tool.
	 * @param type - the type to check.
	 * @return TRUE it if is either, FALSE otherwise.
	 */
	private static boolean isTool(Material type) {
		return TOOL.contains(type);
	}

	/**
	 * Determine if the given type represents an armor or a tool.
	 * @param type - the type to check.
	 * @return TRUE if it does, FALSE otherwise.
	 */
	public static boolean isArmorTool(Material type) {
		return isArmor(type) || isTool(type);
	}
}
