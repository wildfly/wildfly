/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.filters;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/")
public class MicrometerFilterResource {
    @Inject
    private MeterRegistry meterRegistry;
    private List<Counter> counters;
    private AtomicInteger gaugeValue;
    private Gauge gauge;
    private Timer timer;
    private LongTaskTimer longTaskTimer;
    private LongTaskTimer.Sample longTaskTimerSample;
    private DistributionSummary distributionSummary;

    @PostConstruct
    public void setupMeters() {
        counters = Arrays.asList(
                meterRegistry.counter("demo1"),
                meterRegistry.counter("demo2"),
                meterRegistry.counter("demo2-1"),
                meterRegistry.counter("demo3"),
                meterRegistry.counter("demo4"),
                meterRegistry.counter("demo5"),
                meterRegistry.counter("tagged.alpha", "env", "prod"),
                meterRegistry.counter("tagged.bravo", "env", "staging"),
                meterRegistry.counter("tagged.charlie", "priority", "high"),
                meterRegistry.counter("tagged.delta", "priority", "low"),
                meterRegistry.counter("negtest.keep", "env", "prod"),
                meterRegistry.counter("negtest.drop", "env", "dev"),
                meterRegistry.counter("tagged.echo", "env", "qa"),
                meterRegistry.counter("tagged.foxtrot", "env", "testing"),
                meterRegistry.counter("tagged.golf", "env", "stage-ok"),
                meterRegistry.counter("tagged.hotel", "env-special", "dev"),
                meterRegistry.counter("tagged.india", "blocked-start", "prod"),
                meterRegistry.counter("tagged.juliet", "name-blocked", "prod"),
                meterRegistry.counter("tagged.kilo", "middle-name", "prod"),
                meterRegistry.counter("tagged.lima", "safe", "bad-start"),
                meterRegistry.counter("tagged.mike", "safe", "end-bad"),
                meterRegistry.counter("tagged.november", "safe", "very-ugly"),
                meterRegistry.counter("type.counter")
        );
        gaugeValue = new AtomicInteger(1);
        gauge = Gauge.builder("type.gauge", gaugeValue, AtomicInteger::get).register(meterRegistry);
        timer = meterRegistry.timer("type.timer");
        longTaskTimer = meterRegistry.more().longTaskTimer("type.long-task-timer");
        longTaskTimerSample = longTaskTimer.start();
        distributionSummary = meterRegistry.summary("type.distribution-summary");
    }

    @GET
    @Path("/")
    public String sayHello() {
        counters.forEach(Counter::increment);
        timer.record(Duration.ofMillis(1));
        distributionSummary.record(1);
        return "hello";
    }
}
