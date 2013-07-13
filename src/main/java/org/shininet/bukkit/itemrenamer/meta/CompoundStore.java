package org.shininet.bukkit.itemrenamer.meta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shininet.bukkit.itemrenamer.meta.CharCodeEncoder.Segment;

import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.io.NbtBinarySerializer;
import com.google.common.base.Preconditions;

public abstract class CompoundStore {
	private static Method GET_NAME_METHOD = null;
	
	protected ItemStack stack;
	
	protected CompoundStore(ItemStack stack) {
		Preconditions.checkNotNull(stack, "stack cannot be NULL.");
		
		// Create a CraftBukkit stack, if necessary
		if (!MinecraftReflection.isCraftItemStack(stack))
			this.stack = MinecraftReflection.getBukkitItemStack(stack);
		else
			this.stack = stack;
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
	 * Retrieve a compound store that saves and loads compounds from invisible data in the display name.
	 * @param stack - the item stack whose display name will be used to load and save data.
	 * @return The compound store.
	 */
	public static CompoundStore getDisplayNameStore(ItemStack stack) {
		return new CompoundStore(stack) {
			// Unique for this kind of store
			private CharCodeEncoder encoder = new CharCodeEncoder(0x79fe4647);
			private NbtBinarySerializer serializer = new NbtBinarySerializer();
			private BukkitUnwrapper unwrapper = new BukkitUnwrapper();
			
			@Override
			public ItemStack saveCompound(NbtCompound compound) {
				ByteArrayOutputStream store = new ByteArrayOutputStream();
				ItemMeta meta = stack.getItemMeta();
				
				// Retrieve compound as a byte array and store it in the name
				serializer.serialize(compound, new DataOutputStream(store));
				meta.setDisplayName(getName() + encoder.encode(store.toByteArray()));
				stack.setItemMeta(meta);
				
				return stack;
			}
			
			@Override
			public NbtCompound loadCompound() {
				ItemMeta meta = stack.getItemMeta();
				
				if (meta.hasDisplayName()) {
					Segment[] segments = encoder.decode(meta.getDisplayName());
					
					// Retrieve the stored NbtCompound
					if (segments.length > 0) {
						ByteArrayInputStream input = new ByteArrayInputStream(segments[0].getData());
						return serializer.deserializeCompound(new DataInputStream(input));
					}
				}
				return null;
			}
			
			private String getName() {
				Object nmsStack = unwrapper.unwrapItem(stack);
				
				if (GET_NAME_METHOD == null) {
					try {
						GET_NAME_METHOD = nmsStack.getClass().getMethod("getName");
					} catch (Exception e) {
						throw new IllegalStateException("Unable to find " + nmsStack.getClass() + ".getName()", e);
					}
				}
				
				// Attempt to get the item name
				try {
					return (String) GET_NAME_METHOD.invoke(nmsStack);
				} catch (Exception e) {
					throw new IllegalStateException("Unable to look up item name for " + stack);
				}
			}
		};
	}
}
