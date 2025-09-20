package org.itsmanu.battistaAiSpigot.dto;

import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;

public class PlayerLimits extends Limit {
    private volatile long lastAccess;

    public int getRequestPerMinute() {
        return BattistaAiSpigot.getConfigs().getInt("limits.ai_requests_player.requests_per_minute", 3);
    }

    public int getRequestPerHour() {
        return BattistaAiSpigot.getConfigs().getInt("limits.ai_requests_player.requests_per_hour", 60);
    }

    public int getRequestPerDay() {
        return BattistaAiSpigot.getConfigs().getInt("limits.ai_requests_player.requests_per_day", 120);
    }

    /**
     * Checks if the rate limit has been exceeded for the player.
     * Updates the last access time to the current time.
     *
     * @param now The current timestamp in milliseconds
     * @return true if the rate limit has been exceeded, false otherwise
     */
    public boolean isRateLimitExceeded(long now) {
        lastAccess = now;
        return super.isRateLimitExceeded(now);
    }

    /**
     * Checks if the player's last rate limit access time has expired (more than 24 hours ago).
     *
     * @param now The current timestamp in milliseconds
     * @return true if the last rate limit access was more than 24 hours ago, false otherwise
     */
    public boolean isExpired(long now) {
        return now - lastAccess > 86_400_000; // 24 hours
    }

}
