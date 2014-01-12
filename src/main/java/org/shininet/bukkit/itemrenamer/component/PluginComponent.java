package org.shininet.bukkit.itemrenamer.component;

import java.util.Map;
import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class PluginComponent extends AbstractBukkitComponent {	
	private Map<String, Supplier<? extends Component>> suppliers = Maps.newHashMap();
	private Map<String, Component> components = Maps.newHashMap();
	
	/**
	 * Add a new component that will be constructed and registered if the given plugin is enabled.
	 * @param pluginName - the plugin to wait for.
	 * @param supplier - the supplier that will be activated when the given plugin has been enabled.
	 * @return This plugin component, for chaining,
	 */
	public PluginComponent add(String pluginName, Supplier<? extends Component> supplier) {
		suppliers.put(pluginName, supplier);
		
		// Register the component if the plugin has already been enabled
		if (Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
			onPluginEnabled(Bukkit.getPluginManager().getPlugin(pluginName));
		}
		return this;
	}
		
	/**
	 * Add a new component that will be constructed and registered if the given plugin is enabled.
	 * @param pluginName - the plugin to wait for.
	 * @param componentClass - the class of the component to construct.
	 * @return This plugin component, for chaining,
	 */
	public PluginComponent add(String pluginName, Class<? extends Component> componentClass) {
		return add(pluginName, newSupplier(componentClass));
	}
	
	/**
	 * Retrieve a component by its the associated plugin, if it has been constructed.
	 * @param pluginName - the associated plugin.
	 * @return The component, or NULL.
	 */
	public Component get(String pluginName) {
		return components.get(pluginName);
	}
	
	@Override
	protected void onRegistered(@Nonnull Plugin plugin, EventBus bus) {
		super.onRegistered(plugin, bus);

		// Delayed enabled events
		for (Plugin target : Bukkit.getPluginManager().getPlugins()) {
			if (target.isEnabled()) {
				onPluginEnabled(target);
			}
		}
	}
	
	@Override
	protected void onUnregistered(@Nonnull Plugin plugin) {
		for (Component component : components.values())
			component.unregister(registered);
		super.onUnregistered(plugin);
	}
	
	/**
	 * Invoked when we have detected that a plugin has been enabled.
	 * @param plugin - the enabled plugin.
	 */
	private void onPluginEnabled(Plugin plugin) {
		String key = plugin.getName();
		Supplier<? extends Component> supplier = suppliers.get(key);
		
		// Only register the component once
		if (isRegistered() && supplier != null) {
			Component component = components.get(key);
			
			// Construct and register the component
			if (component == null) 
				components.put(key, component = supplier.get());
			if (!component.isRegistered()) 
				component.register(plugin, bus);
		}
	}

	/**
	 * Invoked when a plugin has been disabled.
	 * @param plugin - the disabled plugin.
	 */
	private void onPluginDisabled(Plugin plugin) {
		Component component = components.get(plugin.getName());
		
		if (component != null) {
			component.unregister(registered);
		}
	}
	
	@EventHandler
	public void onPluginEnabled(PluginEnableEvent e) {
		onPluginEnabled(e.getPlugin());
	}
	
	@EventHandler
	public void onPluginDisabled(PluginDisableEvent e) {
		onPluginDisabled(e.getPlugin());
	}
	
	@Override
	protected boolean requireBukkit() {
		return true;
	}

	@Override
	protected boolean requireEventBus() {
		return false;
	}
	
	/**
	 * Return a supplier that constructs instances of the given class using the no-argument constructor.
	 * @param clazz - the class.
	 * @return A new supplier.
	 */
	private static <T extends Component> Supplier<T> newSupplier(final Class<T> clazz) {
		return new Supplier<T>() {
			@Override
			public T get() {
				try {
					return clazz.newInstance();
				} catch (Exception e) {
					throw new UncheckedExecutionException("Cannot construct instance.", e);
				}
			}
		};
	}
}
