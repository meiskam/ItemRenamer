package org.shininet.bukkit.itemrenamer;

import javax.annotation.Nonnull;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.api.RenamerAPI;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;

import com.google.common.base.Preconditions;

/**
 * Simplified API.
 * @author Kristian
 */
class ItemRenamerAPI extends RenamerAPI {
	private ItemRenamerConfiguration config;
	private RenameProcessor processor;
	
	public ItemRenamerAPI(ItemRenamerConfiguration config, RenameProcessor processor) {
		this.config = config;
		this.processor = processor;
	}

	@Override
	@Nonnull
	public ItemRenamerConfiguration getGlobalConfiguration() {
		return config;
	}

	@Override
	@Nonnull
	public RenameConfiguration getRenameConfiguration() {
		return config.getRenameConfig();
	}

	@Override
	public String getRenamePack(@Nonnull Player player) {
		Preconditions.checkNotNull(player, "player cannot be NULL.");
		
		return processor.getPack(player);
	}

	@Override 
	public String getRenamePack(@Nonnull World world) {
		Preconditions.checkNotNull(world, "world cannot be NULL.");
		
		return config.getWorldPack(world.getName());
	}

	@Override
	public String getDefaultPack() {
		return config.getDefaultPack();
	}

	@Override
	public RenameRule getRule(@Nonnull String pack, @Nonnull ItemStack stack) {
		Preconditions.checkNotNull(pack, "pack cannot be NULL.");
		Preconditions.checkNotNull(stack, "stack cannot be NULL.");
		return processor.getRule(pack, stack);
	}

	@Override
	@Nonnull
	public ItemStack process(@Nonnull ItemStack stack, RenameRule rule) {
		if (!RenameRule.isIdentity(rule)) {
			stack = stack.clone();
			return processor.processRule(stack, rule);
		}
		return stack;
	}

	@Override
	@Nonnull
	public ItemStack process(@Nonnull ItemStack stack, @Nonnull Player player) {
		return process(stack, getRule(getRenamePack(player), stack));
	}
}
