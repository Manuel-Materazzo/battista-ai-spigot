package org.itsmanu.battistaAiSpigot.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.itsmanu.battistaAiSpigot.utils.LimitsUtil;

public class PlayerInteractiveAskListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cancel any pending timeout tasks
        LimitsUtil.removePendingQuestions(event.getPlayer());
    }

}
