/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.health.SmallRyeHealth;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MicroProfileHealthReporterTestCase {

    private static final long TIMEOUT = 10;
    private static final ClassLoader CLASS_LOADER = MicroProfileHealthReporterTestCase.class.getClassLoader();

    private MicroProfileHealthReporter reporter;
    private ExecutorService executor;

    @Before
    public void setUp() {
        reporter = new MicroProfileHealthReporter("UP", "UP", "UP", false, "UP", "UP");
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void testLivenessChecksModifiedDuringReport() throws Exception {
        assertChecksCanBeModifiedDuringReport(reporter::addLivenessCheck, reporter::removeLivenessCheck,
                reporter::getLiveness);
    }

    @Test
    public void testReadinessChecksModifiedDuringReport() throws Exception {
        assertChecksCanBeModifiedDuringReport(reporter::addReadinessCheck, reporter::removeReadinessCheck,
                reporter::getReadiness);
    }

    @Test
    public void testStartupChecksModifiedDuringReport() throws Exception {
        assertChecksCanBeModifiedDuringReport(reporter::addStartupCheck, reporter::removeStartupCheck,
                reporter::getStartup);
    }

    private void assertChecksCanBeModifiedDuringReport(BiConsumer<HealthCheck, ClassLoader> add,
            Consumer<HealthCheck> remove,
            Supplier<SmallRyeHealth> report) throws Exception {
        final CountDownLatch iterating = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        final AtomicBoolean paused = new AtomicBoolean();

        add.accept(pausingCheck("check-1", paused, iterating, resume), CLASS_LOADER);
        add.accept(pausingCheck("check-2", paused, iterating, resume), CLASS_LOADER);

        final Future<SmallRyeHealth> health = executor.submit(report::get);
        assertTrue("The report did not start", iterating.await(TIMEOUT, TimeUnit.SECONDS));

        final HealthCheck added = () -> HealthCheckResponse.up("added-during-report");
        add.accept(added, CLASS_LOADER);
        if (remove != null) {
            remove.accept(added);
        }
        resume.countDown();

        assertEquals("UP", health.get(TIMEOUT, TimeUnit.SECONDS).getPayload().getString("status"));
    }

    private static HealthCheck pausingCheck(String name, AtomicBoolean paused, CountDownLatch iterating,
            CountDownLatch resume) {
        return () -> {
            if (paused.compareAndSet(false, true)) {
                iterating.countDown();
                try {
                    resume.await(TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return HealthCheckResponse.up(name);
        };
    }
}
