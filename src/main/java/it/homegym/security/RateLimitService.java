package it.homegym.security;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class RateLimitService {
    public static final RateLimiter ipLimiter;
    public static final RateLimiter emailLimiter;
    private static final ScheduledExecutorService scheduler;

    static {
        // leggi da env o usa valori di default
        int ipMax = parseIntOrDefault(System.getenv("FORGOT_IP_MAX_PER_HOUR"), 5);
        int emailMax = parseIntOrDefault(System.getenv("FORGOT_EMAIL_MAX_PER_HOUR"), 3);
        long windowMs = 60L * 60L * 1000L; // 1 ora
 
        ipLimiter = new RateLimiter(ipMax, windowMs);
        emailLimiter = new RateLimiter(emailMax, windowMs);
 
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ratelimit-cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ipLimiter.cleanup();
                emailLimiter.cleanup();
            } catch (Throwable ignored) {}
        }, 10, 10, TimeUnit.MINUTES);
    }
    
    public static void shutdown() {
        try {
            scheduler.shutdownNow();
        } catch (Throwable ignored) {}
    }

    private static int parseIntOrDefault(String v, int def) {
        try {
            return v != null ? Integer.parseInt(v) : def;
        } catch (Exception ex) { return def; }
    }
 
    private RateLimitService() {}
}
 