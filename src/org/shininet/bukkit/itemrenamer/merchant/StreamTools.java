package org.shininet.bukkit.itemrenamer.merchant;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;

import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftReflection;

public class StreamTools {
	// Cached methods
	private static Method readItemMethod;
	private static Method writeItemMethod;
	
	public static ItemStack readItemStack(DataInputStream input) {
		if (readItemMethod == null)
			readItemMethod = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).
								getMethodByParameters("readPacket", 
										MinecraftReflection.getItemStackClass(), 
										new Class<?>[] {DataInputStream.class});
		try {
			Object nmsItem = readItemMethod.invoke(null, input);
			
			// Convert back to a Bukkit item stack
			return MinecraftReflection.getBukkitItemStack(nmsItem);
			
		} catch (Exception e) {
			throw new RuntimeException("Cannot read item stack.", e);
		}
	}
	
	public static void writeItemStack(DataOutputStream output, ItemStack stack) {
		Object nmsItem = MinecraftReflection.getMinecraftItemStack(stack);
		
		if (writeItemMethod == null)
			writeItemMethod = FuzzyReflection.fromClass(MinecraftReflection.getPacketClass()).
								getMethodByParameters("writePacket", new Class<?>[] { 
										MinecraftReflection.getItemStackClass(), 
										DataOutputStream.class });
		try {
			writeItemMethod.invoke(null, nmsItem, output);
		} catch (Exception e) {
			throw new RuntimeException("Cannot write item stack " + stack, e);
		}
	}
}
