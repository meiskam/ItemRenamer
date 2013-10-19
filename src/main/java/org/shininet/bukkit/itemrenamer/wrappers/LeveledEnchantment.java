package org.shininet.bukkit.itemrenamer.wrappers;

import java.util.Deque;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.shininet.bukkit.itemrenamer.enchants.HideAttributesEnchanter;
import org.shininet.bukkit.itemrenamer.enchants.HideDurabilityEnchanter;
import org.shininet.bukkit.itemrenamer.enchants.VanillaEnchanter;
import org.shininet.bukkit.itemrenamer.enchants.GlowEnchanter;
import org.shininet.bukkit.itemrenamer.enchants.Enchanter;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.base.Objects;

public class LeveledEnchantment {
	public enum CustomEnchantment {
		/**
		 * No custom enchantment. 
		 */
		VANILLA,
		
		/**
		 * Represents an enchantment that preserves the enchantment glow, but does not show up client side.
		 */
		GLOW,
		
		/**
		 * Represents an enchantment that removes all the attribute lines in 1.6.1 and 1.6.2.
		 */
		NO_ATTRIBUTES,
		
		
		/**
		 * Represents an enchanter that removes any visible durability on the item.
		 */
		NO_DURABILITY;
		
		/**
		 * Retrieve the parsed custom enchantment, or NULL if not found.
		 * @param name - the value to parse.
		 * @return The parsed custom enchantment.
		 */
		public static CustomEnchantment parse(String name) {
			try {
				return CustomEnchantment.valueOf(name);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}
	
	private final CustomEnchantment custom;
	private final Enchantment enchantment;
	private final int level;
	
	// The current enchanter
	private transient Enchanter enchanter;
	
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
		
		this.custom = CustomEnchantment.VANILLA;
		this.enchantment = enchantment;
		this.level = level;
	}

	/**
	 * Represents a custom enchantment with a associated level.
	 * @param custom - the custom enchantment.
	 * @param level - the associated level.
	 */
	public LeveledEnchantment(CustomEnchantment custom, int level) {
		if (custom == null)
			throw new IllegalArgumentException("custom cannot be NULL.");
		if (custom == CustomEnchantment.VANILLA)
			throw new IllegalArgumentException("custom cannot be VANILLA.");
		
		this.custom = custom;
		this.enchantment = null;
		this.level = level;
	}
	
	/**
	 * Retrieve the associated enchantment.
	 * <p>
	 * May be NULL if this leveled enchantment represents a custom enchantment.
	 * @return The associated enchantment.
	 */
	public Enchantment getEnchantment() {
		return enchantment;
	}

	/**
	 * Retrieve the custom enchantment, or {@link CustomEnchantment#VANILLA} if the LeveledEnchantment
	 * represents a vanilla enchantment.
	 * @return The custom enchantment.
	 */
	public CustomEnchantment getCustom() {
		return custom;
	}
	
	/**
	 * Retrieve an enchanter that applies the current enchantment to items.
	 * @return An enchanter.
	 */
	public Enchanter getEnchanter() {
		if (enchanter == null) {
			if (custom == CustomEnchantment.VANILLA)
				enchanter = new VanillaEnchanter(enchantment, level);
			else if (custom == CustomEnchantment.GLOW)
				enchanter = new GlowEnchanter();
			else if (custom == CustomEnchantment.NO_ATTRIBUTES)
				enchanter = new HideAttributesEnchanter();
			else if (custom == CustomEnchantment.NO_DURABILITY)
				enchanter = new HideDurabilityEnchanter(level);
			else
				throw new IllegalStateException("Invalid custom enchantment: " + custom);
		}
		return enchanter;
	}
	
	/**
	 * Retrieve the level of the enchantment.
	 * @return The level of the enchantment.
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * Determine if the given leveled enchantment is of the same type.
	 * @param other - the other enchantment.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public boolean sameType(LeveledEnchantment other) {
		if (hasCustomEnchantment())
			return other.hasCustomEnchantment() && other.getCustom() == custom;
		else
			return !other.hasCustomEnchantment() && other.getEnchantment() == enchantment;
	}
	
	/**
	 * Determine if this leveled enchantment represents a custom type.
	 * @return TRUE if it does, FALSE otherwise.
	 */
	public boolean hasCustomEnchantment() {
		return custom != null && custom != CustomEnchantment.VANILLA;
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(custom, enchantment, level);
	}
	
	@Override
	public boolean equals(Object object){
		if (object == this)
			return true;
		if (object instanceof LeveledEnchantment) {
			LeveledEnchantment that = (LeveledEnchantment) object;
			return this.custom == that.custom &&
				   this.enchantment == that.enchantment &&
				   this.level == that.level;
		}
		return false;
	}

	@Override
	public String toString() {
		if (custom == CustomEnchantment.VANILLA)
			return enchantment + " " + level;
		else
			return custom + " " + level;
	}
	
	/**
	 * Parse a leveled enchantment from a given text.
	 * @param value - space-delimited queue.
	 * @return The parsed enchantment, or NULL if the input cannot be parsed.
	 */
	public static LeveledEnchantment parse(Deque<String> args) {
		StringBuilder search = new StringBuilder();
		String candidate = "";
		CustomEnchantment parsedCustom = null;
		
		// Take elements until we are done
		while (args.peek() != null) {
			if (search.length() > 0)
				search.append("_");
			search.append(args.poll().toUpperCase());
			candidate = search.toString();
			
			parsedCustom = CustomEnchantment.parse(candidate);
			
			// And we're done
			if (parsedCustom != null)
				break;
			if (byName.containsKey(candidate)) {
				break;
			}
		}
		
		// Must specify a level
		try {
			int level = Integer.parseInt(Joiner.on(" ").join(args).trim());
			
			if (parsedCustom != null)
				return new LeveledEnchantment(parsedCustom, level);
			else
				return new LeveledEnchantment(byName.get(candidate), level);
			
		} catch (NumberFormatException e) {
			// Either not a valid level, or not specified
			return null;
		}
	}
}
