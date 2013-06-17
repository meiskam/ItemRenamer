/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.shininet.bukkit.itemrenamer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.shininet.bukkit.itemrenamer.configuration.ConfigParsers;
import org.shininet.bukkit.itemrenamer.configuration.DamageLookup;
import org.shininet.bukkit.itemrenamer.configuration.DamageValues;
import org.shininet.bukkit.itemrenamer.configuration.ItemRenamerConfiguration;
import org.shininet.bukkit.itemrenamer.configuration.RenameProcessorFactory;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;
import org.shininet.bukkit.itemrenamer.serialization.DamageSerializer;
import org.shininet.bukkit.itemrenamer.utils.MaterialUtils;
import org.shininet.bukkit.itemrenamer.wrappers.LeveledEnchantment;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Ranges;

public class ItemRenamerCommands implements CommandExecutor {
	// Different permissions
	private static final String PERM_GET = "itemrenamer.config.get";
	private static final String PERM_SET= "itemrenamer.config.set";
	
	// The super command
	private static final Object COMMAND_ITEMRENAMER = "ItemRenamer";
	
	// The selected pack and item for each sender
	private final Map<CommandSender, String> selectedPack = new WeakHashMap<CommandSender, String>();
	
	// Selected items
	private SelectedItemTracker selectedTracker;
	
	// Recognized sub-commands
	public enum Commands {
		GET_AUTO_UPDATE,
		SET_AUTO_UPDATE,
		GET_WORLD_PACK, 
		SET_WORLD_PACK,
		GET_ITEM,
		GET_SELECTED,
		ADD_PACK,
		DELETE_PACK,
		SELECT_PACK,
		SELECT_HAND,
		SELECT_NONE,
		SET_NAME, 
		ADD_LORE, 
		DELETE_LORE,
		ADD_ENCHANTMENT, 
		ADD_DECHANTMENT, 
		DELETE_ENCHANTMENTS,
		DELETE_DECHANTMENTS,
		RELOAD,
		SAVE,
		PAGE,
	}
	
	private final ItemRenamer plugin;
	private final ItemRenamerConfiguration config;
	
	private final CommandMatcher<Commands> matcher;
	
	// Paged output
	private final PagedMessage pagedMessage = new PagedMessage();
	
	public ItemRenamerCommands(ItemRenamer plugin, ItemRenamerConfiguration config, SelectedItemTracker selectedTracker) {
		this.plugin = plugin;
		this.matcher = registerCommands();
		this.config = config;
		this.selectedTracker = selectedTracker;
	}
	
	private CommandMatcher<Commands> registerCommands() {
		CommandMatcher<Commands> output = new CommandMatcher<Commands>();
		output.registerCommand(Commands.GET_AUTO_UPDATE, PERM_GET, "get", "setting", "autoupdate");
		output.registerCommand(Commands.SET_AUTO_UPDATE, PERM_SET, "set", "setting", "autoupdate");
		output.registerCommand(Commands.GET_WORLD_PACK, PERM_GET, "get", "world");
		output.registerCommand(Commands.SET_WORLD_PACK, PERM_SET, "set", "world");
		output.registerCommand(Commands.ADD_PACK, PERM_SET, "add", "pack");
		output.registerCommand(Commands.GET_SELECTED, PERM_GET, "get", "selected");
		output.registerCommand(Commands.DELETE_PACK, PERM_SET, "delete", "pack");
		output.registerCommand(Commands.SELECT_PACK, PERM_SET, "select", "pack");
		output.registerCommand(Commands.SELECT_HAND, PERM_SET, "select", "hand");
		output.registerCommand(Commands.SELECT_NONE, PERM_SET, "select", "none");
		output.registerCommand(Commands.GET_ITEM, PERM_GET, "get", "item");
		output.registerCommand(Commands.SET_NAME, PERM_SET, "set", "name");
		output.registerCommand(Commands.ADD_LORE, PERM_SET, "add", "lore");
		output.registerCommand(Commands.DELETE_LORE, PERM_SET, "delete", "lore");
		output.registerCommand(Commands.ADD_ENCHANTMENT, PERM_SET, "add", "enchantment");
		output.registerCommand(Commands.ADD_DECHANTMENT, PERM_SET, "add", "dechantment");
		output.registerCommand(Commands.DELETE_ENCHANTMENTS, PERM_SET, "delete", "enchantments");
		output.registerCommand(Commands.DELETE_DECHANTMENTS, PERM_SET, "delete", "dechantments");
		output.registerCommand(Commands.RELOAD, PERM_SET, "reload");
		output.registerCommand(Commands.SAVE, PERM_SET, "save");
		output.registerCommand(Commands.PAGE, null, "page");
		return output;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] arguments) {
		if (cmd.getName().equals(COMMAND_ITEMRENAMER)) {
			LinkedList<String> input = Lists.newLinkedList(Arrays.asList(arguments));
			
			// See which node is closest
			CommandMatcher<Commands>.CommandNode node = matcher.matchClosest(input);
			
			if (node.isCommand()) {
				if (node.getPermission() != null && !sender.hasPermission(node.getPermission())) {
					sender.sendMessage(ChatColor.RED + "You need permission " + node.getPermission());
					return true;
				}
				
				try {
					String result = performCommand(sender, node.getCommand(), input);
					
					if (result != null)
						sender.sendMessage(ChatColor.GOLD + result);
				} catch (CommandErrorException e) {
					sender.sendMessage(ChatColor.RED + e.getMessage());
				}
			} else {
				sender.sendMessage(ChatColor.RED + "Sub commands: " + 
									Joiner.on(", ").join(node.getChildren()));
			}
			
			// It's still somewhat correct
			return true;
		} else {
			return false;
		}
	}
	
	private String performCommand(CommandSender sender, Commands command, Deque<String> args) {
		try {
			switch (command) {
				case GET_AUTO_UPDATE: 
					expectCommandCount(args, 0, "No arguments needed.");
					return formatBoolean("Auto update is %s.", config.isAutoUpdate()); 
				case SET_AUTO_UPDATE:
					expectCommandCount(args, 1, "Need a yes/no argument.");
					config.setAutoUpdate(parseBoolean(args.poll()));
					return "Updated auto update.";
				case GET_WORLD_PACK: 
					expectCommandCount(args, 1, "Need a world name.");
					return getWorldPack(args);
				case SET_WORLD_PACK:
					expectCommandCount(args, 2, "Need a world name and a world pack name.");
					return setWorldPack(args);
				case ADD_PACK:
					expectCommandCount(args, 1, "Need a world pack name.");
					return addWorldPack(sender, args);
				case DELETE_PACK:
					expectCommandCount(args, 1, "Need a world pack name.");
					return deleteWorldPack(args);
				case SELECT_HAND:
					expectCommandCount(args, 0, "No arguments needed.");
					return selectCurrent(sender);
				case SELECT_PACK:
					expectCommandCount(args, 1, "Need a world pack name.");
					return selectWorldPack(sender, args);
				case SELECT_NONE:
					String deselectedItem = deselectItemStack(sender);
					return deselectWorldPack(sender) + (deselectedItem != null ? ". " + deselectedItem : "");
				case GET_SELECTED:
					return printSelected(sender);
				case GET_ITEM:
					return getItem(sender, args);
				case SET_NAME:
					return setItemName(sender, args);
				case ADD_LORE:
					return addLore(sender, args);
				case DELETE_LORE:
					return clearLore(sender, args);
				case ADD_ENCHANTMENT:
					return addEnchantment(sender, args);
				case ADD_DECHANTMENT:
					return addDechantment(sender, args);
				case DELETE_ENCHANTMENTS:
					return clearEnchantments(sender, args);
				case DELETE_DECHANTMENTS:
					return clearDechantment(sender, args);
				case RELOAD:
					config.reload();
					return "Reloading configuration.";
				case SAVE:
					config.save();
					return "Saving configuration to file.";
				case PAGE:
					List<Integer> pageNumber = ConfigParsers.getIntegers(args, 1, null);
					
					if (pageNumber.size() == 1) {
						return pagedMessage.getPage(sender, pageNumber.get(0));
					} else {
						throw new CommandErrorException("Must specify a page number.");
					}
			}
			
		} catch (IllegalArgumentException e) {
			throw new CommandErrorException(e.getMessage(), e);
		}
		throw new CommandErrorException("Unrecognized sub command: " + command);
	}

	/**
	 * Perform the "select hand" command. This will use the current items ID and durability,
	 * or the item itself.
	 * @param sender - the command sender.
	 * @return Result string.
	 */
	private String selectCurrent(CommandSender sender) {
		// This will fail if the command sender is not a player
		ItemStack previous = selectedTracker.selectCurrent(sender);
		ItemStack current = selectedTracker.getItemToSelect(sender);
		Player player = (Player) sender;
		
		String worldName = player.getWorld().getName();
		String worldPack = config.getWorldPack(worldName);
		
		// Select the current world too
		if (selectedPack.get(sender) == null) {
			if (worldPack != null)
				selectedPack.put(sender, worldPack);
			else
				return "Please set the world pack first.";
		}
		
		// And we're done
		if (previous != null) {
			return "Selected " + current + " from " + previous;
		} else {
			return "Selected " + current;
		}
	}

	/**
	 * Print the currently selected item and pack.
	 * @param sender - the command sender.
	 * @return The lines to print.
	 */
	private String printSelected(CommandSender sender) {
		List<String> lines = new ArrayList<String>();

		if (selectedPack.get(sender) != null)
			lines.add("Selected pack " + selectedPack.get(sender));
		if (selectedTracker.getSelected(sender) != null)
			lines.add("Selected item " + selectedTracker.getSelected(sender));
		
		if (lines.size() > 0) {
			return Joiner.on("\n").join(lines);
		} else {
			return "Nothing selected.";
		}
	}

	private String getItem(CommandSender sender, Deque<String> args) {
		String pack = selectedPack.get(sender);
		
		// Check for special selection
		if (selectedTracker.hasExactSelector(sender) && pack != null) {
			return "Rename: " + config.getRenameConfig().getExact(pack).
								getRule(selectedTracker.getSelected(sender));
		}

		// Get all the arguments before we begin
		final DamageLookup lookup = getLookup(sender, args);
		
		if (args.isEmpty()) {
			YamlConfiguration yaml = new YamlConfiguration();
			DamageSerializer serializer = new DamageSerializer(yaml);
			serializer.writeLookup(lookup);
			
			// Display the lookup as a YAML
			return pagedMessage.createPage(sender, yaml.saveToString());
		}
		
		final DamageValues damage = getDamageValues(sender, args);
		
		if (damage == DamageValues.ALL)
			return "Rename: " + lookup.getAllRule();
		if (damage == DamageValues.ALL)
			return "Rename: " + lookup.getAllRule();
		else if (damage.getRange().lowerEndpoint().equals(damage.getRange().upperEndpoint())) 
			return "Rename: " + lookup.getDefinedRule(damage.getRange().lowerEndpoint());
		else
			throw new CommandErrorException("Cannot parse damage. Must be a single value, ALL or OTHER.");
	}
	
	private void modifyCurrent(CommandSender sender, Deque<String> args, boolean createNew, RenameProcessorFactory factory) {
		String pack = selectedPack.get(sender);
		
		// Exact matches
		if (selectedTracker.hasExactSelector(sender) && pack != null) {
			ItemStack stack = selectedTracker.getSelected(sender);

			// Apply this to a new exact lookup
			config.getRenameConfig().createExact(pack).
				setTransform(stack, factory.create());
		} else {
			// Get all the arguments before we begin
			DamageLookup lookup = createNew ? createLookup(sender, args) : getLookup(sender, args);
			DamageValues damage = getDamageValues(sender, args);
			
			if (lookup != null)
				lookup.setTransform(damage, factory.create());
			else
				throw new IllegalArgumentException("No rename rule found.");
		}
	}
	
	/**
	 * Retrieve the remaining arguments as a string.
	 * @param args - the arguments.
	 * @return The remaining arguments.
	 */
	private String getRemainder(Deque<String> args) {
		return Joiner.on(" ").join(args);
	}
	
	private String setItemName(CommandSender sender, final Deque<String> args) {
		modifyCurrent(sender, args, true, new RenameProcessorFactory() {
			@Override
			public RenameFunction create() {
				final String name = getRemainder(args);
				// This is Java alright ...
				return new RenameFunction() {
					@Override
					public RenameRule apply(@Nullable RenameRule input) {
						return RenameRule.newBuilder(input).name(name).build();
					}
				};
			}
		});
		return String.format("Set the name of every item %s.", getRemainder(args));
	}
	
	private String addLore(CommandSender sender, final Deque<String> args) {
		// Apply the change
		modifyCurrent(sender, args, true, new RenameProcessorFactory() {
			@Override
			public RenameFunction create() {
				final String lore = getRemainder(args);
				
				return new RenameFunction() {
					@Override
					public RenameRule apply(@Nullable RenameRule input) {
						return RenameRule.newBuilder(input).mergeLoreSections(Arrays.asList(lore)).build();
					}
				};
			}
		});
		return String.format("Add the lore '%s' to every item.", getRemainder(args));
	}
	
	private String clearLore(CommandSender sender, Deque<String> args) {
		// Use the clear attribute utility class
		return clearAttribute(sender, args, new RenameOutputFactory() {
			@Override
			public RenameFunction create() {
				return new RenameFunction() {
					@Override
					public RenameRule apply(@Nullable RenameRule input) {
						output.append("Resetting lore for ").append(input);
						return RenameRule.newBuilder(input).loreSections(null).build();
					}
				};
			}
		});
	}
	
	// The powerful programming technique of COPY PASTE
	// I'm sorry.
	private String clearEnchantments(CommandSender sender, Deque<String> args) {
		return clearAttribute(sender, args, new RenameOutputFactory() {
			@Override
			public RenameFunction create() {
				return new RenameFunction() {
					@Override
					public RenameRule apply(@Nullable RenameRule input) {
						output.append("Resetting enchantments for ").append(input);
						return RenameRule.newBuilder(input).enchantments(null).build();
					}
				};
			}
		});
	}
	
	private String clearDechantment(CommandSender sender, Deque<String> args) {
		return clearAttribute(sender, args, new RenameOutputFactory() {
			@Override
			public RenameFunction create() {
				return new RenameFunction() {
					@Override
					public RenameRule apply(@Nullable RenameRule input) {
						output.append("Resetting dechantments for ").append(input);
						return RenameRule.newBuilder(input).dechantments(null).build();
					}
				};
			}
		});
	}
	
	/**
	 * Modify the current item with the given output factory.
	 * @param sender - the command sender.
	 * @param args - the arguments.
	 * @param factory - the custom output factory.
	 * @return The result string, supplied by the output factory.
	 */
	private String clearAttribute(CommandSender sender, Deque<String> args, RenameOutputFactory factory) {
		// Apply the change
		modifyCurrent(sender, args, true, factory);

		// Inform the user
		if (factory.getOutput().length() == 0)
			return "No items found.";
		else
			return factory.getOutput();
	}
	
	private String addEnchantment(CommandSender sender, final Deque<String> args) {
		// Apply the change
		modifyCurrent(sender, args, true, new RenameProcessorFactory() {
			@Override
			public RenameFunction create() {
				final LeveledEnchantment enchantment = LeveledEnchantment.parse(args);
				
				// Damn it
				if (enchantment == null)
					throw new IllegalArgumentException("Must specify an enchantment and level.");
				
				return new RenameFunction() {
					@Override
					public RenameRule apply(@Nullable RenameRule input) {
						return RenameRule.newBuilder(input).mergeEnchantments(Arrays.asList(enchantment)).build();
					}
				};
			}
		});
		return String.format("Added the enchantment '%s' to every item.", LeveledEnchantment.parse(args));
	}
	
	private String addDechantment(CommandSender sender, final Deque<String> args) {
		// Apply the change
		modifyCurrent(sender, args, true, new RenameProcessorFactory() {
			@Override
			public RenameFunction create() {
				final LeveledEnchantment enchantment = LeveledEnchantment.parse(args);
				
				// Damn it
				if (enchantment == null)
					throw new IllegalArgumentException("Must specify an enchantment and level.");
				
				return new RenameFunction() {
					@Override
					public RenameRule apply(@Nullable RenameRule input) {
						RenameRule result = RenameRule.newBuilder(input).mergeDechantments(Arrays.asList(enchantment)).build();
						
						System.out.println("From: " + input);
						System.out.println("To: " + result);
						return result;
					}
				};
			}
		});
		return String.format("Dechanted '%s' for every item.", LeveledEnchantment.parse(args));
	}
	
	/**
	 * Retrieve the damage lookup based on the item pack and item ID in the parameter stack.
	 * @param sender - the original command sender.
	 * @param args - the parameter stack.
	 * @return The corresponding damage lookup.
	 */
	private DamageLookup getLookup(CommandSender sender, Deque<String> args) {
		String pack = parsePack(sender, args);
		Integer itemID = getItemID(args);
		
		if (pack == null || pack.length() == 0)
			throw new IllegalArgumentException("Must specify an item pack.");
		return config.getRenameConfig().getLookup(pack, itemID);
	}
	
	private String parsePack(CommandSender sender, Deque<String> args) {
		String selected = selectedPack.get(sender);
		
		// Use the selected pack, or parse the input arguments
		if (selected != null) 
			return selected;
		else 
			return args.pollFirst();
	}
	
	private Integer getSelectedItemID(CommandSender sender, Deque<String> alternative) {
		ItemStack stack = selectedTracker.getSelected(sender);
		
		// Either use the selected stack, or look it up using the passed parameters
		return stack != null ? stack.getTypeId() : getItemID(alternative);
	}
	
	private DamageLookup createLookup(CommandSender sender, Deque<String> args) {
		String pack = parsePack(sender, args);
		Integer itemID = getSelectedItemID(sender, args);
		
		if (pack == null || pack.length() == 0)
			throw new IllegalArgumentException("Must specify an item pack.");
		return config.getRenameConfig().createLookup(pack, itemID);
	}
	
	private DamageValues getDamageValues(CommandSender sender, Deque<String> args) {
		try {
			ItemStack item = selectedTracker.getSelected(sender);
			
			if (item != null) {
				if (MaterialUtils.isArmorTool(item.getType()))
					return DamageValues.OTHER;
				else
					return new DamageValues(item.getDurability());
			} else {
				return DamageValues.parse(args);
			}
		} catch (IllegalArgumentException e) {
			// Wrap it in a more specific exception
			throw new CommandErrorException(e.getMessage(), e);
		}
	}
	
	private int getItemID(Deque<String> args) {
		try {
			List<Integer> result = ConfigParsers.getIntegers(args, 1, Ranges.closed(0, 4096));
			
			if (result.size() == 1) {
				return result.get(0);
			} else {
				throw new CommandErrorException("Cannot find item ID.");
			}
		} catch (IllegalArgumentException e) {
			throw new CommandErrorException(e.getMessage(), e);
		}
	}

	private String addWorldPack(CommandSender sender, Deque<String> args) {
		String pack = args.poll();
		
		if (config.getRenameConfig().createPack(pack))
			return "Created pack " + pack;
		else
			return "Pack " + pack + " already exists";
	}
	
	private String selectWorldPack(CommandSender sender, Deque<String> args) {
		String pack = args.poll();
		
		// Either add or remove the selection
		if (pack == null) {
			String previous = selectedPack.remove(sender);
			return previous != null ? "Deselected " + previous : "No pack selected.";
			
		} if (config.getRenameConfig().hasPack(pack)) {
			selectedPack.put(sender, pack);
			return "Selected pack " + pack;
			
		} else {
			return "Pack " + pack + " doesn't exist.";
		}
	}
	
	private String deleteWorldPack(Deque<String> args) {
		String pack = args.poll();
		
		config.getRenameConfig().removePack(pack);
		return "Deleted pack " + pack;
	}

	private String deselectWorldPack(CommandSender sender) {
		String removed = selectedPack.remove(sender);
		
		if (removed != null)
			return "Deselected pack " + removed;
		else
			return "No pack was selected.";
	}
	
	private String deselectItemStack(CommandSender sender) {
		ItemStack removed = selectedTracker.deselectCurrent(sender);
		
		if (removed != null)
			return "Deselected item " + removed;
		else
			return null;
	}

	private void expectCommandCount(Deque<String> args, int expected, String error) {
		if (expected != args.size())
			throw new CommandErrorException("Error: " + error);
	}
	
	private String getWorldPack(Deque<String> args) {
		String world = args.poll();
		
		// Retrieve world pack
		return "Item pack for " + world + ": " + config.getWorldPack(world);
	}

	/**
	 * Set the world pack we will use based on the input arguments.
	 * @param args - the input arguments.
	 * @return The message to return to the player.
	 */
	public String setWorldPack(Deque<String> args) {
		String world = checkWorld(args.poll()), pack = args.poll();
		
		config.setWorldPack(world, pack);
		return "Set the item pack in world " + world + " to " + pack;
	}
	
	/**
	 * Determine if the given world actually exists.
	 * @param world - the world to test.
	 * @return The name of the world.
	 * @throws CommandErrorException If the world doesn't exist.
	 */
	private String checkWorld(String world) {
		// Ensure the world exists
		if (plugin.getServer().getWorld(world) == null)
			throw new CommandErrorException("Cannot find world " + world);
		return world;
	}
	
	private String formatBoolean(String format, boolean value) {
		return String.format(format, value ? "enabled" : "disabled");
	}
	
	// Simple boolean parsing
	private boolean parseBoolean(String value) {
		if (Arrays.asList("true", "yes", "enabled", "on", "1").contains(value))
			return true;
		else if (Arrays.asList("false", "no", "disabled", "off", "0").contains(value)) {
			return false;
		} else {
			throw new CommandErrorException("Cannot parse " + value + " as a boolean (yes/no)");
		}
	}
	
	/**
	 * Represents a output factory that has a string output,
	 * @author Kristian
	 */
	private abstract static class RenameOutputFactory extends RenameProcessorFactory {
		protected StringBuilder output = new StringBuilder();
		
		/**
		 * Retrieve the final output.
		 * @return The final output.
		 */
		public String getOutput() {
			return output.toString();
		}
	}
}
