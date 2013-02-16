package org.shininet.bukkit.itemrenamer;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class PagedMessage {
	/**
	 * Number of lines per page.
	 */
	public static final int PAGE_LINE_COUNT = 9;
	
	// Paged message
	private Map<CommandSender, List<String>> pagedMessage = new WeakHashMap<CommandSender, List<String>>();
	
	public String getPage(CommandSender receiver, int pageIndex) {
		List<String> paged = pagedMessage.get(receiver);
		
		// Make sure the player has any pages
		if (paged != null) {
			List<String> output = Lists.newArrayList();
			int lastPage = ((paged.size() - 1) / PAGE_LINE_COUNT) + 1;
			
			for (int i = PAGE_LINE_COUNT * (pageIndex - 1); i < PAGE_LINE_COUNT * pageIndex; i++) {
				if (i < paged.size()) {
					output.add(" " + paged.get(i));
				}
			}
			
			// More data?
			if (pageIndex < lastPage) {
				output.add("Send /renamer page " + (pageIndex + 1) + " for the next page.");
			}
			
			return Joiner.on("\n").join(output);
			
		} else {
			return ChatColor.RED + "No pages found.";
		}
	}
	
	/**
	 * Send a message by splitting it up into pages.
	 * @param receiver - the reciever.
	 * @param message - the message to send.
	 * @return The preliminary message to send to the player.
	 */
	public String createPage(CommandSender receiver, String message) {
		List<String> messages = Lists.newArrayList(Splitter.on("\n").split(message));
		
		if (messages.size() > 0 && messages.size() > PAGE_LINE_COUNT) {
			// Divide the messages into chuncks
			pagedMessage.put(receiver, messages);
			return getPage(receiver, 1);
			
		} else {
			// Just send it without any fuzz
			return message;
		}
	}
}
