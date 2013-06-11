package org.shininet.bukkit.itemrenamer.configuration;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.wrappers.SpecificItemStack;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

public class MemoryExactLookup implements ExactLookup {
	// No need to account for concurrency - only the main thread (ought) to both read and write to this map
	private Map<SpecificItemStack, RenameRule> lookup = Maps.newHashMap();
	
	// Modifications
	private int modCount;
	
	@Override
	public RenameRule getRule(ItemStack stack) {
		return lookup.get(new SpecificItemStack(stack));
	}

	@Override
	public void setRule(ItemStack stack, RenameRule rule) {
		SpecificItemStack key = new SpecificItemStack(stack.clone());
		RenameRule old = null;

		// Associate or remove
		if (rule != null)
			old = lookup.put(key, rule);
		else
			old = lookup.remove(key);
		
		// We only increment if they are different
		if (!Objects.equal(rule, old)) {
			modCount++;
		}
	}

	@Override
	public Map<ItemStack, RenameRule> toLookup() {
		Map<ItemStack, RenameRule> copy = Maps.newHashMap();
		
		// Unwrap the lookup map
		for (Entry<SpecificItemStack, RenameRule> entry : lookup.entrySet()) {
			copy.put(entry.getKey().getStack(), entry.getValue());
		}
		return copy;
	}

	@Override
	public void clear() {
		lookup.clear();
	}

	@Override
	public int getModificationCount() {
		return modCount;
	}

	@Override
	public void setModificationCount(int value) {
		modCount = value;
	}
}
