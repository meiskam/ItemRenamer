package org.shininet.bukkit.itemrenamer.api;

import javax.annotation.Nonnull;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.shininet.bukkit.itemrenamer.ItemRenamerPlugin;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;

/**
 * Represents the API for accessing and modifying ItemRenamer.
 * 
 * @author Kristian
 */
public abstract class RenamerAPI {
	/**
	 * Retrieve the singleton API instance.
	 * <p>
	 * Note that this may be NULL if ItemRenamer has not been initialized yet, nor has unloaded.
	 * @return The renamer API instance.
	 */
	public static RenamerAPI getAPI() {
		return ItemRenamerPlugin.getRenamerAPI();
	}
	
	/**
	 * Retrieve the configuration file detailing global plugin settings.
	 * @return The current configuration.
	 */
	@Nonnull
	public abstract ItemRenamerConfiguration getGlobalConfiguration();
	
	/**
	 * Retrieve the main section of the configuration file setting all the rename rules and packs.
	 * <p>
	 * Any modification to the configuration will automatically be propagated to every logged on user. Note that 
	 * the configuration must be manually saved ({@link ItemRenamerConfiguration#save()} and loaded.
	 * @return The rename configuration.
	 */
	@Nonnull
	public abstract RenameConfiguration getRenameConfiguration();
	
	/**
	 * Retrieve the rename pack a given player is associated with.
	 * <p>
	 * This can either be through the world it currently belongs to, or due to a permission configuration.
	 * @param player - the player.
	 * @return The rename pack, or NULL if not found.
	 */
	public abstract String getRenamePack(@Nonnull Player player);
	
	/**
	 * Retrieve the rename pack associated with the given world.
	 * @param world - the world.
	 * @return The associated rename pack, or NULL if none can be found.
	 */
	public abstract String getRenamePack(@Nonnull World world);
	
	/**
	 * Retrieve the default rename pack.
	 * <p>
	 * This is used if a world's rename pack is undefined.
	 * @return Default rename pack, or NULL if not found.
	 */
	public abstract String getDefaultPack();
	
	/**
	 * Retrieve the rename rule associated with a particular item stack.
	 * @param pack - the rename pack.
	 * @param stack - the stack we are looking for.
	 * @return The rename rule, or NULL if not found.
	 */
	public abstract RenameRule getRule(@Nonnull String pack, @Nonnull ItemStack stack);
	
	/**
	 * Rename the given item stack with the provided rule.
	 * <p>
	 * The resulting stack will be cloned.
	 * @param stack - the stack to rename.
	 * @param rule - the rule to apply, or NULL for the {@link RenameRule#IDENTITY} rule.
	 * @return A copy of the provided stack.
	 */
	@Nonnull
	public abstract ItemStack process(@Nonnull ItemStack stack, RenameRule rule);
	
	/**
	 * Retrieve the renamed stack in the perspective of the given player.
	 * @param stack - the stack to rename,
	 * @param player - the player whose perspetive we are interested it.
	 * @return The renamed stack.
	 */
	@Nonnull
	public abstract ItemStack process(@Nonnull ItemStack stack, @Nonnull Player player);
}
