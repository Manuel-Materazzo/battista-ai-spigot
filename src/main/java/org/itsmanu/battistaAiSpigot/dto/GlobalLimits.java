package org.itsmanu.battistaAiSpigot.dto;

import org.itsmanu.battistaAiSpigot.BattistaAiSpigot;

public class GlobalLimits extends Limit {

    public int getRequestPerMinute() {
        return BattistaAiSpigot.getConfigs().getInt("limits.ai_requests_global.requests_per_minute", 30);
    }

    public int getRequestPerHour() {
        return BattistaAiSpigot.getConfigs().getInt("limits.ai_requests_global.requests_per_hour", 600);
    }

    public int getRequestPerDay() {
        return BattistaAiSpigot.getConfigs().getInt("limits.ai_requests_global.requests_per_day", 1200);
    }
}
