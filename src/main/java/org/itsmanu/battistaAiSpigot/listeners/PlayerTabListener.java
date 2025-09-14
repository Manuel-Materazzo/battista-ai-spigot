package org.itsmanu.battistaAiSpigot.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.utils.TabUtil;

public class PlayerTabListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Add AI helper when player joins
        Bukkit.getScheduler().runTaskLater(BattistaAiSpigot.getInstance(), () -> {
            TabUtil.addAIHelper(event.getPlayer());
        }, 20L); // 1 second delay
    }

}
