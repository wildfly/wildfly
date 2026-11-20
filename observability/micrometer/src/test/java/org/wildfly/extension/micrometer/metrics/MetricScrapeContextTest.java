/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import org.junit.Test;

/** Verifies the thread-local lifecycle of caller-scoped metric scrape contexts. */
public class MetricScrapeContextTest {

    /** Verifies nested contexts restore the outer context in reverse creation order. */
    @Test
    public void nestedContextsRestoreInLifoOrder() {
        Meter.Id outerMeter = meter("outer");
        Meter.Id innerMeter = meter("inner");

        try (MetricScrapeContext outer = MetricScrapeContext.begin()) {
            try (MetricScrapeContext inner = MetricScrapeContext.begin()) {
                MetricScrapeContext.deny(innerMeter);
                assertEquals(Set.of(innerMeter), inner.deniedMeters());
                assertTrue(outer.deniedMeters().isEmpty());
            }

            MetricScrapeContext.deny(outerMeter);
            assertEquals(Set.of(outerMeter), outer.deniedMeters());
        }
    }

    /** Verifies an out-of-order close fails without changing the active nested context. */
    @Test
    public void mismatchedCloseIsRejected() {
        MetricScrapeContext outer = MetricScrapeContext.begin();
        MetricScrapeContext inner = MetricScrapeContext.begin();
        Meter.Id innerMeter = meter("inner");

        try {
            outer.close();
            fail("Closing a non-current scrape context must fail");
        } catch (IllegalStateException expected) {
            // The active inner context must remain usable after the rejected close.
        }

        try {
            MetricScrapeContext.deny(innerMeter);
            assertEquals(Set.of(innerMeter), inner.deniedMeters());
        } finally {
            inner.close();
            outer.close();
        }
    }

    /** Verifies closing an already closed scrape context is a no-op. */
    @Test
    public void repeatedCloseIsIdempotent() {
        MetricScrapeContext context = MetricScrapeContext.begin();

        context.close();
        context.close();
    }

    /** Verifies exception cleanup leaves a reused worker thread free of the previous scrape context. */
    @Test
    public void exceptionCleanupPreventsThreadReuseLeak() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Meter.Id failedScrapeMeter = meter("failed");
            Future<MetricScrapeContext> failedScrape = executor.submit(() -> {
                MetricScrapeContext context = MetricScrapeContext.begin();
                try (context) {
                    MetricScrapeContext.deny(failedScrapeMeter);
                    throw new IllegalStateException("expected scrape failure");
                } catch (IllegalStateException expected) {
                    return context;
                }
            });
            MetricScrapeContext failedContext = failedScrape.get();
            assertEquals(Set.of(failedScrapeMeter), failedContext.deniedMeters());

            Meter.Id nextScrapeMeter = meter("next");
            Future<Set<Meter.Id>> nextScrape = executor.submit(() -> {
                try (MetricScrapeContext context = MetricScrapeContext.begin()) {
                    MetricScrapeContext.deny(nextScrapeMeter);
                    return context.deniedMeters();
                }
            });
            assertEquals(Set.of(nextScrapeMeter), nextScrape.get());

            Meter.Id leakedMeter = meter("leaked");
            Future<Set<Meter.Id>> afterCleanup = executor.submit(() -> {
                MetricScrapeContext.deny(leakedMeter);
                return failedContext.deniedMeters();
            });
            assertEquals(Set.of(failedScrapeMeter), afterCleanup.get());
        } finally {
            executor.shutdownNow();
        }
    }

    /** Creates a stable meter identity for context assertions. */
    private static Meter.Id meter(String name) {
        return new Meter.Id(name, Tags.empty(), null, null, Meter.Type.GAUGE);
    }
}
