package org.shininet.bukkit.itemrenamer;

import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.utils.CollectionsUtil;

public class SelectedItemTracker {
	// The selected pack and item for each sender
	private final Map<CommandSender, ItemStack> selectedItem = new WeakHashMap<CommandSender, ItemStack>();
	
	// The Bukkit listener
	private Listener bukkitListener;
	
	/**
	 * Retrieve the listener that is registered to interface with the Bukkit event systen.
	 * @return The Bukkit event listener.
 	 */
	public Listener getBukkitListener() {
		if (bukkitListener == null) {
			bukkitListener = new Listener() {
				@EventHandler
				public void onPlayerInventory(InventoryClickEvent e) {
					if (e.getWhoClicked() instanceof Player) {
						Player clicker = (Player) e.getWhoClicked();
						onNewSelected(clicker, clicker.getItemInHand());
					}
				}
				
				@EventHandler
				public void onPlayerItemHeldEvent(PlayerItemHeldEvent e) {
					Player player = e.getPlayer();
					onNewSelected(player, e.getPlayer().getInventory().getItem(e.getNewSlot()));
				}
			};
		}
		return bukkitListener;
	}
	
	/**
	 * Invoked when a player changes the selected item.
	 * @param clicker - the player.
	 * @param changed - the new selected item, or NULL if we deselected.
	 */
	private void onNewSelected(Player clicker, ItemStack changed) {
		ItemStack selected = getSelected(clicker);
		
		if (selected != null && !selected.equals(changed)) {
			ItemStack deselected = deselectCurrent(clicker);
			
			// Inform this player
			if (isValid(deselected)) {
				clicker.sendMessage(ChatColor.GOLD + "Deselected " + deselected);
			}
		}
	}
	
	/**
	 * Retrieve the selected item stack.
	 * @param sender - the command sender.
	 * @return The selected item stack.
	 */
	public ItemStack getSelected(CommandSender sender) {
		return selectedItem.get(sender);
	}
	
	/**
	 * Select the item currently hold by the command sender.
	 * @param sender - the sender.
	 * @return The previously selected item, or NULL if none was previously selected.
	 * @throws CommandErrorException If the sender cannot select an item, or has no selected item.
	 */
	public ItemStack selectCurrent(CommandSender sender) {
		validateSender(sender);
		Player player = (Player) sender;
		ItemStack stack = getItemToSelect(player);
		
		// Ensure that this item is selected
		if (isValid(stack)) {
			return selectedItem.put(sender, stack);
		} else {
			throw new CommandErrorException("Must hold an item to select.");
		}
	}
	
	/**
	 * Determine if the current item can only be matched by an exact lookup.
	 * @return TRUE if it does, FALSE otherwise.
	 */
	public boolean hasExactSelector(CommandSender sender) {
		ItemStack stack = getSelected(sender);
		
		// This is the case if the item has any additional data (NBT) or enchantments
		if (isValid(stack)) {
			return !CollectionsUtil.isEmpty(stack.getEnchantments()) || stack.hasItemMeta();
		}
		return false;
	}
	
	/**
	 * Retrieve the item that will be selected.
	 * @param sender - the sender.
	 * @return The item that will be selected, or NULL if none can be selected.
	 */
	public ItemStack getItemToSelect(CommandSender sender) {
		validateSender(sender);
		return ((Player) sender).getItemInHand();
	}
	
	/**
	 * Deselect any currently selected item.
	 * @param sender - the sender.
	 * @return The previously selected item, or NULL.
	 * @throws CommandErrorException If this sender cannot select anything.
	 */
	public ItemStack deselectCurrent(CommandSender sender) {
		validateSender(sender);
		return selectedItem.remove(sender);
	}
	
	/**
	 * Determine if a given item stack is non-null and not AIR.
	 * @param stack - the stack to test.
	 * @return TRUE if the stack is non-null and non-air, FALSE otherwise.
	 */
	private boolean isValid(ItemStack stack) {
		return stack != null && !Material.AIR.equals(stack.getType());
	}
	
	/**
	 * Throw an exception if the given command sender is not a player.
	 * @param sender - the sender to test.
	 */
	private void validateSender(CommandSender sender) {
		if (!(sender instanceof Player)) 
			throw new CommandErrorException("Select item can only be called by players.");
	}
}
