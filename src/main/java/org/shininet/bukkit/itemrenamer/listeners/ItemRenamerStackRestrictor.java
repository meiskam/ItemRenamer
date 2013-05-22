package org.shininet.bukkit.itemrenamer.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.RenameProcessor;

public class ItemRenamerStackRestrictor implements Listener {
	private final RenameProcessor processor;
	
	public ItemRenamerStackRestrictor(RenameProcessor processor) {
		this.processor = processor;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onInventoryClickEvent(InventoryClickEvent event) {
		ItemStack current = event.getCurrentItem();
		ItemStack cursor = event.getCursor();

		//TODO: Handle shift-click and right-click
		// http://forums.bukkit.org/threads/item-stack-size.103488/#post-1359513
		
		// We want to prevent the player from INCREASING invalid stacks
		if (isEmpty(current) && isEmpty(cursor)) {
			// They should be able to split "too large" stacks, or move them
			// around.
			if (cursor.getAmount() == 0 || event.isRightClick()) {
				return;
			} else if (isNotValid((Player) event.getWhoClicked(), current, cursor)) {
				// But never increase a stack above their personal limit
				event.setCancelled(true);
			}
		}
	}
	
	private boolean isEmpty(ItemStack stack) {
		return stack == null || stack.getType() == Material.AIR;
	}

	private boolean isNotValid(Player player, ItemStack current, ItemStack cursor) {
		ItemStack modCurrent = processor.process(player, current.clone());
		ItemStack modCursor = processor.process(player, cursor.clone());
		
		// Disregard the count
		modCurrent.setAmount(1);
		modCursor.setAmount(1);
		return !modCurrent.equals(modCursor);
	}
}
