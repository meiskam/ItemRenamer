package org.shininet.bukkit.itemrenamer.meta;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.utils.StackUtils;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Represents an ItemMeta that will not remove custom NBT tags.
 * @author Kristian
 */
public class NiceItemMeta {
	protected ItemStack stack;
	protected NbtCompound tag;
	
	private NiceItemMeta(ItemStack stack) {
		Preconditions.checkNotNull(stack, "stack cannot be NULL.");
		this.stack = StackUtils.getCraftItemStack(stack);
		this.tag = NbtFactory.asCompound(NbtFactory.fromItemTag(stack));
	}
	
	/**
	 * Construct a new nice ItemMeta from a given stack.
	 * <p>
	 * Note that the item stack may be changed and must be retrieved from the getStack method when done.
	 * @param stack - the stack to modify.
	 * @return The nice ItemMeta.
	 */
	public static NiceItemMeta fromStack(ItemStack stack) {
		return new NiceItemMeta(stack);
	}
	
	/**
	 * Retrieve the altered stack.
	 * @return The altered stack.
	 */
	public ItemStack getStack() {
		return stack;
	}
	
	/**
	 * Determine if we have a custom display name.
	 * @return TRUE if we do, FALSE otherwise.
	 */
	public boolean hasDisplayName() {
		return getDisplayName() != null;
	}
	
	/**
	 * Determine if we have any lore strings.
	 * @return TRUE if we do, FALSE otherwise.
	 */
	public boolean hasLore() {
		@SuppressWarnings("unchecked")
		NbtList<String> list = (NbtList<String>) StackUtils.getNbtTag(tag, "display", "Lore");
		return (list != null && list.size() > 0);
	}
	 
	/**
	 * Gets the display name that is set. 
	 * @return Display name.
	 */
	public String getDisplayName() {
		Object name = StackUtils.getNbtTag(tag, "display", "Name");
		return name instanceof String ? (String)name : null;
	}
	
	/**
	 * Set the display name of this item.
	 * @param name - the new display name.
	 */
	public void setDisplayName(String name) {
		tag.getCompoundOrDefault("display").put("Name", name);
	}
	
	/**
	 * Gets the lore that is set.
	 * @return The lore.
	 */
	@SuppressWarnings("unchecked")
	public List<String> getLore() {
		Object lore = StackUtils.getNbtTag(tag, "display", "Lore");
		
		if (lore instanceof NbtList) {
			return Lists.newArrayList((NbtList<String>) lore);
		}
		return null;
	}
	
	/**
	 * Set the lore section.
	 * @param lore - the lore.
	 */
	public void setLore(List<String> lore) {
		tag.getCompoundOrDefault("display").put("Lore", NbtFactory.ofList("", lore));
	}
}
