package it.homegym.security;

import java.util.concurrent.*;
import java.util.*;

public class RateLimiter {
    private final ConcurrentHashMap<String, Deque<Long>> map = new ConcurrentHashMap<>();
    private final long windowMs;
    private final int maxRequests;

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean allow(String key) {
        long now = System.currentTimeMillis();

        // normalizza la key per evitare case/whitespace duplicates
        if (key == null) key = "";
        key = key.trim().toLowerCase();

        Deque<Long> dq = map.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (dq) {
            // rimuovi voci vecchie (sliding window)
            while (!dq.isEmpty() && dq.peekFirst() < now - windowMs) {
                dq.pollFirst();
            }
            if (dq.size() >= maxRequests) {
                return false;
            }
            dq.addLast(now);
            return true;
        }
    }

    // opzionale: cleanup map per evitare growth indefinito
    public void cleanup() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Deque<Long>> e : map.entrySet()) {
            Deque<Long> dq = e.getValue();
            synchronized (dq) {
                while (!dq.isEmpty() && dq.peekFirst() < now - windowMs) dq.pollFirst();
                if (dq.isEmpty()) map.remove(e.getKey(), dq);
            }
        }
    }
}
