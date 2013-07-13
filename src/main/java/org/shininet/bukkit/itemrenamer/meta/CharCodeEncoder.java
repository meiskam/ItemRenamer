package org.shininet.bukkit.itemrenamer.meta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.bukkit.ChatColor;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

/**
 * Represents an encoder/decoder that can store arbitrary information as ChatColor symbols.
 * 
 * @author Kristian
 */
public class CharCodeEncoder {
	private static short MAGIC_CODE = 0x29F0;
	private static char SEGMENT_DELIMITER = '§';
	
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
	
	/**
	 * Represents a owned segment.
	 * @author Kristian
	 */
	public static class Segment {
		private final int pluginId;
		private final byte[] data;
				
		public Segment(int pluginId, byte[] data) {
			this.pluginId = pluginId;
			this.data = data;
		}

		/**
		 * Retrieve the data stored in this segment.
		 * @return The data stored in the segment.
		 */
		public byte[] getData() {
			return data;
		}
		
		/**
		 * Retrieve the unique ID of the plugin that generated this segment.
		 * @return The unique plugin ID.
		 */
		public int getPluginId() {
			return pluginId;
		}
	}
	
	private final int pluginId;
	
	/**
	 * Construct a new encoder with the given plugin ID.
	 * <p>
	 * Please go to <a href="http://www.random.org/cgi-bin/randbyte?nbytes=4&format=h ">Random.org</a> to get a new unique ID for your plugin.
	 * @param owner - a unique ID identifying the owner plugin.
	 */
	public CharCodeEncoder(int pluginId) {
		this.pluginId = pluginId;
	}
	
	/**
	 * Encode an array of bytes into a CharCoded text.
	 * @param prefix - a unique prefix to identify this chunk of data. Use NULL to skip.
	 * @param data - the data to encode.
	 * @return The encoded data.
	 */
	public String encode(byte[] data) {
		try {
			ByteArrayOutputStream storage = new ByteArrayOutputStream();
			DeflaterOutputStream output = new DeflaterOutputStream(storage);

			output.write(data);
			output.close();
			return getEncodedData(storage.toByteArray(), data.length);
		
		} catch (IOException e) {
			throw new RuntimeException("Unable to compress data.", e);
		}
	}
	
	/**
	 * Decode every encoded byte array belonging to our plugin.
	 * @param data - the data as text.
	 * @return Decoded blocks of data.
	 */
	public Segment[] decode(String data) {
		return decode(data, true);
	}
	
	/**
	 * Decode every encoded byte arrays.
	 * @param data - the data as text.
	 * @param onlyOurs - if segments by other plugins should be excluded.
	 * @return Decoded blocks of data.
	 */
	public Segment[] decode(String data, boolean onlyOurs) {
		List<Segment> output = Lists.newArrayList();
		StringBuilder candidate = new StringBuilder();
		char[] characters = data.toCharArray();
		
		// Decode each individual encoded segment
		for (int i = 0; i < characters.length - 1; i++) {
			if (characters[i] == ChatColor.COLOR_CHAR) {
				char current = characters[++i];
				
				// Build segment until the end
				if (current == SEGMENT_DELIMITER) {
					// Make sure it's a base encoded segment
					if ((candidate.length() % 4) == 0) {
						Segment decoded = decodeSegment(candidate.toString());
						
						// Add it to the output
						if (decoded != null && (!onlyOurs || decoded.getPluginId() == pluginId)) {
							output.add(decoded);
						}
					}
					candidate.setLength(0);
				} else {
					candidate.append(current);
				}
			} else {
				// No segments here
				if (candidate.length() > 0) {
					candidate.setLength(0);
				}
			}
		}
		return output.toArray(new Segment[0]);
	}

	/**
	 * Retrieve the unique ID of the plugin that controls this encoder.
	 * @return Unique plugin ID.
	 */
	public int getPluginId() {
		return pluginId;
	}
	
	/**
	 * Decode a single segment from a string. 
	 * <p>
	 * Segment delimiter must be excluded.
	 * @param data - the segment.
	 * @return The decoded segment.
	 */
	private Segment decodeSegment(String data) {
		byte[] match = Shorts.toByteArray(MAGIC_CODE);
		byte[] decoded = Base64Coder.decode(data.toString());
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(decoded));
		
		// We can trust available in ByteArrayInputStreams
		try {
			// May have to skip a lot of crap
			while (input.available() >= HEADER_SIZE) {
				if (input.readByte() != match[0] || input.readByte() != match[1])
					continue;
				
				byte[] result = new byte[input.readShort()];
				int pluginId = input.readInt();
				
				InflaterInputStream unzipped = new InflaterInputStream(input);
				ByteStreams.readFully(unzipped, result);
				
				return new Segment(pluginId, result);
			}
			return null;
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException.", e);
		}
	}
	
	private String getEncodedData(byte[] data, int length) {
		String delimiter = ChatColor.COLOR_CHAR + "" + SEGMENT_DELIMITER;
		StringBuilder result = new StringBuilder(delimiter);
		
		// Store the length of the byte array itself
		putEncodedData(result, Bytes.concat(
				Shorts.toByteArray(MAGIC_CODE),
				Shorts.toByteArray((short) length),
				Ints.toByteArray(pluginId),
				data
		));
		result.append(delimiter);
		return result.toString();
	}
	
	private void putEncodedData(StringBuilder result, byte[] data) {
		char[] base = Base64Coder.encode(data);

		for (char character : base) {
			result.append(ChatColor.COLOR_CHAR);
			result.append(character);
		}
	}
}
