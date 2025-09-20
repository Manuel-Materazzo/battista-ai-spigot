package org.itsmanu.battistaAiSpigot.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;
import org.itsmanu.battistaAiSpigot.dto.GlobalLimits;
import org.itsmanu.battistaAiSpigot.dto.PlayerLimits;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LimitsUtil {

    private static final Map<UUID, BukkitTask> pendingInteractiveQuestions = new HashMap<>();
    private static final ConcurrentHashMap<UUID, PlayerLimits> playerLimits = new ConcurrentHashMap<>();
    private static final GlobalLimits globalLimits = new GlobalLimits();

    private static int cleanupTaskId = -1;

    private LimitsUtil() {
    }

    /**
     * Starts a cleanup task that runs every 5 minutes to remove expired player entries.
     * The task runs asynchronously and checks each player's limits to see if they've expired.
     * Expired entries are removed from the playerLimits map.
     */
    public static void startCleanupTask() {
        // Run every 5 minutes (6000 ticks) asynchronously
        cleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(BattistaAiSpigot.getInstance(), () -> {
            long now = System.currentTimeMillis();
            playerLimits.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        }, 6000L, 6000L).getTaskId();
    }

    /**
     * Stops the cleanup task and clears all player limits.
     * This method cancels the scheduled cleanup task and clears the player limits map.
     * After calling this method, no cleanup will occur and all player limits will be removed.
     */
    public static void stopCleanupTask() {
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }
        playerLimits.clear();
    }

    /**
     * Checks if a player has pending questions.
     *
     * @param player The player to check
     * @return true if the player has pending questions, false otherwise
     */
    public static boolean hasPendingQuestions(Player player) {
        return pendingInteractiveQuestions.containsKey(player.getUniqueId());
    }

    /**
     * Adds a pending question for a player with an associated timeout task.
     *
     * @param player The player to add the pending question for
     * @param timeoutTask The BukkitTask that will handle the timeout for this pending question
     */
    public static void addPendingQuestions(Player player, BukkitTask timeoutTask) {
        pendingInteractiveQuestions.put(player.getUniqueId(), timeoutTask);
    }

    /**
     * Removes a pending question for a player and cancels its associated timeout task.
     *
     * @param player The player whose pending question should be removed
     */
    public static void removePendingQuestions(Player player) {
        var task = pendingInteractiveQuestions.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Checks if the global rate limit has been exceeded.
     *
     * @return true if the global rate limit has been exceeded, false otherwise
     */
    public static boolean isGlobalRateLimitExceeded() {
        long now = System.currentTimeMillis();
        return globalLimits.isRateLimitExceeded(now);
    }

    /**
     * Checks if the rate limit has been exceeded for a specific player.
     *
     * @param playerId The UUID of the player to check
     * @return true if the rate limit has been exceeded for the player, false otherwise
     */
    public static boolean isPlayerRateLimitExceeded(UUID playerId) {
        long now = System.currentTimeMillis();
        PlayerLimits limits = playerLimits.computeIfAbsent(playerId, k -> new PlayerLimits());
        return limits.isRateLimitExceeded(now);
    }

}
