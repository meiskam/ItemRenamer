package org.shininet.bukkit.itemrenamer.meta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.utils.StackUtils;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import com.google.common.base.Preconditions;

public abstract class CompoundStore {
	public static final int PLUGIN_ID = 0x79fe4647;
	
	protected ItemStack stack;
	
	protected CompoundStore(ItemStack stack) {
		Preconditions.checkNotNull(stack, "stack cannot be NULL.");
		this.stack = StackUtils.getCraftItemStack(stack);
	}

	/**
	 * Retrieve the item stack where the additional compound will be stored.
	 * @return Item stack where the compound will be stored.
	 */
	public ItemStack getStack() {
		return stack;
	}
	
	/**
	 * Save a given compound to the given item stack.
	 * @param compound - the compound to save.
	 * @return The current item stack.
	 */
	public abstract ItemStack saveCompound(NbtCompound compound);
	
	/**
	 * Load the saved compound.
	 * @return The saved compound, or NULL.
	 */
	public abstract NbtCompound loadCompound();
	
	/**
	 * Retrieve a compound store that saves and loads the compound directly in the NbtCompound tag.
	 * @param stack - the ItemStack whose tag will be used to save and load NbtCompounds.
	 * @return The compound store.
	 */
	public static CompoundStore getNativeStore(ItemStack stack) {
		return new CompoundStore(stack) {
			/**
			 * Storage of the original ItemMeta.
			 */
			private static final String KEY_ORIGINAL = "com.comphenix.original";

			@Override
			public ItemStack saveCompound(NbtCompound compound) {
				getCompound(stack).put(KEY_ORIGINAL, compound);
				return stack;
			}
			
			@Override
			public NbtCompound loadCompound() {
				NbtCompound data = getCompound(stack);
				
				// Check for our marker
				if (data.containsKey(KEY_ORIGINAL)) {
					return data.getCompound(KEY_ORIGINAL);
				} else {
					return null;
				}
			}
			
			/**
			 * Retrieve the NbtCompound that stores additional data in an ItemStack.
			 * @param stack - the item stack.
			 * @return The additional NbtCompound.
			 */
			private NbtCompound getCompound(ItemStack stack) {
				// It should have been a compound in the API ...
				return NbtFactory.asCompound(NbtFactory.fromItemTag(stack));
			}
		};
	}
	
	/** 
	 * Retrieve a compound store that saves and loads compounds from invisible data in the display name or lore.
	 * @param stack - the item stack whose display name or lore will be used to load and save data.
	 * @return The compound store.
	 */
	@Deprecated
	public static CompoundStore getItemMetaStore(ItemStack stack) {
		return new CompoundStore(stack) {
			// Unique for this kind of store
			private CharCodeStore encoder = 
					(isNamingItem(stack) || stack.getItemMeta().hasLore()) ? 
					CharCodeFactory.fromLore(PLUGIN_ID, stack) : 
					CharCodeFactory.fromDisplayName(PLUGIN_ID, stack);
					
			private NbtBinarySerializer serializer = new NbtBinarySerializer();
			
			@Override
			public ItemStack saveCompound(NbtCompound compound) {
				ByteArrayOutputStream store = new ByteArrayOutputStream();

				serializer.serialize(compound, new DataOutputStream(store));
				encoder.getData().setBytes(store.toByteArray());
				encoder.save();
				return stack;
			}
			
			@Override
			public NbtCompound loadCompound() {
				if (encoder.hasData()) {
					ByteArrayInputStream input = new ByteArrayInputStream(encoder.getData().getBytes());
					
					// Retrieve the stored NbtCompound
					return serializer.deserializeCompound(new DataInputStream(input));
				}
				return null;
			}
		};
	}
	
	/**
	 * Determine if the display name of the given item is used to set the name of a mob in any way.
	 * <p>
	 * This includes spawner eggs and naming tags.
	 * @stack - the stack to check. 
	 * @return TRUE if it does, FALSE otherwise.
	 */
	private static boolean isNamingItem(ItemStack stack) {
		return stack.getType() == Material.MONSTER_EGG || 
			   stack.getType() == Material.MONSTER_EGGS || 
			   stack.getType() == Material.NAME_TAG;
	}
}
