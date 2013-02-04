package org.shininet.bukkit.itemrenamer.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.ItemRenamer;

public class ItemRenamerStackRestrictor implements Listener {
	private ItemRenamer renamer;
	
	public ItemRenamerStackRestrictor(ItemRenamer renamer) {
		this.renamer = renamer;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onInventoryClickEvent(InventoryClickEvent event) {
		ItemStack current = event.getCurrentItem();
		ItemStack cursor = event.getCursor();

		//TODO: Handle shift-click and right-click
		// http://forums.bukkit.org/threads/item-stack-size.103488/#post-1359513
		
		// We want to prevent the player from INCREASING invalid stacks
		if (current != null && cursor != null) {
			// They should be able to split "too large" stacks, or move them
			// around.
			if (cursor.getAmount() == 0 || event.isRightClick()) {
				return;
			} else if (!isValid(event.getWhoClicked().getWorld(), current, cursor)) {
				// But never increase a stack above their personal limit
				event.setCancelled(true);
			}
		}
	}

	private boolean isValid(World world, ItemStack current, ItemStack cursor) {
		renamer.process(world.getName(), current);
		renamer.process(world.getName(), cursor);
		
		return !current.equals(cursor);
	}
}
