package org.shininet.bukkit.itemrenamer.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class EquipmentHolder implements InventoryHolder {
	private final LivingEntity entity;
	private final Inventory inventory;
	
	EquipmentHolder(LivingEntity entity, Inventory inventory) {
		this.entity = entity;
		this.inventory = inventory;
	}

	/**
	 * Retrieve the living entity.
	 * @return The living entity.
	 */
	public LivingEntity getEntity() {
		return entity;
	}
		
	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
