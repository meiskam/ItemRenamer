package org.shininet.bukkit.itemrenamer;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;

import org.bukkit.command.CommandSender;

import com.google.common.base.Preconditions;

/**
 * Manages the selected pack for a given command sender.
 * @author Kristian
 */
public class SelectedPackTracker {
	private final Map<CommandSender, String> lookup = new WeakHashMap<CommandSender, String>();
	
	/**
	 * Determine if the given sender has a selected pack.
	 * @param sender - the sender to test.
	 * @return TRUE if it does, FALSE otherwise.
	 */
	public boolean hasSelected(@Nonnull CommandSender sender) {
		Preconditions.checkNotNull(sender, "sender cannot be NULL.");
		return lookup.containsKey(sender);
	}
	
	/**
	 * Select the given rule pack.
	 * @param sender - the current sender.
	 * @param pack - the pack to select.
	 * @return The selected rule pack.
	 */
	public String selectPack(@Nonnull CommandSender sender, @Nonnull String pack) {
		Preconditions.checkNotNull(sender, "sender cannot be NULL.");
		Preconditions.checkNotNull(pack, "pack cannot be NULL.");
		return lookup.put(sender, pack);
	}
	
	/**
	 * Retrieve the selected rule pack.
	 * @param sender - the sender.
	 * @return The selected rule pack.
	 */
	public String getSelected(@Nonnull CommandSender sender) {
		Preconditions.checkNotNull(sender, "sender cannot be NULL.");
		return lookup.get(sender);
	}

	/**
	 * Deselect the currently selected pack.
	 * @param sender - the sender.
	 * @return The pack previously selected by the sender, or NULL if not found.
	 */
	public String deselectPack(@Nonnull CommandSender sender) {
		Preconditions.checkNotNull(sender, "sender cannot be NULL.");
		return lookup.remove(sender);
	}
}
