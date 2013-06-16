package org.shininet.bukkit.itemrenamer.utils;

import java.util.Collection;
import java.util.Map;

public class CollectionsUtil {
	/**
	 * Determine if a given collection is empty or null.
	 * @param collection - the collection to test.
	 * @return TRUE if the collection is either empty or null, FALSE otherwise.
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	/**
	 * Determine if a given map is empty or null.
	 * @param map - the map to test.
	 * @return TRUE if the map is either empty or null, FALSE otherwise.
	 */
	public static boolean isEmpty(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}
	
	/**
	 * Determine if the two collections contain exactly the same elements.
	 * <p>
	 * Order doesn't matter if the first collection is a set.
	 * @param first - the first collection, or NULL.
	 * @param second - the second collection, or NULL.
	 * @return TRUE if they are equal, FALSE otherwise.
	 */
	public static <T> boolean equalsMany(Collection<T> first, Collection<T> second) {
		if (CollectionsUtil.isEmpty(first) ^ CollectionsUtil.isEmpty(second))
			return false;
		return first.containsAll(second);
	}
}
