package org.shininet.bukkit.itemrenamer.enchants;

import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

public class HideAttributesEnchanter extends NbtEnchanter {
	@Override
	public ItemStack enchant(ItemStack stack) {
		NbtCompound compound = getCompound(stack = preprocess(stack));
		compound.put(NbtFactory.ofList("AttributeModifiers"));
		return stack;
	}

	@Override
	public ItemStack disenchant(ItemStack stack) {
		NbtCompound compound = getCompound(stack = preprocess(stack));
		compound.remove("AttributeModifiers");
		return stack;
	}
}
