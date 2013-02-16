package org.shininet.bukkit.itemrenamer;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class PagedMessage {
	/**
	 * Number of lines per page.
	 */
	public static final int PAGE_LINE_COUNT = 9;
	
	// Paged message
	private Map<CommandSender, List<String>> pagedMessage = new WeakHashMap<CommandSender, List<String>>();
	
	public void printPage(CommandSender receiver, int pageIndex) {
		List<String> paged = pagedMessage.get(receiver);
		
		// Make sure the player has any pages
		if (paged != null) {
			int lastPage = ((paged.size() - 1) / PAGE_LINE_COUNT) + 1;
			
			for (int i = PAGE_LINE_COUNT * (pageIndex - 1); i < PAGE_LINE_COUNT * pageIndex; i++) {
				if (i < paged.size()) {
					receiver.sendMessage(" " + paged.get(i));
				}
			}
			
			// More data?
			if (pageIndex < lastPage) {
				receiver.sendMessage("Send /renamer page " + (pageIndex + 1) + " for the next page.");
			}
			
		} else {
			receiver.sendMessage(ChatColor.RED + "No pages found.");
		}
	}
	
	/**
	 * Send a message by splitting it up into pages.
	 * @param receiver - the reciever.
	 * @param message - the message to send.
	 */
	public void sendPaged(CommandSender receiver, String message) {
		List<String> messages = Lists.newArrayList(Splitter.on("\n").split(message));
		
		if (messages.size() > 0 && messages.size() > PAGE_LINE_COUNT) {
			// Divide the messages into chuncks
			pagedMessage.put(receiver, messages);
			printPage(receiver, 1);
			
		} else {
			// Just send it
			receiver.sendMessage(message);
		}
	}
}
