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
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;

import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.google.common.collect.Lists;

public class RenameProcessor {
	/**
	 * Storage of the original ItemMeta.
	 */
	private static final String KEY_ORIGINAL = "com.comphenix.original";
	private final ItemRenamerConfiguration config;
	
	// This should really have been in ProtocolLib
	private static StructureModifier<Object> itemStackModifier;
	
	// Vault
	private final Chat chat;
	
	public RenameProcessor(ItemRenamerConfiguration config, Chat chat) {
		this.config = config;
		this.chat = chat;
	}

	private void packName(ItemMeta itemMeta, RenameRule rule) {
		if (rule.getName() != null) {
			itemMeta.setDisplayName(ChatColor.RESET + 
					ChatColor.translateAlternateColorCodes('&', rule.getName()) + ChatColor.RESET);
		}
	}
	
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

	public ItemStack process(Player player, ItemStack input) {
		return process(getPack(player), input);
	}
	
	private ItemStack process(String pack, ItemStack input) {
		RenameConfiguration renameConfig = config.getRenameConfig();
		
		// The item stack has already been cloned in the packet
		if (input != null && pack != null && renameConfig.hasPack(pack)) {
			RenameRule exactRule = renameConfig.getExact(pack).getRule(input);
			
			// Exact item stacks has priority
			if (exactRule != null) {
				return processRule(input, exactRule);
			}
			
			// Next look at ranged rename rules
			DamageLookup lookup = renameConfig.getLookup(pack, input.getTypeId());

			if (lookup != null) {
				RenameRule rule = lookup.getRule(input.getDurability());
				
				if (rule != null) {
					return processRule(input, rule);
				}
			}
		}
		
		// Just return it - for chaining
		return input;
	}

	/**
	 * Rename or append lore to the given item stack.
	 * @param input - the item stack.
	 * @param rule - the rename rule to apply.
	 * @return The renamed item stack.
	 */
	private ItemStack processRule(ItemStack input, RenameRule rule) {
		ItemMeta itemMeta = input.getItemMeta();
		
		// May have been set by a plugin or a player
		if ((itemMeta.hasDisplayName()) || (itemMeta.hasLore())) 
			return input;
		NbtCompound original = getCompound(input);
		
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
		
		// Remove or add enchantments
		for (LeveledEnchantment removed : rule.getDechantments()) {
			input = removed.getEnchanter().disenchant(input);
		}
		for (LeveledEnchantment added : rule.getEnchantments()) {
			input = added.getEnchanter().enchant(input);
		}
		
		// Add a simple marker allowing us to restore the ItemMeta
		getCompound(input).put(KEY_ORIGINAL, original);
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
			NbtCompound data = getCompound(input);
			
			// Check for our marker
			if (data.containsKey(KEY_ORIGINAL)) {
				saveNbt(input, data.getCompound(KEY_ORIGINAL));
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Save the given compound as TAG in the item stack,
	 * @param stack - the source item stack.
	 * @param compound - the compound to save.
	 */
	private void saveNbt(ItemStack stack, NbtCompound compound) {
		Object nmsStack = MinecraftReflection.getMinecraftItemStack(stack);
		
		// Reuse reflection machinery
		if (itemStackModifier == null) {
			itemStackModifier = new StructureModifier<Object>(nmsStack.getClass(), Object.class, false);
		}
		StructureModifier<NbtBase<?>> modifier = itemStackModifier.
				withTarget(nmsStack).
				withType(MinecraftReflection.getNBTBaseClass(), 
						 BukkitConverters.getNbtConverter());
		modifier.write(0, compound);
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
	private String getPack(Player player) {
		if (chat != null) {
			String pack = chat.getPlayerInfoString(player, "itempack", null);

			// Use this pack instead
			if (pack != null && pack.length() > 0)
				return pack;
		}
		return config.getWorldPack(player.getWorld().getName());
	}
	
	private NbtCompound getCompound(ItemStack stack) {
		// It should have been a compound in the API ...
		return NbtFactory.asCompound(NbtFactory.fromItemTag(stack));
	}
}
