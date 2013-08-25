package org.shininet.bukkit.itemrenamer.component;

import javax.annotation.Nonnull;

import org.bukkit.plugin.Plugin;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents a component that can be registered with a plugin.
 * @author Kristian
 */
public abstract class AbstractComponent implements Component {
	/**
	 * The current registered plugin.
	 */
	protected Plugin registered;
	
	@Override
	public final Component register(@Nonnull Plugin plugin) {
		// We don't permit double registeration
		Preconditions.checkNotNull(plugin, "plugin cannot be NULL.");
		Preconditions.checkState(!isRegistered(), "Cannot register component: Registered with " + registered);
		
		registered = plugin;
		onRegistered(plugin);
		return this;
	}
	
	@Override
	public final boolean unregister(@Nonnull Plugin plugin) {
		// That doesn't make any sense
		Preconditions.checkNotNull(plugin, "plugin cannot be NULL.");
		
		if (Objects.equal(plugin, registered)) {
			// In case the unregister method uses "registered"
			onUnregistered(plugin);
			this.registered = null;
			return true;
		}
		return false;
	}
	
	@Override
	public final boolean isRegistered() {
		return registered != null;
	}
	
	/**
	 * Invoked when a component is ready to be registred.
	 * @param plugin - the target plugin.
	 */
	protected abstract void onRegistered(@Nonnull Plugin plugin);
	
	/**
	 * Invoked when a component is ready to be unregistered.
	 * @param plugin - the target plugin.
	 */
	protected abstract void onUnregistered(@Nonnull Plugin plugin);
}
