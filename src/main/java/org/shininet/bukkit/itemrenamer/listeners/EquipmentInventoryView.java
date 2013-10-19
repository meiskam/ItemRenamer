package org.shininet.bukkit.itemrenamer.listeners;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

class EquipmentInventoryView extends InventoryView {
	private final Inventory topInventory;
	private final Inventory bottomInventory;
	private final Player player;
	
	EquipmentInventoryView(Inventory topInventory, Inventory bottomInventory, Player player) {
		this.topInventory = topInventory;
		this.bottomInventory = bottomInventory;
		this.player = player;
	}

	@Override
	public Inventory getTopInventory() {
		return topInventory;
	}
	
	@Override
	public Inventory getBottomInventory() {
		return bottomInventory;
	}

	@Override
	public HumanEntity getPlayer() {
		return player;
	}

	@Override
	public InventoryType getType() {
		// Intentionally breaking specifications here ...
		return InventoryType.PLAYER;
	}	
}
