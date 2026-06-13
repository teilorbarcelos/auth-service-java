package com.app.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service to manage custom Prometheus metrics.
 */
@ApplicationScoped
public class MetricService {

    @Inject
    MeterRegistry registry;

    /**
     * Increments a counter by 1.
     */
    public void incrementCounter(String name, String... tags) {
        registry.counter(name, parseTags(tags)).increment();
    }

    /**
     * Records a duration in a timer.
     */
    public void recordTimer(String name, long durationMs, String... tags) {
        registry.timer(name, parseTags(tags)).record(durationMs, TimeUnit.MILLISECONDS);
    }

    private List<Tag> parseTags(String[] tags) {
        List<Tag> tagList = new ArrayList<>();
        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Tags must be provided as key-value pairs");
        }
        for (int i = 0; i < tags.length; i += 2) {
            tagList.add(Tag.of(tags[i], tags[i + 1]));
        }
        return tagList;
    }
}
