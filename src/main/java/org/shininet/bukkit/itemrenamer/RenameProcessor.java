package org.shininet.bukkit.itemrenamer;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.shininet.bukkit.itemrenamer.configuration.DamageLookup;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;

import com.google.common.collect.Lists;

public class RenameProcessor {
	private ItemRenamerConfiguration config;
	
	public RenameProcessor(ItemRenamerConfiguration config) {
		this.config = config;
	}

	private void packName(ItemMeta itemMeta, RenameRule rule) {
		itemMeta.setDisplayName(ChatColor.RESET + 
				ChatColor.translateAlternateColorCodes('&', rule.getName()) + ChatColor.RESET);
	}
	
	private void packLore(ItemMeta itemMeta, RenameRule rule) {
		List<String> output = Lists.newArrayList(rule.getLoreSections());
		
		// Translate color codes as well
		for (int i = 0; i < output.size(); i++) {
			output.set(i, ChatColor.translateAlternateColorCodes('&', output.get(i))+ChatColor.RESET);
		}
		itemMeta.setLore(output);
	}

	public ItemStack process(String world, ItemStack input) {
		String pack = config.getWorldPack(world);

		// The item stack has already been cloned in the packet
		if (input != null && pack != null) {
			DamageLookup lookup = config.getRenameConfig().getLookup(pack, input.getTypeId());
		
			if (lookup != null) {
				RenameRule rule = lookup.getRule(input.getDurability());
				
				if (rule == null)
					return input;
				ItemMeta itemMeta = input.getItemMeta();
				
				// May have been set by a plugin or a player
				if ((itemMeta.hasDisplayName()) || (itemMeta.hasLore())) 
					return input;
				
				packName(itemMeta, rule);
				packLore(itemMeta, rule);
				input.setItemMeta(itemMeta);
			}
		}
		
		// Just return it - for chaining
		return input;
	}
	
	public ItemStack[] process(String world, ItemStack[] input) {
		if (input != null) {
			for (int i = 0; i < input.length; i++) {
				process(world, input[i]);
			}
		}
		return input;
	}
}
