package org.itsmanu.battistaAiSpigot.utils;

import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getServer;

public class DependencyUtil {


    private DependencyUtil() {
    }

    public static boolean checkProtocolLib() {
        Plugin protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
        return protocolLib != null && protocolLib.isEnabled();
    }
}
