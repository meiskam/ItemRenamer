package org.shininet.bukkit.itemrenamer;

import java.util.List;
import net.milkbowl.vault.chat.Chat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.SerializeItemStack.StackField;
import org.shininet.bukkit.itemrenamer.api.ItemsListener;
import org.shininet.bukkit.itemrenamer.api.RenamerSnapshot;
import org.shininet.bukkit.itemrenamer.configuration.DamageLookup;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;
import org.shininet.bukkit.itemrenamer.meta.NiceBookMeta;
import org.shininet.bukkit.itemrenamer.meta.NiceItemMeta;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;

import com.google.common.collect.Lists;

public class RenameProcessor extends AbstractRenameProcessor {
	/**
	 * Storage of the original ItemMeta.
	 */
	private static final String KEY_ORIGINAL = "com.comphenix.original";
	
	// Configuration
	private final ItemRenamerConfiguration config;
	
	// Vault
	private final Chat chat;
	
	// Listeners
	private RenameListenerManager listenerMananger;
	
	/**
	 * Construct a new rename processor.
	 * <p>
	 * The Vault chat layer is used to retrieve per-player configuration.
	 * @param listenerManager - the listener manager.
	 * @param config - the current configuration.
	 * @param chat - the current Vault chat abstraction layer.
	 */
	RenameProcessor(RenameListenerManager listenerMananger, ItemRenamerConfiguration config, Chat chat) {
		super(KEY_ORIGINAL);
		this.listenerMananger = listenerMananger;
		this.config = config;
		this.chat = chat;
	}
	
	/**
	 * Create a item stack serializer that doesn't preserve the count field.
	 * @return The serializer.
	 */
	public static SerializeItemStack createSerializer() {
		SerializeItemStack serializer = new SerializeItemStack();
		serializer.removeField(StackField.COUNT);
		return serializer;
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
		
		// Fix a client bug
		if (niceMeta instanceof NiceBookMeta) {
			NiceBookMeta bookMeta = (NiceBookMeta) niceMeta;

			// Create the pages NBT tag
			if (bookMeta.getPageCount() == 0) {
				bookMeta.setPages("");
			}
		}
		
		// No need to rename these types
		if (rule == null)
			return stack;
		if (rule.isSkippingCustomNamed() && (niceMeta.hasDisplayName()) || (niceMeta.hasLore())) 
			return stack;
		
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
	
	@Override
	protected void processSnapshot(Player player, RenamerSnapshot snapshot) {		
		final RenameRule[] rules = new RenameRule[snapshot.size()];
		final String pack = getPack(player);
		
		// Retrieve the rename rule for each item stack
		for (int i = 0; i < rules.length; i++) {
			rules[i] = getRule(pack, snapshot.getSlot(i));
		}
	
		listenerMananger.setRenamerListener(new ItemsListener() {
			@Override
			public void onItemsSending(Player player, RenamerSnapshot snapshot) {
				for (int i = 0; i < rules.length; i++) {
					ItemStack input = snapshot.getSlot(i);
					RenameRule rule = rules[i];
					
					if (input != null) {
						snapshot.setSlot(i, processRule(input, rule));
					}
				}
			}
		});
		
		// Invoke other plugins
		listenerMananger.invokeListeners(player, snapshot);
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
