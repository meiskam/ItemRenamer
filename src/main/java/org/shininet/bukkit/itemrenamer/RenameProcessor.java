package org.shininet.bukkit.itemrenamer;

import java.util.List;
import net.milkbowl.vault.chat.Chat;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.shininet.bukkit.itemrenamer.api.RenamerListener;
import org.shininet.bukkit.itemrenamer.api.RenamerSnapshot;
import org.shininet.bukkit.itemrenamer.configuration.DamageLookup;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;
import org.shininet.bukkit.itemrenamer.meta.CompoundStore;
import org.shininet.bukkit.itemrenamer.meta.NiceItemMeta;
import org.shininet.bukkit.itemrenamer.utils.StackUtils;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;

import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class RenameProcessor {
	private final ItemRenamerConfiguration config;
	
	// Vault
	private final Chat chat;
	
	// Serialize stacks
	private SerializeItemStack serializeStacks = new SerializeItemStack();
	
	// Listeners
	private RenameListenerManager listenerMananger;
	
	// Air NBT
	private NbtCompound AIR = serializeStacks.save(new ItemStack(Material.AIR, 0));
	
	/**
	 * Construct a new rename processor.
	 * <p>
	 * The Vault chat layer is used to retrieve per-player configuration.
	 * @param listenerManager - the listener manager.
	 * @param config - the current configuration.
	 * @param chat - the current Vault chat abstraction layer.
	 */
	RenameProcessor(RenameListenerManager listenerMananger, ItemRenamerConfiguration config, Chat chat) {
		this.listenerMananger = listenerMananger;
		this.config = config;
		this.chat = chat;
	}

	/**
	 * Retrieve the current listener manager.
	 * @return The listener manager.
	 */
	public RenameListenerManager getListenerMananger() {
		return listenerMananger;
	}
	
	/**
	 * Process the name on the given item meta.
	 * @param itemMeta - the item meta.
	 * @param rule - the name rule to apply.
	 */
	private void packName(NiceItemMeta itemMeta, RenameRule rule) {
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
	private void packLore(NiceItemMeta itemMeta, RenameRule rule) {
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
	 * @param slotIndex - the slot index of the item we are changing.
	 * @return The processed item stack.
	 */
	public ItemStack process(Player player, ItemStack input, int offset) {
		ItemStack[] temporary = new ItemStack[] { input };
		return process(player, getPack(player), temporary, offset)[0];
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
			return process(player, pack, input, 0);
		}
		return null;
	}
	
	/**
	 * Apply rename rules to a given item stack.
	 * @param pack - the current rename package.
	 * @param input - the item to process.
	 * @param offset - the current offset.
	 * @return The processed item.
	 */
	private ItemStack[] process(Player player, String pack, ItemStack[] input, int offset) {
		RenameRule[] rules = new RenameRule[input.length];
		
		// Retrieve the rename rule for each item stack
		for (int i = 0; i < rules.length; i++) {
			rules[i] = getRule(pack, input[i]);
		}
		
		// Just return it - for chaining
		return processRules(player, input, rules, offset);
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
	 * @return The renamed item stacks.
	 */
	public ItemStack processRule(ItemStack stack, RenameRule rule) {
		NiceItemMeta niceMeta = NiceItemMeta.fromStack(stack);
		
		// No need to rename these types
		if (rule.isSkippingCustomNamed() && (niceMeta.hasDisplayName()) || (niceMeta.hasLore())) 
			return stack;
		
		// Fix a client bug
		if (stack.getType() == Material.BOOK_AND_QUILL || stack.getType() == Material.WRITTEN_BOOK) {
			ItemMeta itemMeta = stack.getItemMeta();
			BookMeta book = (BookMeta) itemMeta;
			
			// Create the pages NBT tag
			if (book.getPageCount() == 0) {
				book.setPages("");
			}
			stack.setItemMeta(itemMeta);
		}
		
		// Don't overwrite custom NBT tags if we can help it
		packName(niceMeta, rule);
		packLore(niceMeta, rule);
		stack = niceMeta.getStack();
		
		// Remove or add enchantments
		for (LeveledEnchantment removed : rule.getDechantments()) {
			stack = removed.getEnchanter().disenchant(stack);
		}
		for (LeveledEnchantment added : rule.getEnchantments()) {
			stack = added.getEnchanter().enchant(stack);
		}
		return stack;
	}

	/**
	 * Rename or append lore to the given item stack.
	 * @param input - the item stack.
	 * @param rule - the rename rule to apply.
	 * @return The renamed item stacks.
	 */
	private ItemStack[] processRules(Player player, ItemStack[] inputs, final RenameRule[] rules, final int offset) {		
		RenamerSnapshot snapshot = new RenamerSnapshot(inputs, player.getOpenInventory(), offset);

		// Save the original NBT tag
		NbtCompound[] original = new NbtCompound[inputs.length];
		
		for (int i = 0; i < original.length; i++) {
			if (inputs[i] != null) {
				original[i] = serializeStacks.save(inputs[i]);
			} else {
				original[i] = AIR;
			}
		}
		
		listenerMananger.setRenamerListener(new RenamerListener() {
			@Override
			public void onItemsRenaming(Player player, RenamerSnapshot snapshot) {
				for (int i = 0; i < rules.length; i++) {
					ItemStack input = snapshot.getSlot(i);
					RenameRule rule = rules[i];
					
					if (!RenameRule.isIdentity(rule) && input != null) {
						snapshot.setSlot(i, processRule(input, rule));
					}
				}
			}
		});
		
		// Invoke other plugins
		listenerMananger.invokeListeners(player, snapshot);
		
		// Add a simple marker allowing us to restore the item stack
		for (int i = 0; i < original.length; i++) {
			ItemStack converted = snapshot.getSlot(i);
			
			// Ensure that we are dealing with a CraftItemStack
			if (isNotEmpty(converted)) {
				converted = StackUtils.getCraftItemStack(converted);
			} else {
				if (!Objects.equal(original[i], AIR)) {
					throw new IllegalStateException(
						"Attempted to destroy an ItemStack at slot " + i + ": " + converted);
				}
				continue;
			}
			
			NbtCompound extra = snapshot.getCustomData(i, false);
			NbtCompound tag = NbtFactory.asCompound(NbtFactory.fromItemTag(converted));
			
			// Store extra NBT data
			if (extra != null) 
				storeExtra(extra, tag, converted);
			if (hasChanged(original[i], converted, tag)) 
				converted = CompoundStore.getNativeStore(converted).saveCompound(original[i]);
			inputs[i] = converted;	
		}
		return inputs;
	}
	
	private boolean isNotEmpty(ItemStack stack) {
		return stack != null && stack.getType() != Material.AIR;
	}
	
	private boolean hasChanged(NbtCompound savedStack, ItemStack currentStack, NbtCompound currentTag) {
		if (savedStack.getShort("id") != currentStack.getTypeId())
			return true;
		if (savedStack.getByte("count") != currentStack.getAmount())
			return true;
		if (savedStack.getShort("damage") != currentStack.getDurability())
			return true;
		return !Objects.equal(savedStack.getObject("tag"), currentTag);
	}

	private void storeExtra(NbtCompound source, NbtCompound destinationTag, ItemStack destinationStack) {
		// Overwrite parts of the NBT tag
		for (NbtBase<?> base : source)  {
			destinationTag.put(base);
		}
		NbtFactory.setItemTag(destinationStack, destinationTag);
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
