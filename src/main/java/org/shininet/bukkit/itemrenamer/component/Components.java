package org.shininet.bukkit.itemrenamer.component;

import javax.annotation.Nonnull;

import org.bukkit.plugin.Plugin;

import com.google.common.eventbus.EventBus;

public final class Components {	
	private Components() {
		// Don't make it constructable
	}
	
	/**
	 * Construct a registerable component of inner components.
	 * @param components - registerable components.
	 * @return The composite component.
	 */
	public static Component asComposite(final Component... components) {
		return new AbstractComponent() {
			@Override
			protected void onRegistered(@Nonnull Plugin plugin, EventBus bus) {
				for (Component registerable : components) {
					registerable.register(plugin, bus);
				}
			}
			
			@Override
			protected void onUnregistered(@Nonnull Plugin plugin) {
				for (Component registerable : components) {
					registerable.unregister(plugin);
				}
			}
		};
	}
	
	/**
	 * Construct a toggleable component.
	 * @param delegate - the registerable component.
	 * @return The toggleable component.
	 */
	public static ToggleComponent asToggleable(Component delegate) {
		return new ToggleComponent(delegate);
	}
}
