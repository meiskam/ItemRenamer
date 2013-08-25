package org.shininet.bukkit.itemrenamer.component;

import javax.annotation.Nonnull;

import org.bukkit.plugin.Plugin;

/**
 * Represents a registerable that can be enabled or disabled at will.
 * <p>
 * @author Kristian
 */
public class ToggleComponent extends AbstractComponent {
	/**
	 * The delegate component.
	 */
	protected final Component delegate;
	
	/**
	 * The field that stores whether or not the delegate is enabled.
	 */
	protected boolean enabled = true;
	
	public ToggleComponent(Component delegate) {
		this.delegate = delegate;
	}
	
	/**
	 * Set whether or not the registerable component should be enabled.
	 * <p>
	 * The delegate will only be registered if the current component is registered.
	 * @param enabled - TRUE if it should, FALSE otherwise.
	 */
	public void setEnabled(boolean enabled) {
		// We have to update the state
		if (isRegistered() && this.enabled != enabled) {
			if (enabled) {
				delegate.register(registered);
			} else {
				delegate.unregister(registered);
			}
		}
		this.enabled = enabled;
	}
	
	/**
	 * Determine if the registerable component is enabled.
	 * <p>
	 * The delegate component will be active if this instance is both enabled and registered.
	 * @return TRUE if it is enabled, FALSE otherwise.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Retrieve the underlying component.
	 * @return The toggleable component.
	 */
	public Component getDelegate() {
		return delegate;
	}
	
	@Override
	protected void onRegistered(@Nonnull Plugin plugin) {
		if (enabled) {
			delegate.register(plugin);
		}
	}
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		if (enabled) {
			delegate.unregister(plugin);
		}
	}
}
