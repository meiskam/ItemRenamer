package org.shininet.bukkit.itemrenamer.meta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.bukkit.ChatColor;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

/**
 * Represents an encoder/decoder that can store arbitrary information as ChatColor symbols.
 * 
 * @author Kristian
 */
public class CharCodeStore {
	private static short MAGIC_CODE = 0x29F0;
	
	private static char SEGMENT_DELIMITER_CHAR = '§';
	private static String SEGMENT_DELIMITER = ChatColor.COLOR_CHAR + "" + SEGMENT_DELIMITER_CHAR;;
	
	private static int HEADER_SIZE = 8; // bytes
	
	/*
	 * Format:
	 *  Base 64-encoded byte array, interlaced with § so that its skipped by the Minecraft client. 
	 *
	 *  Segment header:
	 *   Bits    |   0     -     15   |   16     -     31      |   32     -     61   |   <Until delimiter>  |
	 *   Field   |    MAGIC CODE      |   Uncompressed size    |      Plugin ID      |  Compressed payload  |
	 *   Comment |  Always the same   |  Excluding header.     |                     |                      |
	 *   
	 *  Segment start and end delimiter:
	 *   §§
	 */
	
	private abstract class Segment {
		/**
		 * Insert the textual representation of this segment into the given output.
		 * @param previous - the previous element, or NULL if this is the start.
		 */
		public abstract void pipeTo(StringBuilder output, Segment previous);
		
		/**
		 * Whether or not this segment can delimit the next data segment.
		 * @return TRUE if it can, FALSE otherwise.
		 */
		public abstract boolean needDelimiter();
	}
	
	/**
	 * Represents a text segment.
	 * @author Kristian
	 */
	private class TextSegment extends Segment {
		private final String text;

		public TextSegment(String text) {
			this.text = text;
		}

		@Override
		public void pipeTo(StringBuilder output, Segment previous) {
			output.append(text);
		}

		@Override
		public boolean needDelimiter() {
			return text != null && text.endsWith(SEGMENT_DELIMITER);
		}
	}
	
	/**
	 * Represents a owned segment.
	 * @author Kristian
	 */
	public class DataSegment extends Segment {
		private final int pluginId;
		private byte[] data;
		
		/**
		 * Construct a new data segment.
		 * @param pluginId - the owner plugin ID.
		 * @param data - the data to store.
		 */
		private DataSegment(int pluginId, byte[] data) {
			this.pluginId = pluginId;
			this.data = data;
		}

		/**
		 * Retrieve the data stored in this segment.
		 * @return The data stored in the segment.
		 */
		public byte[] getBytes() {
			return data;
		}
		
		/**
		 * Set the data stored in this segment.
		 * @param data - the new data to be stored in the segment.
		 */
		public void setBytes(byte[] data) {
			this.data = data;
		}
		
		/**
		 * Retrieve the data stored in this segment as a string.
		 * @return The string data in this segment.
		 */
		public String getString() {
			return new String(data, Charsets.UTF_8);
		}
		
		/**
		 * Set the data stored in this segment as a string.
		 * @param data - the data to be stored.
		 */
		public void setString(String data) {
			Preconditions.checkNotNull(data, "data cannot be NULL.");
			this.data = data.getBytes(Charsets.UTF_8);
		}
		
		/**
		 * Retrieve the unique ID of the plugin that generated this segment.
		 * @return The unique plugin ID.
		 */
		public int getPluginId() {
			return pluginId;
		}

		@Override
		public boolean needDelimiter() {
			return true;
		}
		
		@Override
		public void pipeTo(StringBuilder output, Segment previous) {
			if (!(previous != null && previous.needDelimiter()))
				output.append(SEGMENT_DELIMITER);
			
			encode(output, pluginId, data);
			output.append(SEGMENT_DELIMITER);
		}
	}
	
	private final int pluginId;
	private List<Segment> segments = Lists.newArrayList();
	private Map<Integer, DataSegment> lookup = Maps.newHashMap();
		
	/**
	 * Construct a new encoder with the given plugin ID. 
	 * <p>
	 * Please go to <a href="http://www.random.org/cgi-bin/randbyte?nbytes=4&format=h ">Random.org</a> to get a new unique ID for your plugin.
	 * <p>
	 * Note that {@link #save()} is not implemented.
	 * @param pluginId - a unique ID identifying the owner plugin.
	 */
	public CharCodeStore(int pluginId) {
		this.pluginId = pluginId;
	}
		
	/**
	 * Parse the given text for segments.
	 * @param text - the text to parse.
	 * @return This store, for chaining.
	 */
	public CharCodeStore parse(String text) {
		segments = decode(text);
		
		for (DataSegment segment : getDataSegments()) {
			lookup.put(segment.getPluginId(), segment);
		}
		return this;
	}
	
	/**
	 * Clear the current store for data segments.
	 */
	public void clear() {
		for (Iterator<Segment> it = segments.iterator(); it.hasNext(); ) {
			if (it.next() instanceof DataSegment) {
				it.remove();
			}
		}
		lookup.clear();
	}
	
	/**
	 * Retrieve or create a data segment for the current plugin.
	 * @return The data segment.
	 */
	public DataSegment getData() {
		return getData(pluginId);
	}
	
	/**
	 * Retrieve or create a data segment for a plugin by the given ID.
	 * @param pluginId - the ID of the plugin.
	 * @return The data segment created or already associated with the given plugin.
	 */
	public DataSegment getData(int pluginId) {
		DataSegment segment = lookup.get(pluginId);
		
		if (segment == null) {
			segments.add(segment = new DataSegment(pluginId, new byte[0]));
			lookup.put(pluginId, segment);
		}
		return segment;
	}
	
	/**
	 * Remove the data associated with the given plugin.
	 * @param pluginId - ID of the plugin whose data will be removed.
	 * @return TRUE if the data was removed, FALSE otherwise.
	 */
	public boolean removeData(int pluginId) {
		DataSegment segment = lookup.remove(pluginId);
		
		if (segment != null) {
			segments.remove(segment);
			return true;
		}
		return false;
	}
	
	/**
	 * Determine if the current plugin has stored any data in the character store.
	 * @return TRUE if it has, FALSE otherwise.
	 */
	public boolean hasData() {
		return hasData(pluginId);
	}
	
	/**
	 * Determine if the given plugin has any associated data.
	 * @param pluginId - the plugin to look for.
	 * @return TRUE if it does, FALSE otherwise.
	 */
	public boolean hasData(int pluginId) {
		return lookup.containsKey(pluginId);
	}
	
	/**
	 * Retrieve an immutable collection of the ID of all the plugins in this store.
	 * @return Collection of all the IDs.
	 */
	public Collection<Integer> plugins() {
		return Collections.unmodifiableCollection(lookup.keySet());
	}
	
	/**
	 * Retrieve the unique ID of the plugin that controls this encoder.
	 * @return Unique plugin ID.
	 */
	public int getPluginId() {
		return pluginId;
	}

	/**
	 * Convert this store to the equivalent string.
	 */
	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		Segment last = null;
		
		// Build the output
		for (Segment segment : segments) {
			segment.pipeTo(output, last);
			last = segment;
		}
		return output.toString();
	}
	
	/**
	 * Save the store to the source.
	 * <p>
	 * Optional operation.
	 */
	public void save() {
		throw new UnsupportedOperationException("Call toString() instead.");
	}
	
	/**
	 * Retrieve every data segment in this store.
	 * @return Every data segment.
	 */
	protected Iterable<DataSegment> getDataSegments() {
		return Iterables.filter(segments, DataSegment.class);
	}
	
	/**
	 * Retrieve the output stream that is used to compress the payload.
	 * @param storage - the payload byte array as a stream.
	 * @return The output stream that will be used to compress the payload.
	 */
	protected OutputStream getPayloadOutputStream(OutputStream storage) {
		return new DeflaterOutputStream(storage);
	}
	
	/**
	 * Retrieve the input stream that is used to decompress the payload.
	 * @param storage - the payload byte array as a stream.
	 * @return The input stream that will be used to decompress the payload.
	 */
	protected InputStream getPayloadInputStream(InputStream input) {
		return new InflaterInputStream(input);
	}
	
	/***
	 * Retrieve the payload from a given input stream.
	 * @param uncompressedSize - the uncompressed size of the payload.
	 * @param input - the input stream.
	 * @return The payload itself.
	 * @throws IOException If anything went wrong.
	 */
	protected byte[] getPayload(int uncompressedSize, DataInputStream input) throws IOException {
		InputStream payload = getPayloadInputStream(input);
		byte[] result = new byte[uncompressedSize];
		
		ByteStreams.readFully(payload, result);
		return result;
	}
	
	/**
	 * Encode an array of bytes into a CharCoded text.
	 * @param prefix - a unique prefix to identify this chunk of data. Use NULL to skip.
	 * @param data - the data to encode.
	 */
	private void encode(StringBuilder result, int pluginId, byte[] data) {
		try {
			ByteArrayOutputStream storage = new ByteArrayOutputStream();
			OutputStream output = getPayloadOutputStream(storage);
			
			output.write(data);
			output.close();
			putEncodedData(result, pluginId, storage.toByteArray(), data.length);
		
		} catch (IOException e) {
			throw new RuntimeException("Unable to compress data.", e);
		}
	}
	
	/**
	 * Store a data segment into the given string builder.
	 * @param output - the output.
	 * @param pluginId - the unique ID.
	 * @param data - the compressed data.
	 * @param length - the uncompressed data lenght.
	 */
	private void putEncodedData(StringBuilder output, int pluginId, byte[] data, int length) {
		// Store the length of the byte array itself
		char[] base = Base64Coder.encode(Bytes.concat(
				Shorts.toByteArray(MAGIC_CODE),
				Shorts.toByteArray((short) length),
				Ints.toByteArray(pluginId),
				data
		));
		putEncoded(output, base);
	}

	/**
	 * Encode the given character array by interlacing it with the COLOR_CHAR symbol.
	 * @param output - the string output.
	 * @param characters - the characters to interlace.
	 */
	private void putEncoded(StringBuilder output, char[] characters) {
		for (char character : characters) {
			output.append(ChatColor.COLOR_CHAR);
			output.append(character);
		}
	}
	
	/**
	 * Extract and remove every character from the builder.
	 * @param input - the string builder.
	 * @return The characters that were removed from the builder.
	 */
	private char[] pullChars(StringBuilder input) {
		char[] result = input.toString().toCharArray();
		
		input.setLength(0);
		return result;
	}
	
	/**
	 * Decode every encoded byte arrays.
	 * @param data - the data as text.
	 * @param onlyOurs - if segments by other plugins should be excluded.
	 * @return Decoded blocks of data.
	 */
	private List<Segment> decode(String data) {
		List<Segment> output = Lists.newArrayList();
		StringBuilder dataCandidate = new StringBuilder();
		StringBuilder textCandidate = new StringBuilder();
		StringBuilder missingText = new StringBuilder();
		char[] characters = data.toCharArray();

		for (int i = 0; i < characters.length; i++) {
			if (characters[i] == ChatColor.COLOR_CHAR) {
				// This is probably caused by a substring() - assume we got the end anyways
				boolean missingData = (i >= characters.length - 1);
				char current = missingData ? SEGMENT_DELIMITER_CHAR : characters[++i];
				
				// Build segment until the end
				if (current == SEGMENT_DELIMITER_CHAR) {
					DataSegment decoded = decodeDataSegment(dataCandidate.toString());
					
					// Add it to the output
					if (decoded != null) {
						if (textCandidate.length() > 0) {
							output.add(new TextSegment(textCandidate.toString()));
							textCandidate.setLength(0);
						}
						
						dataCandidate.setLength(0);
						output.add(decoded);
						continue;
					} else {
						// Don't miss this character
						missingText.append(SEGMENT_DELIMITER_CHAR);
					}
					
				} else {
					dataCandidate.append(current);
					continue;
				}
			}
			
			// Unexpected end - must be a text segment then
			if (dataCandidate.length() > 0) 
				putEncoded(textCandidate, pullChars(dataCandidate));
			if (missingText.length() > 0)
				textCandidate.append(pullChars(missingText));
			textCandidate.append(characters[i]);
		}

		// Leftovers
		if (dataCandidate.length() > 0) 
			putEncoded(textCandidate, pullChars(dataCandidate));
		if (textCandidate.length() > 0)
			output.add(new TextSegment(textCandidate.toString()));
		return output;
	}

	/**
	 * Decode a single segment from a string. 
	 * <p>
	 * Segment delimiter must be excluded.
	 * @param data - the segment.
	 * @return The decoded segment.
	 */
	private DataSegment decodeDataSegment(String data) {
		// Correct missing padding
		while ((data.length() % 4) != 0) {
			data += "A";
		}
		
		byte[] match = Shorts.toByteArray(MAGIC_CODE);
		byte[] decoded = Base64Coder.decode(data);
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(decoded));
		
		// We can trust available in ByteArrayInputStreams
		try {
			// May have to skip a lot of crap
			while (input.available() >= HEADER_SIZE) {
				if (input.readByte() != match[0] || input.readByte() != match[1])
					continue;
				
				int uncompressed = input.readShort();
				int pluginId = input.readInt();
				byte[] payload = getPayload(uncompressed, input);
				
				input.close();
				return new DataSegment(pluginId, payload);
			}
			return null;
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException.", e);
		}
	}
}
