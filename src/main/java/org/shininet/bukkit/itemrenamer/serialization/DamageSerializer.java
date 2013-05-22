package org.shininet.bukkit.itemrenamer.serialization;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.shininet.bukkit.itemrenamer.configuration.DamageLookup;
import org.shininet.bukkit.itemrenamer.configuration.RenameRule;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

/**
 * Serialize and deserialize a damage lookup from a configuration file.
 * 
 * @author Kristian
 */
public class DamageSerializer {	
	private static final String DAMAGE_ALL = "all";
	private static final String DAMAGE_OTHER = "other";
	
	private static final String RANGE_DELIMITER = "-";
	
	private ConfigurationSection section;
	private RuleSerializer ruleSerializer;
	
	/**
	 * Initialize a new damage lookup from a configuration section.
	 * @param section - the configuration section.
	 */
	public DamageSerializer(ConfigurationSection section) {
		setSection(section);
	}

	public ConfigurationSection getSection() {
		return section;
	}
	
	private void setSection(ConfigurationSection section) {
		this.section = section;
		this.ruleSerializer = new RuleSerializer(section);
	}
	
	/**
	 * Deserialize the content of the configuration section to the given damage lookup.
	 * @param destination - the input or destination damage lookup.
	 */
	public void readLookup(DamageLookup destination) {
		int oldModCount = destination.getModificationCount();
		
		destination.setAllRule(ruleSerializer.readRule(DAMAGE_ALL));
		destination.setOtherRule(ruleSerializer.readRule(DAMAGE_OTHER));
		
		for (String key : section.getKeys(false)) {
			if (isNotSpecialKey(key)) {
				// Parse and save
				Range<Integer> range = parseRange(key);
				destination.setRule(range.lowerEndpoint(), range.upperEndpoint(), ruleSerializer.readRule(key));
			}
		}
		destination.setModificationCount(oldModCount);
	}

	/**
	 * Serialize (write out) the content of the given damage lookup.
	 * @param source - the damage lookup to write.
	 */
	public void writeLookup(DamageLookup source) {
		// Reset section
		ConfigurationSection parent = section.getParent();
		if (parent != null)
			setSection(parent.createSection(section.getName()));
		
		ruleSerializer.writeRule(DAMAGE_ALL, source.getAllRule());
		ruleSerializer.writeRule(DAMAGE_OTHER, source.getOtherRule());
		
		// Next, sort the ranges
		List<Entry<Range<Integer>, RenameRule>> entries = Lists.newArrayList(source.toLookup().entrySet());
		Collections.sort(entries, new Comparator<Entry<Range<Integer>, RenameRule>>() {
			@Override
			public int compare(Entry<Range<Integer>, RenameRule> a,
							   Entry<Range<Integer>, RenameRule> b) {
				Range<Integer> keyA = a.getKey();
				Range<Integer> keyB = b.getKey();
				
				return ComparisonChain.start().
					compare(keyA.lowerEndpoint(), keyB.lowerEndpoint()).
					compare(keyA.upperEndpoint(), keyB.upperEndpoint()).
				result();
			}
		});
		
		// Save all the rules
		for (Entry<Range<Integer>, RenameRule> rules : entries) {
			Range<Integer> range = rules.getKey();
			
			if (range.lowerEndpoint().equals(range.upperEndpoint())) {
				ruleSerializer.writeRule(range.lowerEndpoint().toString(), rules.getValue());
			} else {
				ruleSerializer.writeRule(range.lowerEndpoint() + "-" + range.upperEndpoint(), rules.getValue());
			}
		}
	}

	private boolean isNotSpecialKey(String key) {
		return !DAMAGE_ALL.equalsIgnoreCase(key) && !DAMAGE_OTHER.equalsIgnoreCase(key);
 	}
	
	/**
	 * Parse a single number or a range (num-num).
	 * @param text - the range to parse.
	 * @return The range.
	 * @throws IllegalArgumentException If this is not a valid range.
	 */
	private static Range<Integer> parseRange(String text) {
		String[] values = text.split(RANGE_DELIMITER, 2);
		
		try {
			// Parse the range
			if (values.length == 1) {
				return Ranges.singleton(Integer.parseInt(values[0].trim()));
			} else if (values.length == 2) {
				return Ranges.closed(
						Integer.parseInt(values[0].trim()), 
						Integer.parseInt(values[1].trim())
				);
			} else {
				throw new IllegalArgumentException("Cannot parse range: " + text);
			}
			
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Unable to parse range " + text);
		}
	}
}
