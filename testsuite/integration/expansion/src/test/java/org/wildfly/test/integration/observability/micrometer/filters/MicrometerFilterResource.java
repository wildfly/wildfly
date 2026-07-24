/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.filters;

import java.util.Arrays;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
                meterRegistry.counter("negtest.drop", "env", "dev")
        );
    }

    @GET
    @Path("/")
    public String sayHello() {
        counters.forEach(Counter::increment);
        return "hello";
    }
}
