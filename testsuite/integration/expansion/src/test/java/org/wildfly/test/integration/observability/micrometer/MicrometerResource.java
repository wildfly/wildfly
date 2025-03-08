/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * @author <a href="mailto:jasondlee@redhat.com">Jason Lee</a>
 */
@RequestScoped
@Path("/")
public class MicrometerResource {
    @Inject
    private MeterRegistry meterRegistry;
    private Counter counter;

    @PostConstruct
    public void setupMeters() {
        counter = meterRegistry.counter("demo_counter");
    }

    @GET
    @Path("/")
    public double getCount() {
        Timer timer = meterRegistry.timer("demo_timer", Tags.of("ts", "" + System.currentTimeMillis()));

        timer.record(() -> {
            try {
                Thread.sleep((long) (Math.random() * 100L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            counter.increment();
        });

        return counter.count();
    }
}
