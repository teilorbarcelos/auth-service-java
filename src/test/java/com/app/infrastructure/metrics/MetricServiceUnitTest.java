package com.app.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MetricServiceUnitTest {

    private MetricService metricService;
    private MeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = mock(MeterRegistry.class);
        metricService = new MetricService();
        metricService.registry = registry;
    }

    @Test
    @DisplayName("Should increment counter with tags")
    void testIncrementCounter() {
        Counter counter = mock(Counter.class);
        when(registry.counter(eq("test_counter"), anyList())).thenReturn(counter);

        metricService.incrementCounter("test_counter", "tag1", "val1");

        verify(counter).increment();
    }

    @Test
    @DisplayName("Should record timer with tags")
    void testRecordTimer() {
        Timer timer = mock(Timer.class);
        when(registry.timer(eq("test_timer"), anyList())).thenReturn(timer);

        metricService.recordTimer("test_timer", 100, "tag1", "val1");

        verify(timer).record(eq(100L), any());
    }

    @Test
    @DisplayName("Should throw exception for odd number of tags")
    void testInvalidTags() {
        assertThrows(IllegalArgumentException.class, () -> 
            metricService.incrementCounter("test", "tag1")
        );
    }
}
