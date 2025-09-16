package org.itsmanu.battistaAiSpigot.utils;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LimitsUtil {

    private static final Map<UUID, BukkitTask> pendingQuestions = new HashMap<>();

    private LimitsUtil() {
    }

    public static boolean hasPendingQuestions(Player player) {
        return pendingQuestions.containsKey(player.getUniqueId());
    }

    public static void addPendingQuestions(Player player, BukkitTask timeoutTask) {
        pendingQuestions.put(player.getUniqueId(), timeoutTask);
    }

    public static void removePendingQuestions(Player player) {
        var task = pendingQuestions.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }


}
