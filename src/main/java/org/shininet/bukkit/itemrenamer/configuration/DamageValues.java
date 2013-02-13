package org.shininet.bukkit.itemrenamer.configuration;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

/**
 * Can represent a range of damage values, or one of the two special types.
 * 
 * @author Kristian
 */
public class DamageValues {
	public static final DamageValues ALL = new DamageValues((byte) -1);
	public static final DamageValues OTHER = new DamageValues((byte) -2);

	private final List<Range<Integer>> ranges;
	
	/**
	 * Construct a special damage value that is not otherwise legal.
	 * @param key - special damage value.
	 */
	@SuppressWarnings("unchecked")
	private DamageValues(byte key) {
		this.ranges = Lists.newArrayList(Ranges.singleton((int) key));
	}
	
	public DamageValues(int value) {
		this(value, value);
	}

	public DamageValues(int minimumValue, int maximumValue) {
		validateValue(minimumValue, "minimumValue");
		validateValue(maximumValue, "maximumValue");
		
		this.ranges = singleton(Ranges.closed(minimumValue, maximumValue));
	}
	
	private List<Range<Integer>> singleton(Range<Integer> range) {
		List<Range<Integer>> result = Lists.newArrayList();
		
		result.add(range);
		return result;
	}
	
	private void validateValue(int value, String name) {
		if (value < 0)
			throw new IllegalArgumentException("Value " + name + " cannot be less than zero (" + value + ")");
		if (value > Short.MAX_VALUE)
			throw new IllegalArgumentException("Value " + name + " cannot be greater than SHORT.MAX_VALUE (" + value + ")");
	}

	public Iterable<Range<Integer>> getRanges() {
		return ranges;
	}
	
	public static DamageValues parse(Deque<String> arguments) {
		String clean = arguments.peekFirst().trim();
		
		if (clean.equalsIgnoreCase("ALL")) {
			return ALL;
		} else if (clean.equalsIgnoreCase("OTHER")) {
			return OTHER;
		} else {

		}
	}
}
