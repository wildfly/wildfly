/*
 * Copyright 2023 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.observability.micrometer;

import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * @author <a href="mailto:jasondlee@redhat.com">Jason Lee</a>
 */
@RequestScoped
@Path("/")
public class MetricResource {
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
                Thread.sleep((long) (Math.random() * 1000L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            counter.increment();
        });

        return counter.count();
    }
}
