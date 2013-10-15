package org.shininet.bukkit.itemrenamer.api;

import org.bukkit.entity.Player;

/**
 * Represents a listener that is executed whenever a group of items is ready to be transmitted to a client.
 * <p>
 * Consumers of this interface should consider their RenamerPriority carefully, in particular whether or
 * not they need to be executed before or after ItemRenamer's standard rename operation. 
 * @author Kristian
 */
public interface ItemsListener {
	/**
	 * Invoked when a snapshot of items from an inventory are being renamed by plugins and ItemRenamer.
	 * @param player - the current player.
	 * @param snapshot - the snapshot of items to rename.
	 */
	public void onItemsSending(Player player, RenamerSnapshot snapshot);
}