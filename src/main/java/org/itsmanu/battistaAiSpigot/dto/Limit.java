package org.itsmanu.battistaAiSpigot.dto;

public abstract class Limit {
    public final SlidingWindow minuteWindow = new SlidingWindow(60_000);
    public final SlidingWindow hourWindow = new SlidingWindow(3_600_000);
    public final SlidingWindow dayWindow = new SlidingWindow(86_400_000);

    /**
     * Gets the maximum number of requests allowed per minute.
     *
     * @return the maximum number of requests allowed per minute
     */
    public abstract int getRequestPerMinute();

    /**
     * Gets the maximum number of requests allowed per hour.
     *
     * @return the maximum number of requests allowed per hour
     */
    public abstract int getRequestPerHour();

    /**
     * Gets the maximum number of requests allowed per day.
     *
     * @return the maximum number of requests allowed per day
     */
    public abstract int getRequestPerDay();

    /**
     * Checks if the rate limit has been exceeded for any of the time windows.
     *
     * @param now The current timestamp in milliseconds since epoch
     * @return true if the rate limit has been exceeded for any window, false otherwise
     */
    public boolean isRateLimitExceeded(long now) {
        if (minuteWindow.isRateLimitExceeded(now, getRequestPerMinute()) ||
                hourWindow.isRateLimitExceeded(now, getRequestPerHour()) ||
                dayWindow.isRateLimitExceeded(now, getRequestPerDay())) {
            return true;
        }

        // Record the request
        minuteWindow.record(now);
        hourWindow.record(now);
        dayWindow.record(now);
        return false;
    }
}
