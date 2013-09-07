package org.shininet.bukkit.itemrenamer;

import java.util.List;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.shininet.bukkit.itemrenamer.configuration.DamageLookup;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;
import org.shininet.bukkit.itemrenamer.meta.CompoundStore;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.google.common.collect.Lists;

public class RenameProcessor {
	private final ItemRenamerConfiguration config;
	
	// Vault
	private final Chat chat;
	
	// Serialize stacks
	private SerializeItemStack serializeStacks = new SerializeItemStack();
	
	/**
	 * Construct a new rename processor.
	 * <p>
	 * The Vault chat layer is used to retrieve per-player configuration.
	 * @param config - the current configuration.
	 * @param chat - the current Vault chat abstraction layer.
	 */
	RenameProcessor(ItemRenamerConfiguration config, Chat chat) {
		this.config = config;
		this.chat = chat;
	}

	/**
	 * Process the name on the given item meta.
	 * @param itemMeta - the item meta.
	 * @param rule - the name rule to apply.
	 */
	private void packName(ItemMeta itemMeta, RenameRule rule) {
		if (rule.getName() != null) {
			itemMeta.setDisplayName(ChatColor.RESET + 
					ChatColor.translateAlternateColorCodes('&', rule.getName()) + ChatColor.RESET);
		}
	}
	
	/**
	 * Process lore on the given item meta.
	 * @param itemMeta - the item meta.
	 * @param rule - the lore rule to apply.
	 */
	private void packLore(ItemMeta itemMeta, RenameRule rule) {
		// Don't process empty rules
		if (rule.getLoreSections().size() == 0)
			return;
		
		List<String> output = Lists.newArrayList(rule.getLoreSections());
		
		// Translate color codes as well
		for (int i = 0; i < output.size(); i++) {
			output.set(i, ChatColor.translateAlternateColorCodes('&', output.get(i)) + ChatColor.RESET);
		}
		itemMeta.setLore(output);
	}

	/**
	 * Apply a player's associated rename rules to a given stack.
	 * <p>
	 * The rename rules are referenced by the world the player is in or by the player itself.
	 * @param player - the player.
	 * @param input - the item to rename.
	 * @return The processed item stack.
	 */
	public ItemStack process(Player player, ItemStack input) {
		return process(getPack(player), input);
	}
	
	/**
	 * Apply rename rules to a given item stack.
	 * @param pack - the current rename package.
	 * @param input - the item to process.
	 * @return The processed item.
	 */
	public ItemStack process(String pack, ItemStack input) {
		RenameRule rule = getRule(pack, input);
		
		// The item stack has already been cloned in the packet
		if (!RenameRule.isIdentity(rule)) {
			return processRule(input, rule);
		}
		// Just return it - for chaining
		return input;
	}
	
	/**
	 * Retrieve the associated rule for the given pack and item stack.
	 * <p>
	 * This will first look for exact rules, then damage lookup rules.
	 * @param pack - the rename pack.
	 * @param input - the item stack.
	 * @return The associated rule, or NULL if not found.
	 */
	public RenameRule getRule(String pack, ItemStack input) {
		// They have no rule
		if (pack == null || input == null)
			return null;
		RenameConfiguration renameConfig = config.getRenameConfig();
		
		// Make sure there is an associated pack
		if (renameConfig.hasPack(pack)) {
			RenameRule exactRule = renameConfig.getExact(pack).getRule(input);
			
			// Exact item stacks has priority
			if (exactRule != null) {
				return exactRule;
			}
			
			// Next look at ranged rename rules
			DamageLookup lookup = renameConfig.getLookup(pack, input.getTypeId());

			if (lookup != null) {
				return lookup.getRule(input.getDurability());
			}
		}
		return null;
	}

	/**
	 * Rename or append lore to the given item stack.
	 * @param input - the item stack.
	 * @param rule - the rename rule to apply.
	 * @return The renamed item stack.
	 */
	public ItemStack processRule(ItemStack input, RenameRule rule) {
		ItemMeta itemMeta = input.getItemMeta();
		
		// May have been set by a plugin or a player
		if (rule.isSkippingCustomNamed() && (itemMeta.hasDisplayName()) || (itemMeta.hasLore())) 
			return input;
		NbtCompound original = serializeStacks.save(input);
		
		// Fix a client bug
		if (itemMeta instanceof BookMeta) {
			BookMeta book = (BookMeta) itemMeta;
			
			// Create the pages NBT tag
			if (book.getPageCount() == 0) {
				book.setPages("");
			}
		}
		
		packName(itemMeta, rule);
		packLore(itemMeta, rule);
		input.setItemMeta(itemMeta);
	
		// Add a simple marker allowing us to restore the item stack
		input = CompoundStore.getNativeStore(input).saveCompound(original);
		
		// Remove or add enchantments
		for (LeveledEnchantment removed : rule.getDechantments()) {
			input = removed.getEnchanter().disenchant(input);
		}
		for (LeveledEnchantment added : rule.getEnchantments()) {
			input = added.getEnchanter().enchant(input);
		}

		return input;
	}

	/**
	 * Undo a item rename, or leave as is.
	 * @param input - the stack to undo.
	 * @return TRUE if we removed the rename and lore, FALSE otherwise.
	 */
	public boolean unprocess(ItemStack input) {
		if (input != null) {
			// This will only be invoked for creative players
			NbtCompound saved = CompoundStore.getNativeStore(input).loadCompound();

			// See if there is something to restore
			if (saved != null) {
				serializeStacks.loadInto(input, saved);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Apply rename rules to an array of item stacks.
	 * @param player - the recieving player.
	 * @param input - the item stack to process.
	 * @return The processed item stacks.
	 */
	public ItemStack[] process(Player player, ItemStack[] input) {
		String pack = getPack(player);
		
		if (input != null) {
            for (int i = 0; i < input.length; i++) {
            	input[i] = process(pack, input[i]);
            }
		}
		return input;
	}
	
	/**
	 * Retrieve the associated rename pack for a given player.
	 * <p>
	 * This may depend on the current world the player is located in.
	 * @param player - the player to look up.
	 * @return The name of the rename pack.
	 */
	public String getPack(Player player) {
		if (chat != null) {
			String pack = chat.getPlayerInfoString(player, "itempack", null);

			// Use this pack instead
			if (pack != null && pack.length() > 0)
				return pack;
		}
		return config.getEffectiveWorldPack(player.getWorld().getName());
	}
}
