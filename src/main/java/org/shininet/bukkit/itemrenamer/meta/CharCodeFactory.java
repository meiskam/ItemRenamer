package org.shininet.bukkit.itemrenamer.meta;

import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.google.common.collect.Lists;

public class CharCodeFactory {
	// For extracting the default name of an item
	private static Method GET_NAME_METHOD = null;
	private static BukkitUnwrapper unwrapper = new BukkitUnwrapper();
	
	private CharCodeFactory() {
		// Not constructable
	}
	
	/**
	 * Construct a store in the lore section of an item stack.
	 * <p>
	 * Call {@link #save()} method once done. Please go to 
	 * <a href="http://www.random.org/cgi-bin/randbyte?nbytes=4&format=h ">Random.org</a> to get a new unique ID for your plugin.
	 * @param pluginId - a unique ID identifying the owner plugin.
	 * @param source - the item source.
	 * @return A data store.
	 */
	public static CharCodeStore fromLore(int pluginId, final ItemStack source) {
		final ItemMeta meta = source.getItemMeta();
		
		return new CharCodeStore(pluginId) {
			{
				if (meta.hasLore()) {
					parse(meta.getLore().get(0));
				}
			}
			
			@Override
			public void save() {
				List<String> lore = meta.hasLore() ? meta.getLore() : Lists.newArrayList("");
				lore.set(0, toString());
				
				// Update lore
				meta.setLore(lore);
				source.setItemMeta(meta);
			}
		};
	}
	
	/**
	 * Construct a store in the display name of an item stack.
	 * <p>
	 * Call {@link #save()} method once done. Please go to 
	 * <a href="http://www.random.org/cgi-bin/randbyte?nbytes=4&format=h ">Random.org</a> to get a new unique ID for your plugin.
	 * @param pluginId - a unique ID identifying the owner plugin.
	 * @param source - the item source.
	 * @return A data store.
	 */
	public static CharCodeStore fromDisplayName(int pluginId, final ItemStack source) {
		final ItemMeta meta = source.getItemMeta();
		
		return new CharCodeStore(pluginId) {
			{
				if (meta.hasDisplayName()) {
					parse(meta.getDisplayName());
				} else {
					parse(ChatColor.RESET + getItemName(source));
				}
			}
			
			@Override
			public void save() {
				meta.setDisplayName(toString());
				source.setItemMeta(meta);
			}
		};
	}
	
	/**
	 * Retrieve the default display name of an item.
	 * @param stack - the stack to rename.
	 * @return The display name of an item.
	 */
	private static String getItemName(ItemStack stack) {
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
}
