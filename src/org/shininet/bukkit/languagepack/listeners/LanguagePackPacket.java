package org.shininet.bukkit.languagepack.listeners;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.shininet.bukkit.languagepack.LanguagePack;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;

public class LanguagePackPacket {

	private LanguagePack myPlugin;
	private ProtocolManager protocolManager;
	private final Logger logger;
	private PacketAdapter packetAdapter;

	public LanguagePackPacket(LanguagePack myPlugin, ProtocolManager protocolManager, Logger logger) {
		this.myPlugin = myPlugin;
		this.protocolManager = protocolManager;
		this.logger = logger;
		addListener();
	}
	
	public void addListener() {

		protocolManager.addPacketListener(packetAdapter = new PacketAdapter(myPlugin, ConnectionSide.SERVER_SIDE, 0x05, 0x00) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				
/*				try {
					switch (event.getPacketID()) {
					case 0x05:
						if (packet.getSpecificModifier(int.class).size() >= 2 && packet.getItemModifier().size() >= 1) {
							int id = packet.getSpecificModifier(int.class).read(0);
							int slot = packet.getSpecificModifier(int.class).read(1);
							if (slot == 4) {
								ItemStack item = packet.getItemModifier().read(0);
								if (item == null) {
									event.setCancelled(true);
									return;
								}
								for (Player player : myPlugin.getServer().getOnlinePlayers()) {
									//logger.info(player.getEntityId()+" == "+id+" ?");
									if (player.getEntityId() == id) {
										//ItemStack item = packet.getItemModifier().read(0);
										item.setTypeId(myPlugin.hat);
										item.setDurability(myPlugin.damage);
										item.setAmount(1);
										//item = new ItemStack(((FakeHat)plugin).hat, 1, ((FakeHat)plugin).damage);
										//logger.info("id: "+id+" .. slot: "+slot+" .. item: "+item.toString());
										break;
									}
								}
							}
						}
						break;
						
					case 0x00:
						break;
					}
				
				} catch (FieldAccessException e) {
					logger.log(Level.SEVERE, "Couldn't access field.", e);
				}
*/			}
		});
	}
	
	public void unregister() {
		protocolManager.removePacketListener(packetAdapter);
	}

}
