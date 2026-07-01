/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.multiple.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RequestScoped
@Path("/" + DuplicateMetricResource2.TAG)
public class DuplicateMetricResource2 {
    public static final String TAG = "app2";
    public static final String METER_NAME = "ping_count";

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private MeterRegistry meterRegistry;
    private Counter counter;

    @PostConstruct
    public void setupMeters() {
        counter = meterRegistry.counter(METER_NAME, "app", TAG);
    }

    @GET
    public String ping() {
        counter.increment();
        return "ping";
    }
}
