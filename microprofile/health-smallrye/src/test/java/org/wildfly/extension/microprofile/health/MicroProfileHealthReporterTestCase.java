/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.smallrye.health.SmallRyeHealth;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.Before;
import org.junit.Test;

public class MicroProfileHealthReporterTestCase {

    private static final ClassLoader CLASS_LOADER = MicroProfileHealthReporterTestCase.class.getClassLoader();

    private MicroProfileHealthReporter reporter;

    @Before
    public void setUp() {
        reporter = new MicroProfileHealthReporter("UP", "UP", "UP", false, "UP", "UP");
    }

    @Test
    public void testLivenessChecksModifiedDuringReport() {
        assertModificationSafe(reporter::addLivenessCheck, reporter::getLiveness);
    }

    @Test
    public void testReadinessChecksModifiedDuringReport() {
        assertModificationSafe(reporter::addReadinessCheck, reporter::getReadiness);
    }

    @Test
    public void testStartupChecksModifiedDuringReport() {
        assertModificationSafe(reporter::addStartupCheck, reporter::getStartup);
    }

    @Test
    public void testHealthChecksModifiedDuringReport() {
        assertModificationSafe(reporter::addHealthCheck, reporter::getHealth);
    }

    @Test
    public void testServerReadinessChecksModifiedDuringReport() {
        assertModificationSafe(reporter::addServerReadinessCheck, reporter::getReadiness);
    }

    private static void assertModificationSafe(BiConsumer<HealthCheck, ClassLoader> add,
            Supplier<SmallRyeHealth> report) {
        AtomicBoolean armed = new AtomicBoolean();
        Runnable mutateOnce = () -> {
            if (armed.compareAndSet(true, false)) {
                add.accept(() -> HealthCheckResponse.up("added"), CLASS_LOADER);
            }
        };

        add.accept(mutatingCheck("check-1", mutateOnce), CLASS_LOADER);
        add.accept(mutatingCheck("check-2", mutateOnce), CLASS_LOADER);

        armed.set(true);
        assertEquals("UP", report.get().getPayload().getString("status"));
        assertFalse("Expected checks to be modified during report", armed.get());
    }

    private static HealthCheck mutatingCheck(String name, Runnable mutate) {
        return new HealthCheck() {
            @Override
            public HealthCheckResponse call() {
                // Fires during direct map iteration (liveness, readiness, startup)
                mutate.run();
                return HealthCheckResponse.up(name);
            }

            @Override
            public int hashCode() {
                // Fires during Map.putAll() copying (health, serverReadiness)
                mutate.run();
                return super.hashCode();
            }
        };
    }
}
