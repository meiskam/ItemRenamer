package org.shininet.bukkit.utils;

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
}
