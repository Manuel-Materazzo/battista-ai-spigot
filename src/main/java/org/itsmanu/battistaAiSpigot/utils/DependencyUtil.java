package org.itsmanu.battistaAiSpigot.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getServer;

public class DependencyUtil {

    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    private DependencyUtil() {
    }

    public static boolean checkProtocolLib() {
        Plugin protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
        return protocolLib != null && protocolLib.isEnabled();
    }

    public static boolean isPacketTypeAvailable(PacketType packetType) {
        try {
            protocolManager.createPacket(packetType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
