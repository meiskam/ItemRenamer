package org.shininet.bukkit.itemrenamer;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.shininet.bukkit.itemrenamer.api.RenamerListener;
import org.shininet.bukkit.itemrenamer.api.RenamerPriority;
import org.shininet.bukkit.itemrenamer.api.RenamerSnapshot;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

/**
 * Represents a listener manager for ItemRenamer.
 * @author Kristian
 */
class RenameListenerManager {
	private static class ListenerContainer implements Comparable<ListenerContainer> {
		public final Plugin plugin;
		public final RenamerPriority priority;
		public final RenamerListener listener;
		
		public ListenerContainer(Plugin plugin, RenamerPriority priority, RenamerListener listener) {
			this.plugin = Preconditions.checkNotNull(plugin, "plugin cannot be NULL");
			this.priority = Preconditions.checkNotNull(priority, "priority cannot be NULL");
			this.listener = Preconditions.checkNotNull(listener, "listener cannot be NULL");
		}

		@Override
		public int compareTo(ListenerContainer o) {
			if (o == this)
				return 0;
			if (o == null)
				return -1;
			ListenerContainer other = (ListenerContainer) o;
			
			// The container will only differ if the listener differs
			if (listener == other.listener) {
				return 0;
			} else {
				return ComparisonChain.start().
					compare(priority, other.priority).
					compare(listener.hashCode(), other.listener.hashCode()).
					result();
			}
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ListenerContainer) {
				return compareTo((ListenerContainer) obj) == 0;
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(priority, listener);
		}
	}
	
	// Every listener in execution order
	private NavigableSet<ListenerContainer> listeners = new ConcurrentSkipListSet<ListenerContainer>();
	
	// The renamer
	private Plugin renamerPlugin;
	private RenamerListener renamerListener;
	
	public RenameListenerManager(Plugin renamerPlugin) {
		this.renamerPlugin = renamerPlugin;
	}

	/**
	 * Add a given listener to the manager.
	 * @param plugin - the owning plugin. Cannot be ItemRenamer.
	 * @param priority - the priority.
	 * @param listener
	 */
	public void addListener(Plugin plugin, RenamerPriority priority, RenamerListener listener) {
		if (plugin == renamerPlugin)
			throw new IllegalArgumentException("Cannot add a listener beloning to ItemRenamer.");
		listeners.add(new ListenerContainer(plugin, priority, listener));
	}
	
	/**
	 * Remove a given listener.
	 * @param listener - the listener to remove.
	 * @return TRUE if the listener was removed, FALSE otherwise.
	 */
	public boolean removeListener(RenamerListener listener) {
		for (Iterator<ListenerContainer> it = listeners.iterator(); it.hasNext(); ) {
			if (it.next().listener == listener) {
				// Break here - the container should only have one listener of this type
				it.remove();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Remove every listener associated with a given plugin.
	 * @param plugin - the owner plugin.
	 * @return TRUE if any listeners were removed, FALSE otherwise.
	 */
	public boolean removeListeners(Plugin plugin) {
		boolean result = false;
		
		// Remove every listener associated with a given plugin
		for (Iterator<ListenerContainer> it = listeners.iterator(); it.hasNext(); ) {
			if (Objects.equals(it.next().plugin, plugin)) {
				it.remove();
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Invoke the current listeners.
	 * @param player - current player.
	 * @param snapshot - the snapshot.
	 */
	public void invokeListeners(Player player, RenamerSnapshot snapshot) {
		boolean executedRenamer = false;
		
		for (ListenerContainer container : listeners) {
			// Should we execute ItemRenamer?
			if (!executedRenamer && RenamerPriority.POST_NORMAL.compareTo(container.priority) <= 0) {
				renamerListener.onItemsRenaming(player, snapshot);
				executedRenamer = true;
			}
			
			try {
				container.listener.onItemsRenaming(player, snapshot);
			} catch (Throwable e) {
				// Use ProtocolLib to report the error
				ProtocolLibrary.getErrorReporter().reportMinimal(container.plugin, "ItemRenamer.onItemsRenaming()", e);
			}
		}
		
		if (!executedRenamer) {
			renamerListener.onItemsRenaming(player, snapshot);
		}
	}
	
	/**
	 * Set the ItemRenamer listener that will be executed explicitly.
	 * @param renamerListener - the renamer listener.
	 */
	public void setRenamerListener(RenamerListener renamerListener) {
		this.renamerListener = renamerListener;
	}
}
