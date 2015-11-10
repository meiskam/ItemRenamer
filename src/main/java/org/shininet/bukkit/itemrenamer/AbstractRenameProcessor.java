package org.shininet.bukkit.itemrenamer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.api.RenamerSnapshot;
import org.shininet.bukkit.itemrenamer.meta.CompoundStore;
import org.shininet.bukkit.itemrenamer.utils.StackUtils;

import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.google.common.base.Objects;

public abstract class AbstractRenameProcessor {
	/**
	 * An item stack serializer/deserializer.
	 */
	protected SerializeItemStack itemSerializer;
	
	/**
	 * An item stack representing AIR.
	 */
	protected NbtCompound compoundAir;
	
	/**
	 * Represents the key of the custom NBT tag that will store the original item stack.
	 */
	protected String compoundKey;

	/**
	 * Construct a new rename processor.
	 * @param compoundKey - key of the custom NBT tag that will store the original item stack.
	 */
	public AbstractRenameProcessor(String compoundKey) {
		this(RenameProcessor.createSerializer(), compoundKey);
	}
	
	/**
	 * Construct a new rename processor.
	 * @param itemSerializer - the item stack serializer/deserializer.
	 * @param compoundKey - key of the custom NBT tag that will store the original item stack.
	 */
	public AbstractRenameProcessor(SerializeItemStack itemSerializer, String compoundKey) {
		super();
		this.itemSerializer = itemSerializer;
		this.compoundAir = itemSerializer.save(new ItemStack(Material.AIR, 0));
		this.compoundKey = compoundKey;

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
		return process(player, player.getOpenInventory(), input, offset);
	}

	/**
	 * Apply a custom modification to the given item stacks.
	 * @param player - the player.
	 * @param InventoyView - the current inventory view.
	 * @param input - the item to rename.
	 * @param slotIndex - index of the item in the inventory view we want to rename.
	 * @return The processed item stack.
	 */
	public ItemStack process(Player player, InventoryView view, ItemStack input, int offset) {
		ItemStack[] temporary = new ItemStack[] { input };
		return process(player, view, temporary, offset)[0];
	}

	/**
	 * Apply a custom modification to the given item stacks.
	 * @param player - the recieving player.
	 * @param input - the item stack to process.
	 * @return The processed item stacks.
	 */
	public ItemStack[] process(Player player, ItemStack[] input) {
		if (input != null) {
			return process(player, player.getOpenInventory(), input, 0);
		}
		return null;
	}

	/**
	 * Apply a custom modification to the given item stacks.
	 * @param view - the current inventory view.
	 * @param input - the item to process.
	 * @param offset - the current offset.
	 * @return The processed item.
	 */
	public ItemStack[] process(Player player, InventoryView view, ItemStack[] input, int offset) {
		final RenamerSnapshot snapshot = new RenamerSnapshot(input, view, offset);
		final NbtCompound[] original = getCompounds(input);
		
		// Just return it - for chaining
		processSnapshot(player, snapshot);
		
		// Save the original NBT tag
		// Add a simple marker allowing us to restore the item stack
		for (int i = 0; i < original.length; i++) {
			ItemStack converted = snapshot.getSlot(i);
			
			// Ensure that we are dealing with a CraftItemStack
			if (isNotEmpty(converted)) {
				converted = StackUtils.getCraftItemStack(converted);
			} else {
				if (!Objects.equal(original[i], compoundAir)) {
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
				converted = CompoundStore.getNativeStore(converted, compoundKey).saveCompound(original[i]);
			input[i] = converted;	
		}
		return input;
	}

	/**
	 * Retrieve the respective NBT compounds of each item stack in the array.
	 * @param input - the items.
	 * @return The compounds.
	 */
	private NbtCompound[] getCompounds(ItemStack[] input) {
		NbtCompound[] original = new NbtCompound[input.length];
		
		for (int i = 0; i < original.length; i++) {
			if (input[i] != null) {
				original[i] = itemSerializer.save(input[i]);
			} else {
				original[i] = compoundAir;
			}
		}
		return original;
	}

	/**
	 * Apply a custom modification to a set of item stacks.
	 * @param player - the current player.
	 * @param snapshot - the items to modify,
	 * @return The modified items.
	 */
	protected abstract void processSnapshot(Player player, RenamerSnapshot snapshot);
	
	/**
	 * Determine if a givne item stack is empty nor not.
	 * @param stack - the stack to test.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	private boolean isNotEmpty(ItemStack stack) {
		return stack != null && stack.getType() != Material.AIR;
	}
	
	/**
	 * Determine if a given item stack (serialized in a compound) has changed.
	 * @param savedStack - the serialized item stack.
	 * @param currentStack - the stack to compare to.
	 * @param currentTag - the current TAG compound.
	 * @return TRUE if it has changed, FALSE otherwise.
	 */
	protected boolean hasChanged(NbtCompound savedStack, ItemStack currentStack, NbtCompound currentTag) {
		if (savedStack.getShort("id") != currentStack.getTypeId())
		//if (savedStack.getValue("Type") != currentStack.getType())
			return true;
		if (savedStack.getShort("damage") != currentStack.getDurability())
			return true;
		return !Objects.equal(savedStack.getObject("tag"), currentTag);
	}

	/**
	 * Merge the source compound into the destination tag, storing the result in the destination stack.
	 * @param source - the source compound.
	 * @param destinationTag - the destination tag.
	 * @param destinationStack - the destination stack.
	 */
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
			NbtCompound saved = CompoundStore.getNativeStore(input, compoundKey).loadCompound();
	
			// See if there is something to restore
			if (saved != null) {
				itemSerializer.loadInto(input, saved, true);
				return true;
			}
		}
		return false;
	}
}