package org.itsmanu.battistaAiSpigot.dto;

import java.util.concurrent.atomic.AtomicInteger;

public class SlidingWindow {
    private final long windowSize;
    public final AtomicInteger count = new AtomicInteger(0);
    private volatile long windowStart;

    public SlidingWindow(long windowSize) {
        this.windowSize = windowSize;
    }

    public synchronized boolean isRateLimitExceeded(long now, int limit) {
        cleanExpired(now);
        return count.get() >= limit;
    }

    public synchronized void record(long now) {
        cleanExpired(now);
        count.incrementAndGet();
    }

    public void cleanExpired(long now) {
        if (now - windowStart >= windowSize) {
            count.set(0);
            windowStart = now;
        }
    }
}
