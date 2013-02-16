package org.shininet.bukkit.itemrenamer.configuration;

import java.util.Deque;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;

public class ConfigParsers {

	/**
	 * Retrieve as many integers as possible from a given list.
	 * @param args - list of candidate integers.
	 * @param count - maximum number of values to consume.
	 * @param range 
	 * @return The list we're looking for.
	 */
	public static List<Integer> getIntegers(Deque<String> args, int count, Range<Integer> range) {
		List<Integer> values = Lists.newArrayList();
		
		try {
			while (!args.isEmpty() && values.size() < count) {
				Integer value = Integer.parseInt(args.peekFirst().trim());
				
				// Make sure its within the range
				if (range == null || range.contains(value)) {
					values.add(value);
					args.pollFirst(); // Consume it if we succeed
				} else {
					throw new IllegalArgumentException("The value " + value + " is outside the legal range of " + range);
				}
			}
			
		} catch (NumberFormatException e) {
			// Fine
		}
		return values;
	}
}
