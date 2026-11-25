package com.amali.blogPlatform.aspect;

import com.amali.blogPlatform.dto.response.MethodMetricsResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps simple statistics per service method (calls, errors, total/average/max time), filled by
 * {@link LoggingAspect} and shown at {@code GET /api/metrics/methods}.
 * <p>
 * Requests are handled by many threads at the same time, so the map is a {@link ConcurrentHashMap}
 * and each {@link Stats} object updates its numbers inside a {@code synchronized} method.
 */
@Component
public class PerformanceMonitor {

    private final Map<String, Stats> statsByMethod = new ConcurrentHashMap<>();

    /** Records one call of {@code method} that took {@code millis}; {@code failed} if it threw. */
    public void record(String method, double millis, boolean failed) {
        statsByMethod.computeIfAbsent(method, name -> new Stats()).add(millis, failed);
    }

    /** All methods, the ones that used the most time in total first. */
    public List<MethodMetricsResponse> snapshot() {
        List<MethodMetricsResponse> result = new ArrayList<>();
        statsByMethod.forEach((method, stats) -> result.add(stats.toResponse(method)));
        result.sort(Comparator.comparingDouble(MethodMetricsResponse::totalMillis).reversed());
        return result;
    }

    public void reset() {
        statsByMethod.clear();
    }

    /** The numbers of one method. */
    private static class Stats {
        private long calls;
        private long errors;
        private double totalMillis;
        private double maxMillis;

        synchronized void add(double millis, boolean failed) {
            calls++;
            if (failed) {
                errors++;
            }
            totalMillis += millis;
            maxMillis = Math.max(maxMillis, millis);
        }

        synchronized MethodMetricsResponse toResponse(String method) {
            double average = calls == 0 ? 0 : totalMillis / calls;
            return new MethodMetricsResponse(method, calls, errors, round(totalMillis), round(average), round(maxMillis));
        }

        private static double round(double value) {
            return Math.round(value * 100) / 100.0;     // 2 decimals
        }
    }
}
