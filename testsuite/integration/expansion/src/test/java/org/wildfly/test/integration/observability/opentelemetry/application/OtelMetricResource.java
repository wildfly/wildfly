/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry.application;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.jboss.logging.Logger;

@RequestScoped
@Path("/metrics")
public class OtelMetricResource {
    public static final String TEST_MESSAGE = "Test log message from application";
    public static final String COUNTER_NAME = "testCounter";
    @Inject
    private Meter sdkMeter;
    private LongCounter longCounter;

    @PostConstruct
    public void init() {
        longCounter = sdkMeter
                .counterBuilder(COUNTER_NAME)
                .setDescription("A test LongCounter")
                .build();
    }

    @GET
    public String sayHello(@QueryParam("name") String name) {
        Logger.getLogger(OtelService1.class).info(TEST_MESSAGE);

        longCounter.add(1);

        return "Hello, " + name;
    }
}
