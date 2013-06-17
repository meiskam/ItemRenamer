package org.shininet.bukkit.itemrenamer.wrappers;

import java.util.Deque;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.base.Objects;

public class LeveledEnchantment {
	private final Enchantment enchantment;
	private final int level;
	
	private static Map<String, Enchantment> byName = Maps.newHashMap();
	
	static {
		for (Enchantment enchantment : Enchantment.values()) {
			byName.put(enchantment.getName(), enchantment);
		}
		
		// Register the rest manually
		byName.put("POWER", Enchantment.ARROW_DAMAGE);
		byName.put("PUNCH", Enchantment.ARROW_KNOCKBACK);
		byName.put("INFINITY", Enchantment.ARROW_INFINITE);
		byName.put("FLAME", Enchantment.ARROW_FIRE);
		byName.put("SHARPNESS", Enchantment.DAMAGE_ALL);
		byName.put("BANE_OF_ARTHROPODS", Enchantment.DAMAGE_ARTHROPODS);
		byName.put("SMITE", Enchantment.DAMAGE_UNDEAD);
		byName.put("EFFICIENCY", Enchantment.DIG_SPEED);
		byName.put("UNBREAKING", Enchantment.DURABILITY);
		byName.put("FIRE_ASPECT", Enchantment.FIRE_ASPECT);
		byName.put("KNOCKBACK", Enchantment.KNOCKBACK);
		byName.put("FORTUNE", Enchantment.LOOT_BONUS_BLOCKS);
		byName.put("LOOTING", Enchantment.LOOT_BONUS_MOBS);
		byName.put("RESPIRATION", Enchantment.OXYGEN);
		byName.put("PROTECTION", Enchantment.PROTECTION_ENVIRONMENTAL);
		byName.put("BLAST_PROTECTION", Enchantment.PROTECTION_EXPLOSIONS);
		byName.put("FEATHER_FALLING", Enchantment.PROTECTION_FALL);
		byName.put("FIRE_PROTECTION", Enchantment.PROTECTION_FIRE);
		byName.put("PROJECTILE_PROTECTION", Enchantment.PROTECTION_PROJECTILE);
		byName.put("SILK_TOUCH", Enchantment.SILK_TOUCH);
		byName.put("THORNS", Enchantment.THORNS);
		byName.put("AQUA_AFFINITY", Enchantment.WATER_WORKER);
	}
	
	/**
	 * Represents an enchantment with a associated level.
	 * @param enchantment - the enchantment.
	 * @param level - the associated level.
	 */
	public LeveledEnchantment(Enchantment enchantment, int level) {
		if (enchantment == null)
			throw new IllegalArgumentException("enchantment cannot be NULL.");
		this.enchantment = enchantment;
		this.level = level;
	}

	/**
	 * Retrieve the associated enchantment.
	 * @return The associated enchantment.
	 */
	public Enchantment getEnchantment() {
		return enchantment;
	}

	/**
	 * Retrieve the level of the enchantment.
	 * @return The level of the enchantment.
	 */
	public int getLevel() {
		return level;
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(enchantment, level);
	}
	
	@Override
	public boolean equals(Object object){
		if (object == this)
			return true;
		if (object instanceof LeveledEnchantment) {
			LeveledEnchantment that = (LeveledEnchantment) object;
			return this.enchantment == that.enchantment &&
				   this.level == that.level;
		}
		return false;
	}

	@Override
	public String toString() {
		return enchantment + " " + level;
	}
	
	/**
	 * Parse a leveled enchantment from a given text.
	 * @param value - space-delimited queue.
	 * @return The parsed enchantment, or NULL if the input cannot be parsed.
	 */
	public static LeveledEnchantment parse(Deque<String> args) {
		StringBuilder search = new StringBuilder();
		
		// Take elements until we are done
		while (args.peek() != null) {
			if (search.length() > 0)
				search.append("_");
			search.append(args.poll().toUpperCase());
			
			// And we're done
			if (byName.containsKey(search.toString())) {
				break;
			}
		}
		
		// Must specify a level
		try {
			int level = Integer.parseInt(Joiner.on(" ").join(args).trim());
			
			return new LeveledEnchantment(byName.get(search.toString()), level);
		} catch (NumberFormatException e) {
			// Either not a valid level, or not specified
			return null;
		}
	}
}
