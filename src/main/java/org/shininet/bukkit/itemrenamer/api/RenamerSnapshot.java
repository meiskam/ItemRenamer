package org.shininet.bukkit.itemrenamer.api;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.google.common.base.Function;

/**
 * Represents the snapshot of an inventory that is being renamed and sent to a player.
 * @author Kristian
 */
public class RenamerSnapshot implements Iterable<ItemStack> {
	// The items that are being renamed
	private ItemStack[] items;
	private NbtCompound[] customData;
	
	// The inventory view of what we are partly renaming
	private InventoryView inventoryView;
	private int offset;
	
	/**
	 * Construct a new snapshot.
	 * @param items - the items to rename.
	 * @param inventoryView - the inventory view.
	 * @param offset - the snapshot offset.
 	 */
	public RenamerSnapshot(ItemStack[] items, InventoryView inventoryView, int offset) {
		this.items = items;
		this.inventoryView = inventoryView;
		this.offset = offset;
	}

	/**
	 * Retrieve the number of slots in the inventory, empty or not.
	 * @return The number of slots.
	 */
	public int size() {
		return items.length;
	}
	
	/**
	 * Retrieve the NBT tag that will be merged with the final ItemStack when the process is done.
	 * <p>
	 * It may be necessary to set your custom NBT tags here, as users of ItemMeta will normally overwrite them.
	 * @param index - index of the slot.
	 * @return The custom NBT of this slot.
	 */
	public NbtCompound getCustomData(int index) {
		return getCustomData(index, true);
	}

	/**
	 * Retrieve the NBT tag that will be merged with the final ItemStack when the process is done.
	 * <p>
	 * It may be necessary to set your custom NBT tags here, as users of ItemMeta will normally overwrite them.
	 * @param index - index of the slot.
	 * @param createNew - whether or not to create a new TAG if it doesn't exist.
	 * @return The custom NBT of this slot.
	 */
	public NbtCompound getCustomData(int index, boolean createNew) {
		// Ensure the array exists
		if (customData == null) {
			if (createNew)
				customData = new NbtCompound[items.length];
			else
				return null;
		}
		// Then make sure the TAG itself exists
		if (customData[index] == null && createNew) {
			customData[index] = NbtFactory.ofCompound("tag");
		}
		return customData[index];
	}
	
	/**
	 * Retrieve the content of a slot in the snapshot.
	 * @param index - the slot index.
	 * @return The content of the slot, which is either NULL or an item.
	 */
	public ItemStack getSlot(int index) {
		return items[index];
	}
	
	/**
	 * Set the content of a slot in the snapshot.
	 * @param index - the slot index.
	 * @param slot - the new content, which is either an item stack or NULL.
	 */
	public void setSlot(int index, ItemStack slot) {
		items[index] = slot;
	}
	
	/**
	 * Apply a given action to every stack in the snapshot.
	 * @param action - the action to apply.
	 */
	public void apply(Function<ItemStack, ItemStack> action) {
		for (int i = 0; i < items.length; i++) {
			items[i] = action.apply(items[i]);
		}
	}
	
	/**
	 * Retrieve the index of the first slot in the inventory view that matches this snapshot.
	 * <p>
	 * This is negative if we're dealing with the cursor item.
	 * @return The snapshot offset.
	 */
	public int getInventoryOffset() {
		return offset;
	}
	
	/**
	 * Retrieve the parent inventory view of this snapshot.
	 * @return The associated inventory view.
	 */
	public InventoryView getInventoryView() {
		return inventoryView;
	}
	
	/**
	 * Retrieve a fixed-sized view of the current snapshot as a list.
	 * <p>
	 * Changes to the list will be reflected in the snapshot.
	 * @return The snapshot as a list.
	 */
	public List<ItemStack> asList() {
		return Arrays.asList(items);
	}
	
	@Override
	public Iterator<ItemStack> iterator() {
		return asList().iterator();
	}
}
